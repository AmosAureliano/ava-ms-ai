package br.ufal.ava.avaliacao.dominio;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

public interface AtividadeRepository extends JpaRepository<Atividade, Long> {
    List<Atividade> findByCursoIdOrderByPrazo(Long cursoId);

    List<Atividade> findTop10ByCursoIdInAndPrazoAfterOrderByPrazo(Collection<Long> cursoIds, LocalDateTime depoisDe);
}
