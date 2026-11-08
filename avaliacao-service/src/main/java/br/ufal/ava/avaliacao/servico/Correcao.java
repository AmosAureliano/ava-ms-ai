package br.ufal.ava.avaliacao.servico;

import br.ufal.ava.avaliacao.dominio.Atividade;
import br.ufal.ava.avaliacao.dominio.Questao;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;

public final class Correcao {

    private Correcao() {
    }

    /**
     * Cada questão objetiva vale o seu peso se a alternativa marcada for a correta.
     * Nota final de 0 a 10, com duas casas decimais.
     */
    public static BigDecimal corrigirObjetivas(Atividade atividade, Map<Long, Long> respostas) {
        BigDecimal obtido = BigDecimal.ZERO;
        BigDecimal total = BigDecimal.ZERO;
        for (Questao q : atividade.getQuestoes()) {
            total = total.add(q.getPeso());
            Long marcada = respostas == null ? null : respostas.get(q.getId());
            boolean acertou = q.getAlternativas().stream()
                    .anyMatch(a -> a.isCorreta() && a.getId().equals(marcada));
            if (acertou) {
                obtido = obtido.add(q.getPeso());
            }
        }
        return total.signum() == 0 ? BigDecimal.ZERO
                : obtido.multiply(BigDecimal.TEN).divide(total, 2, RoundingMode.HALF_UP);
    }
}
