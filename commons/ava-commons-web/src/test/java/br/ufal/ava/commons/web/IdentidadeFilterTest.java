package br.ufal.ava.commons.web;

import br.ufal.ava.commons.security.JwtService;
import br.ufal.ava.commons.security.Perfil;
import br.ufal.ava.commons.security.UsuarioAutenticado;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.context.request.ServletWebRequest;

import static org.junit.jupiter.api.Assertions.*;

class IdentidadeFilterTest {

    private final JwtService jwt = new JwtService("segredo-de-teste-com-mais-de-32-caracteres", 60_000);
    private final IdentidadeFilter filtro = new IdentidadeFilter(jwt, new ObjectMapper().findAndRegisterModules());
    private final UsuarioAutenticadoResolver resolver = new UsuarioAutenticadoResolver();

    private MockHttpServletRequest filtrar(MockHttpServletRequest req, MockHttpServletResponse resp) throws Exception {
        filtro.doFilter(req, resp, new MockFilterChain());
        return req;
    }

    private Object resolver(MockHttpServletRequest req) throws Exception {
        MethodParameter param = new MethodParameter(
                IdentidadeFilterTest.class.getDeclaredMethod("endpoint", UsuarioAutenticado.class), 0);
        return resolver.resolveArgument(param, null, new ServletWebRequest(req), null);
    }

    @SuppressWarnings("unused")
    private void endpoint(UsuarioAutenticado usuario) {
    }

    @Test
    void tokenValidoViraUsuarioAutenticado() throws Exception {
        var usuario = new UsuarioAutenticado(3L, "aluno@ava.br", Perfil.ALUNO);
        var req = new MockHttpServletRequest();
        req.addHeader("Authorization", "Bearer " + jwt.gerar(usuario));

        assertEquals(usuario, resolver(filtrar(req, new MockHttpServletResponse())));
    }

    @Test
    void cabecalhosDeIdentidadeForjadosSaoIgnorados() throws Exception {
        var req = new MockHttpServletRequest();
        req.addHeader("X-Usuario-Id", "1");
        req.addHeader("X-Usuario-Perfil", "ADMIN");

        var filtrada = filtrar(req, new MockHttpServletResponse());
        assertThrows(NaoAutenticadoException.class, () -> resolver(filtrada));
    }

    @Test
    void tokenInvalidoRecebe401SemChegarAoController() throws Exception {
        var req = new MockHttpServletRequest();
        req.addHeader("Authorization", "Bearer forjado");
        var resp = new MockHttpServletResponse();
        var cadeia = new MockFilterChain();

        filtro.doFilter(req, resp, cadeia);

        assertEquals(401, resp.getStatus());
        assertNull(cadeia.getRequest());
    }

    @Test
    void acessoExigePerfilPermitido() {
        var aluno = new UsuarioAutenticado(3L, "a@b.c", Perfil.ALUNO);
        assertDoesNotThrow(() -> Acesso.exigir(aluno, Perfil.ALUNO, Perfil.ADMIN));
        assertThrows(AcessoNegadoException.class, () -> Acesso.exigir(aluno, Perfil.PROFESSOR));
    }

    @Test
    void paginacaoNormalizaValoresDoCliente() {
        var p = Paginacao.de(-3, 10_000, 50, org.springframework.data.domain.Sort.unsorted());
        assertEquals(0, p.getPageNumber());
        assertEquals(50, p.getPageSize());
        assertEquals(1, Paginacao.de(0, 0, 50, org.springframework.data.domain.Sort.unsorted()).getPageSize());
    }
}
