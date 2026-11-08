#!/bin/bash
# Um esquema e um usuário por microsserviço (Database per Service). Script .sh, e não .sql,
# para ler a senha de DB_PASSWORD em vez de deixá-la fixa no repositório.
# Sem "set -e": o entrypoint do MySQL pode executar este arquivo com "source", no próprio shell.

if [[ -z "${DB_PASSWORD:-}" || "${DB_PASSWORD}" == *"'"* || "${DB_PASSWORD}" == *"\\"* ]]; then
  echo "DB_PASSWORD precisa estar definida e não pode conter aspas simples nem barra invertida" >&2
  exit 1
fi

sql="SET NAMES utf8mb4;"
for nome in usuarios cursos matriculas avaliacoes notificacoes recomendacoes agente; do
  sql+="
CREATE DATABASE IF NOT EXISTS db_${nome} CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER IF NOT EXISTS 'ava_${nome}'@'%' IDENTIFIED BY '${DB_PASSWORD}';
GRANT SELECT, INSERT, UPDATE, DELETE ON db_${nome}.* TO 'ava_${nome}'@'%';"
done

mysql --protocol=socket -uroot -p"${MYSQL_ROOT_PASSWORD}" --default-character-set=utf8mb4 <<< "${sql}" || exit 1
