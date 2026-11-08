package br.ufal.ava.matricula.api;

import br.ufal.ava.matricula.dominio.StatusMatricula;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.List;

public final class Dtos {

    private Dtos() {
    }

    public record MatriculaRequest(@NotNull Long cursoId) {
    }

    public record MatriculaDTO(Long id, Long alunoId, Long cursoId, String cursoTitulo, String categoria,
                               StatusMatricula status, int totalAulas, int aulasConcluidas, int percentual,
                               LocalDateTime data, List<Long> aulasConcluidasIds) {
    }

    /** Usado pelo Agente Tutor (ferramenta consultar_progresso). */
    public record ProgressoDTO(boolean matriculado, int aulasConcluidas, int totalAulas, int percentual,
                               List<Long> aulasConcluidasIds) {
    }
}
