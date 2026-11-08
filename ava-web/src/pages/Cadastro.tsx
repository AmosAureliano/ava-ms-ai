import { FormEvent, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { api } from "../api";
import { useAuth } from "../auth";

export function Cadastro() {
  const { entrar } = useAuth();
  const navegar = useNavigate();
  const [form, setForm] = useState({ nome: "", email: "", senha: "" });
  const [erro, setErro] = useState<string | null>(null);

  const enviar = async (e: FormEvent) => {
    e.preventDefault();
    setErro(null);
    try {
      await api("/api/auth/cadastro", { metodo: "POST", corpo: form });
      await entrar(form.email, form.senha);
      navegar("/");
    } catch (err) {
      setErro((err as Error).message);
    }
  };

  return (
    <div className="tela-acesso">
      <form className="cartao-acesso" onSubmit={enviar}>
        <h1>Criar conta</h1>
        <label>Nome<input value={form.nome} onChange={(e) => setForm({ ...form, nome: e.target.value })} required /></label>
        <label>E-mail<input type="email" value={form.email} onChange={(e) => setForm({ ...form, email: e.target.value })} required /></label>
        <label>Senha (mín. 8)<input type="password" minLength={8} value={form.senha}
          onChange={(e) => setForm({ ...form, senha: e.target.value })} required /></label>
        <p className="dica">Contas novas são de aluno. Professores recebem o perfil de um administrador.</p>
        {erro && <p className="erro">{erro}</p>}
        <button>Cadastrar</button>
        <p className="dica">Já tem conta? <Link to="/login">Entrar</Link></p>
      </form>
    </div>
  );
}
