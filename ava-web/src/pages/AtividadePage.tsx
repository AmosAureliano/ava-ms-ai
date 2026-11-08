import { useState } from "react";
import { Link, useParams } from "react-router-dom";
import { api, Atividade, dataHora, Entrega } from "../api";
import { useAuth } from "../auth";
import { PainelServico } from "../ava-ui-components";
import { useDados } from "../useDados";

/** Aluno responde a atividade; professor vê as entregas e lança notas. */
export function AtividadePage() {
  const { id } = useParams();
  const atividadeId = Number(id);
  const { sessao } = useAuth();
  const ehProfessor = sessao?.perfil === "PROFESSOR";
  const atividade = useDados(() => api<Atividade>(`/api/atividades/${atividadeId}`), [atividadeId]);
  const minhas = useDados(() => (ehProfessor ? Promise.resolve([]) : api<Entrega[]>("/api/entregas/minhas")), [ehProfessor]);
  const entregas = useDados(
    () => (ehProfessor ? api<Entrega[]>(`/api/entregas/atividade/${atividadeId}`) : Promise.resolve([])),
    [atividadeId, ehProfessor],
  );
  const [respostas, setRespostas] = useState<Record<number, number>>({});
  const [texto, setTexto] = useState("");
  const [erro, setErro] = useState<string | null>(null);

  if (atividade.erro) return <p className="erro">{atividade.erro}</p>;
  if (!atividade.dados) return <p className="vazio">Carregando…</p>;
  const a = atividade.dados;
  const minha = minhas.dados?.find((e) => e.atividadeId === a.id);

  const enviar = async () => {
    setErro(null);
    try {
      await api("/api/entregas", { metodo: "POST", corpo: { atividadeId: a.id, respostas, texto } });
      minhas.recarregar();
    } catch (e) {
      setErro((e as Error).message);
    }
  };

  return (
    <PainelServico titulo={a.titulo} servico="avaliacao-service" porta={8084}>
      <p><Link to={`/cursos/${a.cursoId}`}>← Voltar ao curso</Link></p>
      <p>{a.descricao}</p>
      <p><small>{a.tipo === "QUESTIONARIO" ? "Questionário com correção automática" : "Trabalho corrigido pelo professor"} ·
        prazo {dataHora(a.prazo)}</small></p>

      {!ehProfessor && minha && (
        <div className="resultado">
          <strong>{minha.status === "AVALIADA" ? `Nota: ${minha.nota}` : "Entrega enviada, aguardando correção"}</strong>
          {minha.comentario && <p>{minha.comentario}</p>}
        </div>
      )}

      {a.tipo === "QUESTIONARIO" && a.questoes.map((q, i) => (
        <fieldset key={q.id} className="questao" disabled={!!minha || ehProfessor}>
          <legend>{i + 1}. {q.enunciado} <small>(peso {q.peso})</small></legend>
          {q.alternativas.map((alt) => (
            <label key={alt.id} className={alt.correta ? "gabarito" : ""}>
              <input type="radio" name={`q${q.id}`} checked={respostas[q.id!] === alt.id}
                onChange={() => setRespostas({ ...respostas, [q.id!]: alt.id! })} />
              {alt.texto} {alt.correta ? "✓" : ""}
            </label>
          ))}
        </fieldset>
      ))}

      {a.tipo === "TRABALHO" && !ehProfessor && !minha && (
        <textarea rows={8} value={texto} onChange={(e) => setTexto(e.target.value)} placeholder="Escreva sua resposta" />
      )}

      {!ehProfessor && !minha && (
        <>
          {erro && <p className="erro">{erro}</p>}
          <button onClick={enviar}>Enviar</button>
        </>
      )}

      {ehProfessor && (
        <>
          <h3>Entregas</h3>
          {entregas.dados?.length === 0 && <p className="vazio">Nenhuma entrega ainda.</p>}
          {entregas.dados?.map((e) => <LancarNota key={e.id} entrega={e} aoSalvar={entregas.recarregar} />)}
        </>
      )}
    </PainelServico>
  );
}

function LancarNota({ entrega, aoSalvar }: { entrega: Entrega; aoSalvar: () => void }) {
  const [nota, setNota] = useState(entrega.nota?.toString() ?? "");
  const [comentario, setComentario] = useState(entrega.comentario ?? "");
  const [erro, setErro] = useState<string | null>(null);

  const salvar = async () => {
    setErro(null);
    try {
      await api(`/api/entregas/${entrega.id}/nota`, { metodo: "PUT", corpo: { nota: Number(nota), comentario } });
      aoSalvar();
    } catch (e) {
      setErro((e as Error).message);
    }
  };

  return (
    <div className="entrega">
      <p><strong>Aluno #{entrega.alunoId}</strong> · {dataHora(entrega.enviadaEm)} · {entrega.status}</p>
      <pre>{entrega.resposta}</pre>
      <div className="linha-form">
        <input type="number" min={0} max={10} step={0.1} value={nota} onChange={(e) => setNota(e.target.value)} placeholder="Nota" />
        <input value={comentario} onChange={(e) => setComentario(e.target.value)} placeholder="Comentário" />
        <button onClick={salvar}>Salvar nota</button>
      </div>
      {erro && <p className="erro">{erro}</p>}
    </div>
  );
}
