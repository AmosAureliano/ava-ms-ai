package br.ufal.ava.matricula.dominio;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/** Aula concluída pelo aluno. Só data_conclusao depende da chave composta inteira (2FN). */
@Entity
@Table(name = "aula_concluida")
@IdClass(AulaConcluidaId.class)
public class AulaConcluida {

    @Id
    @Column(name = "matricula_id")
    private Long matriculaId;

    @Id
    @Column(name = "aula_id")
    private Long aulaId;

    @Column(name = "data_conclusao", nullable = false)
    private LocalDateTime dataConclusao = LocalDateTime.now();

    protected AulaConcluida() {
    }

    public AulaConcluida(Long matriculaId, Long aulaId) {
        this.matriculaId = matriculaId;
        this.aulaId = aulaId;
    }

    public Long getMatriculaId() { return matriculaId; }
    public Long getAulaId() { return aulaId; }
    public LocalDateTime getDataConclusao() { return dataConclusao; }
}
