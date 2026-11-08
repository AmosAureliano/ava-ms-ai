SET NAMES utf8mb4;  -- o entrypoint conecta em latin1; sem isto os acentos ficam com dupla codificação
USE db_avaliacoes;

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

CREATE TABLE atividade (
  id            BIGINT AUTO_INCREMENT PRIMARY KEY,
  curso_id      BIGINT NOT NULL,              -- referência lógica a db_cursos
  professor_id  BIGINT NOT NULL,              -- referência lógica a db_usuarios
  titulo        VARCHAR(150) NOT NULL,
  descricao     TEXT,
  tipo          ENUM('QUESTIONARIO','TRABALHO') NOT NULL,
  prazo         DATETIME NOT NULL
);

CREATE TABLE questao (
  id            BIGINT AUTO_INCREMENT PRIMARY KEY,
  atividade_id  BIGINT NOT NULL,
  enunciado     TEXT NOT NULL,
  peso          DECIMAL(5,2) NOT NULL DEFAULT 1,
  FOREIGN KEY (atividade_id) REFERENCES atividade(id) ON DELETE CASCADE
);

CREATE TABLE alternativa (
  id          BIGINT AUTO_INCREMENT PRIMARY KEY,
  questao_id  BIGINT NOT NULL,
  texto       VARCHAR(500) NOT NULL,
  correta     BOOLEAN NOT NULL DEFAULT FALSE,
  FOREIGN KEY (questao_id) REFERENCES questao(id) ON DELETE CASCADE
);

CREATE TABLE entrega (
  id            BIGINT AUTO_INCREMENT PRIMARY KEY,
  atividade_id  BIGINT NOT NULL,
  aluno_id      BIGINT NOT NULL,              -- referência lógica a db_usuarios
  resposta      TEXT,
  nota          DECIMAL(4,2),
  comentario    VARCHAR(1000),
  status        ENUM('ENVIADA','AVALIADA') NOT NULL DEFAULT 'ENVIADA',
  enviada_em    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_atividade_aluno (atividade_id, aluno_id),
  FOREIGN KEY (atividade_id) REFERENCES atividade(id)
);

INSERT INTO atividade (id, curso_id, professor_id, titulo, descricao, tipo, prazo) VALUES
 (1, 1, 2, 'Questionário: Normalização', 'Questões objetivas sobre 1FN, 2FN e 3FN.', 'QUESTIONARIO', '2027-06-30 23:59:00'),
 (2, 1, 2, 'Entrega: Modelo ER do seu projeto', 'Descreva as entidades, atributos e relacionamentos do seu projeto.', 'TRABALHO', '2027-06-30 23:59:00'),
 (3, 2, 2, 'Questionário: API Gateway', 'Conceitos de gateway e descoberta de serviços.', 'QUESTIONARIO', '2027-06-30 23:59:00');

INSERT INTO questao (id, atividade_id, enunciado, peso) VALUES
 (1, 1, 'Qual forma normal elimina dependências transitivas?', 1),
 (2, 1, 'Atributos multivalorados violam qual forma normal?', 1),
 (3, 3, 'Qual componente mantém o registro de onde cada microsserviço está executando?', 1);

INSERT INTO alternativa (questao_id, texto, correta) VALUES
 (1, 'Primeira Forma Normal (1FN)', FALSE), (1, 'Segunda Forma Normal (2FN)', FALSE),
 (1, 'Terceira Forma Normal (3FN)', TRUE),  (1, 'Forma Normal de Boyce-Codd', FALSE),
 (2, '1FN', TRUE), (2, '2FN', FALSE), (2, '3FN', FALSE), (2, 'Nenhuma', FALSE),
 (3, 'API Gateway', FALSE), (3, 'Servidor de descoberta (Eureka)', TRUE), (3, 'Apache Kafka', FALSE), (3, 'MySQL', FALSE);

-- Bruno (4) já respondeu o questionário de normalização
INSERT INTO entrega (atividade_id, aluno_id, resposta, nota, comentario, status) VALUES
 (1, 4, '{"1":3,"2":5}', 10.00, 'Corrigido automaticamente', 'AVALIADA');
