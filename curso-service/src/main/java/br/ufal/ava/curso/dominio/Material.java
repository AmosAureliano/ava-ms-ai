package br.ufal.ava.curso.dominio;

import jakarta.persistence.*;

@Entity
@Table(name = "material")
public class Material {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "aula_id")
    private Aula aula;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoMaterial tipo;

    @Column(nullable = false, length = 150)
    private String titulo;

    @Column(nullable = false, length = 500)
    private String url;

    protected Material() {
    }

    public Material(Aula aula, TipoMaterial tipo, String titulo, String url) {
        this.aula = aula;
        this.tipo = tipo;
        this.titulo = titulo;
        this.url = url;
    }

    public Long getId() { return id; }
    public Aula getAula() { return aula; }
    public TipoMaterial getTipo() { return tipo; }
    public String getTitulo() { return titulo; }
    public String getUrl() { return url; }
}
