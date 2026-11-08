package br.ufal.ava.curso;

import br.ufal.ava.curso.servico.BuscaTrechos;
import br.ufal.ava.curso.dominio.Aula;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class BuscaTrechosTest {

    private static Aula aula(long id, String titulo, String conteudo) {
        Aula a = Mockito.mock(Aula.class);
        Mockito.when(a.getId()).thenReturn(id);
        Mockito.when(a.getTitulo()).thenReturn(titulo);
        Mockito.when(a.getConteudo()).thenReturn(conteudo);
        return a;
    }

    @Test
    void encontraParagrafoMaisRelevanteIgnorandoAcentos() {
        Aula a = aula(1, "Normalização", """
                O modelo ER representa entidades e relacionamentos.

                A normalização elimina redundâncias. A terceira forma normal remove dependências transitivas.
                """);
        var r = BuscaTrechos.buscar(List.of(a), "O que é a normalizacao e a terceira forma normal?", 3);

        assertFalse(r.isEmpty());
        assertTrue(r.get(0).trecho().contains("terceira forma normal"));
    }

    @Test
    void consultaSemTermosRelevantesNaoRetornaNada() {
        Aula a = aula(1, "Aula", "Conteúdo qualquer");
        assertTrue(BuscaTrechos.buscar(List.of(a), "o que é?", 3).isEmpty());
    }
}
