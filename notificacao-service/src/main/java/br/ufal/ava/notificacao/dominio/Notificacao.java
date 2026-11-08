package br.ufal.ava.notificacao.dominio;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/** Notificação para um usuário (tabela notificacao em db_notificacoes). usuario_id é referência lógica. */
@Entity
@Table(name = "notificacao")
public class Notificacao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "usuario_id", nullable = false)
    private Long usuarioId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoNotificacao tipo;

    @Column(nullable = false, length = 500)
    private String mensagem;

    @Column(nullable = false)
    private boolean lida;

    @Column(name = "criada_em", nullable = false)
    private LocalDateTime criadaEm = LocalDateTime.now();

    @Column(name = "evento_id", length = 100, unique = true)
    private String eventoId;

    protected Notificacao() {
    }

    public Notificacao(Long usuarioId, TipoNotificacao tipo, String mensagem) {
        this.usuarioId = usuarioId;
        this.tipo = tipo;
        this.mensagem = mensagem;
    }

    public void marcarLida() { this.lida = true; }

    public void associarEvento(String eventoId) { this.eventoId = eventoId; }

    public Long getId() { return id; }
    public Long getUsuarioId() { return usuarioId; }
    public TipoNotificacao getTipo() { return tipo; }
    public String getMensagem() { return mensagem; }
    public boolean isLida() { return lida; }
    public LocalDateTime getCriadaEm() { return criadaEm; }
}
