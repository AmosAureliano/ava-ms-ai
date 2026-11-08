package br.ufal.ava.commons.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JwtServiceTest {

    private static final String SEGREDO = "segredo-de-teste-com-mais-de-32-caracteres";

    @Test
    void geraEValidaToken() {
        JwtService jwt = new JwtService(SEGREDO, 60_000);
        UsuarioAutenticado original = new UsuarioAutenticado(7L, "aluno@ava.ufal.br", Perfil.ALUNO);

        UsuarioAutenticado lido = jwt.validar(jwt.gerar(original));

        assertEquals(original, lido);
    }

    @Test
    void rejeitaTokenAssinadoComOutraChave() {
        JwtService a = new JwtService(SEGREDO, 60_000);
        JwtService b = new JwtService(SEGREDO + "-diferente", 60_000);
        String token = a.gerar(new UsuarioAutenticado(1L, "x@y.z", Perfil.ADMIN));

        assertThrows(Exception.class, () -> b.validar(token));
    }

    @Test
    void rejeitaSegredoCurto() {
        assertThrows(IllegalArgumentException.class, () -> new JwtService("curto", 1000));
    }
}
