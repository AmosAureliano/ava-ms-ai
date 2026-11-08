package br.ufal.ava.curso.servico;

import br.ufal.ava.curso.api.Dtos.TrechoDTO;
import br.ufal.ava.curso.dominio.Aula;

import java.text.Normalizer;
import java.util.*;

/**
 * Busca lexical simples sobre o texto das aulas: divide o conteúdo em parágrafos e
 * pontua cada um pela quantidade de termos da consulta que contém.
 * É a ferramenta "buscar_conteudo" usada pelo Agente Tutor (agente-ia-service).
 */
public final class BuscaTrechos {

    private static final Set<String> STOPWORDS = Set.of(
            "que", "para", "com", "uma", "como", "dos", "das", "nos", "nas", "por", "mais", "isso", "esse",
            "essa", "qual", "quais", "sobre", "entre", "quando", "onde", "ser", "sao", "tem", "the", "and");
    private static final int TAMANHO_MAXIMO = 700;

    private BuscaTrechos() {
    }

    public static List<TrechoDTO> buscar(List<Aula> aulas, String consulta, int limite) {
        Set<String> termos = termos(consulta);
        if (termos.isEmpty()) {
            return List.of();
        }
        List<TrechoDTO> candidatos = new ArrayList<>();
        for (Aula aula : aulas) {
            String titulo = normalizar(aula.getTitulo());
            int bonusTitulo = (int) termos.stream().filter(titulo::contains).count();
            for (String paragrafo : paragrafos(aula.getConteudo())) {
                String p = normalizar(paragrafo);
                int pontos = (int) termos.stream().filter(p::contains).count();
                if (pontos > 0) {
                    candidatos.add(new TrechoDTO(aula.getId(), aula.getTitulo(), paragrafo, pontos * 2 + bonusTitulo));
                }
            }
        }
        candidatos.sort(Comparator.comparingInt(TrechoDTO::pontuacao).reversed());
        return candidatos.stream().limit(Math.max(1, limite)).toList();
    }

    static List<String> paragrafos(String texto) {
        if (texto == null || texto.isBlank()) {
            return List.of();
        }
        List<String> partes = new ArrayList<>();
        for (String bloco : texto.split("\\n\\s*\\n")) {
            String b = bloco.strip();
            while (b.length() > TAMANHO_MAXIMO) {          // quebra parágrafos longos em sentenças
                int corte = b.lastIndexOf(". ", TAMANHO_MAXIMO);
                corte = corte > 0 ? corte + 1 : TAMANHO_MAXIMO;
                partes.add(b.substring(0, corte).strip());
                b = b.substring(corte).strip();
            }
            if (!b.isEmpty()) {
                partes.add(b);
            }
        }
        return partes;
    }

    static Set<String> termos(String consulta) {
        Set<String> termos = new LinkedHashSet<>();
        for (String t : normalizar(consulta).split("[^a-z0-9]+")) {
            if (t.length() >= 3 && !STOPWORDS.contains(t)) {
                // radical simples: "normalizacao" e "normalizar" passam a casar pelo prefixo
                termos.add(t.length() > 6 ? t.substring(0, t.length() - 2) : t);
            }
        }
        return termos;
    }

    static String normalizar(String s) {
        if (s == null) {
            return "";
        }
        return Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT);
    }
}
