"""Agente Tutor do AVA-MS.

O modelo de linguagem recebe ferramentas (tools) e decide quando chamá-las:
  - buscar_conteudo: trechos das aulas do curso (curso-service);
  - consultar_progresso: aulas já concluídas pelo aluno (matricula-service).
O laço termina quando o modelo responde sem pedir nova ferramenta.
Sem ANTHROPIC_API_KEY, responde em "modo sem IA" com os trechos mais relevantes.
"""
from __future__ import annotations

from typing import Callable

from app import config
from app.clientes import como_texto

INSTRUCOES = (
    "Você é o tutor de um Ambiente Virtual de Aprendizagem. Responda em português do Brasil, de forma "
    "clara e didática, com base APENAS nos trechos de aula retornados pela ferramenta buscar_conteudo; "
    "cite o título da aula de onde tirou a informação. Se não encontrar a resposta no conteúdo, diga isso "
    "e sugira a aula mais próxima. Não resolva atividades avaliativas pelo aluno: explique o conceito."
)

FERRAMENTAS = [
    {"name": "buscar_conteudo",
     "description": "Busca trechos relevantes nas aulas do curso atual. Use palavras-chave da dúvida.",
     "input_schema": {"type": "object",
                      "properties": {"consulta": {"type": "string", "description": "palavras-chave da busca"}},
                      "required": ["consulta"]}},
    {"name": "consultar_progresso",
     "description": "Retorna quantas e quais aulas o aluno já concluiu neste curso.",
     "input_schema": {"type": "object", "properties": {}}},
]


def executar_ferramenta(servicos, nome: str, entrada: dict, aluno_id: int, curso_id: int) -> str:
    try:
        if nome == "buscar_conteudo":
            return como_texto(servicos.buscar_trechos(curso_id, entrada.get("consulta", ""), 4))
        if nome == "consultar_progresso":
            return como_texto(servicos.progresso(aluno_id, curso_id))
        return "Ferramenta desconhecida"
    except Exception as e:                       # o modelo é informado e pode seguir sem a ferramenta
        return f"Erro ao executar {nome}: {e}"


def responder_sem_ia(servicos, pergunta: str, curso_id: int) -> str:
    trechos = servicos.buscar_trechos(curso_id, pergunta, 3)
    if not trechos:
        return ("(Tutor em modo sem IA) Não encontrei esse assunto nas aulas do curso. "
                "Tente usar outras palavras-chave.")
    partes = [f"• Aula \"{t['aulaTitulo']}\": {t['trecho']}" for t in trechos]
    return "(Tutor em modo sem IA) Trechos das aulas relacionados à sua pergunta:\n\n" + "\n\n".join(partes)


def perguntar(servicos, pergunta: str, aluno_id: int, curso_id: int, historico: list[dict],
              criar_cliente: Callable | None = None) -> str:
    if not config.ANTHROPIC_API_KEY and criar_cliente is None:
        return responder_sem_ia(servicos, pergunta, curso_id)

    if criar_cliente is None:
        import anthropic
        criar_cliente = lambda: anthropic.Anthropic(api_key=config.ANTHROPIC_API_KEY)  # noqa: E731
    llm = criar_cliente()

    mensagens = [{"role": "user" if m["papel"] == "ALUNO" else "assistant", "content": m["texto"]}
                 for m in historico[-config.MENSAGENS_DE_HISTORICO:]]
    mensagens.append({"role": "user", "content": pergunta})

    for _ in range(config.MAX_PASSOS_AGENTE):
        resp = llm.messages.create(model=config.MODELO, max_tokens=1024, system=INSTRUCOES,
                                   tools=FERRAMENTAS, messages=mensagens)
        if resp.stop_reason != "tool_use":
            return "".join(b.text for b in resp.content if b.type == "text").strip()
        mensagens.append({"role": "assistant", "content": resp.content})
        mensagens.append({"role": "user", "content": [
            {"type": "tool_result", "tool_use_id": b.id,
             "content": executar_ferramenta(servicos, b.name, b.input, aluno_id, curso_id)}
            for b in resp.content if b.type == "tool_use"]})
    return "Não consegui concluir a resposta. Tente reformular a pergunta."
