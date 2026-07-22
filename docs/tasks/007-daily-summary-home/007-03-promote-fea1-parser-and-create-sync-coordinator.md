# Task 007-03 — Promover Parser FEA1 E Criar Sync Coordinator

## Status

Pendente.

## Objetivo

Promover parser FEA1 para componente reutilizável único e criar coordinator de sincronização manual de passos sem persistir pacotes espontâneos.

## Contexto

`DevicePocFea1Steps` parseia FEA1 técnico. Fase 07 precisa do mesmo parser para produção e PoC, sem duplicação.

## Documentos Obrigatórios

- `AGENTS.md`
- `docs/specs/007-daily-summary-home.md`
- `docs/tasks/007-daily-summary-home/007-00-overview.md`
- `docs/protocol/ble-command-mapping.md`
- `docs/specs/005-native-ble-poc.md`
- `docs/specs/006-device-management.md`

## Dependências

- `007-02`

## Entradas

- `DevicePocFea1Steps.kt`
- `DeviceConnectionBootstrap.kt`
- `ChampionConnectionManager.kt`
- `ChampionNotificationHub.kt`
- `ChampionTransportSession.kt`
- `ChampionPacket.kt`

## Arquivos Exatos Permitidos

- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/device/DevicePocFea1Steps.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/device/DeviceConnectionBootstrap.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/home/**`
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/bluetooth/**`
- `app/src/test/java/br/com/mwconsulting/championcompanion/**`
- `docs/specs/007-daily-summary-home.md`
- `docs/tasks/007-daily-summary-home/007-00-overview.md`
- `docs/tasks/007-daily-summary-home/007-03-promote-fea1-parser-and-create-sync-coordinator.md`

## Arquivos Proibidos

- `HomeScreen.kt`
- `RoomRepositories.kt`
- `ChampionDaos.kt`
- `ChampionDatabase.kt`
- `AppPreferencesStore.kt`
- `AndroidManifest.xml`
- Gradle files

## Escopo

- Criar parser único FEA1.
- Atualizar PoC para reutilizar parser.
- Criar coordinator sem Compose.
- Coordenador deve aceitar somente pacote fresco da sessão/tentativa ativa.
- Adicionar teste pequeno de parser/pacote fresco quando possível.

## Fora Do Escopo

- Persistência final.
- UI.
- D1/D3/D4.
- Health Connect.
- BLE físico.

## Passos

1. Promover parser mantendo regras de 4 bytes, header `0x01`, 24-bit LE e cópia defensiva.
2. Remover duplicação lógica da PoC.
3. Criar contrato do coordinator com `refreshAttemptId`, `startedAt`, `sessionId`.
4. Reusar sessão atual ou `DeviceConnectionBootstrap` quando necessário.
5. Aguardar FEA1 por timeout explícito.
6. Garantir cleanup de collectors temporários.
7. Atualizar overview.

## Critérios De Aceite

- Existe um parser FEA1 único.
- Pacote anterior ao refresh não conclui tentativa.
- Pacote de sessão errada não conclui tentativa.
- Coordinator não persiste nada sozinho fora do pipeline explícito.

## Validação Mínima

```bash
./gradlew assembleDebug
```

## Evidência Manual

- Descrever onde parser ficou e como PoC reutiliza.

## Atualização Do Overview

- Marcar `007-03` como concluída.

## Validação Final

- Confirmar que não foi criado collector permanente salvando FEA1.
