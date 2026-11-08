import logging
from contextlib import asynccontextmanager

from ava_commons import seguranca
from ava_commons.erros import registrar_tratadores
from ava_commons.eureka import EurekaClient
from ava_commons.seguranca import Usuario, exigir_perfil, usuario_autenticado
from fastapi import Depends, FastAPI, HTTPException
from pydantic import BaseModel, Field

from app import config, db, gerador, tutor
from app.clientes import Servicos

logging.basicConfig(level=logging.INFO, format="%(asctime)s %(levelname)s %(name)s: %(message)s")
log = logging.getLogger("agente-ia")

eureka = EurekaClient(config.EUREKA_URL, "agente-ia-service", config.PORTA, config.INSTANCE_HOST)
servicos = Servicos(eureka)


@asynccontextmanager
async def ciclo_de_vida(_: FastAPI):
    seguranca.segredo()
    eureka.iniciar()
    log.info("Tutor %s", "com IA (" + config.MODELO + ")" if config.ANTHROPIC_API_KEY else "em modo sem IA")
    yield
    await eureka.parar()


app = FastAPI(title="agente-ia-service", lifespan=ciclo_de_vida)
registrar_tratadores(app)


class Pergunta(BaseModel):
    cursoId: int
    pergunta: str = Field(min_length=2, max_length=2000)


class PedidoQuestoes(BaseModel):
    aulaId: int
    quantidade: int = Field(default=3, ge=1, le=10)


def exigir_matricula(aluno: Usuario, curso_id: int) -> None:
    exigir_perfil(aluno, "ALUNO")
    if not servicos.progresso(aluno.id, curso_id).get("matriculado"):
        raise HTTPException(403, "Matricule-se no curso para usar o tutor")


@app.get("/health")
def saude():
    return {"status": "UP", "ia": bool(config.ANTHROPIC_API_KEY)}


@app.post("/api/tutor/perguntar")
def perguntar(req: Pergunta, usuario: Usuario = Depends(usuario_autenticado)):
    exigir_matricula(usuario, req.cursoId)
    if db.perguntas_nas_ultimas_24h(usuario.id) >= config.COTA_DIARIA_PERGUNTAS:
        raise HTTPException(429, f"Limite de {config.COTA_DIARIA_PERGUNTAS} perguntas por dia atingido")
    conversa = db.conversa_do_aluno(usuario.id, req.cursoId)
    historico = db.mensagens(conversa, config.MENSAGENS_DE_HISTORICO)
    try:
        resposta = tutor.perguntar(servicos, req.pergunta, usuario.id, req.cursoId, historico)
    except Exception:
        log.exception("Falha no tutor")
        raise HTTPException(503, "Tutor indisponível no momento, tente novamente em instantes")
    db.salvar_mensagem(conversa, "ALUNO", req.pergunta)
    db.salvar_mensagem(conversa, "TUTOR", resposta)
    return {"resposta": resposta, "modoIa": bool(config.ANTHROPIC_API_KEY)}


@app.get("/api/tutor/conversas/{curso_id}")
def conversa(curso_id: int, usuario: Usuario = Depends(usuario_autenticado)):
    exigir_perfil(usuario, "ALUNO")
    return db.mensagens(db.conversa_do_aluno(usuario.id, curso_id))


@app.post("/api/tutor/gerar-questoes")
def gerar_questoes(req: PedidoQuestoes, usuario: Usuario = Depends(usuario_autenticado)):
    exigir_perfil(usuario, "PROFESSOR")
    aula = servicos.aula(req.aulaId)
    curso = servicos.curso(aula["cursoId"])
    if curso["professorId"] != usuario.id:
        raise HTTPException(403, "A aula pertence a outro professor")
    try:
        return gerador.gerar(aula["titulo"], aula.get("conteudo") or "", req.quantidade)
    except gerador.GeracaoIndisponivel as e:
        raise HTTPException(503, str(e))
    except ValueError as e:
        raise HTTPException(422, str(e))
