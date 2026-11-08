package br.ufal.ava.avaliacao.dominio;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Set;

public interface EntregaRepository extends JpaRepository<Entrega, Long> {
    boolean existsByAtividadeIdAndAlunoId(Long atividadeId, Long alunoId);

    @EntityGraph(attributePaths = "atividade")
    List<Entrega> findByAlunoIdOrderByEnviadaEmDesc(Long alunoId);

    List<Entrega> findByAtividadeIdOrderByEnviadaEm(Long atividadeId);

    @Query("select e.atividade.id from Entrega e where e.alunoId = :alunoId and e.atividade.id in :atividadeIds")
    Set<Long> atividadesEntregues(@Param("alunoId") Long alunoId, @Param("atividadeIds") Collection<Long> atividadeIds);
}
