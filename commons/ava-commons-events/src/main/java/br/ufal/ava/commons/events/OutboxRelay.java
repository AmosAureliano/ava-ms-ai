package br.ufal.ava.commons.events;

import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.support.TransactionTemplate;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Publica no Kafka os eventos pendentes do outbox. A entrega é "pelo menos uma vez": se o serviço cair
 * entre o envio e a marcação, o evento é reenviado; por isso cada mensagem leva o cabeçalho
 * {@link #CABECALHO_ID} e os consumidores descartam repetidos.
 * {@code FOR UPDATE SKIP LOCKED} permite rodar mais de uma instância sem enviar o mesmo lote duas vezes.
 */
public class OutboxRelay {

    public static final String CABECALHO_ID = "ava-evento-id";

    private static final Logger log = LoggerFactory.getLogger(OutboxRelay.class);
    private static final int LOTE = 100;

    private final JdbcTemplate jdbc;
    private final KafkaTemplate<String, String> kafka;
    private final TransactionTemplate transacao;
    private final String origem;

    public OutboxRelay(JdbcTemplate jdbc, KafkaTemplate<String, String> kafka, TransactionTemplate transacao,
                       String origem) {
        this.jdbc = jdbc;
        this.kafka = kafka;
        this.transacao = transacao;
        this.origem = origem;
    }

    private record Pendente(long id, String topico, String chave, String payload) {
    }

    @Scheduled(fixedDelayString = "${ava.events.outbox.intervalo-ms:500}")
    public void publicarPendentes() {
        try {
            transacao.executeWithoutResult(status -> {
                List<Pendente> pendentes = jdbc.query("""
                        SELECT id, topico, chave, payload FROM evento_outbox
                        WHERE enviado_em IS NULL ORDER BY id LIMIT ? FOR UPDATE SKIP LOCKED""",
                        (rs, i) -> new Pendente(rs.getLong(1), rs.getString(2), rs.getString(3), rs.getString(4)),
                        LOTE);
                for (Pendente p : pendentes) {
                    enviar(p);
                    jdbc.update("UPDATE evento_outbox SET enviado_em = CURRENT_TIMESTAMP(6) WHERE id = ?", p.id());
                }
            });
        } catch (Exception e) {
            log.warn("Outbox: publicação adiada ({})", e.getMessage());
        }
    }

    @Scheduled(cron = "0 0 3 * * *")
    public void limparEnviados() {
        int n = jdbc.update("DELETE FROM evento_outbox WHERE enviado_em < NOW() - INTERVAL 7 DAY");
        if (n > 0) {
            log.info("Outbox: {} eventos antigos removidos", n);
        }
    }

    private void enviar(Pendente p) {
        var registro = new ProducerRecord<>(p.topico(), p.chave(), p.payload());
        registro.headers().add(CABECALHO_ID, (origem + ":" + p.id()).getBytes(StandardCharsets.UTF_8));
        try {
            kafka.send(registro).get(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Envio interrompido", e);
        } catch (Exception e) {
            throw new IllegalStateException("Kafka indisponível: " + e.getMessage(), e);
        }
    }
}
