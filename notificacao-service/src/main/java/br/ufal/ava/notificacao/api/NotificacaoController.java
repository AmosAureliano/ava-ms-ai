package br.ufal.ava.notificacao.api;

import br.ufal.ava.commons.security.UsuarioAutenticado;
import br.ufal.ava.commons.web.AcessoNegadoException;
import br.ufal.ava.commons.web.RecursoNaoEncontradoException;
import br.ufal.ava.notificacao.dominio.Notificacao;
import br.ufal.ava.notificacao.dominio.NotificacaoRepository;
import br.ufal.ava.notificacao.dominio.TipoNotificacao;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/notificacoes")
public class NotificacaoController {

    private final NotificacaoRepository repo;

    public NotificacaoController(NotificacaoRepository repo) {
        this.repo = repo;
    }

    public record NotificacaoDTO(Long id, TipoNotificacao tipo, String mensagem, boolean lida, LocalDateTime criadaEm) {
        static NotificacaoDTO de(Notificacao n) {
            return new NotificacaoDTO(n.getId(), n.getTipo(), n.getMensagem(), n.isLida(), n.getCriadaEm());
        }
    }

    public record CaixaDTO(long naoLidas, List<NotificacaoDTO> itens) {
    }

    @GetMapping
    @Transactional(readOnly = true)
    public CaixaDTO minhas(UsuarioAutenticado usuario) {
        Long usuarioId = usuario.id();
        return new CaixaDTO(repo.countByUsuarioIdAndLidaFalse(usuarioId),
                repo.findTop30ByUsuarioIdOrderByCriadaEmDesc(usuarioId).stream().map(NotificacaoDTO::de).toList());
    }

    @PatchMapping("/{id}/lida")
    @Transactional
    public NotificacaoDTO marcarLida(UsuarioAutenticado usuario, @PathVariable Long id) {
        Notificacao n = repo.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Notificação " + id + " não encontrada"));
        if (!n.getUsuarioId().equals(usuario.id())) {
            throw new AcessoNegadoException("Notificação de outro usuário");
        }
        n.marcarLida();
        return NotificacaoDTO.de(n);
    }

    @PatchMapping("/lidas")
    @Transactional
    public int marcarTodas(UsuarioAutenticado usuario) {
        return repo.marcarTodasLidas(usuario.id());
    }
}
