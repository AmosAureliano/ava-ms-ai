package br.ufal.ava.avaliacao.dominio;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** Atividade de um curso (tabela atividade em db_avaliacoes). curso_id é referência lógica ao curso-service. */
@Entity
@Table(name = "atividade")
public class Atividade {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "curso_id", nullable = false)
    private Long cursoId;

    @Column(name = "professor_id", nullable = false)
    private Long professorId;

    @Column(nullable = false, length = 150)
    private String titulo;

    @Column(columnDefinition = "TEXT")
    private String descricao;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoAtividade tipo;

    @Column(nullable = false)
    private LocalDateTime prazo;

    @OneToMany(mappedBy = "atividade", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id")
    private List<Questao> questoes = new ArrayList<>();

    protected Atividade() {
    }

    public Atividade(Long cursoId, Long professorId, String titulo, String descricao, TipoAtividade tipo,
                     LocalDateTime prazo) {
        this.cursoId = cursoId;
        this.professorId = professorId;
        this.titulo = titulo;
        this.descricao = descricao;
        this.tipo = tipo;
        this.prazo = prazo;
    }

    public Questao adicionarQuestao(String enunciado, java.math.BigDecimal peso) {
        Questao q = new Questao(this, enunciado, peso);
        questoes.add(q);
        return q;
    }

    public Long getId() { return id; }
    public Long getCursoId() { return cursoId; }
    public Long getProfessorId() { return professorId; }
    public String getTitulo() { return titulo; }
    public String getDescricao() { return descricao; }
    public TipoAtividade getTipo() { return tipo; }
    public LocalDateTime getPrazo() { return prazo; }
    public List<Questao> getQuestoes() { return questoes; }
}
