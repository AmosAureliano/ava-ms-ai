package br.ufal.ava.curso.dominio;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface CursoRepository extends JpaRepository<Curso, Long> {

    /** Catálogo: cursos publicados, com busca opcional por texto e categoria. */
    @EntityGraph(attributePaths = "categoria")
    @Query("""
            select c from Curso c
            where c.status = :status
              and (:busca is null or lower(c.titulo) like lower(concat('%', :busca, '%'))
                   or lower(c.descricao) like lower(concat('%', :busca, '%')))
              and (:categoriaId is null or c.categoria.id = :categoriaId)
            """)
    Page<Curso> buscar(@Param("status") StatusCurso status, @Param("busca") String busca,
                       @Param("categoriaId") Long categoriaId, Pageable pageable);

    @EntityGraph(attributePaths = "categoria")
    List<Curso> findByProfessorIdOrderByCriadoEmDesc(Long professorId);

    @EntityGraph(attributePaths = "categoria")
    List<Curso> findByStatus(StatusCurso status);

    @EntityGraph(attributePaths = "categoria")
    List<Curso> findByIdIn(Collection<Long> ids);
}
