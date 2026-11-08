/** Barra de progresso do aluno em um curso (0 a 100). */
export function ProgressoBar({ percentual }: { percentual: number }) {
  const p = Math.max(0, Math.min(100, Math.round(percentual)));
  return (
    <div className="progresso" role="progressbar" aria-valuenow={p} aria-valuemin={0} aria-valuemax={100}>
      <div className="progresso-trilho">
        <div className="progresso-barra" style={{ width: `${p}%` }} />
      </div>
      <span className="progresso-rotulo">{p}%</span>
    </div>
  );
}
