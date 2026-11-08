package br.ufal.ava.curso.dominio;

import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

/** Aula de um módulo (tabela aula). O texto em "conteudo" é a base de conhecimento do Agente Tutor. */
@Entity
@Table(name = "aula")
public class Aula {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "modulo_id")
    private Modulo modulo;

    @Column(nullable = false, length = 150)
    private String titulo;

    @Column(columnDefinition = "TEXT")
    private String conteudo;

    @Column(nullable = false)
    private int ordem;

    @OneToMany(mappedBy = "aula", cascade = CascadeType.ALL)
    private List<Material> materiais = new ArrayList<>();

    protected Aula() {
    }

    public Aula(Modulo modulo, String titulo, String conteudo, int ordem) {
        this.modulo = modulo;
        this.titulo = titulo;
        this.conteudo = conteudo;
        this.ordem = ordem;
    }

    public Long getId() { return id; }
    public Modulo getModulo() { return modulo; }
    public String getTitulo() { return titulo; }
    public String getConteudo() { return conteudo; }
    public int getOrdem() { return ordem; }
    public List<Material> getMateriais() { return materiais; }
}
