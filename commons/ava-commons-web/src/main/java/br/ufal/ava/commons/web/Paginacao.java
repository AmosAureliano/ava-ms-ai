package br.ufal.ava.commons.web;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

public final class Paginacao {

    private Paginacao() {
    }

    /** Normaliza valores vindos do cliente: página negativa vira 0 e o tamanho fica entre 1 e {@code maximo}. */
    public static PageRequest de(int pagina, int tamanho, int maximo, Sort ordem) {
        return PageRequest.of(Math.max(0, pagina), Math.clamp(tamanho, 1, maximo), ordem);
    }
}
