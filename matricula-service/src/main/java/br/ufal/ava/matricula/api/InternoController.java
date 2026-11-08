package br.ufal.ava.matricula.api;

import br.ufal.ava.matricula.api.Dtos.ProgressoDTO;
import br.ufal.ava.matricula.servico.MatriculaService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Consultas de outros microsserviços (avaliacao e agente-ia); fora das rotas do gateway. */
@RestController
@RequestMapping("/internal/matriculas")
public class InternoController {

    private final MatriculaService service;

    public InternoController(MatriculaService service) {
        this.service = service;
    }

    @GetMapping("/progresso")
    public ProgressoDTO progresso(@RequestParam Long alunoId, @RequestParam Long cursoId) {
        return service.progresso(alunoId, cursoId);
    }
}
