SET NAMES utf8mb4;  -- o entrypoint conecta em latin1; sem isto os acentos ficam com dupla codificação
USE db_matriculas;

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

CREATE TABLE matricula (
  id           BIGINT AUTO_INCREMENT PRIMARY KEY,
  aluno_id     BIGINT NOT NULL,               -- referência lógica a db_usuarios
  curso_id     BIGINT NOT NULL,               -- referência lógica a db_cursos
  total_aulas  INT NOT NULL,
  status       ENUM('ATIVA','CONCLUIDA','CANCELADA') NOT NULL DEFAULT 'ATIVA',
  data         DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_aluno_curso (aluno_id, curso_id)
);

CREATE TABLE aula_concluida (
  matricula_id    BIGINT NOT NULL,
  aula_id         BIGINT NOT NULL,              -- referência lógica a db_cursos.aula
  data_conclusao  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (matricula_id, aula_id),
  FOREIGN KEY (matricula_id) REFERENCES matricula(id) ON DELETE CASCADE
);

-- Ana (3) faz Banco de Dados; Bruno (4) fez BD e Microsserviços; Carla (5) fez Microsserviços e Agentes de IA
INSERT INTO matricula (id, aluno_id, curso_id, total_aulas, data) VALUES
 (1, 3, 1, 3, '2026-09-02 09:00:00'),
 (2, 4, 1, 3, '2026-09-03 09:00:00'),
 (3, 4, 2, 2, '2026-09-05 09:00:00'),
 (4, 5, 2, 2, '2026-09-06 09:00:00'),
 (5, 5, 4, 1, '2026-09-08 09:00:00');

INSERT INTO aula_concluida (matricula_id, aula_id) VALUES (1, 1), (2, 1), (2, 2), (3, 4);
