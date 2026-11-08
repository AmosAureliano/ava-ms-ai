package br.ufal.ava.commons.web;

import br.ufal.ava.commons.security.JwtService;
import br.ufal.ava.commons.security.UsuarioAutenticado;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;

/**
 * Valida o JWT em cada microsserviço, sem depender de cabeçalhos preenchidos por terceiros:
 * mesmo que alguém alcance o serviço sem passar pelo gateway, a identidade só vem de um token assinado.
 * Requisições sem token seguem anônimas; os endpoints que exigem usuário recebem 401 do
 * {@link UsuarioAutenticadoResolver}.
 */
public class IdentidadeFilter extends OncePerRequestFilter {

    public static final String ATRIBUTO = UsuarioAutenticado.class.getName();
    private static final String PREFIXO = "Bearer ";

    private final JwtService jwt;
    private final ObjectMapper json;

    public IdentidadeFilter(JwtService jwt, ObjectMapper json) {
        this.jwt = jwt;
        this.json = json;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse resp, FilterChain chain)
            throws ServletException, IOException {
        String header = req.getHeader(HttpHeaders.AUTHORIZATION);
        if (header != null && header.startsWith(PREFIXO)) {
            try {
                req.setAttribute(ATRIBUTO, jwt.validar(header.substring(PREFIXO.length())));
            } catch (RuntimeException e) {
                rejeitar(resp);
                return;
            }
        }
        chain.doFilter(req, resp);
    }

    private void rejeitar(HttpServletResponse resp) throws IOException {
        HttpStatus s = HttpStatus.UNAUTHORIZED;
        resp.setStatus(s.value());
        resp.setContentType(MediaType.APPLICATION_JSON_VALUE);
        resp.setCharacterEncoding("UTF-8");
        json.writeValue(resp.getOutputStream(),
                new ApiErro(s.value(), s.getReasonPhrase(), "Token inválido ou expirado", Instant.now()));
    }
}
