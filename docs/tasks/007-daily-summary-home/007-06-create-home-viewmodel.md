# Task 007-06 — Criar HomeViewModel

## Status

Concluida em 2026-07-22.

## Objetivo

Criar `HomeViewModel` como autoridade de estado da Home, dados do dia e refresh.

## Contexto

Home atual usa `DeviceManagementViewModel` apenas para status do relógio e não observa resumo diário.

## Documentos Obrigatórios

- `AGENTS.md`
- `docs/specs/007-daily-summary-home.md`
- `docs/tasks/007-daily-summary-home/007-00-overview.md`
- `007-02`, `007-04`, `007-05`

## Dependências

- `007-02`
- `007-04`
- `007-05`

## Entradas

- Contratos da Home.
- Repositories de resumo, device, preferências e dados pessoais mínimos.
- Coordinator da task `007-03` se já disponível.

## Arquivos Exatos Permitidos

- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/home/HomeFeatureContract.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/home/HomeViewModel.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/data/RepositoryModule.kt`
- `app/src/test/java/br/com/mwconsulting/championcompanion/**`
- docs desta fase

## Arquivos Proibidos

- `HomeScreen.kt`, salvo ajuste mínimo para compilação de tipo público
- `DeviceScreen.kt`
- `DeviceManagementViewModel.kt`
- DAOs e database
- `AndroidManifest.xml`
- Gradle files

## Escopo

- Observar resumo do dia.
- Observar dispositivo associado.
- Observar meta diária.
- Derivar progresso, origem, última atualização e estados vazios.
- Controlar `isRefreshing`, stage, erro e último sucesso.
- Ignorar refresh concorrente.
- Não iniciar sync automático no init.

## Fora Do Escopo

- UI pull-to-refresh.
- BLE físico.
- D1/D3/D4.
- Health Connect.

## Passos

1. Criar `@HiltViewModel` da Home.
2. Combinar flows necessários.
3. Implementar intent de refresh chamando coordinator sem concorrência.
4. Garantir `try/finally` para estado de refresh.
5. Manter resumo anterior visível durante operação.
6. Adicionar testes pequenos de concorrência/progresso se prático.
7. Atualizar overview.

## Critérios De Aceite

- ViewModel controla refresh.
- Nenhum sync automático no init.
- Dados anteriores não são zerados durante refresh.
- Refresh concorrente ignorado.

## Validação Mínima

```bash
./gradlew assembleDebug
```

## Evidência Manual

- Descrever combinações de estado suportadas.

## Evidência Manual

- `HomeViewModel` foi criado em `feature/home/HomeViewModel.kt` como `@HiltViewModel` e implementa `HomeFeatureContract`.
- ViewModel combina:
  - resumo do dia observado;
  - dispositivo associado;
  - meta diária;
  - sessões pendentes recuperáveis;
  - estado BLE atual;
  - estado de refresh interno.
- Nenhum sync automático foi iniciado no `init`; `Load` só reavalia `observedDate` e snapshot do ambiente BLE.
- `Refresh` e `Retry` chamam mesmo pipeline e ignoram concorrência via `Mutex` + guarda de `isRefreshing`.
- Durante refresh, resumo anterior continua visível porque `summary` vem sempre do flow do repository e não é zerado ao iniciar operação.
- Combinações de estado suportadas:
  - `Loading`: estado inicial antes da primeira combinação de flows;
  - `Ready`: resumo existente, com ou sem refresh em andamento;
  - `Empty`: sem resumo, com dispositivo e pré-requisitos OK;
  - `DeviceRequired`: sem resumo e sem relógio associado;
  - `PermissionsRequired`: pré-requisitos de permissão ausentes e sem resumo pronto;
  - `BluetoothDisabled`: Bluetooth desligado e sem resumo pronto;
  - `RefreshFailed`: erro seguro após tentativa explícita, preservando resumo anterior quando houver.
- `RepositoryModule` agora também faz binding de `DailyStepsSyncCoordinator` para `ChampionDailyStepsSyncCoordinator`.
- Nenhum teste novo foi adicionado ou executado nesta task por orientação explícita do usuário para não investir em testes agora.

## Atualização Do Overview

- Marcar `007-06` como concluída.

## Validação Final

- Confirmar que UI não precisaria estado paralelo para refresh.
