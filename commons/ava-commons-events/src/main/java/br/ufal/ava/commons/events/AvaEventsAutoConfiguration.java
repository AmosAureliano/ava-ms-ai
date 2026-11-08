package br.ufal.ava.commons.events;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.jdbc.JdbcTemplateAutoConfiguration;
import org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration;
import org.springframework.boot.autoconfigure.transaction.TransactionAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Auto-configuração do componente de eventos: cria os tópicos do AVA e disponibiliza {@link EventoLeitor}.
 * Serviços que publicam eventos ligam {@code ava.events.outbox.enabled=true} e precisam da tabela
 * {@code evento_outbox}; recebem então {@link EventoPublisher} e o {@link OutboxRelay}.
 */
@AutoConfiguration(after = {KafkaAutoConfiguration.class, JdbcTemplateAutoConfiguration.class,
        TransactionAutoConfiguration.class})
public class AvaEventsAutoConfiguration {

    /** ObjectMapper próprio para eventos: datas em ISO-8601 e tolerante a campos novos. */
    static ObjectMapper mapperEventos() {
        return new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    }

    @Bean
    @ConditionalOnMissingBean
    public EventoLeitor eventoLeitor() {
        return new EventoLeitor(mapperEventos());
    }

    @Bean
    @ConditionalOnBean(KafkaAdmin.class)
    public KafkaAdmin.NewTopics topicosAva() {
        return new KafkaAdmin.NewTopics(
                topico(Topicos.MATRICULA_CRIADA),
                topico(Topicos.AULA_CONCLUIDA),
                topico(Topicos.ENTREGA_AVALIADA),
                topico(Topicos.CURSO_PUBLICADO));
    }

    private static NewTopic topico(String nome) {
        return TopicBuilder.name(nome).partitions(1).replicas(1).build();
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnProperty(prefix = "ava.events.outbox", name = "enabled", havingValue = "true")
    @EnableScheduling
    static class Outbox {

        @Bean
        @ConditionalOnMissingBean
        EventoPublisher eventoPublisher(JdbcTemplate jdbc) {
            return new EventoPublisher(jdbc, mapperEventos());
        }

        @Bean
        OutboxRelay outboxRelay(JdbcTemplate jdbc, KafkaTemplate<String, String> kafka, TransactionTemplate transacao,
                                @Value("${spring.application.name}") String origem) {
            return new OutboxRelay(jdbc, kafka, transacao, origem);
        }
    }
}
