SET NAMES utf8mb4;  -- o entrypoint conecta em latin1; sem isto os acentos ficam com dupla codificação
USE db_notificacoes;

CREATE TABLE notificacao (
  id          BIGINT AUTO_INCREMENT PRIMARY KEY,
  usuario_id  BIGINT NOT NULL,                -- referência lógica a db_usuarios
  tipo        ENUM('MATRICULA','NOTA','CURSO') NOT NULL,
  mensagem    VARCHAR(500) NOT NULL,
  lida        BOOLEAN NOT NULL DEFAULT FALSE,
  criada_em   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  evento_id   VARCHAR(100) NULL,              -- id do evento no outbox: descarta reentregas do Kafka
  INDEX idx_usuario (usuario_id, criada_em),
  UNIQUE KEY uk_evento (evento_id)
);

INSERT INTO notificacao (usuario_id, tipo, mensagem) VALUES
 (3, 'MATRICULA', 'Matrícula confirmada em "Banco de Dados Relacional". Bons estudos!');
