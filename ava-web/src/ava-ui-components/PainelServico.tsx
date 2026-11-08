import { ReactNode } from "react";
import { ServicoBadge } from "./ServicoBadge";

type Props = {
  titulo: string;
  servico: string;
  porta: number;
  children: ReactNode;
  acao?: ReactNode;
  className?: string;
};

export function PainelServico({ titulo, servico, porta, children, acao, className }: Props) {
  return (
    <section className={`painel ${className ?? ""}`}>
      <header className="painel-topo">
        <div>
          <h2>{titulo}</h2>
          <ServicoBadge servico={servico} porta={porta} />
        </div>
        {acao}
      </header>
      {children}
    </section>
  );
}
