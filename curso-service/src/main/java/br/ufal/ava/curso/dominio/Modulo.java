package br.ufal.ava.curso.dominio;

import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "modulo")
public class Modulo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "curso_id")
    private Curso curso;

    @Column(nullable = false, length = 150)
    private String titulo;

    @Column(nullable = false)
    private int ordem;

    @OneToMany(mappedBy = "modulo", cascade = CascadeType.ALL)
    @OrderBy("ordem")
    private List<Aula> aulas = new ArrayList<>();

    protected Modulo() {
    }

    public Modulo(Curso curso, String titulo, int ordem) {
        this.curso = curso;
        this.titulo = titulo;
        this.ordem = ordem;
    }

    public Long getId() { return id; }
    public Curso getCurso() { return curso; }
    public String getTitulo() { return titulo; }
    public int getOrdem() { return ordem; }
    public List<Aula> getAulas() { return aulas; }
}
