package br.ufal.ava.curso.dominio;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "curso")
public class Curso {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String titulo;

    @Column(columnDefinition = "TEXT")
    private String descricao;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "categoria_id")
    private Categoria categoria;

    /** Id do professor no usuario-service: referência lógica, sem FK entre bancos. */
    @Column(name = "professor_id", nullable = false)
    private Long professorId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatusCurso status = StatusCurso.RASCUNHO;

    @Column(name = "criado_em", nullable = false)
    private LocalDateTime criadoEm = LocalDateTime.now();

    @OneToMany(mappedBy = "curso", cascade = CascadeType.ALL)
    @OrderBy("ordem")
    private List<Modulo> modulos = new ArrayList<>();

    protected Curso() {
    }

    public Curso(String titulo, String descricao, Categoria categoria, Long professorId) {
        this.titulo = titulo;
        this.descricao = descricao;
        this.categoria = categoria;
        this.professorId = professorId;
    }

    public Long getId() { return id; }
    public String getTitulo() { return titulo; }
    public String getDescricao() { return descricao; }
    public Categoria getCategoria() { return categoria; }
    public Long getProfessorId() { return professorId; }
    public StatusCurso getStatus() { return status; }
    public LocalDateTime getCriadoEm() { return criadoEm; }
    public List<Modulo> getModulos() { return modulos; }

    public void publicar() { this.status = StatusCurso.PUBLICADO; }
}
