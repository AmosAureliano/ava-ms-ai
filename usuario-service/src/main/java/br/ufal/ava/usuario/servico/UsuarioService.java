package br.ufal.ava.usuario.servico;

import br.ufal.ava.commons.security.JwtService;
import br.ufal.ava.commons.security.Perfil;
import br.ufal.ava.commons.security.UsuarioAutenticado;
import br.ufal.ava.commons.web.NaoAutenticadoException;
import br.ufal.ava.commons.web.PaginaResposta;
import br.ufal.ava.commons.web.Paginacao;
import br.ufal.ava.commons.web.RecursoNaoEncontradoException;
import br.ufal.ava.commons.web.RegraNegocioException;
import br.ufal.ava.usuario.api.Dtos.*;
import br.ufal.ava.usuario.dominio.Usuario;
import br.ufal.ava.usuario.dominio.UsuarioRepository;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class UsuarioService {

    private final UsuarioRepository repo;
    private final PasswordEncoder encoder;
    private final JwtService jwt;
    /** Hash comparado quando o e-mail não existe, para o login levar o mesmo tempo nos dois casos. */
    private final String hashFicticio;

    public UsuarioService(UsuarioRepository repo, PasswordEncoder encoder, JwtService jwt) {
        this.repo = repo;
        this.encoder = encoder;
        this.jwt = jwt;
        this.hashFicticio = encoder.encode("senha-ficticia-para-tempo-constante");
    }

    @Transactional
    public UsuarioDTO cadastrar(CadastroRequest req) {
        String email = req.email().trim().toLowerCase();
        if (repo.existsByEmailIgnoreCase(email)) {
            throw new RegraNegocioException("E-mail já cadastrado");
        }
        Usuario u = repo.save(new Usuario(req.nome().trim(), email, encoder.encode(req.senha()), Perfil.ALUNO));
        return UsuarioDTO.de(u);
    }

    @Transactional(readOnly = true)
    public TokenResponse autenticar(String email, String senha) {
        Optional<Usuario> encontrado = repo.findByEmailIgnoreCase(email.trim());
        boolean senhaConfere = encoder.matches(senha, encontrado.map(Usuario::getSenhaHash).orElse(hashFicticio));
        Usuario u = encontrado.filter(x -> senhaConfere)
                .orElseThrow(() -> new NaoAutenticadoException("E-mail ou senha incorretos"));
        String token = jwt.gerar(new UsuarioAutenticado(u.getId(), u.getEmail(), u.getPerfil()));
        return new TokenResponse(token, u.getId(), u.getNome(), u.getEmail(), u.getPerfil());
    }

    @Transactional(readOnly = true)
    public UsuarioDTO buscar(Long id) {
        return UsuarioDTO.de(usuario(id));
    }

    @Transactional(readOnly = true)
    public UsuarioPublicoDTO buscarPublico(Long id) {
        return UsuarioPublicoDTO.de(usuario(id));
    }

    @Transactional(readOnly = true)
    public PaginaResposta<UsuarioDTO> listar(int pagina, int tamanho) {
        var page = repo.findAll(Paginacao.de(pagina, tamanho, 100, Sort.by("nome")));
        return PaginaResposta.de(page, UsuarioDTO::de);
    }

    /** O administrador não pode rebaixar a si mesmo: evita ficar sem nenhum administrador. */
    @Transactional
    public UsuarioDTO alterarPerfil(Long adminId, Long id, Perfil perfil) {
        if (adminId.equals(id) && perfil != Perfil.ADMIN) {
            throw new RegraNegocioException("Você não pode remover o seu próprio perfil de administrador");
        }
        Usuario u = usuario(id);
        u.setPerfil(perfil);
        return UsuarioDTO.de(u);
    }

    private Usuario usuario(Long id) {
        return repo.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuário " + id + " não encontrado"));
    }
}
