package br.ufal.ava.avaliacao;

import br.ufal.ava.avaliacao.dominio.Alternativa;
import br.ufal.ava.avaliacao.dominio.Atividade;
import br.ufal.ava.avaliacao.dominio.Questao;
import br.ufal.ava.avaliacao.servico.Correcao;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class CorrecaoTest {

    private static Alternativa alt(long id, boolean correta) {
        Alternativa a = mock(Alternativa.class);
        when(a.getId()).thenReturn(id);
        when(a.isCorreta()).thenReturn(correta);
        return a;
    }

    private static Questao questao(long id, String peso, Alternativa... alts) {
        Questao q = mock(Questao.class);
        when(q.getId()).thenReturn(id);
        when(q.getPeso()).thenReturn(new BigDecimal(peso));
        when(q.getAlternativas()).thenReturn(List.of(alts));
        return q;
    }

    @Test
    void notaProporcionalAoPesoDasQuestoesCorretas() {
        Questao q1 = questao(1, "1", alt(10, true), alt(11, false));
        Questao q2 = questao(2, "3", alt(20, false), alt(21, true));
        Atividade atividade = mock(Atividade.class);
        when(atividade.getQuestoes()).thenReturn(List.of(q1, q2));

        // acertou só a questão de peso 3 -> 3/4 * 10 = 7.50
        BigDecimal nota = Correcao.corrigirObjetivas(atividade, Map.of(1L, 11L, 2L, 21L));

        assertEquals(new BigDecimal("7.50"), nota);
    }

    @Test
    void semRespostasNotaZero() {
        Questao q1 = questao(1, "1", alt(10, true), alt(11, false));
        Atividade atividade = mock(Atividade.class);
        when(atividade.getQuestoes()).thenReturn(List.of(q1));

        assertEquals(new BigDecimal("0.00"), Correcao.corrigirObjetivas(atividade, Map.of()));
    }
}
