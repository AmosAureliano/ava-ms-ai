# AVA-MS: Ambiente Virtual de Aprendizagem com Microsserviços, Componentes de Software e Agentes de IA

Este é um AVA em que professores publicam cursos, aulas e atividades, e alunos se matriculam, estudam, entregam atividades, recebem recomendações e tiram dúvidas com um tutor de IA.

## Arquitetura

```
             ava-web (React, :5173)
                    │  HTTPS + JWT
                    ▼
          api-gateway (Spring Cloud Gateway, :8080) ──consulta──► discovery-service (Eureka, :8761)
                    │  lb://<serviço>
  ┌──────────┬──────┴─────┬────────────┬──────────────┬──────────────────┬────────────────┐
usuario    curso      matricula    avaliacao    notificacao     recomendacao        agente-ia
 :8081     :8082        :8083        :8084         :8085        (Python) :8086    (Python) :8087
   │         │            │            │             │                │                 │
db_usuarios db_cursos db_matriculas db_avaliacoes db_notificacoes db_recomendacoes   db_agente
                         └──────── eventos (Apache Kafka) ────────┘
```

- **Banco de dados por serviço** (padrão *Database per Service*): um servidor MySQL 8 com um esquema e um usuário por microsserviço. Referências entre serviços são lógicas, por id.
- **Comunicação síncrona:** REST pelo gateway (`lb://nome-do-servico`, endereço resolvido no Eureka) e OpenFeign entre serviços.
- **Comunicação assíncrona:** Kafka, nos tópicos `matricula.criada`, `aula.concluida`, `entrega.avaliada` e `curso.publicado`. Os eventos usam o padrão *Transactional Outbox*: são gravados na tabela `evento_outbox` na mesma transação da mudança e publicados depois. A entrega é "pelo menos uma vez", e os consumidores descartam repetidos pelo cabeçalho `ava-evento-id`.
- **Resiliência:** as chamadas OpenFeign têm timeout e circuit breaker (Resilience4j).

### Segurança

- **Identidade:** o JWT é validado no gateway e de novo em cada microsserviço, pelo `IdentidadeFilter` do `ava-commons-web` (Java) e pelo `ava_commons.seguranca` (Python). Nenhum serviço confia em cabeçalhos de identidade.
- **Rede:** só o portal, o gateway e o painel do Eureka são publicados, e apenas em `127.0.0.1`. Microsserviços, MySQL e Kafka ficam na rede interna do Docker. Os endpoints usados entre serviços ficam em `/internal/**`, que o gateway não roteia.
- **Autorização:** rascunhos só aparecem para o professor dono. O gabarito só aparece para o professor da atividade. Para ver atividades, entregar e usar o tutor, o aluno precisa estar matriculado no curso.
- **Cadastro:** o cadastro público cria apenas alunos. O perfil de professor é concedido por um administrador.
- **Abuso:** o gateway limita requisições em login, cadastro e agentes de IA. O tutor tem cota diária por aluno.
- **Front-end:** links de materiais só aceitam `http(s)`. O nginx envia CSP e outros cabeçalhos de segurança.
- **Segredos:** nenhum segredo tem valor padrão. Sem eles, os serviços não sobem. Os contêineres rodam sem root.

### Componentes de software reutilizáveis

| Componente | Interface fornecida | Usado por |
| --- | --- | --- |
| `commons/ava-commons-security` | `JwtService`, `UsuarioAutenticado`, `Perfil`, `CabecalhosAva` | api-gateway, usuario-service e, indiretamente, todos os serviços Java |
| `commons/ava-commons-web` | `ApiErro`, `GlobalExceptionHandler`, `PaginaResposta<T>`, `Acesso`, exceções de negócio | todos os serviços Java |
| `commons/ava-commons-events` | `EventoPublisher`, `EventoLeitor`, `Topicos`, records de eventos | curso, matricula, avaliacao e notificacao |
| `ava-web/src/ava-ui-components` | `ServicoBadge`, `PainelServico`, `CursoCard`, `ProgressoBar`, `ChatTutor` | todas as telas do front-end |

Os componentes Java são **auto-configurados**: basta adicionar a dependência no `pom.xml`, sem nenhuma configuração extra no serviço.

### Microsserviços

| Serviço | Linguagem / framework | Banco | Responsabilidade |
| --- | --- | --- | --- |
| discovery-service | Java 21, Spring Cloud Netflix Eureka | — | registro e descoberta de serviços |
| api-gateway | Java 21, Spring Cloud Gateway | — | entrada única, rotas, JWT, CORS |
| usuario-service | Java 21, Spring Boot 3.3 | db_usuarios | cadastro, login (JWT), perfis |
| curso-service | Java 21, Spring Boot 3.3 | db_cursos | cursos, módulos, aulas, materiais, busca de trechos para o tutor |
| matricula-service | Java 21, Spring Boot 3.3, OpenFeign | db_matriculas | matrículas e progresso |
| avaliacao-service | Java 21, Spring Boot 3.3, OpenFeign | db_avaliacoes | atividades, questionários com correção automática, notas |
| notificacao-service | Java 21, Spring Boot 3.3, Spring Kafka | db_notificacoes | avisos gerados a partir de eventos |
| recomendacao-service | Python 3.12, FastAPI, scikit-learn | db_recomendacoes | recomendação híbrida (TF-IDF + colaborativa) |
| agente-ia-service | Python 3.12, FastAPI, SDK Anthropic | db_agente | Agente Tutor e Agente Gerador de Questões |
| ava-web | TypeScript, React 18, Vite | — | interface web |

## Como executar (Docker)

**Pré-requisitos:** Docker Desktop (ou Docker 24+ com Compose). Não é preciso ter Java, Maven, Node ou Python instalados: o build acontece dentro dos contêineres.

```bash
git clone https://github.com/AmosAureliano/ava-ms-ai.git
cd ava-ms-ai
cp .env.example .env          # no Windows (PowerShell): copy .env.example .env
# preencha MYSQL_ROOT_PASSWORD, DB_PASSWORD e JWT_SECRET (o .env.example mostra como gerar valores aleatórios)
docker compose up -d --build
```

O primeiro build baixa as dependências e leva alguns minutos. Depois disso:

| O quê | Endereço |
| --- | --- |
| Portal (front-end) | http://localhost:5173 |
| Painel do Eureka (microsserviços em execução) | http://localhost:8761 |
| API Gateway | http://localhost:8080 |

O Swagger dos serviços Java (`/swagger-ui.html`) e a documentação dos serviços Python (`/docs`) ficam só na rede interna. Para consultá-los durante o desenvolvimento, publique temporariamente a porta do serviço, por exemplo com `ports: ["127.0.0.1:8082:8082"]` em um `docker-compose.override.yml`, que não deve ser versionado.

Aguarde cerca de 1 minuto até todos os serviços aparecerem como **UP** no Eureka.

### Usuários de teste (senha `123456`)

| E-mail | Perfil |
| --- | --- |
| aluno@ava.ufal.br | Aluno (Ana) |
| bruno@ava.ufal.br, carla@ava.ufal.br | Alunos com histórico (alimentam a recomendação colaborativa) |
| professor@ava.ufal.br | Professor, dono dos 5 cursos de exemplo |
| admin@ava.ufal.br | Administrador |

Esses usuários existem só nos dados de exemplo. Novas contas são criadas como aluno, com senha de no mínimo 8 caracteres.

### Agente de IA

Coloque sua chave em `ANTHROPIC_API_KEY` no `.env` e rode `docker compose up -d agente-ia-service`.

- **Sem chave:** o tutor funciona em **modo sem IA** e devolve os trechos das aulas mais relevantes para a pergunta. A geração de questões fica indisponível.
- **Com chave:** o Agente Tutor usa *tool use*. O modelo decide quando chamar `buscar_conteudo` (curso-service) e `consultar_progresso` (matricula-service) e responde com base no conteúdo das aulas. O modelo é configurável em `ANTHROPIC_MODEL`.

## Desenvolvimento sem Docker

1. Suba só a infraestrutura: `docker compose up -d mysql kafka`. MySQL e Kafka não são publicados no host, então publique as portas em um `docker-compose.override.yml` local (`127.0.0.1:3306:3306`, por exemplo). O Kafka anuncia `kafka:9092`; para usá-lo a partir do host, adicione um listener `localhost`.
2. Exporte `JWT_SECRET`, `SPRING_DATASOURCE_PASSWORD` (Java) e `DB_PASSWORD` (Python) com os valores do `.env`.
3. Java: `mvn clean install -DskipTests`. Depois, em cada serviço: `mvn -pl curso-service spring-boot:run`. Comece pelo `discovery-service`.
4. Python (o `requirements-dev.txt` instala o componente `commons/ava-commons-py`):
   ```bash
   cd recomendacao-service
   pip install -r requirements-dev.txt
   uvicorn app.main:app --port 8086
   ```
5. Front-end: `cd ava-web && npm ci && npm run dev`.

Os valores padrão dos `application.yml` e `config.py` apontam para `localhost`.

## Testes

```bash
mvn test                                                      # testes Java (inclui gateway, filtro JWT e autorização)
cd commons/ava-commons-py && pytest                           # JWT e Eureka do componente Python
cd recomendacao-service && pytest                             # recomendador híbrido e API
cd agente-ia-service && pytest                                # agentes com modelo simulado e API
cd ava-web && npm run build                                   # checagem de tipos + build
```

## Estrutura do repositório

```
ava-ms-ai/
├── commons/
│   ├── ava-commons-security/     # componente JWT
│   ├── ava-commons-web/          # componente de erros, paginação e controle de perfil
│   ├── ava-commons-events/       # componente de eventos Kafka (outbox)
│   └── ava-commons-py/           # componente Python: Eureka, erros e JWT
├── discovery-service/
├── api-gateway/
├── usuario-service/
├── curso-service/
├── matricula-service/
├── avaliacao-service/
├── notificacao-service/
├── recomendacao-service/         # Python / FastAPI
├── agente-ia-service/            # Python / FastAPI
├── ava-web/                      # React + ava-ui-components
├── infra/mysql/init/             # esquemas, usuários e dados de exemplo
├── Dockerfile.java               # imagem única dos módulos Java (arg SERVICE)
├── docker-compose.yml
└── .env.example
```

## Problemas comuns

- **Um serviço não aparece no Eureka:** veja o log com `docker compose logs -f curso-service`. Os serviços se reconectam sozinhos quando MySQL, Kafka e Eureka terminam de subir.
- **Banco com dados antigos:** os scripts de `infra/mysql/init` só rodam na primeira vez. Para recriar: `docker compose down -v && docker compose up -d --build`.
- **Porta ocupada:** altere o lado esquerdo do mapeamento `ports` no `docker-compose.yml` (por exemplo `127.0.0.1:8081:8080` para o gateway). Nesse caso, ajuste também `VITE_API_URL` do `ava-web` e `AVA_FRONTEND_URL` do gateway.
- **`defina ... no .env`:** falta um segredo obrigatório no `.env`. Veja o `.env.example`.
