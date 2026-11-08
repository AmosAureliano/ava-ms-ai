"""Clientes dos outros microsserviços, localizados pelo Eureka (ferramentas dos agentes).
Usam os endpoints /internal/**, que o gateway não expõe."""
import json

import httpx
from ava_commons.eureka import EurekaClient


class Servicos:
    def __init__(self, eureka: EurekaClient):
        self.eureka = eureka

    def _get(self, app: str, caminho: str, **params) -> dict | list:
        r = httpx.get(self.eureka.url_de(app) + caminho, params=params, timeout=10)
        r.raise_for_status()
        return r.json()

    def buscar_trechos(self, curso_id: int, consulta: str, limite: int = 4) -> list[dict]:
        return self._get("CURSO-SERVICE", f"/internal/cursos/{curso_id}/trechos", q=consulta, limite=limite)

    def curso(self, curso_id: int) -> dict:
        return self._get("CURSO-SERVICE", f"/internal/cursos/{curso_id}/resumo")

    def aula(self, aula_id: int) -> dict:
        return self._get("CURSO-SERVICE", f"/internal/aulas/{aula_id}")

    def progresso(self, aluno_id: int, curso_id: int) -> dict:
        return self._get("MATRICULA-SERVICE", "/internal/matriculas/progresso", alunoId=aluno_id, cursoId=curso_id)


def como_texto(dado) -> str:
    return json.dumps(dado, ensure_ascii=False)
