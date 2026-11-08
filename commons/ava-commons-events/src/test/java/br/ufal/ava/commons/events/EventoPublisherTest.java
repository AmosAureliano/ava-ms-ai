package br.ufal.ava.commons.events;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

class EventoPublisherTest {

    private final JdbcTemplate jdbc = mock(JdbcTemplate.class);
    private final EventoPublisher publisher = new EventoPublisher(jdbc, AvaEventsAutoConfiguration.mapperEventos());
    private final MatriculaCriadaEvento evento = new MatriculaCriadaEvento(1L, 2L, 3L, Instant.parse("2026-11-08T12:00:00Z"));

    @AfterEach
    void limpar() {
        TransactionSynchronizationManager.setActualTransactionActive(false);
    }

    @Test
    void gravaNoOutboxDentroDaTransacao() {
        TransactionSynchronizationManager.setActualTransactionActive(true);

        publisher.publicar(Topicos.MATRICULA_CRIADA, "1", evento);

        verify(jdbc).update(startsWith("INSERT INTO evento_outbox"), eq(Topicos.MATRICULA_CRIADA), eq("1"),
                contains("\"alunoId\":2"));
    }

    @Test
    void recusaPublicarForaDeTransacao() {
        assertThrows(IllegalStateException.class, () -> publisher.publicar(Topicos.MATRICULA_CRIADA, "1", evento));
        verifyNoInteractions(jdbc);
    }
}
