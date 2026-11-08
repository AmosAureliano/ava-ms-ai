import { useState } from "react";
import { Link } from "react-router-dom";
import { api, AtividadeResumo, dataHora, Entrega, Matricula, Notificacao, Recomendacao } from "../api";
import { ChatTutor, CursoCard, PainelServico, ProgressoBar } from "../ava-ui-components";
import { useDados } from "../useDados";

export function Painel() {
  const matriculas = useDados(() => api<Matricula[]>("/api/matriculas/minhas"));
  const cursoIds = (matriculas.dados ?? []).map((m) => m.cursoId).join(",");
  const atividades = useDados(
    () => (cursoIds ? api<AtividadeResumo[]>(`/api/atividades/proximas?cursoIds=${cursoIds}`) : Promise.resolve([])),
    [cursoIds],
  );
  const notas = useDados(() => api<Entrega[]>("/api/entregas/minhas"));
  const recomendacoes = useDados(() => api<Recomendacao[]>("/api/recomendacoes?limite=3"));
  const caixa = useDados(() => api<{ naoLidas: number; itens: Notificacao[] }>("/api/notificacoes"));
  const [cursoTutor, setCursoTutor] = useState<number | null>(null);

  const titulos = new Map((matriculas.dados ?? []).map((m) => [m.cursoId, m.cursoTitulo]));
  const tutorCurso = cursoTutor ?? matriculas.dados?.[0]?.cursoId ?? null;

  const marcarLidas = async () => {
    await api("/api/notificacoes/lidas", { metodo: "PATCH" });
    caixa.recarregar();
  };

  return (
    <div className="grade-painel">
      <PainelServico titulo="Meus cursos em andamento" servico="matricula-service" porta={8083} className="largo">
        <Estado {...matriculas} vazio="Você ainda não está matriculado. Veja o catálogo de cursos." />
        {matriculas.dados?.map((m) => (
          <div key={m.id} className="linha-curso">
            <Link to={`/cursos/${m.cursoId}`}>{m.cursoTitulo}</Link>
            <ProgressoBar percentual={m.percentual} />
          </div>
        ))}
      </PainelServico>

      <PainelServico titulo="Próximas atividades" servico="avaliacao-service" porta={8084}>
        <Estado {...atividades} vazio="Nenhum prazo próximo." />
        <ul className="lista">
          {atividades.dados?.map((a) => (
            <li key={a.id}>
              <Link to={`/atividades/${a.id}`}>{a.titulo}</Link>
              <small>{titulos.get(a.cursoId)} · até {dataHora(a.prazo)} {a.entregue ? "· entregue" : ""}</small>
            </li>
          ))}
        </ul>
      </PainelServico>

      <PainelServico titulo="Recomendados para você" servico="recomendacao-service" porta={8086} className="largo">
        <Estado {...recomendacoes} vazio="Sem recomendações por enquanto." />
        <div className="cartoes">
          {recomendacoes.dados?.map((r) => (
            <CursoCard key={r.cursoId} id={r.cursoId} titulo={r.titulo ?? `Curso ${r.cursoId}`} categoria={r.categoria}
              detalhe={<small>{r.motivo}</small>} />
          ))}
        </div>
      </PainelServico>

      <PainelServico titulo="Notificações" servico="notificacao-service" porta={8085}
        acao={caixa.dados?.naoLidas ? <button className="link" onClick={marcarLidas}>Marcar como lidas</button> : null}>
        <Estado {...caixa} vazio="" />
        {caixa.dados?.itens.length === 0 && <p className="vazio">Nenhuma notificação.</p>}
        <ul className="lista">
          {caixa.dados?.itens.slice(0, 6).map((n) => (
            <li key={n.id} className={n.lida ? "lida" : "nova"}>
              {n.mensagem}
              <small>{dataHora(n.criadaEm)}</small>
            </li>
          ))}
        </ul>
      </PainelServico>

      <PainelServico titulo="Minhas notas" servico="avaliacao-service" porta={8084}>
        <Estado {...notas} vazio="Nenhuma entrega ainda." />
        <ul className="lista">
          {notas.dados?.map((e) => (
            <li key={e.id}>
              <Link to={`/atividades/${e.atividadeId}`}>{e.atividadeTitulo}</Link>
              <small>{e.status === "AVALIADA" ? `Nota ${e.nota}` : "Aguardando correção"}</small>
            </li>
          ))}
        </ul>
      </PainelServico>

      <PainelServico titulo="Tutor de IA" servico="agente-ia-service" porta={8087} className="largo"
        acao={matriculas.dados && matriculas.dados.length > 0 ? (
          <select value={tutorCurso ?? ""} onChange={(e) => setCursoTutor(Number(e.target.value))}>
            {matriculas.dados.map((m) => <option key={m.cursoId} value={m.cursoId}>{m.cursoTitulo}</option>)}
          </select>
        ) : null}>
        {tutorCurso ? <ChatTutor cursoId={tutorCurso} /> : <p className="vazio">Matricule-se em um curso para usar o tutor.</p>}
      </PainelServico>
    </div>
  );
}

function Estado({ carregando, erro, dados, vazio }: { carregando: boolean; erro: string | null; dados: unknown; vazio: string }) {
  if (carregando && !dados) return <p className="vazio">Carregando…</p>;
  if (erro) return <p className="erro">Serviço indisponível: {erro}</p>;
  if (Array.isArray(dados) && dados.length === 0 && vazio) return <p className="vazio">{vazio}</p>;
  return null;
}
