package br.ufal.ava.matricula.cliente;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Collection;
import java.util.List;

@FeignClient(name = "curso-service")
public interface CursoClient {

    @GetMapping("/internal/cursos/{id}/resumo")
    CursoResumo buscar(@PathVariable("id") Long id);

    @GetMapping("/internal/cursos/resumos")
    List<CursoResumo> buscarVarios(@RequestParam("ids") Collection<Long> ids);

    @GetMapping("/internal/aulas/{id}")
    AulaResumo aula(@PathVariable("id") Long id);

    record CursoResumo(Long id, String titulo, String categoria, Long professorId, String status, int totalAulas) {
    }

    record AulaResumo(Long id, Long cursoId, String titulo) {
    }
}
