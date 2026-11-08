package br.ufal.ava.matricula;

import br.ufal.ava.commons.events.EventoPublisher;
import br.ufal.ava.commons.security.Perfil;
import br.ufal.ava.commons.security.UsuarioAutenticado;
import br.ufal.ava.commons.web.AcessoNegadoException;
import br.ufal.ava.commons.web.RegraNegocioException;
import br.ufal.ava.matricula.cliente.CursoClient;
import br.ufal.ava.matricula.cliente.CursoClient.CursoResumo;
import br.ufal.ava.matricula.dominio.AulaConcluida;
import br.ufal.ava.matricula.dominio.AulaConcluidaRepository;
import br.ufal.ava.matricula.dominio.Matricula;
import br.ufal.ava.matricula.dominio.MatriculaRepository;
import br.ufal.ava.matricula.dominio.StatusMatricula;
import br.ufal.ava.matricula.servico.MatriculaService;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class MatriculaServiceTest {

    private static final long PROFESSOR = 2L;

    private final MatriculaRepository repo = mock(MatriculaRepository.class);
    private final AulaConcluidaRepository concluidas = mock(AulaConcluidaRepository.class);
    private final CursoClient cursos = mock(CursoClient.class);
    private final EventoPublisher eventos = mock(EventoPublisher.class);
    private final MatriculaService service = new MatriculaService(repo, concluidas, cursos, eventos);

    private static CursoResumo curso(String status, int totalAulas) {
        return new CursoResumo(1L, "BD", "Dados", PROFESSOR, status, totalAulas);
    }

    private static Matricula matricula(long id, long alunoId, int totalAulas) {
        Matricula m = new Matricula(alunoId, 1L, totalAulas);
        ReflectionTestUtils.setField(m, "id", id);
        return m;
    }

    @Test
    void naoMatriculaEmCursoNaoPublicado() {
        when(cursos.buscar(1L)).thenReturn(curso("RASCUNHO", 3));
        assertThrows(RegraNegocioException.class, () -> service.matricular(10L, 1L));
        verifyNoInteractions(eventos);
    }

    @Test
    void naoMatriculaDuasVezes() {
        when(cursos.buscar(1L)).thenReturn(curso("PUBLICADO", 3));
        when(repo.existsByAlunoIdAndCursoId(10L, 1L)).thenReturn(true);
        assertThrows(RegraNegocioException.class, () -> service.matricular(10L, 1L));
    }

    @Test
    void matriculaConcorrenteViraErroDeNegocio() {
        when(cursos.buscar(1L)).thenReturn(curso("PUBLICADO", 3));
        when(repo.saveAndFlush(any())).thenThrow(new DataIntegrityViolationException("uk_aluno_curso"));

        assertThrows(RegraNegocioException.class, () -> service.matricular(10L, 1L));
        verifyNoInteractions(eventos);
    }

    @Test
    void matriculaPublicaEvento() {
        when(cursos.buscar(1L)).thenReturn(curso("PUBLICADO", 3));
        when(repo.saveAndFlush(any(Matricula.class))).thenAnswer(inv -> {
            Matricula m = inv.getArgument(0);
            ReflectionTestUtils.setField(m, "id", 99L);
            return m;
        });

        service.matricular(10L, 1L);

        verify(eventos).publicar(eq("matricula.criada"), eq("99"), any());
    }

    @Test
    void professorNaoVeAlunosDeCursoAlheio() {
        when(cursos.buscar(1L)).thenReturn(curso("PUBLICADO", 3));
        var outroProfessor = new UsuarioAutenticado(PROFESSOR + 1, "p@ava.br", Perfil.PROFESSOR);

        assertThrows(AcessoNegadoException.class, () -> service.doCurso(outroProfessor, 1L));
        verify(repo, never()).findByCursoIdOrderByDataDesc(any());
    }

    @Test
    void listarMatriculasNaoAlteraAEntidadeEBuscaCursosEmLote() {
        Matricula m = matricula(5L, 10L, 2);
        when(repo.findByAlunoIdOrderByDataDesc(10L)).thenReturn(List.of(m));
        when(cursos.buscarVarios(any())).thenReturn(List.of(curso("PUBLICADO", 4)));
        when(concluidas.findByMatriculaIdIn(List.of(5L))).thenReturn(List.of(new AulaConcluida(5L, 7L)));

        var dto = service.doAluno(10L).getFirst();

        assertEquals(4, dto.totalAulas());
        assertEquals(25, dto.percentual());
        assertEquals(2, m.getTotalAulas());
        assertEquals(StatusMatricula.ATIVA, m.getStatus());
        verify(cursos, never()).buscar(any());
    }
}
