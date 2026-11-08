import os

PORTA = int(os.getenv("PORT", "8086"))
EUREKA_URL = os.getenv("EUREKA_URL", "http://localhost:8761/eureka")
INSTANCE_HOST = os.getenv("INSTANCE_HOST", "localhost")
KAFKA_BOOTSTRAP_SERVERS = os.getenv("KAFKA_BOOTSTRAP_SERVERS", "localhost:9092")

DB = {
    "host": os.getenv("DB_HOST", "localhost"),
    "port": int(os.getenv("DB_PORT", "3306")),
    "user": os.getenv("DB_USER", "ava_recomendacoes"),
    "password": os.environ["DB_PASSWORD"],
    "database": os.getenv("DB_NAME", "db_recomendacoes"),
}

# Peso de cada tipo de interação na matriz aluno x curso
PESO_MATRICULA = 1.0
PESO_AULA = 0.5
PESO_NOTA_ALTA = 1.0
NOTA_MINIMA = 7.0
INTERVALO_RETREINO_S = 30
