# 029-07 — Implementar read model e avaliador de recordes pessoais

- **Ticker:** `029`
- **Número:** `07`
- **Status:** `completed`
- **Depends on:** `029-06`

## Objetivo

Criar a leitura dos recordistas atuais e o avaliador capaz de detectar uma nova conquista a partir do snapshot anterior e do snapshot persistido novo.

## Recordes iniciais

- maior distância final por modalidade comparável;
- maior duração;
- maior quantidade de passos em modalidade elegível.

Ficam fora nesta versão:

- maior BPM;
- maior cadência;
- menor pace/melhor velocidade sem contrato de distância/modalidade específico;
- badges persistidos ou gamificação extensa.

## Avaliação

Para cada métrica/escopo:

```text
baselineOther = melhor valor das demais atividades elegíveis
oldEligibleValue = valor anterior da própria atividade, se antes elegível no mesmo escopo
previousBest = max(baselineOther, oldEligibleValue)

newValue > previousBest
    -> nova conquista
```

A atividade atual deve ser excluída de `baselineOther`.

## Regras de idempotência

- empate não é novo recorde;
- re-save sem mudança não gera conquista;
- se a atividade já era recordista e seu valor persistido aumenta, o valor anterior participa de `previousBest` e a melhoria pode gerar nova conquista;
- mudança de tipo compara contra a baseline do novo tipo;
- atividade antiga compara contra todo o histórico atualmente conhecido, não somente contra atividades cronologicamente anteriores.

## Falha segura

A avaliação acontece depois do commit funcional. Erro ao consultar/evaluar recordes:

- não desfaz o save;
- não muda workflow;
- não cria estado falso de recorde;
- pode ser registrado apenas por mecanismo diagnóstico seguro já vigente.

## Critérios de aceite

1. Recordistas atuais são deriváveis sem tabela de badges.
2. Distância usa o valor final válido por modalidade.
3. `CYCLING` não usa passos D4 como recorde.
4. Re-save sem alteração é idempotente para o alerta.
5. Melhoria da própria atividade recordista usa o valor anterior como baseline.
6. Atividade antiga só gera conquista se for o melhor valor atualmente conhecido.
7. Falha do evaluator não converte save bem-sucedido em falha.

## Validação

```bash
git diff --check
./gradlew lint assembleDebug assembleRelease
```

## Implementação

### Arquivos alterados

- `app/src/main/java/br/com/mwconsulting/championcompanion/core/model/EvolutionContracts.kt`;
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/model/RepositoryContracts.kt`;
- `app/src/main/java/br/com/mwconsulting/championcompanion/data/LocalEvolutionReadModel.kt`;
- `app/src/main/java/br/com/mwconsulting/championcompanion/data/RoomRepositories.kt`;
- `app/src/main/java/br/com/mwconsulting/championcompanion/data/LocalPersonalRecordsAggregator.kt`;
- `app/src/main/java/br/com/mwconsulting/championcompanion/data/PersonalRecordEvaluator.kt`.

### Decisões e desvios

- A leitura de recordes foi separada da leitura temporal: `observePersonalRecordsSnapshot()`
  consulta todo histórico elegível, sem janela ou filtro da superfície de evolução.
- A leitura all-history usa caminho estrito do repository: qualquer entidade que não
  possa ser mapeada falha fechada como dado local inválido; o caminho tolerante legado
  permanece preservado para os demais consumidores.
- Recordes são agregados por modalidade: distância final para corrida, caminhada,
  esteira e bicicleta; duração para todas as modalidades confirmadas; passos para
  corrida, caminhada, esteira e treino livre. Bicicleta não participa de recorde de
  passos e treino livre não participa de recorde de distância.
- Desempates usam `achievedAt` e `activityId` em ordem determinística. A data do
  recorde permanece o `startedAt` real da atividade.
- `PersonalRecordEvaluator` exclui a atividade atual da baseline, combina o melhor
  valor das demais atividades com o valor anterior elegível da mesma modalidade e
  emite conquista somente quando o valor novo é estritamente maior.
- Conquista permanece evento efêmero. Integração pós-save e apresentação pertencem
  à `029-09`; nenhum fluxo de persistência foi alterado.
- Não houve migration, Room/DAO, UI, BLE, Health Connect, GPS ou teste automatizado.

### Comandos executados e resultados

- `git diff --check` — passou;
- `./gradlew :app:compileDebugKotlin :app:compileReleaseKotlin` — `BUILD SUCCESSFUL`;
- `./gradlew lint assembleDebug assembleRelease` — `BUILD SUCCESSFUL`;
- testes automatizados não foram criados nem executados, conforme guard rail.

### Evidências

- `PersonalRecordsReadSnapshot` preserva população all-history normalizada e falha
  fechada para IDs duplicados ou atividades inválidas.
- `observeActivitiesStrict()` impede descarte silencioso durante o mapeamento e
  converte corrupção local em `INVALID_LOCAL_DATA`; indisponibilidade de storage
  permanece `SOURCE_UNAVAILABLE` e retryável.
- `LocalPersonalRecordsAggregator` deriva `PersonalRecordsSnapshot` sem tabela
  materializada e usa métricas finais persistidas já presentes no snapshot.
- `PersonalRecordEvaluator` cobre atividade nova, empate, resave sem alteração,
  melhoria do próprio recordista, mudança de modalidade e atividade antiga contra
  histórico atualmente conhecido.
- Estados `Loading`, `Empty`, `Ready` e `Failed` foram definidos para leitura de
  recordes sem fabricar recordistas quando não houver dados.
- Lint e builds debug/release concluíram com `BUILD SUCCESSFUL`.

### Riscos residuais

- `EvolutionSnapshot.personalRecords` continua sendo composto pelo consumer a
  partir da leitura all-history; integração visual pertence à `029-08`.
- Avaliação pós-save ainda não está conectada aos pontos de mutação; isso pertence
  à `029-09` e deve continuar sem bloquear saves.
- A leitura estrita falha o histórico inteiro quando qualquer entidade, inclusive
  inelegível para recordes, é inválida; isso privilegia baseline íntegra sobre
  disponibilidade parcial.
- Ausência de testes automatizados deixa cenários extremos dependentes de inspeção
  e validação de build, conforme guard rail do projeto.
