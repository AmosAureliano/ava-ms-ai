package br.ufal.ava.curso.servico;

import br.ufal.ava.commons.security.Perfil;
import br.ufal.ava.commons.security.UsuarioAutenticado;
import br.ufal.ava.commons.web.RecursoNaoEncontradoException;
import br.ufal.ava.curso.api.Dtos.MaterialRequest;
import br.ufal.ava.curso.dominio.Categoria;
import br.ufal.ava.curso.dominio.Curso;
import br.ufal.ava.curso.dominio.TipoMaterial;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class VisibilidadeEValidacaoTest {

    private static final long DONO = 2L;
    private final Validator validador = Validation.buildDefaultValidatorFactory().getValidator();

    private static Curso rascunho() {
        Curso c = new Curso("BD", "desc", mock(Categoria.class), DONO);
        ReflectionTestUtils.setField(c, "id", 1L);
        return c;
    }

    @Test
    void rascunhoSoApareceParaODonoEParaOAdministrador() {
        Curso c = rascunho();
        assertDoesNotThrow(() -> CursoService.exigirVisivel(c, new UsuarioAutenticado(DONO, "p@a.br", Perfil.PROFESSOR)));
        assertDoesNotThrow(() -> CursoService.exigirVisivel(c, new UsuarioAutenticado(1L, "adm@a.br", Perfil.ADMIN)));
        assertThrows(RecursoNaoEncontradoException.class,
                () -> CursoService.exigirVisivel(c, new UsuarioAutenticado(9L, "aluno@a.br", Perfil.ALUNO)));
        assertThrows(RecursoNaoEncontradoException.class,
                () -> CursoService.exigirVisivel(c, new UsuarioAutenticado(DONO + 1, "outro@a.br", Perfil.PROFESSOR)));
    }

    @Test
    void cursoPublicadoApareceParaQualquerUsuarioAutenticado() {
        Curso c = rascunho();
        c.publicar();
        assertDoesNotThrow(() -> CursoService.exigirVisivel(c, new UsuarioAutenticado(9L, "aluno@a.br", Perfil.ALUNO)));
    }

    @ParameterizedTest
    @ValueSource(strings = {"javascript:alert(document.cookie)", "JaVaScRiPt:alert(1)", "data:text/html,<script>",
            "ftp://x", "//evil.com", "https://"})
    void materialRejeitaEsquemasPerigosos(String url) {
        assertFalse(validador.validate(new MaterialRequest(TipoMaterial.LINK, "Link", url)).isEmpty(), url);
    }

    @ParameterizedTest
    @ValueSource(strings = {"https://dev.mysql.com/doc/", "http://exemplo.com/aula.pdf", "HTTPS://YOUTU.BE/x"})
    void materialAceitaHttpEHttps(String url) {
        assertTrue(validador.validate(new MaterialRequest(TipoMaterial.LINK, "Link", url)).isEmpty(), url);
    }
}
