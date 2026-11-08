package br.ufal.ava.avaliacao.cliente;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "curso-service")
public interface CursoClient {

    @GetMapping("/internal/cursos/{id}/resumo")
    CursoResumo buscar(@PathVariable("id") Long id);

    record CursoResumo(Long id, String titulo, Long professorId) {
    }
}
