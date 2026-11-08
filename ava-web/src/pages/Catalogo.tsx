import { FormEvent, useState } from "react";
import { useNavigate } from "react-router-dom";
import { api, Categoria, CursoResumo, Pagina } from "../api";
import { useAuth } from "../auth";
import { CursoCard, PainelServico } from "../ava-ui-components";
import { useDados } from "../useDados";

export function Catalogo() {
  const { sessao } = useAuth();
  const navegar = useNavigate();
  const [busca, setBusca] = useState("");
  const [filtro, setFiltro] = useState({ busca: "", categoriaId: "" });
  const [mensagem, setMensagem] = useState<string | null>(null);
  const categorias = useDados(() => api<Categoria[]>("/api/cursos/categorias"));
  const cursos = useDados(() => {
    const q = new URLSearchParams({ tamanho: "24" });
    if (filtro.busca) q.set("busca", filtro.busca);
    if (filtro.categoriaId) q.set("categoriaId", filtro.categoriaId);
    return api<Pagina<CursoResumo>>(`/api/cursos/publicos?${q}`);
  }, [filtro]);

  const buscar = (e: FormEvent) => {
    e.preventDefault();
    setFiltro({ ...filtro, busca });
  };

  const matricular = async (cursoId: number) => {
    setMensagem(null);
    try {
      await api("/api/matriculas", { metodo: "POST", corpo: { cursoId } });
      navegar(`/cursos/${cursoId}`);
    } catch (err) {
      setMensagem((err as Error).message);
    }
  };

  return (
    <PainelServico titulo="Catálogo de cursos" servico="curso-service" porta={8082}>
      <form className="filtros" onSubmit={buscar}>
        <input placeholder="Buscar por título ou descrição" value={busca} onChange={(e) => setBusca(e.target.value)} />
        <select value={filtro.categoriaId} onChange={(e) => setFiltro({ ...filtro, categoriaId: e.target.value })}>
          <option value="">Todas as categorias</option>
          {categorias.dados?.map((c) => <option key={c.id} value={c.id}>{c.nome}</option>)}
        </select>
        <button>Buscar</button>
      </form>
      {mensagem && <p className="erro">{mensagem}</p>}
      {cursos.erro && <p className="erro">{cursos.erro}</p>}
      {cursos.dados?.itens.length === 0 && <p className="vazio">Nenhum curso encontrado.</p>}
      <div className="cartoes">
        {cursos.dados?.itens.map((c) => (
          <CursoCard key={c.id} id={c.id} titulo={c.titulo} categoria={c.categoria} descricao={c.descricao}
            detalhe={<small>{c.totalAulas} aula(s)</small>}
            acao={sessao?.perfil === "ALUNO" ? (
              <button onClick={() => matricular(c.id)}>
                Matricular-se <small className="via">via matricula-service</small>
              </button>
            ) : !sessao ? <small>Entre para se matricular</small> : null} />
        ))}
      </div>
    </PainelServico>
  );
}
