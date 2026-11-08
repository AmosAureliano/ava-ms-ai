import time

import jwt
import pytest
from fastapi import HTTPException

from ava_commons import eureka, seguranca

SEGREDO = "segredo-de-teste-com-mais-de-32-caracteres"


@pytest.fixture(autouse=True)
def _segredo(monkeypatch):
    monkeypatch.setenv("JWT_SECRET", SEGREDO)


def _token(segredo=SEGREDO, algoritmo="HS256", expira_em=60, **extra):
    dados = {"sub": "7", "perfil": "ALUNO", "exp": int(time.time()) + expira_em, **extra}
    return jwt.encode(dados, segredo, algorithm=algoritmo)


def test_token_valido_gera_usuario():
    assert seguranca.usuario_autenticado(f"Bearer {_token()}") == seguranca.Usuario(7, "ALUNO")


def test_aceita_hmac_escolhido_pelo_jjwt_para_chaves_longas(monkeypatch):
    longo = "x" * 64
    monkeypatch.setenv("JWT_SECRET", longo)
    assert seguranca.validar_token(_token(longo, "HS512")).id == 7


@pytest.mark.parametrize("cabecalho", [None, "", "Basic abc", "Bearer lixo"])
def test_sem_token_valido_responde_401(cabecalho):
    with pytest.raises(HTTPException) as erro:
        seguranca.usuario_autenticado(cabecalho)
    assert erro.value.status_code == 401


def test_rejeita_token_assinado_com_outra_chave_ou_expirado():
    for token in (_token(segredo=SEGREDO + "-outro"), _token(expira_em=-5)):
        with pytest.raises(HTTPException):
            seguranca.validar_token(token)


def test_rejeita_algoritmo_none():
    token = jwt.encode({"sub": "1", "perfil": "ADMIN", "exp": int(time.time()) + 60}, None, algorithm="none")
    with pytest.raises(HTTPException):
        seguranca.validar_token(token)


def test_segredo_curto_impede_a_validacao(monkeypatch):
    monkeypatch.setenv("JWT_SECRET", "curto")
    with pytest.raises(RuntimeError):
        seguranca.segredo()


def test_exigir_perfil():
    seguranca.exigir_perfil(seguranca.Usuario(1, "ADMIN"), "ADMIN")
    with pytest.raises(HTTPException) as erro:
        seguranca.exigir_perfil(seguranca.Usuario(1, "ALUNO"), "ADMIN")
    assert erro.value.status_code == 403


def test_eureka_reaproveita_a_consulta_dentro_do_cache(monkeypatch):
    chamadas = []

    class Resposta:
        def raise_for_status(self):
            pass

        def json(self):
            return {"application": {"instance": {"status": "UP", "ipAddr": "10.0.0.5", "port": {"$": 8082}}}}

    monkeypatch.setattr(eureka.httpx, "get", lambda url, **kw: chamadas.append(url) or Resposta())
    cliente = eureka.EurekaClient("http://eureka/eureka", "teste", 1)

    assert cliente.url_de("curso-service") == "http://10.0.0.5:8082"
    assert cliente.url_de("CURSO-SERVICE") == "http://10.0.0.5:8082"
    assert len(chamadas) == 1
