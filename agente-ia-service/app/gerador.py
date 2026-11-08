"""Agente Gerador de Questões: propõe questões de múltipla escolha a partir do texto de uma aula.
O professor revisa as sugestões antes de salvá-las no avaliacao-service."""
from __future__ import annotations

import json
import re
from typing import Callable

from app import config

INSTRUCOES = (
    "Você cria questões de múltipla escolha para um curso universitário, em português do Brasil. "
    "Use apenas o conteúdo da aula fornecida. Cada questão tem exatamente 4 alternativas e somente 1 correta; "
    "as erradas devem ser plausíveis. Responda SOMENTE com um array JSON, sem texto antes ou depois, no formato: "
    '[{"enunciado": "...", "alternativas": [{"texto": "...", "correta": true}, '
    '{"texto": "...", "correta": false}, {"texto": "...", "correta": false}, {"texto": "...", "correta": false}]}]'
)


class GeracaoIndisponivel(Exception):
    pass


def extrair_questoes(texto: str) -> list[dict]:
    """Lê o array JSON da resposta do modelo e descarta questões mal formadas."""
    achado = re.search(r"\[.*\]", texto, re.S)
    if not achado:
        raise ValueError("Resposta sem JSON")
    validas = []
    for q in json.loads(achado.group(0)):
        alts = q.get("alternativas") or []
        if q.get("enunciado") and len(alts) >= 2 and sum(1 for a in alts if a.get("correta")) == 1:
            validas.append({"enunciado": str(q["enunciado"]).strip(), "peso": 1,
                            "alternativas": [{"texto": str(a["texto"]).strip(), "correta": bool(a.get("correta"))}
                                             for a in alts]})
    return validas


def gerar(titulo: str, conteudo: str, quantidade: int, criar_cliente: Callable | None = None) -> list[dict]:
    if not conteudo or not conteudo.strip():
        raise ValueError("A aula não tem conteúdo em texto para gerar questões")
    if not config.ANTHROPIC_API_KEY and criar_cliente is None:
        raise GeracaoIndisponivel("Configure ANTHROPIC_API_KEY para gerar questões com IA")
    if criar_cliente is None:
        import anthropic
        criar_cliente = lambda: anthropic.Anthropic(api_key=config.ANTHROPIC_API_KEY)  # noqa: E731

    resp = criar_cliente().messages.create(
        model=config.MODELO, max_tokens=2048, system=INSTRUCOES,
        messages=[{"role": "user", "content": f"Aula: {titulo}\n\n{conteudo}\n\nGere {quantidade} questões."}])
    texto = "".join(b.text for b in resp.content if b.type == "text")
    return extrair_questoes(texto)[:quantidade]
