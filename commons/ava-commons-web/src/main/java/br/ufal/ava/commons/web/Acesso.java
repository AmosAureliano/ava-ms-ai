package br.ufal.ava.commons.web;

import br.ufal.ava.commons.security.Perfil;
import br.ufal.ava.commons.security.UsuarioAutenticado;

import java.util.Arrays;

public final class Acesso {

    private Acesso() {
    }

    /** Lança 403 se o perfil do usuário não estiver entre os permitidos. */
    public static void exigir(UsuarioAutenticado usuario, Perfil... permitidos) {
        if (Arrays.stream(permitidos).noneMatch(p -> p == usuario.perfil())) {
            throw new AcessoNegadoException("Operação permitida apenas para: " + Arrays.toString(permitidos));
        }
    }
}
