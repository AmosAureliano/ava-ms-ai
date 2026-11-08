package br.ufal.ava.gateway;

import br.ufal.ava.commons.security.JwtService;
import br.ufal.ava.commons.security.Perfil;
import br.ufal.ava.commons.security.UsuarioAutenticado;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class FiltrosGatewayTest {

    private final JwtService jwt = new JwtService("segredo-de-teste-com-mais-de-32-caracteres", 60_000);
    private final JwtAutenticacaoFilter autenticacao = new JwtAutenticacaoFilter(jwt);
    private final AtomicInteger encaminhadas = new AtomicInteger();
    private final GatewayFilterChain cadeia = ex -> {
        encaminhadas.incrementAndGet();
        return Mono.empty();
    };

    private HttpStatus status(JwtAutenticacaoFilter filtro, MockServerHttpRequest req) {
        ServerWebExchange ex = MockServerWebExchange.from(req);
        filtro.filter(ex, cadeia).block();
        return (HttpStatus) ex.getResponse().getStatusCode();
    }

    @Test
    void rotaPublicaPassaSemToken() {
        assertNull(status(autenticacao, MockServerHttpRequest.post("/api/auth/login").build()));
        assertEquals(1, encaminhadas.get());
    }

    @Test
    void rotaPublicaSoValeParaOMetodoPrevisto() {
        assertEquals(HttpStatus.UNAUTHORIZED, status(autenticacao, MockServerHttpRequest.delete("/api/cursos/publicos").build()));
    }

    @Test
    void rotaProtegidaSemTokenOuComTokenInvalidoRecebe401() {
        assertEquals(HttpStatus.UNAUTHORIZED, status(autenticacao, MockServerHttpRequest.get("/api/usuarios").build()));
        assertEquals(HttpStatus.UNAUTHORIZED, status(autenticacao, MockServerHttpRequest.get("/api/usuarios")
                .header("Authorization", "Bearer abc").build()));
        assertEquals(0, encaminhadas.get());
    }

    @Test
    void tokenValidoPassa() {
        String token = jwt.gerar(new UsuarioAutenticado(1L, "a@b.c", Perfil.ADMIN));
        assertNull(status(autenticacao, MockServerHttpRequest.get("/api/usuarios")
                .header("Authorization", "Bearer " + token).build()));
    }

    @Test
    void caminhoComTravessiaOuBarraCodificadaEhRejeitado() {
        for (String caminho : new String[]{"/api/auth/login/../../usuarios/1", "/api/cursos/publicos/%2e%2e/x",
                "/api/auth/login/..%2Fusuarios", "/api/cursos/publicos/%5C..%5Cx"}) {
            var req = MockServerHttpRequest.method(HttpMethod.GET, URI.create("http://gw" + caminho)).build();
            assertEquals(HttpStatus.BAD_REQUEST, status(autenticacao, req), caminho);
        }
        assertEquals(0, encaminhadas.get());
    }

    @Test
    void limiteBloqueiaExcessoEReabreNaJanelaSeguinte() {
        var relogio = new RelogioAjustavel();
        var limite = new LimiteRequisicoesFilter(relogio);
        var regra = LimiteRequisicoesFilter.REGRAS.getFirst();

        for (int i = 0; i < regra.maximo(); i++) {
            assertTrue(limite.permitir(regra, "ip1"));
        }
        assertFalse(limite.permitir(regra, "ip1"));
        assertTrue(limite.permitir(regra, "ip2"));

        relogio.avancar(regra.janela());
        assertTrue(limite.permitir(regra, "ip1"));
    }

    @Test
    void limiteResponde429ComRetryAfter() {
        var limite = new LimiteRequisicoesFilter();
        HttpStatus ultimo = null;
        for (int i = 0; i <= 10; i++) {
            ServerWebExchange ex = MockServerWebExchange.from(MockServerHttpRequest.post("/api/auth/login").build());
            limite.filter(ex, cadeia).block();
            ultimo = (HttpStatus) ex.getResponse().getStatusCode();
            if (ultimo == HttpStatus.TOO_MANY_REQUESTS) {
                assertNotNull(ex.getResponse().getHeaders().getFirst("Retry-After"));
            }
        }
        assertEquals(HttpStatus.TOO_MANY_REQUESTS, ultimo);
        assertEquals(10, encaminhadas.get());
    }

    private static final class RelogioAjustavel extends Clock {
        private Instant agora = Instant.parse("2026-11-08T12:00:00Z");

        void avancar(Duration d) {
            agora = agora.plus(d);
        }

        @Override
        public ZoneOffset getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(java.time.ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return agora;
        }
    }
}
