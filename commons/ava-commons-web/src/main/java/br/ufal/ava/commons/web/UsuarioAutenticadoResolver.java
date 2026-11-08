package br.ufal.ava.commons.web;

import br.ufal.ava.commons.security.UsuarioAutenticado;
import org.springframework.core.MethodParameter;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

/** Injeta o {@link UsuarioAutenticado} validado pelo {@link IdentidadeFilter}; sem token, responde 401. */
public class UsuarioAutenticadoResolver implements HandlerMethodArgumentResolver {

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.getParameterType() == UsuarioAutenticado.class;
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mav, NativeWebRequest request,
                                  WebDataBinderFactory binderFactory) {
        Object usuario = request.getAttribute(IdentidadeFilter.ATRIBUTO, RequestAttributes.SCOPE_REQUEST);
        if (usuario == null) {
            throw new NaoAutenticadoException("Usuário não autenticado");
        }
        return usuario;
    }
}
