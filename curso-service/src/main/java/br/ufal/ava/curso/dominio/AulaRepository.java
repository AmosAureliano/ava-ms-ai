package br.ufal.ava.curso.dominio;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface AulaRepository extends JpaRepository<Aula, Long> {

    record ContagemAulas(Long cursoId, Long total) {
    }

    @Query("select count(a) from Aula a where a.modulo.curso.id = :cursoId")
    int contarPorCurso(@Param("cursoId") Long cursoId);

    @Query("""
            select new br.ufal.ava.curso.dominio.AulaRepository$ContagemAulas(m.curso.id, count(a))
            from Aula a join a.modulo m where m.curso.id in :cursoIds group by m.curso.id""")
    List<ContagemAulas> contarPorCursos(@Param("cursoIds") Collection<Long> cursoIds);

    @Query("select a from Aula a join fetch a.modulo m where m.curso.id = :cursoId order by m.ordem, a.ordem")
    List<Aula> listarPorCurso(@Param("cursoId") Long cursoId);
}
