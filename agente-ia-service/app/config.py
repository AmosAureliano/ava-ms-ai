import os

PORTA = int(os.getenv("PORT", "8087"))
EUREKA_URL = os.getenv("EUREKA_URL", "http://localhost:8761/eureka")
INSTANCE_HOST = os.getenv("INSTANCE_HOST", "localhost")

# Sem chave, o tutor funciona em "modo sem IA": devolve os trechos de aula mais relevantes.
ANTHROPIC_API_KEY = os.getenv("ANTHROPIC_API_KEY", "").strip()
MODELO = os.getenv("ANTHROPIC_MODEL", "claude-sonnet-5-5")

DB = {
    "host": os.getenv("DB_HOST", "localhost"),
    "port": int(os.getenv("DB_PORT", "3306")),
    "user": os.getenv("DB_USER", "ava_agente"),
    "password": os.environ["DB_PASSWORD"],
    "database": os.getenv("DB_NAME", "db_agente"),
}

MAX_PASSOS_AGENTE = 5
MENSAGENS_DE_HISTORICO = 10

# Cota por aluno: limita o custo da API da Anthropic mesmo com o limite por minuto do gateway.
COTA_DIARIA_PERGUNTAS = 50
