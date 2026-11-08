import { FormEvent, useState } from "react";
import { Link } from "react-router-dom";
import { api, Categoria, CursoResumo } from "../api";
import { CursoCard, PainelServico } from "../ava-ui-components";
import { useDados } from "../useDados";

export function Professor() {
  const cursos = useDados(() => api<CursoResumo[]>("/api/cursos/meus"));
  const categorias = useDados(() => api<Categoria[]>("/api/cursos/categorias"));
  const [form, setForm] = useState({ titulo: "", descricao: "", categoriaId: "" });
  const [erro, setErro] = useState<string | null>(null);

  const criar = async (e: FormEvent) => {
    e.preventDefault();
    setErro(null);
    try {
      await api("/api/cursos", { metodo: "POST", corpo: { ...form, categoriaId: Number(form.categoriaId) } });
      setForm({ titulo: "", descricao: "", categoriaId: "" });
      cursos.recarregar();
    } catch (err) {
      setErro((err as Error).message);
    }
  };

  return (
    <div className="grade-painel">
      <PainelServico titulo="Meus cursos" servico="curso-service" porta={8082} className="largo">
        {cursos.dados?.length === 0 && <p className="vazio">Você ainda não criou cursos.</p>}
        <div className="cartoes">
          {cursos.dados?.map((c) => (
            <CursoCard key={c.id} id={c.id} titulo={c.titulo} categoria={c.categoria}
              detalhe={<small>{c.status.toLowerCase()} · {c.totalAulas} aula(s)</small>}
              acao={<Link className="botao" to={`/professor/cursos/${c.id}`}>Gerenciar</Link>} />
          ))}
        </div>
      </PainelServico>

      <PainelServico titulo="Novo curso" servico="curso-service" porta={8082}>
        <form className="form" onSubmit={criar}>
          <label>Título<input value={form.titulo} onChange={(e) => setForm({ ...form, titulo: e.target.value })} required /></label>
          <label>Descrição<textarea rows={3} value={form.descricao}
            onChange={(e) => setForm({ ...form, descricao: e.target.value })} /></label>
          <label>Categoria
            <select value={form.categoriaId} onChange={(e) => setForm({ ...form, categoriaId: e.target.value })} required>
              <option value="">Selecione</option>
              {categorias.dados?.map((c) => <option key={c.id} value={c.id}>{c.nome}</option>)}
            </select>
          </label>
          {erro && <p className="erro">{erro}</p>}
          <button>Criar curso</button>
        </form>
      </PainelServico>
    </div>
  );
}
