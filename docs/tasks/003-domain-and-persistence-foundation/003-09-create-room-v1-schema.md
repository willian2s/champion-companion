# Task 003-09 — Criar Schema Room V1

## Status

Pendente.

## Dependências

- `003-03` a `003-08` concluídas.

## Objetivo

Criar entidades Room 3 V1, converters e relações físicas separadas dos modelos de domínio.

## Pode modificar

- `core/database/**`, schemas exportados e testes de schema.
- Este arquivo.

## Passos

1. Criar database V1 Champion Companion com `androidx.room3`, `SQLiteDriver` e APIs de coroutines; não usar `SupportSQLite`, `Cursor` ou executor. Criar entities para device, perfil, peso, resumo diário, BPM, atividade, importação, intervalo, amostra BPM, calibração, sessão, operação de sync e payload diagnóstico.
2. Persistir datas/horas em forma canônica; converters explícitos para enums/value objects. Nunca armazenar texto formatado de UI.
3. Declarar PKs UUIDv7, `NOT NULL`, FKs e índices conforme relações da spec.
4. Declarar unicidades: device por identificador permitido; daily por device+source+local date; D1 por chave resolvida; D3/D4 por device+source+CRC; calibração por atividade+revisão; saída por payload+tipo+ordinal; sync por agregado+tipo de record.
5. Não criar FK polimórfica para sync. Campos opcionais somente quando ausência é semântica válida.
6. Exportar schema V1 e testar criação, FKs, nulidade, índices e unicidades em Room in-memory/instrumentado.

## Aceite

- Modelo Room 3 não reutiliza classes de domínio.
- Banco inicia em V1, vazio, sem migration/import/acesso MotionLog.
- Nenhuma coluna/modelo armazena rota ou coordenadas.
