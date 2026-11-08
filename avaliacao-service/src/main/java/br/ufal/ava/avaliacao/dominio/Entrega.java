package br.ufal.ava.avaliacao.dominio;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Entrega de um aluno para uma atividade (tabela entrega). aluno_id é referência lógica. */
@Entity
@Table(name = "entrega")
public class Entrega {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "atividade_id")
    private Atividade atividade;

    @Column(name = "aluno_id", nullable = false)
    private Long alunoId;

    @Column(columnDefinition = "TEXT")
    private String resposta;

    @Column(precision = 4, scale = 2)
    private BigDecimal nota;

    @Column(length = 1000)
    private String comentario;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatusEntrega status = StatusEntrega.ENVIADA;

    @Column(name = "enviada_em", nullable = false)
    private LocalDateTime enviadaEm = LocalDateTime.now();

    protected Entrega() {
    }

    public Entrega(Atividade atividade, Long alunoId, String resposta) {
        this.atividade = atividade;
        this.alunoId = alunoId;
        this.resposta = resposta;
    }

    public void avaliar(BigDecimal nota, String comentario) {
        this.nota = nota;
        this.comentario = comentario;
        this.status = StatusEntrega.AVALIADA;
    }

    public Long getId() { return id; }
    public Atividade getAtividade() { return atividade; }
    public Long getAlunoId() { return alunoId; }
    public String getResposta() { return resposta; }
    public BigDecimal getNota() { return nota; }
    public String getComentario() { return comentario; }
    public StatusEntrega getStatus() { return status; }
    public LocalDateTime getEnviadaEm() { return enviadaEm; }
}
