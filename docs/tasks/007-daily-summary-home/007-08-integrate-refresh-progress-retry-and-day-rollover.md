# Task 007-08 — Integrar Progresso, Retry E Mudança De Dia

## Status

Pendente.

## Objetivo

Integrar pipeline completo de refresh, progresso textual, retry manual, cleanup garantido e atualização de data da Home.

## Contexto

Após ViewModel, coordinator, merge e UI existirem, a fase precisa provar operação fim-a-fim sem concorrência e sem auto-sync.

## Documentos Obrigatórios

- `AGENTS.md`
- `docs/specs/007-daily-summary-home.md`
- `docs/tasks/007-daily-summary-home/007-00-overview.md`
- `007-03`
- `007-04`
- `007-05`
- `007-06`
- `007-07`

## Dependências

- `007-03`
- `007-04`
- `007-05`
- `007-06`
- `007-07`

## Entradas

- `HomeViewModel`
- `HomeScreen`
- coordinator de sync diário
- parser FEA1 promovido
- merge repository

## Arquivos Exatos Permitidos

- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/home/**`
- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/device/DeviceConnectionBootstrap.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/bluetooth/**`
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/model/**`
- `app/src/main/java/br/com/mwconsulting/championcompanion/data/RoomRepositories.kt`
- `app/src/test/java/br/com/mwconsulting/championcompanion/**`
- docs desta fase

## Arquivos Proibidos

- `DeviceManagementViewModel.kt`, salvo ajuste mínimo inevitável e documentado
- `DeviceScreen.kt`
- `AndroidManifest.xml`
- Gradle files
- D1/D3/D4 files futuros
- Health Connect code

## Escopo

- Intent única de refresh.
- Bloqueio de concorrência.
- Estágios.
- Conexão controlada.
- Espera por pacote fresco.
- Timeout.
- Cancelamento.
- Retry.
- Cleanup.
- Encerramento em `finally`.
- Atualização dos metadados.
- Mudança de dia.
- Retorno ao foreground.
- Estado após recriação da tela.

## Fora Do Escopo

- Serviço em background.
- Sincronização automática.
- Health Connect.
- D1/D3/D4.
- Compose test matrix.

## Passos

1. Ligar UI e ViewModel ao mesmo intent de refresh/retry.
2. Garantir que segundo gesto durante operação seja ignorado.
3. Exibir estágios corretos.
4. Garantir sessão/conexão controlada sem scan amplo.
5. Aplicar contrato de pacote fresco.
6. Garantir timeout e cleanup dos collectors.
7. Garantir `finally` para encerrar barra.
8. Reavaliar dia em foreground, meia-noite e novo refresh.
9. Confirmar estado após recriação de tela.
10. Atualizar overview.

## Critérios De Aceite

- Retry usa mesmo pipeline do gesto.
- Barra encerra em sucesso, erro, timeout, disconnect, payload inválido, persistência falha, cancelamento, permissão ausente e Bluetooth desligado.
- `lastImportedAt` só muda após pacote válido e persistência.
- Mudança de dia não altera dia anterior.

## Validação Mínima

```bash
./gradlew assembleDebug
```

## Evidência Manual

- Registrar cenários simulados/inspecionados e limitações.

## Atualização Do Overview

- Marcar `007-08` como concluída.

## Validação Final

- Confirmar que não existe import silencioso ao abrir Home/app.
