import { Link, NavLink, Outlet, useNavigate } from "react-router-dom";
import { useAuth } from "./auth";
import { ServicoBadge } from "./ava-ui-components";

export function Layout() {
  const { sessao, sair } = useAuth();
  const navegar = useNavigate();

  return (
    <div className="app">
      <header className="topo">
        <Link to="/" className="marca">AVA-MS</Link>
        <nav>
          {sessao?.perfil === "ALUNO" && <NavLink to="/" end>Meu painel</NavLink>}
          <span className="nav-item">
            <NavLink to="/catalogo">Catálogo de cursos</NavLink>
            <ServicoBadge servico="curso-service" porta={8082} />
          </span>
          {sessao?.perfil === "PROFESSOR" && <NavLink to="/professor">Meus cursos</NavLink>}
          {sessao?.perfil === "ADMIN" && <NavLink to="/admin">Usuários</NavLink>}
        </nav>
        <div className="usuario">
          {sessao ? (
            <>
              <span className="nav-item">
                <span title={sessao.email}>{sessao.nome} · {sessao.perfil.toLowerCase()}</span>
                <ServicoBadge servico="usuario-service" porta={8081} />
              </span>
              <button className="secundario" onClick={() => { sair(); navegar("/login"); }}>Sair</button>
            </>
          ) : (
            <Link to="/login">Entrar</Link>
          )}
        </div>
      </header>
      <main className="conteudo">
        <Outlet />
      </main>
      <footer className="rodape">
        AVA-MS · Engenharia de Software · UFAL 2026.2 · Todas as requisições passam pelo API Gateway (8080)
      </footer>
    </div>
  );
}
