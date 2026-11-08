SET NAMES utf8mb4;  -- o entrypoint conecta em latin1; sem isto os acentos ficam com dupla codificação
USE db_usuarios;

CREATE TABLE usuario (
  id          BIGINT AUTO_INCREMENT PRIMARY KEY,
  nome        VARCHAR(120) NOT NULL,
  email       VARCHAR(150) NOT NULL UNIQUE,
  senha_hash  VARCHAR(100) NOT NULL,              -- BCrypt
  perfil      ENUM('ALUNO','PROFESSOR','ADMIN') NOT NULL,
  criado_em   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Usuários de teste (senha de todos: 123456)
INSERT INTO usuario (id, nome, email, senha_hash, perfil) VALUES
 (1, 'Administrador AVA',  'admin@ava.ufal.br',     '$2a$10$HGvheiP25XyPSgL5GnG/wuei5bO3C3X.L6GfeMHs0TuVj3tn56prS', 'ADMIN'),
 (2, 'Prof. Rafael Lima',  'professor@ava.ufal.br', '$2a$10$HGvheiP25XyPSgL5GnG/wuei5bO3C3X.L6GfeMHs0TuVj3tn56prS', 'PROFESSOR'),
 (3, 'Ana Souza',          'aluno@ava.ufal.br',     '$2a$10$HGvheiP25XyPSgL5GnG/wuei5bO3C3X.L6GfeMHs0TuVj3tn56prS', 'ALUNO'),
 (4, 'Bruno Costa',        'bruno@ava.ufal.br',     '$2a$10$HGvheiP25XyPSgL5GnG/wuei5bO3C3X.L6GfeMHs0TuVj3tn56prS', 'ALUNO'),
 (5, 'Carla Mendes',       'carla@ava.ufal.br',     '$2a$10$HGvheiP25XyPSgL5GnG/wuei5bO3C3X.L6GfeMHs0TuVj3tn56prS', 'ALUNO');
