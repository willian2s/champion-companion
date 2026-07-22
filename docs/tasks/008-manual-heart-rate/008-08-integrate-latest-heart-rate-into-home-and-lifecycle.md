# Task 008-08 - Integrar ultimo BPM na Home e lifecycle final

## Status

Planejada.

## Objetivo

Integrar ultima medicao persistida na Home e fechar concerns de lifecycle da Saude: screen awake, cancelamento em background, confirmacao ao sair e bloqueio de concorrencia com refresh diario.

## Contexto

`HomeScreen` e `HomeViewModel` ja controlam resumo diario e refresh. Fase 08 precisa adicionar BPM persistido sem acionar BLE pela Home e precisa completar comportamento de saida/background da Saude.

## Documentos Obrigatorios

- `AGENTS.md`
- `docs/specs/008-manual-heart-rate.md`
- `docs/tasks/008-manual-heart-rate/008-00-overview.md`
- `008-04`
- `008-05`
- `008-06`
- `008-07`
- ADR-008

## Dependencias

- `008-04`
- `008-05`
- `008-06`
- `008-07`

## Entradas

- `HomeFeatureContract.kt`
- `HomeViewModel.kt`
- `HomeScreen.kt`
- `ChampionNavigation.kt`
- `HealthScreen.kt`
- `HealthViewModel.kt`

## Arquivos Exatos Permitidos

- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/home/**`
- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/health/**`
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/navigation/ChampionNavigation.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/device/DeviceManagementViewModel.kt`
- `docs/specs/008-manual-heart-rate.md`
- `docs/tasks/008-manual-heart-rate/008-00-overview.md`
- `docs/tasks/008-manual-heart-rate/008-08-integrate-latest-heart-rate-into-home-and-lifecycle.md`

## Arquivos Proibidos

- `ChampionDatabase.kt`
- `ChampionDaos.kt`
- `AndroidManifest.xml`
- Gradle files

## Escopo

- Home observa ultimo BPM valido persistido.
- Home mostra valor e horario ou `Sem medicoes`.
- Home nao inicia BLE para BPM.
- Tela Saude mantem tela acordada durante medicao.
- Confirmacao ao sair durante medicao.
- Cancelamento ao background.
- Bloqueio de concorrencia com refresh diario e operacoes foreground BLE relevantes.

## Fora Do Escopo

- Health Connect.
- Historico rico da Home.
- Novas rotas alem do necessario para confirmacao de saida.

## Passos

1. Integrar query de ultimo BPM no `HomeViewModel`.
2. Adicionar bloco/card na Home sem impactar resumo diario.
3. Implementar screen-awake durante medicao.
4. Implementar confirmacao ao sair da Saude durante medicao.
5. Cancelar medicao ao background.
6. Garantir que Device/Home respeitem gate quando medicao estiver ativa.
7. Atualizar overview.

## Criterios De Aceite

- Home mostra ultimo BPM persistido e horario.
- Home nao mede nem conecta para BPM.
- Background cancela medicao.
- Saida da tela durante medicao exige confirmacao.
- Refresh diario e medicao manual nao concorrem.

## Testes Pequenos Aplicaveis

- query generica de ultimo BPM valida Home;
- background cancela sem persistencia;
- confirmacao de saida nao deixa sessao ativa silenciosamente.

## Validacao Minima

```bash
./gradlew assembleDebug
```

## Evidencia Manual

- Resumo do comportamento de lifecycle e do bloco de BPM da Home.

## Atualizacao Do Overview

- Marcar `008-08` como concluida.
- Registrar se confirmacao de saida exigiu tocar navegacao raiz.

## Validacao Final

- Confirmar que Home nao le estado tecnico da PoC.
