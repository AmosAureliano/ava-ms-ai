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
@RequestMapping("/api/entregas")
public class EntregaController {

    private final AvaliacaoService service;

    public EntregaController(AvaliacaoService service) {
        this.service = service;
    }

    /** Envia resposta (questionário é corrigido na hora). */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EntregaDTO entregar(UsuarioAutenticado usuario, @Valid @RequestBody EntregaRequest req) {
        Acesso.exigir(usuario, Perfil.ALUNO);
        return service.entregar(usuario.id(), req);
    }

    @PutMapping("/{id}/nota")
    public EntregaDTO lancarNota(UsuarioAutenticado usuario, @PathVariable Long id,
                                 @Valid @RequestBody NotaRequest req) {
        Acesso.exigir(usuario, Perfil.PROFESSOR);
        return service.lancarNota(usuario.id(), id, req);
    }

    @GetMapping("/minhas")
    public List<EntregaDTO> minhas(UsuarioAutenticado usuario) {
        return service.doAluno(usuario.id());
    }

    @GetMapping("/atividade/{atividadeId}")
    public List<EntregaDTO> daAtividade(UsuarioAutenticado usuario, @PathVariable Long atividadeId) {
        Acesso.exigir(usuario, Perfil.PROFESSOR);
        return service.daAtividade(usuario.id(), atividadeId);
    }
}
