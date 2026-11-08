package br.ufal.ava.commons.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * Componente de segurança reutilizável do AVA-MS.
 * <p>
 * Interface fornecida: {@link #gerar(UsuarioAutenticado)} e {@link #validar(String)}.<br>
 * Interface requerida: a chave secreta (propriedade {@code ava.security.jwt-secret}),
 * com pelo menos 32 caracteres (HMAC-SHA256).
 */
public class JwtService {

    private final SecretKey chave;
    private final long validadeMs;

    public JwtService(String segredo, long validadeMs) {
        if (segredo == null || segredo.length() < 32) {
            throw new IllegalArgumentException("ava.security.jwt-secret deve ter pelo menos 32 caracteres");
        }
        this.chave = Keys.hmacShaKeyFor(segredo.getBytes(StandardCharsets.UTF_8));
        this.validadeMs = validadeMs;
    }

    /** Gera o token com id (subject), e-mail e perfil do usuário. */
    public String gerar(UsuarioAutenticado u) {
        long agora = System.currentTimeMillis();
        return Jwts.builder()
                .subject(u.id().toString())
                .claim("email", u.email())
                .claim("perfil", u.perfil().name())
                .issuedAt(new Date(agora))
                .expiration(new Date(agora + validadeMs))
                .signWith(chave)
                .compact();
    }

    /**
     * Valida assinatura e expiração do token.
     *
     * @throws JwtException se o token for inválido ou estiver expirado
     */
    public UsuarioAutenticado validar(String token) {
        Claims c = Jwts.parser().verifyWith(chave).build()
                .parseSignedClaims(token)
                .getPayload();
        return new UsuarioAutenticado(
                Long.valueOf(c.getSubject()),
                c.get("email", String.class),
                Perfil.valueOf(c.get("perfil", String.class)));
    }
}
