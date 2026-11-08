package br.ufal.ava.usuario.api;

import br.ufal.ava.commons.security.Perfil;
import br.ufal.ava.commons.security.UsuarioAutenticado;
import br.ufal.ava.commons.web.Acesso;
import br.ufal.ava.commons.web.PaginaResposta;
import br.ufal.ava.usuario.api.Dtos.*;
import br.ufal.ava.usuario.servico.UsuarioService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/usuarios")
public class UsuarioController {

    private final UsuarioService service;

    public UsuarioController(UsuarioService service) {
        this.service = service;
    }

    @GetMapping("/me")
    public UsuarioDTO eu(UsuarioAutenticado usuario) {
        return service.buscar(usuario.id());
    }

    /** Nome e perfil de um usuário (ex.: o professor de um curso); o e-mail não é exposto. */
    @GetMapping("/{id}")
    public UsuarioPublicoDTO buscar(UsuarioAutenticado usuario, @PathVariable Long id) {
        return service.buscarPublico(id);
    }

    @GetMapping
    public PaginaResposta<UsuarioDTO> listar(UsuarioAutenticado usuario,
                                             @RequestParam(defaultValue = "0") int pagina,
                                             @RequestParam(defaultValue = "20") int tamanho) {
        Acesso.exigir(usuario, Perfil.ADMIN);
        return service.listar(pagina, tamanho);
    }

    @PutMapping("/{id}/perfil")
    public UsuarioDTO alterarPerfil(UsuarioAutenticado usuario, @PathVariable Long id,
                                    @Valid @RequestBody AlterarPerfilRequest req) {
        Acesso.exigir(usuario, Perfil.ADMIN);
        return service.alterarPerfil(usuario.id(), id, req.perfil());
    }
}
