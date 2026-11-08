import { FormEvent, useState } from "react";
import { Link, Navigate, useNavigate } from "react-router-dom";
import { useAuth } from "../auth";
import { ServicoBadge } from "../ava-ui-components";

export function Login() {
  const { sessao, entrar } = useAuth();
  const navegar = useNavigate();
  const [email, setEmail] = useState("aluno@ava.ufal.br");
  const [senha, setSenha] = useState("123456");
  const [erro, setErro] = useState<string | null>(null);
  const [enviando, setEnviando] = useState(false);

  if (sessao) return <Navigate to="/" replace />;

  const enviar = async (e: FormEvent) => {
    e.preventDefault();
    setEnviando(true);
    setErro(null);
    try {
      await entrar(email, senha);
      navegar("/");
    } catch (err) {
      setErro((err as Error).message);
    } finally {
      setEnviando(false);
    }
  };

  return (
    <div className="tela-acesso">
      <form className="cartao-acesso" onSubmit={enviar}>
        <h1>AVA-MS</h1>
        <p className="subtitulo">Ambiente Virtual de Aprendizagem com microsserviços e agentes de IA</p>
        <label>E-mail<input type="email" value={email} onChange={(e) => setEmail(e.target.value)} required /></label>
        <label>Senha<input type="password" value={senha} onChange={(e) => setSenha(e.target.value)} required /></label>
        {erro && <p className="erro">{erro}</p>}
        <button disabled={enviando}>{enviando ? "Entrando…" : "Entrar"}</button>
        <p className="dica">
          Autenticação via <ServicoBadge servico="usuario-service" porta={8081} /> · <Link to="/cadastro">Criar conta</Link>
        </p>
        <details className="dica">
          <summary>Usuários de teste (senha 123456)</summary>
          aluno@ava.ufal.br · professor@ava.ufal.br · admin@ava.ufal.br
        </details>
      </form>
    </div>
  );
}
