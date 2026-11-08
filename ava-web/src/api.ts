export const API_URL = (import.meta.env.VITE_API_URL as string | undefined) ?? "http://localhost:8080";

export type Perfil = "ALUNO" | "PROFESSOR" | "ADMIN";

export interface Sessao { token: string; id: number; nome: string; email: string; perfil: Perfil; }
export interface Pagina<T> { itens: T[]; pagina: number; tamanho: number; total: number; totalPaginas: number; }
export interface Categoria { id: number; nome: string; }
export interface CursoResumo {
  id: number; titulo: string; descricao: string; categoria: string; professorId: number;
  status: "RASCUNHO" | "PUBLICADO" | "ARQUIVADO"; totalAulas: number;
}
export interface Material { id: number; tipo: "PDF" | "VIDEO" | "LINK"; titulo: string; url: string; }
export interface Aula { id: number; moduloId: number; cursoId: number; titulo: string; conteudo: string; ordem: number; materiais: Material[]; }
export interface Modulo { id: number; titulo: string; ordem: number; aulas: Aula[]; }
export interface CursoDetalhe {
  id: number; titulo: string; descricao: string; categoria: Categoria; professorId: number;
  status: CursoResumo["status"]; modulos: Modulo[];
}
export interface Matricula {
  id: number; alunoId: number; cursoId: number; cursoTitulo: string; categoria: string | null;
  status: "ATIVA" | "CONCLUIDA" | "CANCELADA"; totalAulas: number; aulasConcluidas: number; percentual: number;
  data: string; aulasConcluidasIds: number[];
}
export interface AtividadeResumo {
  id: number; cursoId: number; titulo: string; tipo: "QUESTIONARIO" | "TRABALHO"; prazo: string;
  questoes: number; entregue: boolean | null;
}
export interface Alternativa { id?: number; texto: string; correta: boolean | null; }
export interface Questao { id?: number; enunciado: string; peso: number; alternativas: Alternativa[]; }
export interface Atividade {
  id: number; cursoId: number; titulo: string; descricao: string; tipo: AtividadeResumo["tipo"];
  prazo: string; questoes: Questao[];
}
export interface Entrega {
  id: number; atividadeId: number; atividadeTitulo: string; cursoId: number; alunoId: number; resposta: string;
  status: "ENVIADA" | "AVALIADA"; nota: number | null; comentario: string | null; enviadaEm: string;
}
export interface Notificacao { id: number; tipo: string; mensagem: string; lida: boolean; criadaEm: string; }
export interface Recomendacao { cursoId: number; titulo: string; categoria: string; escore: number | null; motivo: string; }
export interface MensagemTutor { papel: "ALUNO" | "TUTOR"; texto: string; criadaEm?: string; }
export interface Usuario { id: number; nome: string; email: string; perfil: Perfil; }

const CHAVE = "ava.sessao";

export function sessaoSalva(): Sessao | null {
  try {
    const s = localStorage.getItem(CHAVE);
    return s ? (JSON.parse(s) as Sessao) : null;
  } catch {
    return null;
  }
}

export function salvarSessao(s: Sessao | null) {
  try {
    if (s) localStorage.setItem(CHAVE, JSON.stringify(s));
    else localStorage.removeItem(CHAVE);
  } catch {
    /* navegação privada: a sessão fica só em memória */
  }
}

export class ErroApi extends Error {
  constructor(public status: number, mensagem: string) {
    super(mensagem);
  }
}

export async function api<T>(caminho: string, opcoes: { metodo?: string; corpo?: unknown } = {}): Promise<T> {
  const sessao = sessaoSalva();
  const resp = await fetch(API_URL + caminho, {
    method: opcoes.metodo ?? "GET",
    headers: {
      ...(opcoes.corpo !== undefined ? { "Content-Type": "application/json" } : {}),
      ...(sessao ? { Authorization: `Bearer ${sessao.token}` } : {}),
    },
    body: opcoes.corpo !== undefined ? JSON.stringify(opcoes.corpo) : undefined,
  }).catch(() => {
    throw new ErroApi(0, "Não foi possível falar com o API Gateway. Ele está no ar?");
  });
  const texto = await resp.text();
  const dados = lerJson(texto);
  if (!resp.ok) {
    if (resp.status === 401 && sessao) window.dispatchEvent(new Event("ava:sessao-expirada"));
    throw new ErroApi(resp.status, dados?.mensagem ?? `Erro ${resp.status}`);
  }
  return dados as T;
}

/** Respostas de erro de um proxy (HTML do nginx, por exemplo) não são JSON. */
function lerJson(texto: string): any {
  if (!texto) return null;
  try {
    return JSON.parse(texto);
  } catch {
    return null;
  }
}

/** Só devolve links http(s); qualquer outro esquema (javascript:, data:...) é descartado. */
export function urlSegura(url: string): string | undefined {
  try {
    const u = new URL(url);
    return u.protocol === "http:" || u.protocol === "https:" ? u.href : undefined;
  } catch {
    return undefined;
  }
}

export const dataHora = (iso: string) =>
  new Date(iso).toLocaleString("pt-BR", { dateStyle: "short", timeStyle: "short" });
