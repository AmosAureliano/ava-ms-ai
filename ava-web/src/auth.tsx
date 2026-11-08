import { createContext, ReactNode, useContext, useEffect, useState } from "react";
import { api, salvarSessao, Sessao, sessaoSalva } from "./api";

interface Auth {
  sessao: Sessao | null;
  entrar: (email: string, senha: string) => Promise<Sessao>;
  sair: () => void;
}

const AuthContext = createContext<Auth | null>(null);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [sessao, setSessao] = useState<Sessao | null>(sessaoSalva());

  const sair = () => {
    salvarSessao(null);
    setSessao(null);
  };

  useEffect(() => {
    window.addEventListener("ava:sessao-expirada", sair);
    return () => window.removeEventListener("ava:sessao-expirada", sair);
  }, []);

  const entrar = async (email: string, senha: string) => {
    const s = await api<Sessao>("/api/auth/login", { metodo: "POST", corpo: { email, senha } });
    salvarSessao(s);
    setSessao(s);
    return s;
  };

  return <AuthContext.Provider value={{ sessao, entrar, sair }}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error("useAuth fora do AuthProvider");
  return ctx;
}
