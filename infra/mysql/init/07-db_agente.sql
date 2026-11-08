SET NAMES utf8mb4;  -- o entrypoint conecta em latin1; sem isto os acentos ficam com dupla codificação
USE db_agente;

CREATE TABLE conversa (
  id         BIGINT AUTO_INCREMENT PRIMARY KEY,
  aluno_id   BIGINT NOT NULL,                 -- referência lógica a db_usuarios
  curso_id   BIGINT NOT NULL,                 -- referência lógica a db_cursos
  criada_em  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_aluno_curso (aluno_id, curso_id)
);

CREATE TABLE mensagem (
  id           BIGINT AUTO_INCREMENT PRIMARY KEY,
  conversa_id  BIGINT NOT NULL,
  papel        ENUM('ALUNO','TUTOR') NOT NULL,
  texto        TEXT NOT NULL,
  criada_em    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (conversa_id) REFERENCES conversa(id) ON DELETE CASCADE,
  INDEX idx_papel_data (papel, criada_em)
);
