package br.ufal.ava.commons.web;

import org.springframework.data.domain.Page;

import java.util.List;
import java.util.function.Function;

/** Página de resultados em formato estável para o front-end (independe da serialização do Spring Data). */
public record PaginaResposta<T>(List<T> itens, int pagina, int tamanho, long total, int totalPaginas) {

    public static <E, T> PaginaResposta<T> de(Page<E> page, Function<E, T> conversor) {
        return new PaginaResposta<>(page.getContent().stream().map(conversor).toList(),
                page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages());
    }
}
