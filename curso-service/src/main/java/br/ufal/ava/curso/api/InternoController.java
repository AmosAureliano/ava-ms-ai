package br.ufal.ava.curso.api;

import br.ufal.ava.curso.api.Dtos.*;
import br.ufal.ava.curso.servico.CursoService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Consultas usadas por outros microsserviços (matricula, avaliacao, recomendacao e agente-ia).
 * O gateway só roteia /api/**, então /internal/** só é alcançável dentro da rede dos contêineres.
 */
@RestController
@RequestMapping("/internal")
public class InternoController {

    private final CursoService service;

    public InternoController(CursoService service) {
        this.service = service;
    }

    @GetMapping("/cursos/catalogo")
    public List<CursoResumoDTO> catalogoCompleto() {
        return service.catalogoCompleto();
    }

    @GetMapping("/cursos/resumos")
    public List<CursoResumoDTO> resumos(@RequestParam List<Long> ids) {
        return service.resumos(ids);
    }

    @GetMapping("/cursos/{id}/resumo")
    public CursoResumoDTO resumo(@PathVariable Long id) {
        return service.resumo(id);
    }

    /** Ferramenta buscar_conteudo do Agente Tutor. */
    @GetMapping("/cursos/{id}/trechos")
    public List<TrechoDTO> trechos(@PathVariable Long id, @RequestParam("q") String consulta,
                                   @RequestParam(defaultValue = "4") int limite) {
        return service.trechos(id, consulta, limite);
    }

    @GetMapping("/aulas/{id}")
    public AulaDTO aula(@PathVariable Long id) {
        return service.aulaInterna(id);
    }
}
