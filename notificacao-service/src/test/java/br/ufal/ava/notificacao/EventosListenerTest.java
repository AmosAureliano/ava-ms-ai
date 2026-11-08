package br.ufal.ava.notificacao;

import br.ufal.ava.commons.events.EventoLeitor;
import br.ufal.ava.notificacao.dominio.Notificacao;
import br.ufal.ava.notificacao.dominio.NotificacaoRepository;
import br.ufal.ava.notificacao.dominio.TipoNotificacao;
import br.ufal.ava.notificacao.eventos.EventosListener;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

class EventosListenerTest {

    private static final String NOTA = """
            {"entregaId":1,"atividadeId":2,"atividadeTitulo":"Quiz ER","cursoId":3,"alunoId":42,
             "nota":8.5,"data":"2026-11-08T12:00:00Z"}""";

    private final NotificacaoRepository repo = mock(NotificacaoRepository.class);
    private final EventoLeitor leitor = new EventoLeitor(new ObjectMapper().registerModule(new JavaTimeModule())
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES));
    private final EventosListener listener = new EventosListener(repo, leitor);

    @Test
    void notaGeraNotificacaoParaOAluno() {
        listener.aoAvaliar(NOTA, "avaliacao-service:7".getBytes(StandardCharsets.UTF_8));

        ArgumentCaptor<Notificacao> cap = ArgumentCaptor.forClass(Notificacao.class);
        verify(repo).save(cap.capture());
        assertEquals(42L, cap.getValue().getUsuarioId());
        assertEquals(TipoNotificacao.NOTA, cap.getValue().getTipo());
        assertTrue(cap.getValue().getMensagem().contains("8.5"));
    }

    @Test
    void eventoRepetidoNaoGeraSegundaNotificacao() {
        when(repo.existsByEventoId("avaliacao-service:7")).thenReturn(true);

        listener.aoAvaliar(NOTA, "avaliacao-service:7".getBytes(StandardCharsets.UTF_8));

        verify(repo, never()).save(any());
    }
}
