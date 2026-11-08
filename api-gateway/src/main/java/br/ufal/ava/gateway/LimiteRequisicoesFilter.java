package br.ufal.ava.gateway;

import br.ufal.ava.commons.security.UsuarioAutenticado;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.InetSocketAddress;
import java.time.Clock;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Limite de requisições em janela fixa, em memória, para as rotas sensíveis a força bruta e a custo
 * (login, cadastro e agentes de IA). A chave é o usuário autenticado ou, sem token, o IP de origem.
 * Com mais de uma instância do gateway o limite passa a valer por instância; nesse cenário o
 * RequestRateLimiter do Spring Cloud Gateway com Redis é o substituto natural.
 */
@Component
public class LimiteRequisicoesFilter implements GlobalFilter, Ordered {

    record Regra(String nome, HttpMethod metodo, String caminho, int maximo, Duration janela) {
        boolean aplica(ServerHttpRequest req) {
            return metodo.equals(req.getMethod()) && caminho.equals(req.getPath().value());
        }
    }

    static final List<Regra> REGRAS = List.of(
            new Regra("login", HttpMethod.POST, "/api/auth/login", 10, Duration.ofMinutes(1)),
            new Regra("cadastro", HttpMethod.POST, "/api/auth/cadastro", 5, Duration.ofMinutes(10)),
            new Regra("tutor", HttpMethod.POST, "/api/tutor/perguntar", 10, Duration.ofMinutes(1)),
            new Regra("gerador", HttpMethod.POST, "/api/tutor/gerar-questoes", 5, Duration.ofMinutes(1)));

    private static final int MAX_CHAVES = 50_000;

    private record Janela(long inicio, int contagem) {
    }

    private final Map<String, Janela> janelas = new ConcurrentHashMap<>();
    private final Clock relogio;

    public LimiteRequisicoesFilter() {
        this(Clock.systemUTC());
    }

    LimiteRequisicoesFilter(Clock relogio) {
        this.relogio = relogio;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest req = exchange.getRequest();
        for (Regra regra : REGRAS) {
            if (regra.aplica(req) && !permitir(regra, chave(exchange))) {
                exchange.getResponse().getHeaders().set("Retry-After", String.valueOf(regra.janela().toSeconds()));
                return Respostas.erro(exchange, HttpStatus.TOO_MANY_REQUESTS,
                        "Muitas requisições. Tente novamente em instantes.");
            }
        }
        return chain.filter(exchange);
    }

    boolean permitir(Regra regra, String chave) {
        long agora = relogio.millis();
        long janelaMs = regra.janela().toMillis();
        if (janelas.size() > MAX_CHAVES) {
            janelas.values().removeIf(j -> agora - j.inicio() >= janelaMs);
        }
        Janela j = janelas.compute(regra.nome() + "|" + chave, (k, atual) ->
                atual == null || agora - atual.inicio() >= janelaMs
                        ? new Janela(agora, 1)
                        : new Janela(atual.inicio(), atual.contagem() + 1));
        return j.contagem() <= regra.maximo();
    }

    private static String chave(ServerWebExchange exchange) {
        UsuarioAutenticado u = exchange.getAttribute(JwtAutenticacaoFilter.ATRIBUTO_USUARIO);
        if (u != null) {
            return "u" + u.id();
        }
        InetSocketAddress origem = exchange.getRequest().getRemoteAddress();
        return origem == null ? "desconhecido" : "ip" + origem.getAddress().getHostAddress();
    }

    @Override
    public int getOrder() {
        return -1;
    }
}
