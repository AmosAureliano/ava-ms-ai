package br.ufal.ava.commons.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

/**
 * Auto-configuração do componente: basta adicionar a dependência e definir
 * {@code ava.security.jwt-secret} para ter um {@link JwtService} disponível para injeção.
 */
@AutoConfiguration
@ConditionalOnProperty(prefix = "ava.security", name = "jwt-secret")
public class AvaSecurityAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public JwtService jwtService(@Value("${ava.security.jwt-secret}") String segredo,
                                 @Value("${ava.security.jwt-validade-ms:7200000}") long validadeMs) {
        return new JwtService(segredo, validadeMs);
    }
}
