# 020-01 — Restringir integridade à janela do Calendário

- **Ticker:** `020`
- **Número:** `01`
- **Status:** concluído

## Objetivo e resultado esperado

Fazer `HealthSyncQueueObservation` calcular operações inválidas e agregados não
resolvidos somente na janela solicitada pelo Calendário. O mês deixa de receber
falso `Dados parciais` por operações de outros períodos, enquanto problemas
reais dentro do mês continuam observáveis.

## Dependências

- Spec `020-integridade-do-banner-mensal-do-calendario`;
- baseline atual e `AGENTS.md`;
- análise de `LocalCalendarRepository`, `RoomRepositories` e mapeadores de fila.

## Escopo incluído

- preservar agregado recuperável no mapeamento interno da operação;
- filtrar operações e flags de integridade pelo conjunto solicitado;
- remover comparação global no read model mensal;
- registrar validação e evidências nesta subtarefa e no overview 020.

## Escopo excluído

- alteração de UI, texto, navegação ou modelos visuais;
- criação de operações para passos, FC ou distância ausentes;
- alteração de writer, importador, Health Connect remoto ou migrations;
- testes automatizados.

## Arquivos e símbolos prováveis

- `data/RoomRepositories.kt`: `observeForAggregatesWithIntegrity`,
  `MappedHealthSyncOperation`;
- `data/LocalCalendarRepository.kt`: `observeWindow`;
- esta subtarefa e overview 020.

## Critérios de aceite cobertos

1. Operação fora do conjunto solicitado não define `hasInvalidOperation` nem
   `hasUnresolvedAggregate` da observação.
2. Operação dentro do conjunto mantém diagnóstico seguro quando inválida ou não
   resolvida.
3. Operações válidas da janela preservam contagem e estados no Calendário.
4. Ausência legítima de passos ou FC não é convertida em erro sem operação
   inelegível correspondente.

## Passos de implementação

1. Mapear agregado recuperável antes de descartar operação malformada.
2. Restringir operações válidas, inválidas e não resolvidas a `aggregates`.
3. Remover filtragem/comparação redundante no repositório do Calendário.
4. Revisar diff e executar comandos de validação.

## Comandos de validação

```bash
git diff --check
./gradlew lint assembleDebug assembleRelease
```

Não criar nem executar testes automatizados.

## Definição de pronto

- código altera somente escopo da integridade da janela;
- documentação registra status, arquivos, decisões, comandos, resultados e
  riscos residuais;
- overview marca somente `020-01` após validação real;
- revisão independente é solicitada.

## Riscos residuais

- Não há smoke físico reproduzido com banco contendo operação fora da janela;
  validação de dispositivo permanece recomendável.
- Operações sem agregado recuperável continuam diagnóstico limitado pela
  informação disponível no registro persistido.

## Execução atual

- **Arquivos alterados:**
  `app/src/main/java/br/com/mwconsulting/championcompanion/data/RoomRepositories.kt`;
  `app/src/main/java/br/com/mwconsulting/championcompanion/data/LocalCalendarRepository.kt`.
- **Decisão:** `MappedHealthSyncOperation` preserva agregado recuperável; operações,
  invalididade e resolução semântica são calculadas somente para agregados
  solicitados. O Calendário consome operações e flags já escopadas, sem comparação
  global redundante.
- **Preservações:** entidades malformadas cujo agregado é recuperável continuam
  sinalizando invalididade dentro da janela; ausência de passos ou FC não cria
  operação nem degradação.

## Comandos executados e resultados

- `git diff --check`: sucesso, sem erros.
- `./gradlew app:compileDebugKotlin`: `BUILD SUCCESSFUL`.
- `./gradlew app:lintDebug`: `BUILD SUCCESSFUL`.
- `./gradlew lint assembleDebug assembleRelease`: `BUILD SUCCESSFUL`.
- Nenhum teste automatizado foi criado, alterado ou executado, conforme spec,
  subtarefa e `AGENTS.md`.

## Bloqueios e próximos passos

- O gate completo `./gradlew lint assembleDebug assembleRelease` permanece
  concluído pelo agente principal.
- Não houve reprodução física com banco contendo operação fora da janela nem
  revisão independente nesta subtarefa.
- Build completo emitiu somente dois avisos de override deprecated em
  `RoomRepositories.kt`; não houve falha de lint ou compilação.
- Revisão independente permanece pendente.
