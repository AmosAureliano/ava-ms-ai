package br.ufal.ava.gateway;

import br.ufal.ava.commons.security.JwtService;
import br.ufal.ava.commons.security.UsuarioAutenticado;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.util.pattern.PathPattern;
import org.springframework.web.util.pattern.PathPatternParser;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Primeira barreira de autenticação: rejeita cedo requisições sem JWT válido fora das rotas públicas.
 * O token segue até o microsserviço, que o valida de novo (ava-commons-web) e é a fonte de verdade da identidade.
 */
@Component
public class JwtAutenticacaoFilter implements GlobalFilter, Ordered {

    public static final String ATRIBUTO_USUARIO = UsuarioAutenticado.class.getName();

    private record RotaPublica(HttpMethod metodo, PathPattern caminho) {
        boolean aceita(ServerHttpRequest req) {
            return metodo.equals(req.getMethod()) && caminho.matches(req.getPath().pathWithinApplication());
        }
    }

    private static final PathPatternParser PARSER = PathPatternParser.defaultInstance;
    private static final List<RotaPublica> PUBLICAS = List.of(
            new RotaPublica(HttpMethod.POST, PARSER.parse("/api/auth/login")),
            new RotaPublica(HttpMethod.POST, PARSER.parse("/api/auth/cadastro")),
            new RotaPublica(HttpMethod.GET, PARSER.parse("/api/cursos/publicos")),
            new RotaPublica(HttpMethod.GET, PARSER.parse("/api/cursos/categorias")));

    /** Segmentos "." / ".." e barras codificadas: o serviço de destino poderia normalizar para outra rota. */
    private static final Pattern CAMINHO_SUSPEITO = Pattern.compile("(^|/)\\.{1,2}(/|$)|%2e|%2f|%5c|\\\\|//");

    private final JwtService jwtService;

    public JwtAutenticacaoFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest req = exchange.getRequest();
        String bruto = req.getURI().getRawPath();
        if (bruto == null || CAMINHO_SUSPEITO.matcher(bruto.toLowerCase(Locale.ROOT)).find()) {
            return Respostas.erro(exchange, HttpStatus.BAD_REQUEST, "Caminho inválido");
        }
        if (HttpMethod.OPTIONS.equals(req.getMethod()) || PUBLICAS.stream().anyMatch(r -> r.aceita(req))) {
            return chain.filter(exchange);
        }

        String header = req.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (header == null || !header.startsWith("Bearer ")) {
            return Respostas.erro(exchange, HttpStatus.UNAUTHORIZED, "Token ausente");
        }
        try {
            exchange.getAttributes().put(ATRIBUTO_USUARIO, jwtService.validar(header.substring(7)));
        } catch (RuntimeException e) {
            return Respostas.erro(exchange, HttpStatus.UNAUTHORIZED, "Token inválido ou expirado");
        }
        return chain.filter(exchange);
    }

    @Override
    public int getOrder() {
        return -2; // antes do roteamento e do limite de requisições
    }
}
