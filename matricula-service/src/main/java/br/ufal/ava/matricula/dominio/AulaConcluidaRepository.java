package br.ufal.ava.matricula.dominio;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface AulaConcluidaRepository extends JpaRepository<AulaConcluida, AulaConcluidaId> {
    List<AulaConcluida> findByMatriculaId(Long matriculaId);

    List<AulaConcluida> findByMatriculaIdIn(Collection<Long> matriculaIds);
}
