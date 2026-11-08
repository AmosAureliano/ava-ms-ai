import { useState } from "react";
import { Link, useParams } from "react-router-dom";
import { api, AtividadeResumo, Aula, CursoDetalhe, dataHora, ErroApi, Matricula, urlSegura } from "../api";
import { useAuth } from "../auth";
import { ChatTutor, PainelServico, ProgressoBar } from "../ava-ui-components";
import { useDados } from "../useDados";

export function CursoPage() {
  const { id } = useParams();
  const cursoId = Number(id);
  const { sessao } = useAuth();
  const ehAluno = sessao?.perfil === "ALUNO";
  const curso = useDados(() => api<CursoDetalhe>(`/api/cursos/${cursoId}`), [cursoId]);
  const matricula = useDados<Matricula | null>(
    () => (ehAluno ? api<Matricula>(`/api/matriculas/curso/${cursoId}/minha`).catch((e: ErroApi) => {
      if (e.status === 404) return null;
      throw e;
    }) : Promise.resolve(null)),
    [cursoId, ehAluno],
  );
  const atividades = useDados(() => api<AtividadeResumo[]>(`/api/atividades/curso/${cursoId}`), [cursoId]);
  const [aberta, setAberta] = useState<Aula | null>(null);
  const [erro, setErro] = useState<string | null>(null);

  const matricular = async () => {
    setErro(null);
    try {
      matricula.setDados(await api<Matricula>("/api/matriculas", { metodo: "POST", corpo: { cursoId } }));
    } catch (e) {
      setErro((e as Error).message);
    }
  };

  const concluir = async (aulaId: number) => {
    if (!matricula.dados) return;
    try {
      matricula.setDados(await api<Matricula>(`/api/matriculas/${matricula.dados.id}/aulas/${aulaId}/concluir`, { metodo: "POST" }));
    } catch (e) {
      setErro((e as Error).message);
    }
  };

  if (curso.erro) return <p className="erro">{curso.erro}</p>;
  if (!curso.dados) return <p className="vazio">Carregando…</p>;
  const c = curso.dados;
  const concluidas = new Set(matricula.dados?.aulasConcluidasIds ?? []);

  return (
    <div className="grade-curso">
      <div className="coluna-principal">
        <PainelServico titulo={c.titulo} servico="curso-service" porta={8082}>
          <p className="curso-categoria">{c.categoria.nome}</p>
          <p>{c.descricao}</p>
          {erro && <p className="erro">{erro}</p>}
          {c.modulos.map((m) => (
            <div key={m.id} className="modulo">
              <h3>{m.ordem}. {m.titulo}</h3>
              <ul className="lista">
                {m.aulas.map((a) => (
                  <li key={a.id} className={concluidas.has(a.id) ? "concluida" : ""}>
                    <button className="link" onClick={() => setAberta(aberta?.id === a.id ? null : a)}>
                      {concluidas.has(a.id) ? "✓ " : ""}{a.titulo}
                    </button>
                    {aberta?.id === a.id && (
                      <div className="aula">
                        {a.conteudo?.split(/\n\s*\n/).map((p, i) => <p key={i}>{p}</p>)}
                        {a.materiais.length > 0 && (
                          <ul className="materiais">
                            {a.materiais.map((mt) => (
                              <li key={mt.id}><span className="tag">{mt.tipo}</span>
                                {urlSegura(mt.url)
                                  ? <a href={urlSegura(mt.url)} target="_blank" rel="noopener noreferrer">{mt.titulo}</a>
                                  : <span title="Link bloqueado: endereço inválido">{mt.titulo}</span>}</li>
                            ))}
                          </ul>
                        )}
                        {matricula.dados && !concluidas.has(a.id) && (
                          <button onClick={() => concluir(a.id)}>Marcar aula como concluída</button>
                        )}
                      </div>
                    )}
                  </li>
                ))}
              </ul>
            </div>
          ))}
        </PainelServico>

        <PainelServico titulo="Atividades" servico="avaliacao-service" porta={8084}>
          {atividades.dados?.length === 0 && <p className="vazio">Nenhuma atividade neste curso.</p>}
          <ul className="lista">
            {atividades.dados?.map((a) => (
              <li key={a.id}>
                <Link to={`/atividades/${a.id}`}>{a.titulo}</Link>
                <small>{a.tipo === "QUESTIONARIO" ? "Questionário" : "Trabalho"} · até {dataHora(a.prazo)}
                  {a.entregue ? " · entregue" : ""}</small>
              </li>
            ))}
          </ul>
        </PainelServico>
      </div>

      <aside className="coluna-lateral">
        {ehAluno && (
          <PainelServico titulo="Meu progresso" servico="matricula-service" porta={8083}>
            {matricula.dados ? (
              <>
                <ProgressoBar percentual={matricula.dados.percentual} />
                <small>{matricula.dados.aulasConcluidas} de {matricula.dados.totalAulas} aulas</small>
              </>
            ) : matricula.carregando ? <p className="vazio">Carregando…</p> : (
              <button onClick={matricular} disabled={c.status !== "PUBLICADO"}>Matricular-se neste curso</button>
            )}
          </PainelServico>
        )}
        {ehAluno && matricula.dados && (
          <PainelServico titulo="Tutor de IA" servico="agente-ia-service" porta={8087}>
            <ChatTutor cursoId={cursoId} />
          </PainelServico>
        )}
      </aside>
    </div>
  );
}
