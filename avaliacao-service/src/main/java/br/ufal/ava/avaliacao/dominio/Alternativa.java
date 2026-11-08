package br.ufal.ava.avaliacao.dominio;

import jakarta.persistence.*;

@Entity
@Table(name = "alternativa")
public class Alternativa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "questao_id")
    private Questao questao;

    @Column(nullable = false, length = 500)
    private String texto;

    @Column(nullable = false)
    private boolean correta;

    protected Alternativa() {
    }

    Alternativa(Questao questao, String texto, boolean correta) {
        this.questao = questao;
        this.texto = texto;
        this.correta = correta;
    }

    public Long getId() { return id; }
    public String getTexto() { return texto; }
    public boolean isCorreta() { return correta; }
}
