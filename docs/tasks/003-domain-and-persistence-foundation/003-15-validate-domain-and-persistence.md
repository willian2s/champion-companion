# Task 003-15 — Validar Domínio e Persistência

## Status

Pendente.

## Dependências

- `003-01` a `003-14` concluídas.

## Objetivo

Consolidar evidências, validar spec 003 e encerrar `003-00`.

## Pode modificar

- Este arquivo.
- `003-00-overview.md` exclusivamente para checklist/status final.

## Passos

1. Revisar modelos, entities, DAOs, mapeadores e repositories contra limites arquiteturais e matriz de requisitos da spec.
2. Confirmar V1 vazia, schema exportado, migrations somente futuras Champion Companion e ausência total de MotionLog.
3. Confirmar estados/invariantes: BPM parcial, D3/D4 pendente, parser parcial/inválido bloqueado, descarte terminal, esteira calibrada, sessão recuperável e reprocessamento idempotente.
4. Confirmar deduplicação, tempo/DST, retenção de 14 dias ou 10 MiB, Auto Backup desligado e ausência de rota/coordenadas.
5. Rodar `./gradlew lint testDebugUnitTest assembleDebug assembleRelease`.
6. Quando Galaxy S22 conectado, rodar testes instrumentados de Room e registrar resultado; se indisponível, registrar bloqueio reproduzível.
7. Registrar comandos, resultados, limitações e evidências; atualizar status próprio e checklist de `003-00`.

## Aceite

- Build completo passa e requisitos têm evidência ou bloqueio explícito/reproduzível.
- Sem BLE, Health Connect, GPS persistido, serviço, parser funcional, backup, MotionLog, rota ou coordenadas.
