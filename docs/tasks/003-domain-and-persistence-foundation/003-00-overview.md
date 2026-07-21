# Task 003-00 — Overview Domínio e Persistência

## Status

Pendente.

## Objetivo

Entregar domínio puro, Room V1 e DataStore para Champion Companion. Banco novo, vazio e independente de MotionLog.

## Leitura obrigatória

- `docs/core/current-project-baseline.md`
- `docs/specs/003-domain-and-persistence-foundation.md`
- `docs/specs/_inputs/phase-03-domain-persistence-direction.md`
- `docs/roadmap/champion-companion-roadmap.md`
- `docs/decisions/adr-001-native-rewrite.md`
- Referências listadas na seção Contexto da spec 003.

## Regras

- Não implementar BLE, parsers reais D0/D1/D3/D4, Health Connect, GPS, serviço, exercício funcional, backup ou migração MotionLog.
- Banco V1 começa vazio. Migrations futuras: somente Champion Companion.
- Separar modelos de domínio, entidades Room, entrada de parser, UI e records externos; usar mapeadores explícitos.
- Domínio não importa Android, Room, Compose, BluetoothGatt, payload binário nem Health Connect.
- Proibir latitude, longitude, altitude por ponto, rota, polyline, segmentos, GPX, TCX, FIT e `ExerciseRoute` em domínio, Room, mapeadores, logs, fixtures e testes.
- Não registrar payloads, identificadores, saúde ou dados pessoais. Auto Backup Android desativado.
- IDs técnicos: UUIDv7. Nunca usar autoincremento como identidade/deduplicação única.
- Cada task atualiza apenas próprio `Status`; `003-15` consolida este checklist.

## Ordem e paralelismo

1. `003-01` primeiro.
2. `003-02` após `003-01`.
3. `003-03` a `003-08` após `003-02`; podem ser paralelas se cada uma limitar arquivos ao agregado.
4. `003-09` após `003-03` a `003-08`.
5. `003-10` e `003-11` após `003-09`; sem editar mesmos arquivos.
6. `003-12` após `003-10` e `003-11`.
7. `003-13` e `003-14` após `003-12`; podem ser paralelas.
8. `003-15` última.

## Checklist

- [x] `003-01` Build, dependências e limites de persistência preparados.
- [x] `003-02` Primitivos de domínio, tempo, IDs, proveniência e invariantes comuns criados.
- [x] `003-03` Dispositivo, perfil, peso e resumo diário modelados.
- [x] `003-04` BPM e sync individual futura modelados.
- [x] `003-05` Atividades D3/D4, importação e revisão modeladas.
- [x] `003-06` Calibração de esteira preserva origem e métricas derivadas.
- [x] `003-07` Sessões iniciadas pelo app e recuperação modeladas.
- [x] `003-08` Diagnóstico, validade de parser, retenção e reprocessamento modelados.
- [x] `003-09` Schema Room V1, entidades, converters, FKs e índices criados.
- [x] `003-10` DAOs e consultas requeridas criados.
- [x] `003-11` DataStore, Hilt e contratos de repository criados.
- [x] `003-12` Mapeadores, repositories e transações implementados.
- [x] `003-13` Testes JVM de domínio, IDs, mapeadores e regras criados.
- [x] `003-14` Testes Room e guarda automatizada de privacidade criados.
- [x] `003-15` Validação final concluída.

## Aceite da fase

- Room V1 próprio, DataStore para preferências simples e Auto Backup desativado.
- Estados, deduplicação, tempo, calibração, sessões, sync futuro e reprocessamento não geram contradições ou perdas de edição.
- Nenhuma dependência, acesso, migração ou compatibilidade MotionLog; nenhuma persistência de rota.
