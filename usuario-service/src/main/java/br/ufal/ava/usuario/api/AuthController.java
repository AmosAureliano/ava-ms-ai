package br.ufal.ava.usuario.api;

import br.ufal.ava.usuario.api.Dtos.*;
import br.ufal.ava.usuario.servico.UsuarioService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UsuarioService service;

    public AuthController(UsuarioService service) {
        this.service = service;
    }

    @PostMapping("/cadastro")
    @ResponseStatus(HttpStatus.CREATED)
    public UsuarioDTO cadastrar(@Valid @RequestBody CadastroRequest req) {
        return service.cadastrar(req);
    }

    @PostMapping("/login")
    public TokenResponse login(@Valid @RequestBody LoginRequest req) {
        return service.autenticar(req.email(), req.senha());
    }
}
