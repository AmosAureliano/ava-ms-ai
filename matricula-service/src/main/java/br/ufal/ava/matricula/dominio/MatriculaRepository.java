package br.ufal.ava.matricula.dominio;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MatriculaRepository extends JpaRepository<Matricula, Long> {
    boolean existsByAlunoIdAndCursoId(Long alunoId, Long cursoId);

    Optional<Matricula> findByAlunoIdAndCursoId(Long alunoId, Long cursoId);

    List<Matricula> findByAlunoIdOrderByDataDesc(Long alunoId);

    List<Matricula> findByCursoIdOrderByDataDesc(Long cursoId);
}
