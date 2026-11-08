package br.ufal.ava.commons.web;

import br.ufal.ava.commons.security.AvaSecurityAutoConfiguration;
import br.ufal.ava.commons.security.JwtService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.Ordered;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

/**
 * O {@link JwtService} é obrigatório: sem {@code ava.security.jwt-secret} o serviço não sobe,
 * em vez de rodar sem validar identidade.
 */
@AutoConfiguration(after = AvaSecurityAutoConfiguration.class)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@Import(GlobalExceptionHandler.class)
public class AvaWebAutoConfiguration {

    @Bean
    public FilterRegistrationBean<IdentidadeFilter> identidadeFilter(JwtService jwt, ObjectMapper json) {
        var registro = new FilterRegistrationBean<>(new IdentidadeFilter(jwt, json));
        registro.setOrder(Ordered.HIGHEST_PRECEDENCE + 10);
        return registro;
    }

    @Bean
    public WebMvcConfigurer usuarioAutenticadoConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
                resolvers.add(new UsuarioAutenticadoResolver());
            }
        };
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(name = {"feign.FeignException",
            "org.springframework.cloud.client.circuitbreaker.NoFallbackAvailableException"})
    @Import(FeignErrosHandler.class)
    static class Feign {
    }
}
