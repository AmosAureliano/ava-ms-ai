import { ReactNode } from "react";
import { Navigate, Route, Routes } from "react-router-dom";
import { Perfil } from "./api";
import { useAuth } from "./auth";
import { Layout } from "./Layout";
import { Login } from "./pages/Login";
import { Cadastro } from "./pages/Cadastro";
import { Painel } from "./pages/Painel";
import { Catalogo } from "./pages/Catalogo";
import { CursoPage } from "./pages/CursoPage";
import { AtividadePage } from "./pages/AtividadePage";
import { Professor } from "./pages/Professor";
import { ProfessorCurso } from "./pages/ProfessorCurso";
import { Admin } from "./pages/Admin";

function Protegida({ children, perfis }: { children: ReactNode; perfis?: Perfil[] }) {
  const { sessao } = useAuth();
  if (!sessao) return <Navigate to="/login" replace />;
  if (perfis && !perfis.includes(sessao.perfil)) return <Navigate to="/" replace />;
  return <>{children}</>;
}

function Inicio() {
  const { sessao } = useAuth();
  if (!sessao) return <Navigate to="/login" replace />;
  if (sessao.perfil === "PROFESSOR") return <Navigate to="/professor" replace />;
  if (sessao.perfil === "ADMIN") return <Navigate to="/admin" replace />;
  return <Painel />;
}

export function App() {
  return (
    <Routes>
      <Route path="/login" element={<Login />} />
      <Route path="/cadastro" element={<Cadastro />} />
      <Route element={<Layout />}>
        <Route path="/" element={<Inicio />} />
        <Route path="/catalogo" element={<Catalogo />} />
        <Route path="/cursos/:id" element={<Protegida><CursoPage /></Protegida>} />
        <Route path="/atividades/:id" element={<Protegida><AtividadePage /></Protegida>} />
        <Route path="/professor" element={<Protegida perfis={["PROFESSOR"]}><Professor /></Protegida>} />
        <Route path="/professor/cursos/:id" element={<Protegida perfis={["PROFESSOR"]}><ProfessorCurso /></Protegida>} />
        <Route path="/admin" element={<Protegida perfis={["ADMIN"]}><Admin /></Protegida>} />
      </Route>
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
}
