package br.ufal.ava.notificacao.dominio;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface NotificacaoRepository extends JpaRepository<Notificacao, Long> {
    List<Notificacao> findTop30ByUsuarioIdOrderByCriadaEmDesc(Long usuarioId);

    long countByUsuarioIdAndLidaFalse(Long usuarioId);

    boolean existsByEventoId(String eventoId);

    @Modifying
    @Query("update Notificacao n set n.lida = true where n.usuarioId = :usuarioId and n.lida = false")
    int marcarTodasLidas(@Param("usuarioId") Long usuarioId);
}
