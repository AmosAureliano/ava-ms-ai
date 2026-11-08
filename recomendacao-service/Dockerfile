# Contexto de build: raiz do repositório (para instalar o componente commons/ava-commons-py).
FROM python:3.12-slim
WORKDIR /srv
ENV PYTHONDONTWRITEBYTECODE=1 PYTHONUNBUFFERED=1
COPY commons/ava-commons-py /tmp/ava-commons-py
COPY recomendacao-service/requirements.txt .
RUN pip install --no-cache-dir /tmp/ava-commons-py -r requirements.txt && rm -rf /tmp/ava-commons-py
COPY recomendacao-service/app ./app
RUN useradd --system --no-create-home ava
USER ava
EXPOSE 8086
CMD ["uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8086"]
