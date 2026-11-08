"""Testes dos agentes com um modelo falso (sem chamar a API real)."""
from types import SimpleNamespace as NS

from app import gerador, tutor


class ServicosFalsos:
    def __init__(self):
        self.chamadas = []

    def buscar_trechos(self, curso_id, consulta, limite=4):
        self.chamadas.append(("trechos", consulta))
        return [{"aulaId": 7, "aulaTitulo": "Normalização", "trecho": "A 3FN remove dependências transitivas."}]

    def progresso(self, aluno_id, curso_id):
        return {"matriculado": True, "aulasConcluidas": 1}


class LlmFalso:
    """1ª chamada pede a ferramenta buscar_conteudo; 2ª responde com texto."""

    def __init__(self):
        self.passo = 0
        self.messages = self

    def create(self, **kw):
        self.passo += 1
        if self.passo == 1:
            return NS(stop_reason="tool_use",
                      content=[NS(type="tool_use", id="t1", name="buscar_conteudo", input={"consulta": "3FN"})])
        ultimo = kw["messages"][-1]["content"][0]
        assert ultimo["type"] == "tool_result" and "transitivas" in ultimo["content"]
        return NS(stop_reason="end_turn", content=[NS(type="text", text="Segundo a aula Normalização, a 3FN...")])


def test_tutor_usa_ferramenta_e_responde():
    s = ServicosFalsos()
    r = tutor.perguntar(s, "O que é 3FN?", 1, 2, [], criar_cliente=LlmFalso)
    assert r.startswith("Segundo a aula Normalização")
    assert s.chamadas == [("trechos", "3FN")]


def test_tutor_sem_ia_devolve_trechos(monkeypatch):
    monkeypatch.setattr(tutor.config, "ANTHROPIC_API_KEY", "")
    r = tutor.perguntar(ServicosFalsos(), "O que é 3FN?", 1, 2, [])
    assert "modo sem IA" in r and "transitivas" in r


def test_extrair_questoes_descarta_invalidas():
    texto = """Aqui estão: [
      {"enunciado": "O que a 3FN remove?", "alternativas": [
        {"texto": "Dependências transitivas", "correta": true}, {"texto": "Chaves", "correta": false}]},
      {"enunciado": "Sem correta", "alternativas": [{"texto": "a", "correta": false}, {"texto": "b", "correta": false}]}
    ]"""
    qs = gerador.extrair_questoes(texto)
    assert len(qs) == 1 and qs[0]["alternativas"][0]["correta"] is True
