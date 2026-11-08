import { ReactNode } from "react";
import { Link } from "react-router-dom";

type Props = {
  id: number;
  titulo: string;
  categoria?: string | null;
  descricao?: string;
  detalhe?: ReactNode;
  acao?: ReactNode;
};

export function CursoCard({ id, titulo, categoria, descricao, detalhe, acao }: Props) {
  return (
    <article className="curso-card">
      {categoria && <span className="curso-categoria">{categoria}</span>}
      <h3>
        <Link to={`/cursos/${id}`}>{titulo}</Link>
      </h3>
      {descricao && <p className="curso-descricao">{descricao}</p>}
      {detalhe && <div className="curso-detalhe">{detalhe}</div>}
      {acao && <div className="curso-acao">{acao}</div>}
    </article>
  );
}
