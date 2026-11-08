"""Recomendador híbrido de cursos do AVA-MS.

- Conteúdo: TF-IDF sobre título + descrição + categoria; similaridade do cosseno entre cursos.
- Colaborativo: similaridade item-item na matriz aluno x curso ("quem fez A também fez B").
- Escore final = PESO_CONTEUDO * conteudo + PESO_COLAB * colaborativo.
- Partida a frio: aluno sem histórico recebe os cursos mais populares.
"""
from __future__ import annotations

import numpy as np
import pandas as pd
from sklearn.feature_extraction.text import TfidfVectorizer
from sklearn.metrics.pairwise import cosine_similarity

PESO_CONTEUDO = 0.6
PESO_COLAB = 0.4

STOPWORDS_PT = [
    "a", "o", "as", "os", "de", "da", "do", "das", "dos", "e", "em", "no", "na", "nos", "nas", "um", "uma",
    "para", "por", "com", "que", "se", "ao", "aos", "como", "mais", "sobre", "entre", "seu", "sua",
]


class RecomendadorHibrido:

    def __init__(self) -> None:
        self.ids: list[int] = []
        self.info: dict[int, dict] = {}
        self.sim_conteudo = np.zeros((0, 0))
        self.sim_colab = np.zeros((0, 0))
        self.matriz = pd.DataFrame()
        self.populares: list[int] = []

    def treinar(self, cursos: list[dict], interacoes: list[tuple[int, int, float]]) -> None:
        """Monta as duas matrizes de similaridade curso x curso.

        cursos:     [{"id", "titulo", "descricao", "categoria"}, ...] (somente cursos publicados)
        interacoes: [(aluno_id, curso_id, peso), ...]
        """
        df = pd.DataFrame(cursos, columns=["id", "titulo", "descricao", "categoria"])
        self.ids = [int(i) for i in df["id"]]
        self.info = {int(r.id): {"titulo": r.titulo, "categoria": r.categoria} for r in df.itertuples()}
        n = len(self.ids)
        if n == 0:
            self.__init__()
            return

        # 1) Similaridade por conteúdo (texto dos cursos)
        textos = (df["titulo"].fillna("") + " " + df["descricao"].fillna("") + " " + df["categoria"].fillna(""))
        try:
            tfidf = TfidfVectorizer(max_features=5000, strip_accents="unicode",
                                    stop_words=STOPWORDS_PT).fit_transform(textos)
            self.sim_conteudo = cosine_similarity(tfidf)
        except ValueError:                       # vocabulário vazio
            self.sim_conteudo = np.eye(n)

        # 2) Similaridade colaborativa (quem fez A também fez B)
        inter = pd.DataFrame(interacoes, columns=["aluno_id", "curso_id", "peso"])
        inter = inter[inter["curso_id"].isin(self.ids)]
        self.matriz = (inter.pivot_table(index="aluno_id", columns="curso_id", values="peso",
                                         aggfunc="sum", fill_value=0)
                       .reindex(columns=self.ids, fill_value=0)) if len(inter) else pd.DataFrame(columns=self.ids)
        self.sim_colab = (cosine_similarity(self.matriz.T.values.astype(float))
                          if len(self.matriz) else np.zeros((n, n)))

        # 3) Popularidade (partida a frio); cursos sem interação entram no fim da fila
        soma = self.matriz.sum(axis=0) if len(self.matriz) else pd.Series(0, index=self.ids)
        self.populares = [int(c) for c in soma.sort_values(ascending=False, kind="stable").index]

    def recomendar(self, aluno_id: int, limite: int = 5) -> list[dict]:
        """Até `limite` cursos ainda não feitos pelo aluno, com escore e motivo."""
        if not self.ids:
            return []
        if aluno_id not in self.matriz.index or self.matriz.loc[aluno_id].sum() == 0:
            return self._populares(set(), limite)

        perfil = self.matriz.loc[aluno_id].values.astype(float)
        feitos = perfil > 0
        total = perfil[feitos].sum()
        # média ponderada da similaridade com os cursos que o aluno já fez
        conteudo = self.sim_conteudo[:, feitos] @ perfil[feitos] / total
        colab = self.sim_colab[:, feitos] @ perfil[feitos] / total
        escore = PESO_CONTEUDO * conteudo + PESO_COLAB * colab
        escore[feitos] = -1                                          # não recomenda o que já fez

        resultado = []
        for i in np.argsort(-escore, kind="stable"):
            if len(resultado) >= limite or escore[i] <= 0:
                break
            motivo = "Parecido com seus cursos" if conteudo[i] >= colab[i] else "Alunos como você também fizeram"
            resultado.append(self._item(self.ids[i], round(float(escore[i]), 3), motivo))

        ja = {r["cursoId"] for r in resultado} | {self.ids[i] for i in np.flatnonzero(feitos)}
        resultado += self._populares(ja, limite - len(resultado))
        return resultado

    def _populares(self, excluir: set[int], limite: int) -> list[dict]:
        if limite <= 0:
            return []
        return [self._item(c, None, "Popular no AVA") for c in self.populares if c not in excluir][:limite]

    def _item(self, curso_id: int, escore: float | None, motivo: str) -> dict:
        info = self.info.get(curso_id, {})
        return {"cursoId": curso_id, "titulo": info.get("titulo"), "categoria": info.get("categoria"),
                "escore": escore, "motivo": motivo}
