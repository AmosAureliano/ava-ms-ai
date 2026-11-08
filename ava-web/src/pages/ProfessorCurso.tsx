import { FormEvent, useState } from "react";
import { Link, useParams } from "react-router-dom";
import { api, AtividadeResumo, CursoDetalhe, dataHora, Matricula, Questao } from "../api";
import { PainelServico, ProgressoBar } from "../ava-ui-components";
import { useDados } from "../useDados";

export function ProfessorCurso() {
  const { id } = useParams();
  const cursoId = Number(id);
  const curso = useDados(() => api<CursoDetalhe>(`/api/cursos/${cursoId}`), [cursoId]);
  const atividades = useDados(() => api<AtividadeResumo[]>(`/api/atividades/curso/${cursoId}`), [cursoId]);
  const alunos = useDados(() => api<Matricula[]>(`/api/matriculas/curso/${cursoId}`), [cursoId]);
  const [erro, setErro] = useState<string | null>(null);

  const executar = async (fn: () => Promise<unknown>, depois: () => void) => {
    setErro(null);
    try {
      await fn();
      depois();
    } catch (e) {
      setErro((e as Error).message);
    }
  };

  if (!curso.dados) return <p className={curso.erro ? "erro" : "vazio"}>{curso.erro ?? "Carregando…"}</p>;
  const c = curso.dados;
  const aulas = c.modulos.flatMap((m) => m.aulas.map((a) => ({ ...a, modulo: m.titulo })));

  return (
    <div className="grade-painel">
      <PainelServico titulo={c.titulo} servico="curso-service" porta={8082} className="largo"
        acao={c.status !== "PUBLICADO" ? (
          <button onClick={() => executar(() => api(`/api/cursos/${cursoId}/publicar`, { metodo: "PATCH" }), curso.recarregar)}>
            Publicar curso
          </button>
        ) : <span className="tag">publicado</span>}>
        {erro && <p className="erro">{erro}</p>}
        {c.modulos.map((m) => (
          <div key={m.id} className="modulo">
            <h3>{m.ordem}. {m.titulo}</h3>
            <ul className="lista">
              {m.aulas.map((a) => <li key={a.id}>{a.titulo} <small>{a.materiais.length} material(is)</small></li>)}
            </ul>
          </div>
        ))}
        <div className="formularios">
          <FormSimples titulo="Novo módulo" campos={["titulo"]}
            aoEnviar={(v) => executar(() => api(`/api/cursos/${cursoId}/modulos`, { metodo: "POST", corpo: v }), curso.recarregar)} />
          <NovaAula modulos={c.modulos.map((m) => ({ id: m.id, titulo: m.titulo }))}
            aoEnviar={(v) => executar(() => api("/api/aulas", { metodo: "POST", corpo: v }), curso.recarregar)} />
          <NovoMaterial aulas={aulas}
            aoEnviar={(aulaId, v) => executar(() => api(`/api/aulas/${aulaId}/materiais`, { metodo: "POST", corpo: v }), curso.recarregar)} />
        </div>
      </PainelServico>

      <PainelServico titulo="Atividades" servico="avaliacao-service" porta={8084}>
        <ul className="lista">
          {atividades.dados?.map((a) => (
            <li key={a.id}><Link to={`/atividades/${a.id}`}>{a.titulo}</Link>
              <small>{a.tipo.toLowerCase()} · até {dataHora(a.prazo)}</small></li>
          ))}
        </ul>
        <NovaAtividade cursoId={cursoId} aulas={aulas}
          aoCriar={(v) => executar(() => api("/api/atividades", { metodo: "POST", corpo: v }), atividades.recarregar)} />
      </PainelServico>

      <PainelServico titulo="Alunos matriculados" servico="matricula-service" porta={8083}>
        {alunos.dados?.length === 0 && <p className="vazio">Nenhum aluno matriculado.</p>}
        {alunos.dados?.map((m) => (
          <div key={m.id} className="linha-curso">
            <span>Aluno #{m.alunoId}</span>
            <ProgressoBar percentual={m.percentual} />
          </div>
        ))}
      </PainelServico>
    </div>
  );
}

function FormSimples({ titulo, campos, aoEnviar }: { titulo: string; campos: string[]; aoEnviar: (v: Record<string, string>) => void }) {
  const [v, setV] = useState<Record<string, string>>({});
  return (
    <form className="form" onSubmit={(e: FormEvent) => { e.preventDefault(); aoEnviar(v); setV({}); }}>
      <h4>{titulo}</h4>
      {campos.map((c) => <input key={c} placeholder={c} value={v[c] ?? ""} onChange={(e) => setV({ ...v, [c]: e.target.value })} required />)}
      <button>Adicionar</button>
    </form>
  );
}

function NovaAula({ modulos, aoEnviar }: { modulos: { id: number; titulo: string }[]; aoEnviar: (v: object) => void }) {
  const [v, setV] = useState({ moduloId: "", titulo: "", conteudo: "" });
  return (
    <form className="form" onSubmit={(e) => { e.preventDefault(); aoEnviar({ ...v, moduloId: Number(v.moduloId) }); setV({ moduloId: v.moduloId, titulo: "", conteudo: "" }); }}>
      <h4>Nova aula</h4>
      <select value={v.moduloId} onChange={(e) => setV({ ...v, moduloId: e.target.value })} required>
        <option value="">Módulo</option>
        {modulos.map((m) => <option key={m.id} value={m.id}>{m.titulo}</option>)}
      </select>
      <input placeholder="Título" value={v.titulo} onChange={(e) => setV({ ...v, titulo: e.target.value })} required />
      <textarea rows={5} placeholder="Conteúdo em texto (parágrafos separados por linha em branco). É a base do tutor de IA."
        value={v.conteudo} onChange={(e) => setV({ ...v, conteudo: e.target.value })} />
      <button>Adicionar aula</button>
    </form>
  );
}

function NovoMaterial({ aulas, aoEnviar }: { aulas: { id: number; titulo: string }[]; aoEnviar: (aulaId: number, v: object) => void }) {
  const [v, setV] = useState({ aulaId: "", tipo: "LINK", titulo: "", url: "" });
  return (
    <form className="form" onSubmit={(e) => { e.preventDefault(); aoEnviar(Number(v.aulaId), { tipo: v.tipo, titulo: v.titulo, url: v.url }); setV({ ...v, titulo: "", url: "" }); }}>
      <h4>Novo material</h4>
      <select value={v.aulaId} onChange={(e) => setV({ ...v, aulaId: e.target.value })} required>
        <option value="">Aula</option>
        {aulas.map((a) => <option key={a.id} value={a.id}>{a.titulo}</option>)}
      </select>
      <select value={v.tipo} onChange={(e) => setV({ ...v, tipo: e.target.value })}>
        <option>LINK</option><option>PDF</option><option>VIDEO</option>
      </select>
      <input placeholder="Título" value={v.titulo} onChange={(e) => setV({ ...v, titulo: e.target.value })} required />
      <input type="url" pattern="https?://.+" title="Use um endereço http:// ou https://" placeholder="https://…" value={v.url} onChange={(e) => setV({ ...v, url: e.target.value })} required />
      <button>Anexar</button>
    </form>
  );
}

const questaoVazia = (): Questao => ({
  enunciado: "", peso: 1,
  alternativas: [0, 1, 2, 3].map((i) => ({ texto: "", correta: i === 0 })),
});

function NovaAtividade({ cursoId, aulas, aoCriar }: { cursoId: number; aulas: { id: number; titulo: string }[]; aoCriar: (v: object) => void }) {
  const [tipo, setTipo] = useState<"QUESTIONARIO" | "TRABALHO">("QUESTIONARIO");
  const [titulo, setTitulo] = useState("");
  const [descricao, setDescricao] = useState("");
  const [prazo, setPrazo] = useState("");
  const [questoes, setQuestoes] = useState<Questao[]>([questaoVazia()]);
  const [aulaIa, setAulaIa] = useState("");
  const [gerando, setGerando] = useState(false);
  const [aviso, setAviso] = useState<string | null>(null);

  const alterar = (i: number, q: Questao) => setQuestoes(questoes.map((x, j) => (j === i ? q : x)));

  /** Agente Gerador de Questões (agente-ia-service): o professor revisa antes de salvar. */
  const gerarComIa = async () => {
    setGerando(true);
    setAviso(null);
    try {
      const geradas = await api<Questao[]>("/api/tutor/gerar-questoes", { metodo: "POST", corpo: { aulaId: Number(aulaIa), quantidade: 3 } });
      setQuestoes([...questoes.filter((q) => q.enunciado.trim()), ...geradas]);
      setAviso(`${geradas.length} questão(ões) sugerida(s) pela IA. Revise antes de salvar.`);
    } catch (e) {
      setAviso((e as Error).message);
    } finally {
      setGerando(false);
    }
  };

  const enviar = (e: FormEvent) => {
    e.preventDefault();
    aoCriar({ cursoId, titulo, descricao, tipo, prazo, questoes: tipo === "QUESTIONARIO" ? questoes : [] });
    setTitulo(""); setDescricao(""); setQuestoes([questaoVazia()]); setAviso(null);
  };

  return (
    <form className="form nova-atividade" onSubmit={enviar}>
      <h4>Nova atividade</h4>
      <select value={tipo} onChange={(e) => setTipo(e.target.value as typeof tipo)}>
        <option value="QUESTIONARIO">Questionário (correção automática)</option>
        <option value="TRABALHO">Trabalho (correção manual)</option>
      </select>
      <input placeholder="Título" value={titulo} onChange={(e) => setTitulo(e.target.value)} required />
      <textarea rows={2} placeholder="Descrição" value={descricao} onChange={(e) => setDescricao(e.target.value)} />
      <label>Prazo<input type="datetime-local" value={prazo} onChange={(e) => setPrazo(e.target.value)} required /></label>

      {tipo === "QUESTIONARIO" && (
        <>
          <div className="ia">
            <select value={aulaIa} onChange={(e) => setAulaIa(e.target.value)}>
              <option value="">Aula para gerar questões</option>
              {aulas.map((a) => <option key={a.id} value={a.id}>{a.titulo}</option>)}
            </select>
            <button type="button" className="secundario" disabled={!aulaIa || gerando} onClick={gerarComIa}>
              {gerando ? "Gerando…" : "Gerar questões com IA"}
            </button>
            <small className="via">via agente-ia-service</small>
          </div>
          {aviso && <p className="dica">{aviso}</p>}
          {questoes.map((q, i) => (
            <fieldset key={i} className="questao">
              <legend>Questão {i + 1}</legend>
              <textarea rows={2} placeholder="Enunciado" value={q.enunciado} onChange={(e) => alterar(i, { ...q, enunciado: e.target.value })} required />
              {q.alternativas.map((alt, k) => (
                <label key={k} className="alternativa">
                  <input type="radio" name={`correta-${i}`} checked={!!alt.correta}
                    onChange={() => alterar(i, { ...q, alternativas: q.alternativas.map((x, j) => ({ ...x, correta: j === k })) })} />
                  <input placeholder={`Alternativa ${k + 1}`} value={alt.texto} required
                    onChange={(e) => alterar(i, { ...q, alternativas: q.alternativas.map((x, j) => (j === k ? { ...x, texto: e.target.value } : x)) })} />
                </label>
              ))}
              {questoes.length > 1 && (
                <button type="button" className="link" onClick={() => setQuestoes(questoes.filter((_, j) => j !== i))}>Remover questão</button>
              )}
            </fieldset>
          ))}
          <button type="button" className="secundario" onClick={() => setQuestoes([...questoes, questaoVazia()])}>+ Questão</button>
        </>
      )}
      <button>Criar atividade</button>
    </form>
  );
}
