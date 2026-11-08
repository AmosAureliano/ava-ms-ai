package br.ufal.ava.matricula.dominio;

import java.io.Serializable;
import java.util.Objects;

public class AulaConcluidaId implements Serializable {

    private Long matriculaId;
    private Long aulaId;

    public AulaConcluidaId() {
    }

    public AulaConcluidaId(Long matriculaId, Long aulaId) {
        this.matriculaId = matriculaId;
        this.aulaId = aulaId;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof AulaConcluidaId other
                && Objects.equals(matriculaId, other.matriculaId) && Objects.equals(aulaId, other.aulaId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(matriculaId, aulaId);
    }
}
