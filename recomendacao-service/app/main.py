"""Consome eventos do Kafka, grava as interações em db_recomendacoes e retreina o modelo quando há dados novos."""
import asyncio
import logging
from contextlib import asynccontextmanager

from aiokafka import AIOKafkaConsumer
from ava_commons import seguranca
from ava_commons.erros import registrar_tratadores
from ava_commons.eureka import EurekaClient
from ava_commons.seguranca import Usuario, exigir_perfil, usuario_autenticado
from fastapi import Depends, FastAPI

from app import config, dados
from app.recomendador import RecomendadorHibrido

logging.basicConfig(level=logging.INFO, format="%(asctime)s %(levelname)s %(name)s: %(message)s")
log = logging.getLogger("recomendacao")

eureka = EurekaClient(config.EUREKA_URL, "recomendacao-service", config.PORTA, config.INSTANCE_HOST)
modelo = RecomendadorHibrido()
estado = {"sujo": True, "treinado": False}
TOPICOS = ("matricula.criada", "aula.concluida", "entrega.avaliada", "curso.publicado")


def retreinar() -> None:
    modelo.treinar(dados.carregar_cursos(eureka), dados.carregar_interacoes())
    estado.update(sujo=False, treinado=True)
    log.info("Modelo treinado: %d cursos, %d alunos", len(modelo.ids), len(modelo.matriz))


async def laco_retreino() -> None:
    while True:
        if estado["sujo"]:
            try:
                await asyncio.to_thread(retreinar)
            except Exception as e:                       # curso-service/MySQL ainda subindo
                log.warning("Retreino adiado: %s", e)
        await asyncio.sleep(config.INTERVALO_RETREINO_S)


async def consumir_eventos() -> None:
    while True:
        consumidor = AIOKafkaConsumer(*TOPICOS, bootstrap_servers=config.KAFKA_BOOTSTRAP_SERVERS,
                                      group_id="recomendacao-service", auto_offset_reset="earliest")
        try:
            await consumidor.start()
            log.info("Consumindo eventos do Kafka")
            async for msg in consumidor:
                try:
                    interacao = dados.interpretar_evento(msg.topic, msg.value)
                    if interacao:
                        await asyncio.to_thread(dados.registrar_interacao, *interacao,
                                                dados.id_do_evento(msg.headers))
                    estado["sujo"] = True
                except Exception as e:
                    log.error("Evento ignorado (%s): %s", msg.topic, e)
        except asyncio.CancelledError:
            raise
        except Exception as e:
            log.warning("Kafka indisponível (%s); nova tentativa em 5 s", e)
            await asyncio.sleep(5)
        finally:
            await consumidor.stop()


@asynccontextmanager
async def ciclo_de_vida(_: FastAPI):
    seguranca.segredo()
    eureka.iniciar()
    tarefas = [asyncio.create_task(laco_retreino()), asyncio.create_task(consumir_eventos())]
    yield
    for t in tarefas:
        t.cancel()
    await eureka.parar()


app = FastAPI(title="recomendacao-service", lifespan=ciclo_de_vida)
registrar_tratadores(app)


@app.get("/health")
def saude():
    return {"status": "UP", "modeloTreinado": estado["treinado"]}


@app.get("/api/recomendacoes")
def recomendar(limite: int = 5, usuario: Usuario = Depends(usuario_autenticado)):
    return modelo.recomendar(usuario.id, max(1, min(limite, 20)))


@app.post("/api/recomendacoes/retreinar")
def forcar_retreino(usuario: Usuario = Depends(usuario_autenticado)):
    exigir_perfil(usuario, "ADMIN")
    retreinar()
    return {"status": "modelo atualizado", "cursos": len(modelo.ids)}
