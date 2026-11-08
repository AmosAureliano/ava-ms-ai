package br.ufal.ava.usuario;

import br.ufal.ava.commons.security.JwtService;
import br.ufal.ava.commons.security.Perfil;
import br.ufal.ava.commons.web.NaoAutenticadoException;
import br.ufal.ava.commons.web.RegraNegocioException;
import br.ufal.ava.usuario.api.Dtos.CadastroRequest;
import br.ufal.ava.usuario.dominio.Usuario;
import br.ufal.ava.usuario.dominio.UsuarioRepository;
import br.ufal.ava.usuario.servico.UsuarioService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class UsuarioServiceTest {

    private final UsuarioRepository repo = mock(UsuarioRepository.class);
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(4);
    private final JwtService jwt = new JwtService("segredo-de-teste-com-mais-de-32-caracteres", 60_000);
    private final UsuarioService service = new UsuarioService(repo, encoder, jwt);

    @Test
    void cadastroPublicoSempreCriaAluno() {
        when(repo.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

        service.cadastrar(new CadastroRequest("Ana", " Ana@Ava.Ufal.br ", "12345678"));

        ArgumentCaptor<Usuario> salvo = ArgumentCaptor.forClass(Usuario.class);
        verify(repo).save(salvo.capture());
        assertEquals(Perfil.ALUNO, salvo.getValue().getPerfil());
        assertEquals("ana@ava.ufal.br", salvo.getValue().getEmail());
    }

    @Test
    void loginComSenhaCorretaGeraTokenValido() {
        Usuario u = new Usuario("Ana", "ana@ava.ufal.br", encoder.encode("123456"), Perfil.ALUNO);
        ReflectionTestUtils.setField(u, "id", 3L);
        when(repo.findByEmailIgnoreCase("ana@ava.ufal.br")).thenReturn(Optional.of(u));

        var resp = service.autenticar("ana@ava.ufal.br", "123456");

        var usuario = jwt.validar(resp.token());
        assertEquals(3L, usuario.id());
        assertEquals(Perfil.ALUNO, usuario.perfil());
    }

    @Test
    void loginComSenhaErradaFalha() {
        Usuario u = new Usuario("Ana", "ana@ava.ufal.br", encoder.encode("123456"), Perfil.ALUNO);
        when(repo.findByEmailIgnoreCase("ana@ava.ufal.br")).thenReturn(Optional.of(u));

        assertThrows(NaoAutenticadoException.class, () -> service.autenticar("ana@ava.ufal.br", "errada"));
    }

    @Test
    void loginComEmailInexistenteFalhaComAMesmaMensagem() {
        when(repo.findByEmailIgnoreCase("nao@existe.br")).thenReturn(Optional.empty());

        var erro = assertThrows(NaoAutenticadoException.class, () -> service.autenticar("nao@existe.br", "123456"));
        assertEquals("E-mail ou senha incorretos", erro.getMessage());
    }

    @Test
    void administradorNaoRebaixaASiMesmo() {
        assertThrows(RegraNegocioException.class, () -> service.alterarPerfil(1L, 1L, Perfil.ALUNO));
        verify(repo, never()).findById(any());
    }
}
