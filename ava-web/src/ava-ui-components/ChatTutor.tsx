import { FormEvent, useEffect, useRef, useState } from "react";
import { api, MensagemTutor } from "../api";

export function ChatTutor({ cursoId }: { cursoId: number }) {
  const [mensagens, setMensagens] = useState<MensagemTutor[]>([]);
  const [texto, setTexto] = useState("");
  const [enviando, setEnviando] = useState(false);
  const [erro, setErro] = useState<string | null>(null);
  const fim = useRef<HTMLDivElement>(null);

  useEffect(() => {
    setMensagens([]);
    api<MensagemTutor[]>(`/api/tutor/conversas/${cursoId}`).then(setMensagens).catch(() => setMensagens([]));
  }, [cursoId]);

  useEffect(() => {
    fim.current?.scrollIntoView({ block: "nearest" });
  }, [mensagens]);

  const enviar = async (e: FormEvent) => {
    e.preventDefault();
    const pergunta = texto.trim();
    if (pergunta.length < 2) return;
    setEnviando(true);
    setErro(null);
    setMensagens((m) => [...m, { papel: "ALUNO", texto: pergunta }]);
    setTexto("");
    try {
      const r = await api<{ resposta: string }>("/api/tutor/perguntar", { metodo: "POST", corpo: { cursoId, pergunta } });
      setMensagens((m) => [...m, { papel: "TUTOR", texto: r.resposta }]);
    } catch (err) {
      setErro((err as Error).message);
    } finally {
      setEnviando(false);
    }
  };

  return (
    <div className="chat">
      <div className="chat-mensagens">
        {mensagens.length === 0 && (
          <p className="vazio">Pergunte ao tutor sobre o conteúdo do curso. Ex.: “o que é a terceira forma normal?”</p>
        )}
        {mensagens.map((m, i) => (
          <div key={i} className={`chat-msg ${m.papel === "ALUNO" ? "eu" : "tutor"}`}>
            {m.texto}
          </div>
        ))}
        {enviando && <div className="chat-msg tutor digitando">O tutor está consultando as aulas…</div>}
        <div ref={fim} />
      </div>
      {erro && <p className="erro">{erro}</p>}
      <form className="chat-form" onSubmit={enviar}>
        <input value={texto} onChange={(e) => setTexto(e.target.value)} placeholder="Pergunte ao tutor sobre o conteúdo do curso"
          disabled={enviando} />
        <button disabled={enviando || texto.trim().length < 2}>Enviar</button>
      </form>
    </div>
  );
}
