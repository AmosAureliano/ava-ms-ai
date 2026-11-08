"""Erros no mesmo formato JSON (ApiErro) usado pelos serviços Java."""
import logging
from datetime import datetime, timezone
from http import HTTPStatus

from fastapi import FastAPI, HTTPException, Request
from fastapi.exceptions import RequestValidationError
from fastapi.responses import JSONResponse
from starlette.exceptions import HTTPException as StarletteHTTPException

log = logging.getLogger("erros")


def _corpo(status: int, mensagem: str) -> dict:
    return {"status": status, "erro": HTTPStatus(status).phrase, "mensagem": mensagem,
            "momento": datetime.now(timezone.utc).isoformat()}


def registrar_tratadores(app: FastAPI) -> None:
    @app.exception_handler(StarletteHTTPException)
    async def http_erro(_: Request, e: HTTPException):
        return JSONResponse(_corpo(e.status_code, str(e.detail)), status_code=e.status_code)

    @app.exception_handler(RequestValidationError)
    async def validacao(_: Request, e: RequestValidationError):
        campos = "; ".join(f"{'.'.join(map(str, err['loc'][1:]))}: {err['msg']}" for err in e.errors())
        return JSONResponse(_corpo(400, campos or "Requisição inválida"), status_code=400)

    @app.exception_handler(Exception)
    async def inesperado(_: Request, e: Exception):
        log.exception("Erro inesperado")
        return JSONResponse(_corpo(500, "Erro interno no serviço"), status_code=500)
