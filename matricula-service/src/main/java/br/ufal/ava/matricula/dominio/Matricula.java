package br.ufal.ava.matricula.dominio;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/** Entidade Matricula (tabela matricula em db_matriculas). aluno_id e curso_id são referências lógicas. */
@Entity
@Table(name = "matricula")
public class Matricula {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "aluno_id", nullable = false)
    private Long alunoId;

    @Column(name = "curso_id", nullable = false)
    private Long cursoId;

    @Column(name = "total_aulas", nullable = false)
    private int totalAulas;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatusMatricula status = StatusMatricula.ATIVA;

    @Column(nullable = false)
    private LocalDateTime data = LocalDateTime.now();

    protected Matricula() {
    }

    public Matricula(Long alunoId, Long cursoId, int totalAulas) {
        this.alunoId = alunoId;
        this.cursoId = cursoId;
        this.totalAulas = totalAulas;
    }

    public Long getId() { return id; }
    public Long getAlunoId() { return alunoId; }
    public Long getCursoId() { return cursoId; }
    public int getTotalAulas() { return totalAulas; }
    public StatusMatricula getStatus() { return status; }
    public LocalDateTime getData() { return data; }

    /** O curso pode ganhar aulas depois da matrícula: mantém o total atualizado. */
    public void atualizarTotalAulas(int total) { this.totalAulas = total; }

    public void concluir() { this.status = StatusMatricula.CONCLUIDA; }
}
