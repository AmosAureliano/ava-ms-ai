import { useCallback, useEffect, useState } from "react";

/** Carrega dados de uma função assíncrona e expõe carregando/erro/recarregar. */
export function useDados<T>(carregar: () => Promise<T>, deps: unknown[] = []) {
  const [dados, setDados] = useState<T | null>(null);
  const [erro, setErro] = useState<string | null>(null);
  const [carregando, setCarregando] = useState(true);

  // eslint-disable-next-line react-hooks/exhaustive-deps
  const recarregar = useCallback(() => {
    setCarregando(true);
    setErro(null);
    return carregar()
      .then(setDados)
      .catch((e: Error) => setErro(e.message))
      .finally(() => setCarregando(false));
  }, deps);

  useEffect(() => {
    recarregar();
  }, [recarregar]);

  return { dados, erro, carregando, recarregar, setDados };
}
