package br.ufal.ava.matricula.api;

import br.ufal.ava.commons.security.Perfil;
import br.ufal.ava.commons.security.UsuarioAutenticado;
import br.ufal.ava.commons.web.Acesso;
import br.ufal.ava.matricula.api.Dtos.*;
import br.ufal.ava.matricula.servico.MatriculaService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/matriculas")
public class MatriculaController {

    private final MatriculaService service;

    public MatriculaController(MatriculaService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MatriculaDTO matricular(UsuarioAutenticado usuario, @Valid @RequestBody MatriculaRequest req) {
        Acesso.exigir(usuario, Perfil.ALUNO);
        return service.matricular(usuario.id(), req.cursoId());
    }

    @GetMapping("/minhas")
    public List<MatriculaDTO> minhas(UsuarioAutenticado usuario) {
        return service.doAluno(usuario.id());
    }

    /** Matrícula do aluno logado em um curso específico (404 se não matriculado). */
    @GetMapping("/curso/{cursoId}/minha")
    public MatriculaDTO minhaNoCurso(UsuarioAutenticado usuario, @PathVariable Long cursoId) {
        return service.doAlunoNoCurso(usuario.id(), cursoId);
    }

    /** Alunos matriculados em um curso: só o professor responsável ou um administrador. */
    @GetMapping("/curso/{cursoId}")
    public List<MatriculaDTO> doCurso(UsuarioAutenticado usuario, @PathVariable Long cursoId) {
        Acesso.exigir(usuario, Perfil.PROFESSOR, Perfil.ADMIN);
        return service.doCurso(usuario, cursoId);
    }

    @PostMapping("/{id}/aulas/{aulaId}/concluir")
    public MatriculaDTO concluir(UsuarioAutenticado usuario, @PathVariable Long id, @PathVariable Long aulaId) {
        return service.concluirAula(usuario.id(), id, aulaId);
    }
}
