"""Validação do JWT emitido pelo usuario-service, no mesmo modelo do IdentidadeFilter dos serviços Java:
a identidade vem de um token assinado, nunca de cabeçalhos que qualquer cliente poderia preencher."""
import os
from dataclasses import dataclass

import jwt
from fastapi import Header, HTTPException

# O jjwt escolhe o HMAC pelo tamanho da chave (32+ bytes: HS256, 48+: HS384, 64+: HS512).
_ALGORITMOS = ["HS256", "HS384", "HS512"]


@dataclass(frozen=True)
class Usuario:
    id: int
    perfil: str


def segredo() -> str:
    valor = os.environ.get("JWT_SECRET", "")
    if len(valor.encode()) < 32:
        raise RuntimeError("JWT_SECRET deve estar definido e ter pelo menos 32 caracteres")
    return valor


def validar_token(token: str) -> Usuario:
    try:
        dados = jwt.decode(token, segredo(), algorithms=_ALGORITMOS, options={"require": ["sub", "exp"]})
        return Usuario(id=int(dados["sub"]), perfil=str(dados["perfil"]))
    except (jwt.PyJWTError, KeyError, ValueError):
        raise HTTPException(401, "Token inválido ou expirado")


def usuario_autenticado(authorization: str | None = Header(default=None)) -> Usuario:
    """Dependência FastAPI: exige "Authorization: Bearer <token>" válido."""
    if not authorization or not authorization.startswith("Bearer "):
        raise HTTPException(401, "Usuário não autenticado")
    return validar_token(authorization.removeprefix("Bearer "))


def exigir_perfil(usuario: Usuario, *perfis: str) -> None:
    if usuario.perfil not in perfis:
        raise HTTPException(403, "Operação permitida apenas para: " + ", ".join(perfis))
