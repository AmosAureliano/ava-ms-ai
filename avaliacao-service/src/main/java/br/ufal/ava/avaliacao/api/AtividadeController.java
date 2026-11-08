package br.ufal.ava.avaliacao.api;

import br.ufal.ava.avaliacao.api.Dtos.*;
import br.ufal.ava.avaliacao.servico.AvaliacaoService;
import br.ufal.ava.commons.security.Perfil;
import br.ufal.ava.commons.security.UsuarioAutenticado;
import br.ufal.ava.commons.web.Acesso;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/atividades")
public class AtividadeController {

    private final AvaliacaoService service;

    public AtividadeController(AvaliacaoService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AtividadeDTO criar(UsuarioAutenticado usuario, @Valid @RequestBody AtividadeRequest req) {
        Acesso.exigir(usuario, Perfil.PROFESSOR);
        return service.criar(usuario.id(), req);
    }

    /** Atividades do curso com prazos (para o aluno, indica se já entregou). */
    @GetMapping("/curso/{cursoId}")
    public List<AtividadeResumoDTO> doCurso(UsuarioAutenticado usuario, @PathVariable Long cursoId) {
        return service.doCurso(cursoId, usuario.perfil() == Perfil.ALUNO ? usuario.id() : null);
    }

    @GetMapping("/proximas")
    public List<AtividadeResumoDTO> proximas(UsuarioAutenticado usuario,
                                             @RequestParam(defaultValue = "") List<Long> cursoIds) {
        return service.proximas(cursoIds, usuario.id());
    }

    @GetMapping("/{id}")
    public AtividadeDTO detalhe(UsuarioAutenticado usuario, @PathVariable Long id) {
        return service.detalhe(id, usuario);
    }
}
