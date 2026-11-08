import pymysql

from app import config


def _conexao():
    return pymysql.connect(**config.DB, autocommit=True, charset="utf8mb4",
                           cursorclass=pymysql.cursors.DictCursor)


def conversa_do_aluno(aluno_id: int, curso_id: int) -> int:
    """Uma conversa por aluno e curso; cria se ainda não existir."""
    with _conexao() as c, c.cursor() as cur:
        cur.execute("SELECT id FROM conversa WHERE aluno_id=%s AND curso_id=%s", (aluno_id, curso_id))
        linha = cur.fetchone()
        if linha:
            return linha["id"]
        cur.execute("INSERT INTO conversa (aluno_id, curso_id) VALUES (%s, %s)", (aluno_id, curso_id))
        return cur.lastrowid


def salvar_mensagem(conversa_id: int, papel: str, texto: str) -> None:
    with _conexao() as c, c.cursor() as cur:
        cur.execute("INSERT INTO mensagem (conversa_id, papel, texto) VALUES (%s, %s, %s)",
                    (conversa_id, papel, texto))


def mensagens(conversa_id: int, limite: int = 200) -> list[dict]:
    with _conexao() as c, c.cursor() as cur:
        cur.execute("""SELECT papel, texto, criada_em FROM (
                         SELECT id, papel, texto, criada_em FROM mensagem WHERE conversa_id=%s
                         ORDER BY id DESC LIMIT %s) t ORDER BY id""", (conversa_id, limite))
        return [{"papel": m["papel"], "texto": m["texto"], "criadaEm": m["criada_em"].isoformat()}
                for m in cur.fetchall()]


def perguntas_nas_ultimas_24h(aluno_id: int) -> int:
    with _conexao() as c, c.cursor() as cur:
        cur.execute("""SELECT COUNT(*) AS n FROM mensagem m JOIN conversa c ON c.id = m.conversa_id
                       WHERE c.aluno_id=%s AND m.papel='ALUNO' AND m.criada_em > NOW() - INTERVAL 1 DAY""",
                    (aluno_id,))
        return cur.fetchone()["n"]
