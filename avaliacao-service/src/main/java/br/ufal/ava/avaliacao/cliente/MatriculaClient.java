package br.ufal.ava.avaliacao.cliente;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "matricula-service")
public interface MatriculaClient {

    @GetMapping("/internal/matriculas/progresso")
    Progresso progresso(@RequestParam("alunoId") Long alunoId, @RequestParam("cursoId") Long cursoId);

    record Progresso(boolean matriculado) {
    }
}
