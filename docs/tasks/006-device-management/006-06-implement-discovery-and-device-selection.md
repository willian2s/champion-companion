# Task 006-06 — Implementar discovery e seleção de dispositivo

## Status

Concluida em 2026-07-22.

## Objetivo

Implementar scan manual com lista de candidatos Champion, seleção e cancelamento, mantendo resultados efêmeros.

## Contexto

Scanner atual retorna primeiro Champion encontrado. Feature final precisa listar candidatos encontrados sem persistir todos os resultados.

## Documentos Obrigatórios

- `AGENTS.md`
- `docs/specs/006-device-management.md`
- `docs/tasks/006-device-management/006-00-overview.md`
- `docs/tasks/006-device-management/006-05-create-device-management-viewmodel.md`
- `docs/specs/004-native-ble-core.md`
- `docs/protocol/champion-c001.md`

## Dependências

- `006-05`.

## Entradas

- `ChampionBleScanner`.
- `AndroidChampionBleScanner`.
- `ChampionDeviceFilter`.
- ViewModel da task anterior.

## Arquivos Exatos Permitidos

- `app/src/main/java/br/com/mwconsulting/championcompanion/core/bluetooth/ChampionBleScanner.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/bluetooth/ChampionBleContracts.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/bluetooth/android/AndroidChampionBleScanner.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/device/DeviceManagement*.kt`
- testes pequenos em `app/src/test/java/br/com/mwconsulting/championcompanion/core/bluetooth/` ou `feature/device/`
- `docs/tasks/006-device-management/006-00-overview.md`
- `docs/tasks/006-device-management/006-06-implement-discovery-and-device-selection.md`

## Arquivos Proibidos

- `DeviceScreen.kt`, salvo se criar preview estático sem comportamento.
- `core/database/**`
- `data/**`
- Gradle, ADRs, roadmap, baseline e fases concluídas.

## Escopo

- Suportar lista efêmera de candidatos durante janela de scan.
- Deduplicar por identificador de transporte ou chave segura.
- Expor nome e identificador seguro para UI.
- Cancelar scan.
- Impedir scans simultâneos.

## Fora Do Escopo

- Persistir resultados de scan.
- Conectar automaticamente ao encontrar.
- Suporte a outros modelos.

## Passos

1. Ler documentos obrigatórios.
2. Avaliar menor mudança no scanner para coletar múltiplos candidatos.
3. Atualizar ViewModel para receber lista efêmera.
4. Implementar seleção de candidato sem conectar automaticamente.
5. Garantir cancelamento e timeout.
6. Rodar validação mínima.
7. Atualizar overview.

## Critérios De Aceite

- Scan só inicia por ação da ViewModel.
- Lista contém somente Champion compatíveis.
- Seleção não conecta automaticamente.
- Resultados não são persistidos.
- Scans simultâneos são impedidos.

## Validação Mínima

- `./gradlew assembleDebug`

## Evidência Manual Esperada

- Descrição do comportamento de scan/lista.
- Confirmação de que nenhuma operação BLE física foi feita na task, salvo se task explicitamente executada em ambiente manual posterior.

## Atualização Do Overview

- Marcar `006-06` como concluída.
- Registrar mudanças no scanner como risco se houver.

## Validação Final

- Registrar comando e resultado.

## Execucao

- `ChampionBleScanner` passou a expor `scanResults` efemeros via `StateFlow`, permitindo acumular candidatos Champion durante janela unica de scan sem persistencia.
- `AndroidChampionBleScanner` deixou de encerrar no primeiro achado; agora deduplica por chave segura de transporte, atualiza lista incrementalmente e so conclui por timeout, erro ou cancelamento.
- `DeviceManagementViewModel` passou a observar `scanResults`, refletindo candidatos enquanto scan esta em andamento, preservando selecao valida e mantendo conexao apenas por acao explicita.
- Cancelamento de scan agora encerra janela de discovery sem transformar cancelamento do usuario em erro de UI.
- Scans simultaneos seguem bloqueados pelo scanner e pela `ViewModel` via `activeOperation`.

## Evidencia Final

- Comportamento de scan/lista: `AddDevice` inicia scan manual, UI recebe lista efemera deduplicada durante janela de 12 segundos, usuario pode cancelar scan e selecionar candidato sem conectar automaticamente.
- Confirmacao BLE fisica: nenhuma operacao BLE fisica foi executada nesta task; trabalho restrito a implementacao e build local.

## Validacao Final

Comando executado:

```bash
./gradlew assembleDebug
```

Resultado final:

- `BUILD SUCCESSFUL` com configuration cache reutilizado.
