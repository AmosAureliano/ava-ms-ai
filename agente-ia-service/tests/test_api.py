import time

import jwt
import pytest
from fastapi.testclient import TestClient

from app import config, main

SEGREDO = "segredo-de-teste-com-mais-de-32-caracteres"


def _auth(perfil="ALUNO", usuario_id=7):
    token = jwt.encode({"sub": str(usuario_id), "perfil": perfil, "exp": int(time.time()) + 60}, SEGREDO, "HS256")
    return {"Authorization": f"Bearer {token}"}


@pytest.fixture
def api(monkeypatch):
    monkeypatch.setenv("JWT_SECRET", SEGREDO)
    monkeypatch.setattr(config, "ANTHROPIC_API_KEY", "")
    monkeypatch.setattr(main.db, "perguntas_nas_ultimas_24h", lambda aluno: 0)
    monkeypatch.setattr(main.db, "conversa_do_aluno", lambda aluno, curso: 1)
    monkeypatch.setattr(main.db, "mensagens", lambda conversa, limite=200: [])
    monkeypatch.setattr(main.db, "salvar_mensagem", lambda *a: None)
    monkeypatch.setattr(main.servicos, "progresso", lambda aluno, curso: {"matriculado": True})
    monkeypatch.setattr(main.servicos, "buscar_trechos", lambda *a: [])
    return TestClient(main.app)


PERGUNTA = {"cursoId": 1, "pergunta": "O que é 3FN?"}


def test_sem_token_responde_401(api):
    r = api.post("/api/tutor/perguntar", json=PERGUNTA)
    assert r.status_code == 401
    assert r.json()["mensagem"] == "Usuário não autenticado"


def test_cabecalho_forjado_nao_substitui_o_token(api):
    r = api.post("/api/tutor/perguntar", json=PERGUNTA, headers={"X-Usuario-Id": "7", "X-Usuario-Perfil": "ALUNO"})
    assert r.status_code == 401


def test_aluno_matriculado_recebe_resposta(api):
    r = api.post("/api/tutor/perguntar", json=PERGUNTA, headers=_auth())
    assert r.status_code == 200 and "modo sem IA" in r.json()["resposta"]


def test_aluno_nao_matriculado_e_bloqueado(api, monkeypatch):
    monkeypatch.setattr(main.servicos, "progresso", lambda aluno, curso: {"matriculado": False})
    assert api.post("/api/tutor/perguntar", json=PERGUNTA, headers=_auth()).status_code == 403


def test_professor_nao_usa_o_tutor(api):
    assert api.post("/api/tutor/perguntar", json=PERGUNTA, headers=_auth("PROFESSOR")).status_code == 403


def test_cota_diaria_esgotada_responde_429(api, monkeypatch):
    monkeypatch.setattr(main.db, "perguntas_nas_ultimas_24h", lambda aluno: config.COTA_DIARIA_PERGUNTAS)
    assert api.post("/api/tutor/perguntar", json=PERGUNTA, headers=_auth()).status_code == 429


def test_falha_do_tutor_nao_vaza_detalhes(api, monkeypatch):
    def falha(*a, **kw):
        raise RuntimeError("senha=segredo host=mysql")

    monkeypatch.setattr(main.tutor, "perguntar", falha)
    r = api.post("/api/tutor/perguntar", json=PERGUNTA, headers=_auth())
    assert r.status_code == 503 and "segredo" not in r.text


def test_gerador_exige_professor_dono_da_aula(api, monkeypatch):
    monkeypatch.setattr(main.servicos, "aula", lambda aula_id: {"cursoId": 1, "titulo": "Aula", "conteudo": "x"})
    monkeypatch.setattr(main.servicos, "curso", lambda curso_id: {"professorId": 2})
    r = api.post("/api/tutor/gerar-questoes", json={"aulaId": 1}, headers=_auth("PROFESSOR", usuario_id=99))
    assert r.status_code == 403
