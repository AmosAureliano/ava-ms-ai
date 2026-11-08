/**
 * Componente ava-ui-components: selo que identifica, na interface, qual microsserviço
 * fornece os dados de um painel. Props: servico (nome no Eureka) e porta.
 */
type Props = { servico: string; porta: number };

export function ServicoBadge({ servico, porta }: Props) {
  return (
    <span className="servico-badge" title={`Atendido por ${servico}:${porta}`}>
      {servico}
    </span>
  );
}
