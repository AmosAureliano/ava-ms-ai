import time

import jwt
from fastapi.testclient import TestClient

from app import dados, main

SEGREDO = "segredo-de-teste-com-mais-de-32-caracteres"


def _auth(perfil, usuario_id=7):
    token = jwt.encode({"sub": str(usuario_id), "perfil": perfil, "exp": int(time.time()) + 60}, SEGREDO, "HS256")
    return {"Authorization": f"Bearer {token}"}


def test_recomendacoes_usam_o_usuario_do_token(monkeypatch):
    monkeypatch.setenv("JWT_SECRET", SEGREDO)
    pedidos = []
    monkeypatch.setattr(main.modelo, "recomendar", lambda aluno, limite: pedidos.append(aluno) or [])
    api = TestClient(main.app)

    assert api.get("/api/recomendacoes", headers={"X-Usuario-Id": "1"}).status_code == 401
    assert api.get("/api/recomendacoes", headers=_auth("ALUNO", 42)).status_code == 200
    assert pedidos == [42]


def test_retreino_e_restrito_ao_administrador(monkeypatch):
    monkeypatch.setenv("JWT_SECRET", SEGREDO)
    monkeypatch.setattr(main, "retreinar", lambda: None)
    api = TestClient(main.app)

    assert api.post("/api/recomendacoes/retreinar", headers=_auth("ALUNO")).status_code == 403
    assert api.post("/api/recomendacoes/retreinar", headers=_auth("ADMIN")).status_code == 200


def test_id_do_evento_vem_do_cabecalho_do_outbox():
    assert dados.id_do_evento([("outro", b"x"), ("ava-evento-id", b"matricula-service:12")]) == "matricula-service:12"
    assert dados.id_do_evento(None) is None
