SET NAMES utf8mb4;  -- o entrypoint conecta em latin1; sem isto os acentos ficam com dupla codificação
USE db_recomendacoes;

-- Interações aluno x curso que alimentam a filtragem colaborativa
CREATE TABLE interacao (
  id         BIGINT AUTO_INCREMENT PRIMARY KEY,
  aluno_id   BIGINT NOT NULL,                 -- referência lógica a db_usuarios
  curso_id   BIGINT NOT NULL,                 -- referência lógica a db_cursos
  peso       DECIMAL(6,2) NOT NULL,
  origem     ENUM('MATRICULA','AULA','NOTA') NOT NULL,
  criada_em  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  evento_id  VARCHAR(100) NULL,               -- id do evento no outbox: descarta reentregas do Kafka
  INDEX idx_aluno (aluno_id),
  UNIQUE KEY uk_evento (evento_id)
);

-- Coerente com as matrículas e aulas concluídas de exemplo
INSERT INTO interacao (aluno_id, curso_id, peso, origem) VALUES
 (3, 1, 1.0, 'MATRICULA'), (3, 1, 0.5, 'AULA'),
 (4, 1, 1.0, 'MATRICULA'), (4, 1, 0.5, 'AULA'), (4, 1, 0.5, 'AULA'), (4, 1, 1.0, 'NOTA'),
 (4, 2, 1.0, 'MATRICULA'), (4, 2, 0.5, 'AULA'),
 (5, 2, 1.0, 'MATRICULA'), (5, 4, 1.0, 'MATRICULA');
