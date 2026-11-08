package br.ufal.ava.commons.events;

/** Tópicos Kafka usados pelo AVA-MS (também consumidos pelo recomendacao-service em Python). */
public final class Topicos {
    public static final String MATRICULA_CRIADA = "matricula.criada";
    public static final String AULA_CONCLUIDA = "aula.concluida";
    public static final String ENTREGA_AVALIADA = "entrega.avaliada";
    public static final String CURSO_PUBLICADO = "curso.publicado";

    private Topicos() {
    }
}
