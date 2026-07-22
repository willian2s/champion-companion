# Task 008-06 - Criar HealthViewModel

## Status

Concluída em 2026-07-22.

## Resultado

- `HealthViewModel` foi criado como `@HiltViewModel` e agora é owner do fluxo de produto da tela Saúde.
- ViewModel observa dispositivo associado, última medição válida e recentes manuais pelos repositories, sem iniciar medição no `init` nem no intent `Load`.
- Resultado válido emitido pelo coordinator agora passa por persistência no ViewModel e só então fecha em `Completed`; falha no salvamento vira erro seguro `STORAGE_FAILED`.
- `CancelMeasurement`, `ConfirmExitMeasurement` e `AppMovedToBackground` cancelam medição sem persistir parcial.
- `HealthFeatureContract` ganhou intent explícito para background, mantendo Compose desacoplado de detalhes técnicos da medição.
- Testes pequenos cobrem `Load` sem auto-start, persistência do resultado válido, cancelamento em background sem persistência e conflito de gate como erro seguro.

## Objetivo

Criar `HealthViewModel` como owner do fluxo manual de frequencia cardiaca, do estado da tela Saude e da persistencia do resultado final.

## Contexto

`HealthScreen` atual e placeholder. Fase 08 precisa ViewModel proprio para combinar repository, associado, coordinator manual, gate, lifecycle e historico sem mover logica para Compose.

## Documentos Obrigatorios

- `AGENTS.md`
- `docs/specs/008-manual-heart-rate.md`
- `docs/tasks/008-manual-heart-rate/008-00-overview.md`
- `008-02`
- `008-04`
- `008-05`

## Dependencias

- `008-02`
- `008-04`
- `008-05`

## Entradas

- `HealthFeatureContract.kt`
- `HeartRateRepository`
- `DeviceRepository`
- coordinator manual

## Arquivos Exatos Permitidos

- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/health/HealthFeatureContract.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/health/HealthViewModel.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/data/RepositoryModule.kt`
- `app/src/test/java/br/com/mwconsulting/championcompanion/**`
- `docs/specs/008-manual-heart-rate.md`
- `docs/tasks/008-manual-heart-rate/008-00-overview.md`
- `docs/tasks/008-manual-heart-rate/008-06-create-health-viewmodel.md`

## Arquivos Proibidos

- `HealthScreen.kt`, salvo ajuste minimo de tipo publico para compilar
- `HomeScreen.kt`
- DAOs/database
- `AndroidManifest.xml`
- Gradle files

## Escopo

- Criar `@HiltViewModel` da Saude.
- Observar ultima medicao valida e recentes manuais.
- Controlar start, cancel, retry e confirmacao de saida.
- Persistir resultado final valido via repository.
- Cancelar ao background.
- Restaurar em `Idle` apos morte de processo.

## Fora Do Escopo

- UI Compose final.
- Screen-awake visual.
- Navegacao de confirmacao na raiz.

## Passos

1. Criar `HealthViewModel` e combinar flows de dados historicos.
2. Integrar coordinator manual ao pipeline de `StartMeasurement`.
3. Persistir resultado final valido e atualizar estado `Completed`.
4. Tratar cancelamento, falha, conflito de gate e background.
5. Garantir que `Load` nao inicia medicao.
6. Atualizar overview.

## Criterios De Aceite

- ViewModel e owner do fluxo.
- Nenhuma medicao inicia no `init`.
- Ultima medicao e recentes saem do repository.
- Background cancela medicao sem persistir parcial.

## Testes Pequenos Aplicaveis

- cancelamento nao persiste;
- resultado valido persiste e vira `Completed`;
- `Load` nao inicia medicao;
- conflito de gate vira erro seguro.

## Validacao Minima

```bash
./gradlew assembleDebug
```

## Evidencia Manual

- Descrever combinacoes de estado suportadas e pipeline de persistencia.

- Combinações de estado suportadas no ViewModel: `Idle`, `Preparing`, `Connecting`, `Starting`, `Measuring`, `Stopping`, `Completed`, `Cancelled` e `Failed`.
- Pipeline de persistência: `StartMeasurement` explícito -> estado do coordinator -> `Completed` com `finalResult` -> `saveManualMeasurement(...)` no repository -> `Completed` final ou `Failed(STORAGE_FAILED)`.
- `Load` não inicia medição. `AppMovedToBackground` cancela sessão ativa e preserva apenas histórico já persistido.

## Atualizacao Do Overview

- Marcar `008-06` como concluida.

## Validacao Final

- Confirmar que Composable nao precisa conhecer detalhes tecnicos da medicao.

- Confirmado: Composable continua consumindo `HealthUiState` e emitindo `HealthIntent`, sem acesso a BLE, Room, DAO, gate ou persistência.
