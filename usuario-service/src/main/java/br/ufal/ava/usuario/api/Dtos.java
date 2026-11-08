package br.ufal.ava.usuario.api;

import br.ufal.ava.commons.security.Perfil;
import br.ufal.ava.usuario.dominio.Usuario;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public final class Dtos {

    private Dtos() {
    }

    /** O cadastro público cria sempre um aluno; o perfil de professor é concedido por um administrador. */
    public record CadastroRequest(
            @NotBlank @Size(max = 120) String nome,
            @NotBlank @Email @Size(max = 150) String email,
            @NotBlank @Size(min = 8, max = 72) String senha) {
    }

    public record LoginRequest(@NotBlank @Email String email, @NotBlank @Size(max = 72) String senha) {
    }

    public record TokenResponse(String token, Long id, String nome, String email, Perfil perfil) {
    }

    public record AlterarPerfilRequest(@NotNull Perfil perfil) {
    }

    public record UsuarioDTO(Long id, String nome, String email, Perfil perfil) {
        public static UsuarioDTO de(Usuario u) {
            return new UsuarioDTO(u.getId(), u.getNome(), u.getEmail(), u.getPerfil());
        }
    }

    public record UsuarioPublicoDTO(Long id, String nome, Perfil perfil) {
        public static UsuarioPublicoDTO de(Usuario u) {
            return new UsuarioPublicoDTO(u.getId(), u.getNome(), u.getPerfil());
        }
    }
}
