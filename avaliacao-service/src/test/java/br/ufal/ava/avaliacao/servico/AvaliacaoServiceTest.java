package br.ufal.ava.avaliacao.servico;

import br.ufal.ava.avaliacao.api.Dtos.EntregaRequest;
import br.ufal.ava.avaliacao.cliente.CursoClient;
import br.ufal.ava.avaliacao.cliente.MatriculaClient;
import br.ufal.ava.avaliacao.cliente.MatriculaClient.Progresso;
import br.ufal.ava.avaliacao.dominio.*;
import br.ufal.ava.commons.events.EventoPublisher;
import br.ufal.ava.commons.security.Perfil;
import br.ufal.ava.commons.security.UsuarioAutenticado;
import br.ufal.ava.commons.web.AcessoNegadoException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AvaliacaoServiceTest {

    private static final long PROFESSOR = 2L;
    private static final long ALUNO = 10L;

    private final AtividadeRepository atividades = mock(AtividadeRepository.class);
    private final EntregaRepository entregas = mock(EntregaRepository.class);
    private final MatriculaClient matriculas = mock(MatriculaClient.class);
    private final AvaliacaoService service = new AvaliacaoService(atividades, entregas, mock(CursoClient.class),
            matriculas, mock(EventoPublisher.class), new ObjectMapper());

    private Atividade questionario() {
        Atividade a = new Atividade(1L, PROFESSOR, "Quiz", "", TipoAtividade.QUESTIONARIO,
                LocalDateTime.now().plusDays(1));
        a.adicionarQuestao("3FN remove?", BigDecimal.ONE).adicionarAlternativa("Dependências transitivas", true);
        ReflectionTestUtils.setField(a, "id", 5L);
        when(atividades.findById(5L)).thenReturn(Optional.of(a));
        return a;
    }

    private static Boolean gabarito(br.ufal.ava.avaliacao.api.Dtos.AtividadeDTO dto) {
        return dto.questoes().getFirst().alternativas().getFirst().correta();
    }

    @Test
    void professorResponsavelVeOGabarito() {
        questionario();
        var dto = service.detalhe(5L, new UsuarioAutenticado(PROFESSOR, "p@a.br", Perfil.PROFESSOR));
        assertEquals(Boolean.TRUE, gabarito(dto));
    }

    @Test
    void outroProfessorNaoAcessaAAtividade() {
        questionario();
        assertThrows(AcessoNegadoException.class,
                () -> service.detalhe(5L, new UsuarioAutenticado(PROFESSOR + 1, "x@a.br", Perfil.PROFESSOR)));
    }

    @Test
    void alunoMatriculadoVeSemGabarito() {
        questionario();
        when(matriculas.progresso(ALUNO, 1L)).thenReturn(new Progresso(true));
        var dto = service.detalhe(5L, new UsuarioAutenticado(ALUNO, "a@a.br", Perfil.ALUNO));
        assertNull(gabarito(dto));
    }

    @Test
    void alunoNaoMatriculadoNaoVeNemEntrega() {
        questionario();
        when(matriculas.progresso(ALUNO, 1L)).thenReturn(new Progresso(false));
        var aluno = new UsuarioAutenticado(ALUNO, "a@a.br", Perfil.ALUNO);

        assertThrows(AcessoNegadoException.class, () -> service.detalhe(5L, aluno));
        assertThrows(AcessoNegadoException.class,
                () -> service.entregar(ALUNO, new EntregaRequest(5L, Map.of(1L, 1L), null)));
        verify(entregas, never()).saveAndFlush(any());
    }

    @Test
    void administradorVeSemGabarito() {
        questionario();
        var dto = service.detalhe(5L, new UsuarioAutenticado(1L, "adm@a.br", Perfil.ADMIN));
        assertNull(gabarito(dto));
    }
}
