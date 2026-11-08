package br.ufal.ava.curso.api;

import br.ufal.ava.commons.security.Perfil;
import br.ufal.ava.commons.security.UsuarioAutenticado;
import br.ufal.ava.commons.web.Acesso;
import br.ufal.ava.curso.api.Dtos.*;
import br.ufal.ava.curso.servico.CursoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/aulas")
public class AulaController {

    private final CursoService service;

    public AulaController(CursoService service) {
        this.service = service;
    }

    @GetMapping("/{id}")
    public AulaDTO buscar(UsuarioAutenticado usuario, @PathVariable Long id) {
        return service.aula(id, usuario);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AulaDTO criar(UsuarioAutenticado usuario, @Valid @RequestBody AulaRequest req) {
        Acesso.exigir(usuario, Perfil.PROFESSOR);
        return service.criarAula(usuario.id(), req);
    }

    @PostMapping("/{id}/materiais")
    @ResponseStatus(HttpStatus.CREATED)
    public MaterialDTO adicionarMaterial(UsuarioAutenticado usuario, @PathVariable Long id,
                                         @Valid @RequestBody MaterialRequest req) {
        Acesso.exigir(usuario, Perfil.PROFESSOR);
        return service.adicionarMaterial(usuario.id(), id, req);
    }
}
