"""Cliente mínimo do Eureka via API REST.

Registra o serviço Python no mesmo servidor de descoberta usado pelos serviços Spring,
envia heartbeats a cada 30 s e descobre o endereço de outros serviços pelo nome.
Assim o API Gateway encaminha /api/... para este serviço com lb://<nome>, como faz com os serviços Java.
"""
import asyncio
import logging
import random
import socket
import time

import httpx

log = logging.getLogger("eureka")

CACHE_S = 30


class EurekaClient:
    def __init__(self, servidor: str, app: str, porta: int, host: str | None = None):
        self.servidor = servidor.rstrip("/")
        self.app = app.upper()
        self.porta = porta
        self.host = host or socket.gethostname()
        try:
            self.ip = socket.gethostbyname(socket.gethostname())
        except OSError:
            self.ip = "127.0.0.1"
        self.instance_id = f"{self.host}:{app}:{porta}"
        self._tarefa: asyncio.Task | None = None
        self._cache: dict[str, tuple[float, list[str]]] = {}

    def _instancia(self) -> dict:
        base = f"http://{self.host}:{self.porta}"
        return {"instance": {
            "instanceId": self.instance_id,
            "hostName": self.host,
            "app": self.app,
            "ipAddr": self.ip,
            "vipAddress": self.app.lower(),
            "secureVipAddress": self.app.lower(),
            "status": "UP",
            "port": {"$": self.porta, "@enabled": "true"},
            "securePort": {"$": 443, "@enabled": "false"},
            "homePageUrl": f"{base}/",
            "statusPageUrl": f"{base}/health",
            "healthCheckUrl": f"{base}/health",
            "dataCenterInfo": {"@class": "com.netflix.appinfo.InstanceInfo$DefaultDataCenterInfo", "name": "MyOwn"},
            "leaseInfo": {"renewalIntervalInSecs": 30, "durationInSecs": 90},
        }}

    async def _registrar(self, cliente: httpx.AsyncClient) -> None:
        r = await cliente.post(f"{self.servidor}/apps/{self.app}", json=self._instancia())
        r.raise_for_status()
        log.info("Registrado no Eureka como %s (%s)", self.app, self.instance_id)

    async def _ciclo(self) -> None:
        """Registra (com novas tentativas) e mantém o heartbeat enquanto o serviço estiver no ar."""
        registrado = False
        async with httpx.AsyncClient(timeout=5) as cliente:
            while True:
                try:
                    if not registrado:
                        await self._registrar(cliente)
                        registrado = True
                    else:
                        r = await cliente.put(f"{self.servidor}/apps/{self.app}/{self.instance_id}")
                        if r.status_code == 404:          # o servidor esqueceu a instância: registra de novo
                            registrado = False
                            continue
                except httpx.HTTPError as e:
                    log.warning("Eureka indisponível (%s); nova tentativa em breve", e)
                    registrado = False
                await asyncio.sleep(30 if registrado else 5)

    def iniciar(self) -> None:
        self._tarefa = asyncio.create_task(self._ciclo())

    async def parar(self) -> None:
        if self._tarefa:
            self._tarefa.cancel()
        try:
            async with httpx.AsyncClient(timeout=3) as cliente:
                await cliente.delete(f"{self.servidor}/apps/{self.app}/{self.instance_id}")
        except httpx.HTTPError:
            pass

    def url_de(self, app: str) -> str:
        """Endereço base de uma instância UP do serviço informado (balanceamento aleatório simples)."""
        return random.choice(self._instancias_ativas(app.upper()))

    def _instancias_ativas(self, app: str) -> list[str]:
        """Consulta o Eureka no máximo a cada CACHE_S segundos por serviço, como o cache do cliente Java."""
        agora = time.monotonic()
        em_cache = self._cache.get(app)
        if em_cache and agora - em_cache[0] < CACHE_S:
            return em_cache[1]
        r = httpx.get(f"{self.servidor}/apps/{app}", headers={"Accept": "application/json"}, timeout=5)
        r.raise_for_status()
        instancias = r.json()["application"]["instance"]
        if isinstance(instancias, dict):
            instancias = [instancias]
        ativas = [f"http://{i['ipAddr']}:{i['port']['$']}" for i in instancias if i.get("status") == "UP"]
        if not ativas:
            raise RuntimeError(f"Nenhuma instância UP de {app}")
        self._cache[app] = (agora, ativas)
        return ativas
