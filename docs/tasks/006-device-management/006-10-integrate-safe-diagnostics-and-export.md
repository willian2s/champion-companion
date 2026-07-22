# Task 006-10 — Integrar diagnóstico seguro e exportação

## Status

Concluida em 2026-07-22.

## Objetivo

Adicionar diagnóstico seguro da feature e exportação de relatório sanitizado sem dados de saúde, payload BLE ou identificadores sensíveis.

## Contexto

Fase 05 possui relatório sanitizado da PoC. Fase 06 deve reutilizar a abordagem, mas adaptar para feature final de dispositivo.

## Documentos Obrigatórios

- `AGENTS.md`
- `docs/specs/006-device-management.md`
- `docs/tasks/006-device-management/006-00-overview.md`
- `docs/specs/005-native-ble-poc.md`
- `docs/tasks/005-native-ble-poc/005-10-create-sanitized-cycle-report-and-fixtures.md`
- `docs/decisions/adr-004-operational-privacy-guards.md`

## Dependências

- `006-05`
- `006-08`

## Entradas

- `DevicePocCycleReport.kt`
- `DevicePocReportExporter.kt`
- ViewModel state final.
- `FileProvider` já configurado.

## Arquivos Exatos Permitidos

- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/device/DevicePocReportExporter.kt`
- novos arquivos em `app/src/main/java/br/com/mwconsulting/championcompanion/feature/device/` com prefixo `DeviceDiagnostics` ou `DeviceReport`
- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/device/DeviceScreen.kt`
- testes pequenos de sanitização em `app/src/test/java/br/com/mwconsulting/championcompanion/feature/device/`
- `docs/tasks/006-device-management/006-00-overview.md`
- `docs/tasks/006-device-management/006-10-integrate-safe-diagnostics-and-export.md`

## Arquivos Proibidos

- `core/database/**`
- `data/**`
- `core/bluetooth/android/**`
- Gradle, ADRs, roadmap, baseline e fases concluídas.

## Escopo

- Criar modelo de relatório sanitizado final.
- Mostrar diagnóstico seguro na UI ou painel interno da tela.
- Exportar JSON sanitizado por ação explícita.
- Mascarar identificador do dispositivo.
- Não incluir passos, BPM, payload, bytes, hex, MAC completo, senha ou stack trace.

## Fora Do Escopo

- Diagnóstico avançado da Fase 19.
- Captura de payload.
- Retenção persistente de relatórios.

## Passos

1. Ler documentos obrigatórios.
2. Definir campos permitidos no relatório.
3. Reutilizar exporter da PoC quando possível.
4. Integrar ação `Exportar relatório` na tela final.
5. Adicionar teste pequeno de sanitização se simples.
6. Rodar validação mínima.
7. Atualizar overview.

## Critérios De Aceite

- Diagnóstico não mostra dados sensíveis.
- Export não contém saúde, payload, bytes, hex, MAC completo, senha ou stack trace.
- Export exige ação explícita.
- Relatório usa cache/área privada + `FileProvider`.

## Validação Mínima

- `./gradlew assembleDebug`

## Evidência Manual Esperada

- Amostra descrita dos campos exportados.
- Confirmação de campos proibidos ausentes.

## Atualização Do Overview

- Marcar `006-10` como concluída.

## Validação Final

- Registrar comando e resultado.

## Execucao

- Diagnostico final da feature passou a ser gerado a partir de `DeviceManagementUiState` combinado com snapshot seguro do ambiente Android, sem depender de Room, payload BLE, bytes crus, MAC ou dados de saude.
- Foi criado `DeviceDiagnosticsReport` com JSON sanitizado contendo apenas versao do app, Android, suporte BLE, Bluetooth ativo, permissoes, estado da tela/conexao, contagem de candidatos, retry, categoria de erro e dispositivo associado com identificador mascarado.
- `DeviceScreen` agora abre painel local de diagnostico seguro e exporta relatorio JSON somente por acao explicita do usuario, gravando em `cacheDir/device-reports/` e compartilhando via `FileProvider`.
- Teste pequeno de sanitizacao cobre ausencia de nome pessoalizado, chave de transporte bruta e termos proibidos como `payload`, `bytes`, `hex`, `mac`, `bpm`, `steps` e coordenadas.

## Evidencia Final

- Amostra de campos exportados: `captured_at`, `app_version`, `android_api_level`, `handset_model`, `ble_supported`, `bluetooth_enabled`, `granted_permissions`, `missing_permissions`, `screen_state`, `connection_state`, `candidate_count`, `associated_device.safe_identifier`, `last_error_category`.
- Confirmacao de campos proibidos ausentes: relatorio nao inclui nome do dispositivo associado, `platformIdentifier` cru, MAC completo, payload BLE, bytes, hex, senha, passos, BPM, stack trace ou coordenadas.

## Validacao Final

Comando executado:

```bash
./gradlew assembleDebug
```

Resultado final:

- `BUILD SUCCESSFUL` com configuration cache reutilizado.
