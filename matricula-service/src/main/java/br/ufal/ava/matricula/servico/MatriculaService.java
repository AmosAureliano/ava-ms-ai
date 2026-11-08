package br.ufal.ava.matricula.servico;

import br.ufal.ava.commons.events.AulaConcluidaEvento;
import br.ufal.ava.commons.events.EventoPublisher;
import br.ufal.ava.commons.events.MatriculaCriadaEvento;
import br.ufal.ava.commons.events.Topicos;
import br.ufal.ava.commons.security.Perfil;
import br.ufal.ava.commons.security.UsuarioAutenticado;
import br.ufal.ava.commons.web.AcessoNegadoException;
import br.ufal.ava.commons.web.RecursoNaoEncontradoException;
import br.ufal.ava.commons.web.RegraNegocioException;
import br.ufal.ava.matricula.api.Dtos.MatriculaDTO;
import br.ufal.ava.matricula.api.Dtos.ProgressoDTO;
import br.ufal.ava.matricula.cliente.CursoClient;
import br.ufal.ava.matricula.cliente.CursoClient.CursoResumo;
import br.ufal.ava.matricula.dominio.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class MatriculaService {

    private static final Logger log = LoggerFactory.getLogger(MatriculaService.class);

    private final MatriculaRepository repo;
    private final AulaConcluidaRepository concluidas;
    private final CursoClient cursoClient;
    private final EventoPublisher eventos;

    public MatriculaService(MatriculaRepository repo, AulaConcluidaRepository concluidas,
                            CursoClient cursoClient, EventoPublisher eventos) {
        this.repo = repo;
        this.concluidas = concluidas;
        this.cursoClient = cursoClient;
        this.eventos = eventos;
    }

    @Transactional
    public MatriculaDTO matricular(Long alunoId, Long cursoId) {
        CursoResumo curso = cursoClient.buscar(cursoId);
        if (!"PUBLICADO".equals(curso.status())) {
            throw new RegraNegocioException("Curso não está aberto para matrícula");
        }
        if (repo.existsByAlunoIdAndCursoId(alunoId, cursoId)) {
            throw new RegraNegocioException("Aluno já matriculado neste curso");
        }
        Matricula m;
        try {
            m = repo.saveAndFlush(new Matricula(alunoId, cursoId, curso.totalAulas()));
        } catch (DataIntegrityViolationException e) {
            throw new RegraNegocioException("Aluno já matriculado neste curso");
        }
        eventos.publicar(Topicos.MATRICULA_CRIADA, m.getId().toString(),
                new MatriculaCriadaEvento(m.getId(), alunoId, cursoId, Instant.now()));
        return dto(m, curso, List.of());
    }

    @Transactional(readOnly = true)
    public List<MatriculaDTO> doAluno(Long alunoId) {
        return dtos(repo.findByAlunoIdOrderByDataDesc(alunoId));
    }

    @Transactional(readOnly = true)
    public MatriculaDTO doAlunoNoCurso(Long alunoId, Long cursoId) {
        Matricula m = repo.findByAlunoIdAndCursoId(alunoId, cursoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Aluno não matriculado neste curso"));
        return dto(m, cursoOuNulo(cursoId), idsConcluidas(m.getId()));
    }

    @Transactional(readOnly = true)
    public List<MatriculaDTO> doCurso(UsuarioAutenticado usuario, Long cursoId) {
        CursoResumo curso = cursoClient.buscar(cursoId);
        if (usuario.perfil() == Perfil.PROFESSOR && !usuario.id().equals(curso.professorId())) {
            throw new AcessoNegadoException("Somente o professor responsável pode ver os alunos deste curso");
        }
        List<Matricula> lista = repo.findByCursoIdOrderByDataDesc(cursoId);
        Map<Long, List<Long>> feitas = concluidasPorMatricula(lista);
        return lista.stream().map(m -> dto(m, curso, feitas.getOrDefault(m.getId(), List.of()))).toList();
    }

    /** Idempotente: concluir de novo a mesma aula não gera outro evento. */
    @Transactional
    public MatriculaDTO concluirAula(Long alunoId, Long matriculaId, Long aulaId) {
        Matricula m = repo.findById(matriculaId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Matrícula " + matriculaId + " não encontrada"));
        if (!m.getAlunoId().equals(alunoId)) {
            throw new AcessoNegadoException("Matrícula pertence a outro aluno");
        }
        if (!cursoClient.aula(aulaId).cursoId().equals(m.getCursoId())) {
            throw new RegraNegocioException("A aula não pertence ao curso desta matrícula");
        }
        if (!concluidas.existsById(new AulaConcluidaId(matriculaId, aulaId))) {
            concluidas.save(new AulaConcluida(matriculaId, aulaId));
            eventos.publicar(Topicos.AULA_CONCLUIDA, m.getId().toString(),
                    new AulaConcluidaEvento(m.getId(), alunoId, m.getCursoId(), aulaId, Instant.now()));
        }
        CursoResumo curso = cursoOuNulo(m.getCursoId());
        if (curso != null) {
            m.atualizarTotalAulas(curso.totalAulas());
        }
        List<Long> ids = idsConcluidas(matriculaId);
        if (percentual(ids.size(), m.getTotalAulas()) == 100) {
            m.concluir();
        }
        return dto(m, curso, ids);
    }

    @Transactional(readOnly = true)
    public ProgressoDTO progresso(Long alunoId, Long cursoId) {
        return repo.findByAlunoIdAndCursoId(alunoId, cursoId)
                .map(m -> {
                    List<Long> ids = idsConcluidas(m.getId());
                    return new ProgressoDTO(true, ids.size(), m.getTotalAulas(),
                            percentual(ids.size(), m.getTotalAulas()), ids);
                })
                .orElse(new ProgressoDTO(false, 0, 0, 0, List.of()));
    }

    private List<MatriculaDTO> dtos(List<Matricula> lista) {
        if (lista.isEmpty()) {
            return List.of();
        }
        Map<Long, CursoResumo> cursos = cursosOuVazio(lista.stream().map(Matricula::getCursoId).collect(Collectors.toSet()));
        Map<Long, List<Long>> feitas = concluidasPorMatricula(lista);
        return lista.stream()
                .map(m -> dto(m, cursos.get(m.getCursoId()), feitas.getOrDefault(m.getId(), List.of())))
                .toList();
    }

    /** Leitura pura: o total de aulas mais recente vem do curso-service, sem alterar a matrícula. */
    private static MatriculaDTO dto(Matricula m, CursoResumo curso, List<Long> idsConcluidas) {
        int total = curso != null ? curso.totalAulas() : m.getTotalAulas();
        int pct = percentual(idsConcluidas.size(), total);
        StatusMatricula status = pct == 100 ? StatusMatricula.CONCLUIDA : m.getStatus();
        return new MatriculaDTO(m.getId(), m.getAlunoId(), m.getCursoId(),
                curso != null ? curso.titulo() : "Curso " + m.getCursoId(),
                curso != null ? curso.categoria() : null,
                status, total, idsConcluidas.size(), pct, m.getData(), idsConcluidas);
    }

    private Map<Long, List<Long>> concluidasPorMatricula(List<Matricula> lista) {
        if (lista.isEmpty()) {
            return Map.of();
        }
        return concluidas.findByMatriculaIdIn(lista.stream().map(Matricula::getId).toList()).stream()
                .collect(Collectors.groupingBy(AulaConcluida::getMatriculaId,
                        Collectors.mapping(AulaConcluida::getAulaId, Collectors.toList())));
    }

    private List<Long> idsConcluidas(Long matriculaId) {
        return concluidas.findByMatriculaId(matriculaId).stream().map(AulaConcluida::getAulaId).toList();
    }

    static int percentual(int feitas, int total) {
        return total <= 0 ? 0 : Math.min(100, Math.round(feitas * 100f / total));
    }

    /** Se o curso-service estiver fora do ar, as matrículas ainda aparecem, só sem título. */
    private Map<Long, CursoResumo> cursosOuVazio(Set<Long> ids) {
        try {
            return cursoClient.buscarVarios(ids).stream().collect(Collectors.toMap(CursoResumo::id, Function.identity()));
        } catch (Exception e) {
            log.warn("curso-service indisponível ao listar matrículas: {}", e.getMessage());
            return Map.of();
        }
    }

    private CursoResumo cursoOuNulo(Long cursoId) {
        try {
            return cursoClient.buscar(cursoId);
        } catch (Exception e) {
            log.warn("curso-service indisponível para o curso {}: {}", cursoId, e.getMessage());
            return null;
        }
    }
}
