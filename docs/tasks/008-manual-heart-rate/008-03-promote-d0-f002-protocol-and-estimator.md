# Task 008-03 - Promover protocolo D0/F002 e estimador

## Status

Planejada.

## Objetivo

Promover componentes tecnicos da PoC para implementacoes reutilizaveis unicas de protocolo manual D0/F002, buffer, qualidade, estimador e selecao final.

## Contexto

Hoje `DevicePocHeartRateSession` e `DevicePocF002BpmEstimator` vivem na feature debug. Fase 08 precisa reutilizar exatamente essas regras sem manter duas implementacoes distintas para produto e PoC.

## Documentos Obrigatorios

- `AGENTS.md`
- `docs/specs/008-manual-heart-rate.md`
- `docs/tasks/008-manual-heart-rate/008-00-overview.md`
- `docs/specs/005-native-ble-poc.md`
- `docs/protocol/heart-rate-d0-f002.md`

## Dependencias

- `008-02`

## Entradas

- `DevicePocHeartRateSession.kt`
- `DevicePocF002BpmEstimator.kt`
- `DevicePocCoordinator.kt`
- testes existentes de `DevicePocHeartRateSessionTest` e `DevicePocF002BpmEstimatorTest`

## Arquivos Exatos Permitidos

- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/device/DevicePocHeartRateSession.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/device/DevicePocF002BpmEstimator.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/device/DevicePocCoordinator.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/bluetooth/**`
- `app/src/test/java/br/com/mwconsulting/championcompanion/feature/device/DevicePocHeartRateSessionTest.kt`
- `app/src/test/java/br/com/mwconsulting/championcompanion/feature/device/DevicePocF002BpmEstimatorTest.kt`
- `docs/specs/008-manual-heart-rate.md`
- `docs/tasks/008-manual-heart-rate/008-00-overview.md`
- `docs/tasks/008-manual-heart-rate/008-03-promote-d0-f002-protocol-and-estimator.md`

## Arquivos Proibidos

- `HealthScreen.kt`
- `HealthFeatureContract.kt`
- `RoomRepositories.kt`
- `AndroidManifest.xml`
- Gradle files

## Escopo

- Criar componente unico para `D0 01`, `D0 00`, parse D0, parse F002 e buffer limitado.
- Criar componente unico de estimador F002, qualidade e selecao final.
- Atualizar PoC para delegar aos componentes promovidos.
- Adicionar testes pequenos de regressao tecnica critica quando aplicavel.

## Fora Do Escopo

- Coordinator de produto.
- Persistencia.
- UI.

## Passos

1. Definir nomes finais dos componentes promovidos.
2. Mover ou extrair logica tecnica para local reutilizavel.
3. Garantir que PoC continue usando implementacao promovida.
4. Confirmar que nao ha duplicacao de comandos, parsers, buffer ou estimador.
5. Adicionar pequenos testes de protecao para regras mais sensiveis.
6. Atualizar overview.

## Criterios De Aceite

- Existe uma unica implementacao para `D0 01`.
- Existe uma unica implementacao para `D0 00`.
- Existe um unico parser D0.
- Existe um unico parser F002.
- Existe um unico buffer limitado F002.
- Existe um unico estimador F002 e unica regra de selecao final.
- PoC continua compilando ao reutilizar componentes promovidos.

## Testes Pequenos Aplicaveis

- payload `D0 01`;
- payload `D0 00`;
- D0 ignora pacote antigo;
- D0 ignora valor invalido;
- F002 little-endian;
- byte impar;
- buffer limitado;
- selecao final D0/F002/indisponivel.

## Validacao Minima

```bash
./gradlew assembleDebug
```

## Evidencia Manual

- Lista dos componentes promovidos e de onde PoC passou a delegar.

## Atualizacao Do Overview

- Marcar `008-03` como concluida.

## Validacao Final

- Confirmar que nao sobraram duas implementacoes tecnicas paralelas.
