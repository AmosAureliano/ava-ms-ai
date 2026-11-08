import { useState } from "react";
import { api, Pagina, Perfil, Usuario } from "../api";
import { PainelServico } from "../ava-ui-components";
import { useDados } from "../useDados";

export function Admin() {
  const usuarios = useDados(() => api<Pagina<Usuario>>("/api/usuarios?tamanho=100"));
  const [erro, setErro] = useState<string | null>(null);

  const alterar = async (u: Usuario, perfil: Perfil) => {
    setErro(null);
    try {
      await api(`/api/usuarios/${u.id}/perfil`, { metodo: "PUT", corpo: { perfil } });
      usuarios.recarregar();
    } catch (e) {
      setErro((e as Error).message);
    }
  };

  return (
    <div className="grade-painel">
      <PainelServico titulo="Usuários" servico="usuario-service" porta={8081} className="largo">
        {(usuarios.erro || erro) && <p className="erro">{usuarios.erro ?? erro}</p>}
        <table className="tabela">
          <thead><tr><th>Nome</th><th>E-mail</th><th>Perfil</th></tr></thead>
          <tbody>
            {usuarios.dados?.itens.map((u) => (
              <tr key={u.id}>
                <td>{u.nome}</td>
                <td>{u.email}</td>
                <td>
                  <select value={u.perfil} onChange={(e) => alterar(u, e.target.value as Perfil)}>
                    <option>ALUNO</option><option>PROFESSOR</option><option>ADMIN</option>
                  </select>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </PainelServico>
      <PainelServico titulo="Microsserviços em execução" servico="discovery-service" porta={8761}>
        <p>O servidor Eureka lista todas as instâncias registradas e o status de cada uma.</p>
        <a className="botao" href="http://localhost:8761" target="_blank" rel="noreferrer">Abrir painel do Eureka</a>
      </PainelServico>
    </div>
  );
}
