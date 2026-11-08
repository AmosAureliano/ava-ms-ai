package br.ufal.ava.curso.dominio;

import jakarta.persistence.*;

/** Categoria do curso (tabela separada para manter a 3FN). */
@Entity
@Table(name = "categoria")
public class Categoria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 80)
    private String nome;

    protected Categoria() {
    }

    public Long getId() { return id; }
    public String getNome() { return nome; }
}
