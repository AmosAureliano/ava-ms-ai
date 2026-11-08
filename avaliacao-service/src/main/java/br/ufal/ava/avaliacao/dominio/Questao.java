package br.ufal.ava.avaliacao.dominio;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "questao")
public class Questao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "atividade_id")
    private Atividade atividade;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String enunciado;

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal peso;

    /** Alternativas em tabela própria (1FN), em vez de colunas alt_a, alt_b... */
    @OneToMany(mappedBy = "questao", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id")
    private List<Alternativa> alternativas = new ArrayList<>();

    protected Questao() {
    }

    Questao(Atividade atividade, String enunciado, BigDecimal peso) {
        this.atividade = atividade;
        this.enunciado = enunciado;
        this.peso = peso;
    }

    public void adicionarAlternativa(String texto, boolean correta) {
        alternativas.add(new Alternativa(this, texto, correta));
    }

    public Long getId() { return id; }
    public Atividade getAtividade() { return atividade; }
    public String getEnunciado() { return enunciado; }
    public BigDecimal getPeso() { return peso; }
    public List<Alternativa> getAlternativas() { return alternativas; }
}
