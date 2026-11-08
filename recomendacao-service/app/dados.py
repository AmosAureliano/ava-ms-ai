import json
import logging

import httpx
import pymysql

from ava_commons.eureka import EurekaClient

from app import config

log = logging.getLogger("dados")


def _conexao():
    return pymysql.connect(**config.DB, autocommit=True, charset="utf8mb4")


def registrar_interacao(aluno_id: int, curso_id: int, peso: float, origem: str, evento_id: str | None) -> None:
    """INSERT IGNORE + UNIQUE(evento_id): o outbox reentrega eventos e cada um deve contar uma vez só."""
    with _conexao() as c, c.cursor() as cur:
        cur.execute("INSERT IGNORE INTO interacao (aluno_id, curso_id, peso, origem, evento_id) "
                    "VALUES (%s, %s, %s, %s, %s)", (aluno_id, curso_id, peso, origem, evento_id))


def id_do_evento(headers) -> str | None:
    for nome, valor in headers or ():
        if nome == "ava-evento-id":
            return valor.decode()
    return None


def carregar_interacoes() -> list[tuple[int, int, float]]:
    with _conexao() as c, c.cursor() as cur:
        cur.execute("SELECT aluno_id, curso_id, SUM(peso) FROM interacao GROUP BY aluno_id, curso_id")
        return [(int(a), int(cu), float(p)) for a, cu, p in cur.fetchall()]


def carregar_cursos(eureka: EurekaClient) -> list[dict]:
    base = eureka.url_de("CURSO-SERVICE")
    r = httpx.get(f"{base}/internal/cursos/catalogo", timeout=10)
    r.raise_for_status()
    return [{"id": c["id"], "titulo": c["titulo"], "descricao": c.get("descricao") or "",
             "categoria": c.get("categoria") or ""} for c in r.json()]


def interpretar_evento(topico: str, valor: bytes) -> tuple[int, int, float, str] | None:
    """Converte uma mensagem Kafka em (aluno_id, curso_id, peso, origem), ou None se não gera interação."""
    e = json.loads(valor)
    if topico == "matricula.criada":
        return e["alunoId"], e["cursoId"], config.PESO_MATRICULA, "MATRICULA"
    if topico == "aula.concluida":
        return e["alunoId"], e["cursoId"], config.PESO_AULA, "AULA"
    if topico == "entrega.avaliada" and e.get("nota") is not None and float(e["nota"]) >= config.NOTA_MINIMA:
        return e["alunoId"], e["cursoId"], config.PESO_NOTA_ALTA, "NOTA"
    return None
