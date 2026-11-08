SET NAMES utf8mb4;  -- o entrypoint conecta em latin1; sem isto os acentos ficam com dupla codificação
USE db_cursos;

-- Transactional Outbox: o evento é gravado na mesma transação da mudança e publicado no Kafka depois.
CREATE TABLE evento_outbox (
  id          BIGINT AUTO_INCREMENT PRIMARY KEY,
  topico      VARCHAR(100) NOT NULL,
  chave       VARCHAR(100) NOT NULL,
  payload     TEXT NOT NULL,
  criado_em   DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  enviado_em  DATETIME(6) NULL,
  INDEX idx_pendentes (enviado_em, id)
);

CREATE TABLE categoria (
  id    BIGINT AUTO_INCREMENT PRIMARY KEY,
  nome  VARCHAR(80) NOT NULL UNIQUE
);

CREATE TABLE curso (
  id            BIGINT AUTO_INCREMENT PRIMARY KEY,
  titulo        VARCHAR(150) NOT NULL,
  descricao     TEXT,
  categoria_id  BIGINT NOT NULL,
  professor_id  BIGINT NOT NULL,              -- referência lógica a db_usuarios
  status        ENUM('RASCUNHO','PUBLICADO','ARQUIVADO') NOT NULL DEFAULT 'RASCUNHO',
  criado_em     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (categoria_id) REFERENCES categoria(id)
);

CREATE TABLE modulo (
  id        BIGINT AUTO_INCREMENT PRIMARY KEY,
  curso_id  BIGINT NOT NULL,
  titulo    VARCHAR(150) NOT NULL,
  ordem     INT NOT NULL,
  FOREIGN KEY (curso_id) REFERENCES curso(id) ON DELETE CASCADE
);

CREATE TABLE aula (
  id         BIGINT AUTO_INCREMENT PRIMARY KEY,
  modulo_id  BIGINT NOT NULL,
  titulo     VARCHAR(150) NOT NULL,
  conteudo   TEXT,                            -- texto usado pelo Agente Tutor
  ordem      INT NOT NULL,
  FOREIGN KEY (modulo_id) REFERENCES modulo(id) ON DELETE CASCADE
);

CREATE TABLE material (
  id       BIGINT AUTO_INCREMENT PRIMARY KEY,
  aula_id  BIGINT NOT NULL,
  tipo     ENUM('PDF','VIDEO','LINK') NOT NULL,
  titulo   VARCHAR(150) NOT NULL,
  url      VARCHAR(500) NOT NULL,
  FOREIGN KEY (aula_id) REFERENCES aula(id) ON DELETE CASCADE
);

INSERT INTO categoria (id, nome) VALUES
 (1, 'Banco de Dados'), (2, 'Arquitetura de Software'), (3, 'Front-end'), (4, 'Inteligência Artificial');

INSERT INTO curso (id, titulo, descricao, categoria_id, professor_id, status, criado_em) VALUES
 (1, 'Banco de Dados Relacional',
     'Modelo Entidade-Relacionamento, normalização e SQL no MySQL: da modelagem conceitual ao banco em produção.',
     1, 2, 'PUBLICADO', '2026-08-01 10:00:00'),
 (2, 'Microsserviços com Spring Cloud',
     'Arquitetura de microsserviços com API Gateway, Eureka, comunicação REST e eventos com Kafka.',
     2, 2, 'PUBLICADO', '2026-08-10 10:00:00'),
 (3, 'React para Iniciantes',
     'Componentes, estado, hooks e consumo de APIs REST para construir interfaces web modernas.',
     3, 2, 'PUBLICADO', '2026-08-20 10:00:00'),
 (4, 'Introdução a Agentes de IA',
     'Modelos de linguagem, uso de ferramentas (tool use) e agentes que apoiam tarefas em sistemas reais.',
     4, 2, 'PUBLICADO', '2026-09-01 10:00:00'),
 (5, 'SQL Avançado no MySQL',
     'Consultas complexas, índices, transações e otimização de desempenho em bancos relacionais MySQL.',
     1, 2, 'PUBLICADO', '2026-09-10 10:00:00');

INSERT INTO modulo (id, curso_id, titulo, ordem) VALUES
 (1, 1, 'Modelagem de dados', 1),
 (2, 1, 'SQL e MySQL', 2),
 (3, 2, 'Fundamentos de microsserviços', 1),
 (4, 3, 'Primeiros passos com React', 1),
 (5, 4, 'Agentes e ferramentas', 1),
 (6, 5, 'Desempenho e consistência', 1);

INSERT INTO aula (id, modulo_id, titulo, ordem, conteudo) VALUES
 (1, 1, 'Modelo Entidade-Relacionamento', 1,
'O Modelo Entidade-Relacionamento (ER) é uma forma de descrever os dados de um sistema no nível conceitual, antes de pensar em tabelas. Ele foi proposto por Peter Chen em 1976.

Uma entidade representa um objeto do mundo real sobre o qual queremos guardar dados, como Aluno, Curso ou Matrícula. Cada entidade possui atributos, por exemplo nome, e-mail e data de nascimento.

O atributo identificador (ou chave) distingue cada ocorrência da entidade. No modelo lógico ele se torna a chave primária (PK) da tabela.

Um relacionamento associa entidades. A cardinalidade indica quantas ocorrências participam: 1:1 (um para um), 1:N (um para muitos) e N:N (muitos para muitos). Um relacionamento N:N, como Aluno e Curso, vira uma tabela associativa no modelo relacional, por exemplo a tabela matrícula.

Na passagem para o modelo relacional, relacionamentos 1:N são implementados com uma chave estrangeira (FK) no lado N, apontando para a chave primária do lado 1.'),
 (2, 1, 'Normalização', 2,
'Normalização é o processo de organizar as tabelas para reduzir redundância e evitar anomalias de inserção, atualização e exclusão.

Primeira Forma Normal (1FN): todos os atributos devem ser atômicos, sem grupos repetidos ou atributos multivalorados. Em vez de colunas telefone1, telefone2, cria-se uma tabela de telefones.

Segunda Forma Normal (2FN): a tabela está na 1FN e todo atributo não chave depende da chave primária inteira. Só faz diferença quando a chave é composta: um atributo que depende de apenas parte da chave deve ir para outra tabela.

Terceira Forma Normal (3FN): a tabela está na 2FN e não há dependências transitivas, isto é, um atributo não chave não pode depender de outro atributo não chave. Exemplo: guardar o nome da categoria dentro da tabela curso cria dependência transitiva (curso -> categoria_id -> nome); a solução é a tabela categoria.

Na prática, a maioria dos sistemas transacionais é projetada até a 3FN. Desnormalizar pode ser aceitável por desempenho, mas deve ser uma decisão consciente.'),
 (3, 2, 'Comandos SQL no MySQL', 1,
'SQL é dividida em sublinguagens. A DDL (Data Definition Language) define estruturas: CREATE TABLE, ALTER TABLE e DROP TABLE.

A DML (Data Manipulation Language) manipula dados: INSERT insere linhas, UPDATE altera, DELETE remove e SELECT consulta.

Uma consulta com JOIN combina linhas de tabelas relacionadas pela chave estrangeira. Exemplo: SELECT c.titulo, cat.nome FROM curso c JOIN categoria cat ON cat.id = c.categoria_id.

No MySQL, o motor InnoDB oferece transações ACID e integridade referencial com FOREIGN KEY. Restrições como NOT NULL, UNIQUE e CHECK ajudam a manter a qualidade dos dados.'),
 (4, 3, 'O que são microsserviços', 1,
'Microsserviços são uma arquitetura em que o sistema é dividido em serviços pequenos e independentes, cada um responsável por uma capacidade de negócio e implantado separadamente.

Cada microsserviço tem seu próprio banco de dados (padrão Database per Service). Assim um serviço não acessa diretamente as tabelas do outro: a integração acontece por APIs REST ou por eventos.

O API Gateway é o ponto único de entrada: recebe as requisições do front-end, autentica e encaminha para o serviço correto. O servidor de descoberta (Eureka) mantém o registro de onde cada serviço está executando.

A comunicação assíncrona com Apache Kafka desacopla os serviços: quem publica um evento não precisa saber quem vai consumi-lo.'),
 (5, 3, 'API Gateway e Eureka na prática', 2,
'No Spring Cloud Gateway, cada rota tem um predicado (por exemplo, Path=/api/cursos/**) e um destino. O destino lb://curso-service indica balanceamento de carga entre as instâncias registradas no Eureka.

Filtros globais permitem validar o token JWT uma única vez no gateway e repassar a identidade do usuário aos serviços em cabeçalhos HTTP.

Para registrar um serviço no Eureka basta a dependência spring-cloud-starter-netflix-eureka-client e o endereço do servidor em eureka.client.service-url.defaultZone.'),
 (6, 4, 'Componentes e estado', 1,
'Em React, a interface é composta por componentes: funções que recebem propriedades (props) e devolvem JSX.

O estado local é criado com o hook useState. Quando o estado muda, o React renderiza o componente novamente.

O hook useEffect executa efeitos colaterais, como buscar dados de uma API REST quando o componente é montado.

Componentes reutilizáveis, como um cartão de curso ou uma barra de progresso, são um exemplo de reuso de software no front-end.'),
 (7, 5, 'Modelos de linguagem e uso de ferramentas', 1,
'Um agente de IA combina um modelo de linguagem com ferramentas. O modelo decide quando chamar uma ferramenta (por exemplo, buscar conteúdo de uma aula) e usa o resultado para responder.

O ciclo do agente é: receber a pergunta, decidir se precisa de uma ferramenta, executar a ferramenta, ler o resultado e então responder ou pedir outra ferramenta.

Limitar o agente ao conteúdo recuperado reduz respostas inventadas. Por isso o tutor do AVA responde apenas com base nos trechos das aulas.'),
 (8, 6, 'Índices e transações', 1,
'Um índice acelera buscas em colunas usadas em WHERE e JOIN, ao custo de mais espaço e de escritas mais lentas. No MySQL, a chave primária do InnoDB é o índice clusterizado.

O comando EXPLAIN mostra o plano de execução de uma consulta e ajuda a descobrir se um índice está sendo usado.

Transações agrupam operações com as propriedades ACID: atomicidade, consistência, isolamento e durabilidade. Use START TRANSACTION, COMMIT e ROLLBACK.');

INSERT INTO material (aula_id, tipo, titulo, url) VALUES
 (1, 'LINK',  'Documentação do MySQL',              'https://dev.mysql.com/doc/'),
 (2, 'VIDEO', 'Revisão de BD (monitoria 2025.1)',   'https://classroom.google.com/'),
 (3, 'LINK',  'Referência de SQL do MySQL 8',       'https://dev.mysql.com/doc/refman/8.0/en/sql-statements.html'),
 (4, 'LINK',  'Microservices (Martin Fowler)',      'https://martinfowler.com/articles/microservices.html'),
 (5, 'LINK',  'Spring Cloud Gateway',               'https://spring.io/projects/spring-cloud-gateway'),
 (6, 'LINK',  'Documentação do React',              'https://react.dev/learn'),
 (7, 'LINK',  'Claude API: tool use',               'https://docs.claude.com');
