# Task 008-04 - Implementar coordinator de medicao manual

## Status

Planejada.

## Objetivo

Implementar coordinator de produto da medicao manual, separado da UI e reaproveitando componentes promovidos D0/F002, com gate BLE, timeout de 60 segundos, progresso monotonic, cancelamento e cleanup completos.

## Contexto

`DevicePocCoordinator` ja prova viabilidade tecnica, mas mistura concerns debug e fluxo de produto. Fase 08 precisa coordinator proprio sem copiar mecanicamente a classe inteira da PoC.

## Documentos Obrigatorios

- `AGENTS.md`
- `docs/specs/008-manual-heart-rate.md`
- `docs/tasks/008-manual-heart-rate/008-00-overview.md`
- `docs/tasks/008-manual-heart-rate/008-02-define-manual-heart-rate-contracts-state-and-operation-gate.md`
- `docs/tasks/008-manual-heart-rate/008-03-promote-d0-f002-protocol-and-estimator.md`
- ADR-007
- ADR-008

## Dependencias

- `008-02`
- `008-03`

## Entradas

- `DeviceConnectionBootstrap.kt`
- `ChampionConnectionManager.kt`
- `ChampionTransportSession.kt`
- `ChampionNotificationHub.kt`
- componentes promovidos de D0/F002

## Arquivos Exatos Permitidos

- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/health/**`
- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/device/DeviceConnectionBootstrap.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/device/DeviceManagementViewModel.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/bluetooth/**`
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/model/**`
- `app/src/test/java/br/com/mwconsulting/championcompanion/**`
- `docs/specs/008-manual-heart-rate.md`
- `docs/tasks/008-manual-heart-rate/008-00-overview.md`
- `docs/tasks/008-manual-heart-rate/008-04-implement-manual-heart-rate-coordinator.md`

## Arquivos Proibidos

- `HealthScreen.kt`
- `HomeScreen.kt`
- `RoomRepositories.kt`
- `ChampionDaos.kt`
- `ChampionDatabase.kt`
- `AndroidManifest.xml`
- Gradle files

## Escopo

- Criar coordinator de medicao manual.
- Implementar aquisicao/liberacao do gate.
- Reusar sessao ativa compativel ou conectar associado.
- Garantir handshake e F002 antes de start.
- Medir 60s com relogio monotonic.
- Expor progresso, remaining, BPM ao vivo e qualidade efemera.
- Tentar `D0 00` em conclusao, cancelamento, timeout, erro e background quando seguro.
- Ignorar callbacks tardios.

## Fora Do Escopo

- Persistencia final no repository.
- UI Compose.
- Health Connect.

## Passos

1. Implementar aquisicao atomica do gate com ownership explicito.
2. Reusar `currentSession()` quando associado coincidir; senao usar bootstrap controlado.
3. Garantir `F002_NOTIFY` antes do start.
4. Enviar `D0 01` e registrar inicio monotonic/civil.
5. Coletar D0 e F002 da sessao atual com guards por `bleSessionId + manualMeasurementSessionId`.
6. Rodar ticker monotonic para `elapsed`, `remaining` e `progress`.
7. Implementar stop e cleanup em `finally`.
8. Expor resultado final sem persistir direto no coordinator.
9. Atualizar overview.

## Criterios De Aceite

- `D0 01` so ocorre apos handshake e F002 subscribed.
- `D0 00` e tentado quando seguro em todos caminhos principais.
- Cleanup independe do resultado do stop.
- Callback tardio e ignorado.
- Conflito com operacao BLE foreground vira erro seguro.

## Testes Pequenos Aplicaveis

- callback tardio ignorado;
- gate impede simultaneidade;
- cancelamento nao retorna resultado persistivel;
- stop falho nao bloqueia cleanup;
- timeout fecha sessao sem reaproveitar valor antigo.

## Validacao Minima

```bash
./gradlew assembleDebug
```

## Evidencia Manual

- Resumo do fluxo do coordinator, ownership do gate e regras de cleanup.

## Atualizacao Do Overview

- Marcar `008-04` como concluida.
- Registrar se gate exigira ADR.

## Validacao Final

- Confirmar que coordinator nao depende de Compose nem persiste diretamente em DAO.
