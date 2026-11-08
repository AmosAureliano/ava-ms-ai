package br.ufal.ava.commons.security;

/**
 * Identidade que circula entre o gateway e os microsserviços.
 * O gateway extrai estes dados do JWT e os repassa nos cabeçalhos
 * X-Usuario-Id e X-Usuario-Perfil.
 */
public record UsuarioAutenticado(Long id, String email, Perfil perfil) {
}
