package br.ufal.ava.avaliacao.api;

import br.ufal.ava.avaliacao.dominio.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public final class Dtos {

    private Dtos() {
    }

    public record AlternativaRequest(@NotBlank @Size(max = 500) String texto, boolean correta) {
    }

    public record QuestaoRequest(@NotBlank @Size(max = 5000) String enunciado,
                                 @DecimalMin("0.1") @DecimalMax("100") BigDecimal peso,
                                 @NotEmpty @Size(max = 10) @Valid List<AlternativaRequest> alternativas) {
    }

    public record AtividadeRequest(@NotNull Long cursoId, @NotBlank @Size(max = 150) String titulo,
                                   @Size(max = 10_000) String descricao,
                                   @NotNull TipoAtividade tipo, @NotNull @Future LocalDateTime prazo,
                                   @Size(max = 50) @Valid List<QuestaoRequest> questoes) {
    }

    /** respostas: id da questão -> id da alternativa marcada (questionário); texto: resposta livre (trabalho). */
    public record EntregaRequest(@NotNull Long atividadeId, @Size(max = 50) Map<Long, Long> respostas,
                                 @Size(max = 20_000) String texto) {
    }

    public record NotaRequest(@NotNull @DecimalMin("0") @DecimalMax("10") BigDecimal nota,
                              @Size(max = 1000) String comentario) {
    }

    public record AlternativaDTO(Long id, String texto, Boolean correta) {
    }

    public record QuestaoDTO(Long id, String enunciado, BigDecimal peso, List<AlternativaDTO> alternativas) {
    }

    public record AtividadeResumoDTO(Long id, Long cursoId, String titulo, TipoAtividade tipo, LocalDateTime prazo,
                                     int questoes, Boolean entregue) {
    }

    public record AtividadeDTO(Long id, Long cursoId, String titulo, String descricao, TipoAtividade tipo,
                               LocalDateTime prazo, List<QuestaoDTO> questoes) {
        /** Gabarito só aparece para o professor responsável pela atividade. */
        public static AtividadeDTO de(Atividade a, boolean mostrarGabarito) {
            return new AtividadeDTO(a.getId(), a.getCursoId(), a.getTitulo(), a.getDescricao(), a.getTipo(),
                    a.getPrazo(), a.getQuestoes().stream().map(q -> new QuestaoDTO(q.getId(), q.getEnunciado(),
                    q.getPeso(), q.getAlternativas().stream().map(alt -> new AlternativaDTO(alt.getId(), alt.getTexto(),
                    mostrarGabarito ? alt.isCorreta() : null)).toList())).toList());
        }
    }

    public record EntregaDTO(Long id, Long atividadeId, String atividadeTitulo, Long cursoId, Long alunoId,
                             String resposta, StatusEntrega status, BigDecimal nota, String comentario,
                             LocalDateTime enviadaEm) {
        public static EntregaDTO de(Entrega e) {
            Atividade a = e.getAtividade();
            return new EntregaDTO(e.getId(), a.getId(), a.getTitulo(), a.getCursoId(), e.getAlunoId(), e.getResposta(),
                    e.getStatus(), e.getNota(), e.getComentario(), e.getEnviadaEm());
        }
    }
}
