# Task 008-06 - Criar HealthViewModel

## Status

Planejada.

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

## Atualizacao Do Overview

- Marcar `008-06` como concluida.

## Validacao Final

- Confirmar que Composable nao precisa conhecer detalhes tecnicos da medicao.
