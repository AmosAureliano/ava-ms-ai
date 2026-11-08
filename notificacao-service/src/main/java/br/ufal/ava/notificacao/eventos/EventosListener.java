package br.ufal.ava.notificacao.eventos;

import br.ufal.ava.commons.events.*;
import br.ufal.ava.notificacao.dominio.Notificacao;
import br.ufal.ava.notificacao.dominio.NotificacaoRepository;
import br.ufal.ava.notificacao.dominio.TipoNotificacao;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;

/**
 * Transforma eventos de outros serviços em notificações. O outbox entrega "pelo menos uma vez",
 * então o id do evento é gravado com UNIQUE e repetições são ignoradas.
 */
@Component
public class EventosListener {

    private final NotificacaoRepository repo;
    private final EventoLeitor leitor;

    public EventosListener(NotificacaoRepository repo, EventoLeitor leitor) {
        this.repo = repo;
        this.leitor = leitor;
    }

    @KafkaListener(topics = Topicos.MATRICULA_CRIADA)
    @Transactional
    public void aoMatricular(@Payload String json,
                             @Header(name = OutboxRelay.CABECALHO_ID, required = false) byte[] eventoId) {
        MatriculaCriadaEvento e = leitor.ler(json, MatriculaCriadaEvento.class);
        salvar(eventoId, new Notificacao(e.alunoId(), TipoNotificacao.MATRICULA, "Matrícula confirmada! Bons estudos."));
    }

    @KafkaListener(topics = Topicos.ENTREGA_AVALIADA)
    @Transactional
    public void aoAvaliar(@Payload String json,
                          @Header(name = OutboxRelay.CABECALHO_ID, required = false) byte[] eventoId) {
        EntregaAvaliadaEvento e = leitor.ler(json, EntregaAvaliadaEvento.class);
        salvar(eventoId, new Notificacao(e.alunoId(), TipoNotificacao.NOTA,
                "Sua atividade \"" + e.atividadeTitulo() + "\" foi corrigida. Nota: " + e.nota()));
    }

    @KafkaListener(topics = Topicos.CURSO_PUBLICADO)
    @Transactional
    public void aoPublicar(@Payload String json,
                           @Header(name = OutboxRelay.CABECALHO_ID, required = false) byte[] eventoId) {
        CursoPublicadoEvento e = leitor.ler(json, CursoPublicadoEvento.class);
        salvar(eventoId, new Notificacao(e.professorId(), TipoNotificacao.CURSO,
                "O curso \"" + e.titulo() + "\" foi publicado e já aceita matrículas."));
    }

    private void salvar(byte[] eventoId, Notificacao n) {
        String id = eventoId == null ? null : new String(eventoId, StandardCharsets.UTF_8);
        if (id != null && repo.existsByEventoId(id)) {
            return;
        }
        n.associarEvento(id);
        repo.save(n);
    }
}
