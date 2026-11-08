package br.ufal.ava.commons.events;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Registra eventos na tabela {@code evento_outbox}, na mesma transação da mudança de negócio
 * (padrão Transactional Outbox): se a transação sofrer rollback o evento some junto, e se o Kafka
 * estiver fora do ar o evento fica guardado até o {@link OutboxRelay} conseguir publicá-lo.
 * <p>Interface fornecida: {@link #publicar(String, String, Object)}.
 */
public class EventoPublisher {

    private final JdbcTemplate jdbc;
    private final ObjectMapper mapper;

    public EventoPublisher(JdbcTemplate jdbc, ObjectMapper mapper) {
        this.jdbc = jdbc;
        this.mapper = mapper;
    }

    public void publicar(String topico, String chave, Object evento) {
        if (!TransactionSynchronizationManager.isActualTransactionActive()) {
            throw new IllegalStateException("Eventos devem ser publicados dentro da transação da mudança de negócio");
        }
        try {
            jdbc.update("INSERT INTO evento_outbox (topico, chave, payload) VALUES (?, ?, ?)",
                    topico, chave, mapper.writeValueAsString(evento));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Evento não serializável: " + evento.getClass().getSimpleName(), e);
        }
    }
}
