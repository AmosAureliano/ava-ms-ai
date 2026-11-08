package br.ufal.ava.curso.api;

import br.ufal.ava.curso.dominio.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

public final class Dtos {

    private Dtos() {
    }

    public record CursoRequest(@NotBlank @Size(max = 150) String titulo, @Size(max = 5000) String descricao,
                               @NotNull Long categoriaId) {
    }

    public record ModuloRequest(@NotBlank @Size(max = 150) String titulo) {
    }

    public record AulaRequest(@NotNull Long moduloId, @NotBlank @Size(max = 150) String titulo,
                              @Size(max = 50_000) String conteudo) {
    }

    /** Só http(s): um link "javascript:" seria executado no navegador do aluno que clicasse nele. */
    public record MaterialRequest(@NotNull TipoMaterial tipo, @NotBlank @Size(max = 150) String titulo,
                                  @NotBlank @Size(max = 500)
                                  @Pattern(regexp = "https?://\\S+", flags = Pattern.Flag.CASE_INSENSITIVE,
                                          message = "deve começar com http:// ou https://") String url) {
    }

    public record CategoriaDTO(Long id, String nome) {
        public static CategoriaDTO de(Categoria c) {
            return new CategoriaDTO(c.getId(), c.getNome());
        }
    }

    public record CursoResumoDTO(Long id, String titulo, String descricao, String categoria, Long professorId,
                                 StatusCurso status, int totalAulas) {
    }

    public record MaterialDTO(Long id, TipoMaterial tipo, String titulo, String url) {
        public static MaterialDTO de(Material m) {
            return new MaterialDTO(m.getId(), m.getTipo(), m.getTitulo(), m.getUrl());
        }
    }

    public record AulaDTO(Long id, Long moduloId, Long cursoId, String titulo, String conteudo, int ordem,
                          List<MaterialDTO> materiais) {
        public static AulaDTO de(Aula a) {
            return new AulaDTO(a.getId(), a.getModulo().getId(), a.getModulo().getCurso().getId(), a.getTitulo(),
                    a.getConteudo(), a.getOrdem(), a.getMateriais().stream().map(MaterialDTO::de).toList());
        }
    }

    public record ModuloDTO(Long id, String titulo, int ordem, List<AulaDTO> aulas) {
        public static ModuloDTO de(Modulo m) {
            return new ModuloDTO(m.getId(), m.getTitulo(), m.getOrdem(), m.getAulas().stream().map(AulaDTO::de).toList());
        }
    }

    public record CursoDetalheDTO(Long id, String titulo, String descricao, CategoriaDTO categoria, Long professorId,
                                  StatusCurso status, List<ModuloDTO> modulos) {
        public static CursoDetalheDTO de(Curso c) {
            return new CursoDetalheDTO(c.getId(), c.getTitulo(), c.getDescricao(), CategoriaDTO.de(c.getCategoria()),
                    c.getProfessorId(), c.getStatus(), c.getModulos().stream().map(ModuloDTO::de).toList());
        }
    }

    public record TrechoDTO(Long aulaId, String aulaTitulo, String trecho, int pontuacao) {
    }
}
