# Task 015A-02 - Revisar cenários e fechar correção

## Status

concluída em 29/07/2026.

## Objetivo

Executar uma única revisão final da implementação `015A-01` contra o contrato de
cenários, registrar validações reais e fechar documentação da correção.

## Resultado esperado

- todos os cenários recebem classificação esperada por inspeção reproduzível;
- preservações de `PARTIAL`, granularidade e histórico dedicado de FC são
  confirmadas explicitamente;
- comandos permitidos e riscos residuais ficam documentados;
- spec e overview são fechados somente quando houver evidência suficiente.

## Requisitos cobertos

- revisão única pela tabela contratual;
- validação da separação entre operação inválida e aggregate não resolvido;
- confirmação de ausência de regressões funcionais;
- fechamento documental sem declarar reproduções inexistentes.

## Escopo incluído

- revisão read-only do diff de `015A-01`;
- execução dos comandos de build permitidos;
- atualização da spec `015A`, overview e subtarefas;
- correção de código somente se a revisão encontrar achado novo e materialmente
  diferente.

## Escopo excluído

- segunda rodada de mudanças sem achado novo;
- refatoração oportunista;
- alteração de UI, Health Connect writer, provider, BLE ou importadores;
- schema, migration, índices ou entities;
- testes automatizados;
- fechamento da Fase 15, `015-12`, `015-13` ou `015-15`.

## Dependências

- `015A-01` concluída e documentada.

## Arquivos e símbolos prováveis

- diff produzido pela `015A-01`;
- `docs/specs/015A-health-sync-queue-semantic-integrity.md`;
- `docs/tasks/015A-health-sync-queue-semantic-integrity/015A-00-overview.md`;
- arquivos das subtarefas `015A-01` e `015A-02`.

## Passos de implementação

1. Ler relatório e diff completos da `015A-01`.
2. Confirmar que alterações ficaram restritas à validação read-only da fila.
3. Revisar cada cenário da tabela uma única vez.
4. Confirmar que operação malformada não recebe duas flags.
5. Confirmar resolução global independente da janela aberta no calendário.
6. Confirmar regras específicas de todos os pares aggregate/record.
7. Confirmar por inspeção que `PARTIAL` não foi trocado por exigência global de
   `VALID`.
8. Confirmar que complementos permanecem independentes.
9. Confirmar que histórico dedicado de FC não ganhou dependência da fila e que
   query, composição e identidade das amostras foram preservadas.
10. Executar comandos permitidos e registrar resultados reais.
11. Se existir achado novo e materialmente diferente, devolvê-lo à `015A-01`,
    corrigir uma vez e repetir somente cenários afetados.
12. Sem achados bloqueantes, atualizar status, progresso e riscos residuais.

## Tabela de revisão obrigatória

| Cenário | `hasInvalidOperation` | `hasUnresolvedAggregate` |
|---|---:|---:|
| Operação malformada | `true` | `false` |
| Aggregate inexistente | `false` | `true` |
| `DailySummary` com `validity = INVALID` | `false` | `true` |
| `HeartRateMeasurement` sem timestamp | `false` | `true` |
| `HeartRateMeasurement` com origem inelegível | `false` | `true` |
| `WorkoutActivity` com `parserValidity = INVALID` | `false` | `true` |
| `WorkoutActivity PARTIAL` com sessão válida | `false` | `false` |
| `WorkoutActivity PARTIAL` sem distância e sem operação `DISTANCE` | `false` | `false` |
| `WorkoutActivity PARTIAL` sem distância, mas com operação `DISTANCE` | `false` | `true` |
| `WorkoutActivity PARTIAL` com BPM real e operação `HEART_RATE` | `false` | `false` |
| Atividade sem BPM e sem operação `HEART_RATE` | `false` | `false` |
| Atividade sem BPM, mas com operação `HEART_RATE` | `false` | `true` |
| Aggregate válido fora da janela aberta no calendário | `false` | `false` |
| Aggregate válido e elegível | `false` | `false` |

## Testes e comandos de validação

Não criar, alterar ou executar testes automatizados.

```bash
git diff --check
./gradlew lint assembleDebug assembleRelease
```

Buscas estáticas adicionais podem confirmar:

- ausência de mudança em `HealthRecordMapper.findEligibleWorkoutRecordTypes()`;
- ausência de mudança em `WorkoutActivity.sessionWindowOrNull()`;
- ausência de mudança em `WorkoutDetailDao.observeHeartRateHistoryBetween()`;
- ausência de mudança em `RoomRepositories.observeHistory()`;
- ausência de mudanças em `app/src/test` e `app/src/androidTest`;
- ausência de alteração de schema e `ChampionDatabase.kt`.

## Definição de pronto

- cada linha da tabela possui resultado registrado;
- nenhuma validação manual inexistente foi declarada;
- `ParserValidity.PARTIAL` está explicitamente confirmado como aceito;
- granularidade dos records está explicitamente confirmada;
- BPM de atividades está explicitamente confirmado no histórico dedicado;
- comandos permitidos concluíram ou falhas reais estão registradas;
- limitações e riscos residuais estão documentados;
- `015A-01`, `015A-02`, overview e spec possuem status coerentes.

## Riscos e cuidados

- inspeção estática não substitui cenário manual não reproduzido;
- não alterar código após revisão final sem achado novo e materialmente distinto;
- não marcar spec concluída apenas porque aggregates passam por mapper;
- não tratar complemento ausente como invalidação global da atividade;
- não confundir amostra de atividade com `HeartRateMeasurement`.

## Relatório final obrigatório

Apresentar:

- causa raiz;
- arquivos alterados;
- definição adotada para aggregate resolvido;
- regras por `aggregate + recordType`;
- resultado de cada cenário da tabela;
- confirmação de aceitação de `ParserValidity.PARTIAL`;
- confirmação de sincronização granular;
- confirmação de BPM de exercícios na tela dedicada de FC;
- comandos de validação executados;
- limitações ou riscos residuais.

## Execução

### Causa raiz

`RoomRepositories.observeForAggregatesWithIntegrity()` originalmente resolvia
aggregate por presença estrutural global e ignorava `recordType`. Assim, registro
mapeável, mas inelegível para operação específica, era tratado como resolvido.

### Arquivos alterados nesta subtarefa

- `docs/tasks/015A-health-sync-queue-semantic-integrity/015A-02-revisar-cenarios-e-fechar-correcao.md`;
- `docs/tasks/015A-health-sync-queue-semantic-integrity/015A-00-overview.md`;
- `docs/specs/015A-health-sync-queue-semantic-integrity.md`.

Nenhum arquivo de produção foi alterado nesta revisão. Foram inspecionados o
diff e os arquivos de `015A-01` em `ChampionDaos.kt`, `DatabaseMappers.kt` e
`RoomRepositories.kt`, além dos mappers do writer e das queries do histórico
dedicado.

### Definição adotada para aggregate resolvido

Operação válida possui aggregate resolvido somente quando a chave completa
`aggregate.type + aggregate.id + recordType` encontra candidato local mapeável e
elegível pelo predicado nomeado do par. Operação que falha em
`HealthSyncOperationEntity.toDomain()` ou combina par não suportado entra somente
em `hasInvalidOperation`; operação válida sem candidato semântico entra somente
em `hasUnresolvedAggregate`.

### Regras confirmadas por par

- `DailySummary + STEPS`: domínio mapeável, `VALID`, origem
  `WATCH_DAILY_SUMMARY`, dispositivo, steps não negativos e janela válida.
- `HeartRateMeasurement + HEART_RATE`: domínio mapeável, `VALID`, timestamp,
  BPM entre 30 e 220 e origem manual ou D1; demais origens inelegíveis.
- `WorkoutActivity + EXERCISE_SESSION`: importação relacionada válida, parser
  diferente de `INVALID`, workflow compatível, tipo confirmado, duração e janela
  válidas.
- `WorkoutActivity + DISTANCE`: regras base e distância positiva e finita na
  prioridade calibrada, original e estimada.
- `WorkoutActivity + TOTAL_CALORIES_BURNED`: regras base e calorias positivas e
  finitas.
- `WorkoutActivity + HEART_RATE`: regras base, amostras reais dentro da janela
  inclusiva e BPM válido.
- `WeightMeasurement + WEIGHT`: domínio mapeável e peso convertido positivo e
  finito.
- `UserProfileHeight + HEIGHT`: perfil mapeável e altura positiva e finita.

### Resultado de cada cenário

Classificação abaixo resulta de inspeção estática reproduzível de
`observeForAggregatesWithIntegrity()`, `isSemanticallyResolved()` e predicados
nomeados. Não representa reprodução manual em dispositivo ou teste automatizado.

| Cenário | `hasInvalidOperation` | `hasUnresolvedAggregate` | Resultado |
|---|---:|---:|---|
| Operação malformada | `true` | `false` | confirmado |
| Aggregate inexistente | `false` | `true` | confirmado |
| `DailySummary` com `validity = INVALID` | `false` | `true` | confirmado |
| `HeartRateMeasurement` sem timestamp | `false` | `true` | confirmado |
| `HeartRateMeasurement` com origem inelegível | `false` | `true` | confirmado |
| `WorkoutActivity` com `parserValidity = INVALID` | `false` | `true` | confirmado |
| `WorkoutActivity PARTIAL` com sessão válida | `false` | `false` | confirmado |
| `WorkoutActivity PARTIAL` sem distância e sem operação `DISTANCE` | `false` | `false` | confirmado |
| `WorkoutActivity PARTIAL` sem distância, mas com operação `DISTANCE` | `false` | `true` | confirmado |
| `WorkoutActivity PARTIAL` com BPM real e operação `HEART_RATE` | `false` | `false` | confirmado |
| Atividade sem BPM e sem operação `HEART_RATE` | `false` | `false` | confirmado |
| Atividade sem BPM, mas com operação `HEART_RATE` | `false` | `true` | confirmado |
| Aggregate válido fora da janela aberta no calendário | `false` | `false` | confirmado |
| Aggregate válido e elegível | `false` | `false` | confirmado |

### Preservações confirmadas

- `ParserValidity.PARTIAL` permanece aceito quando as regras do par passam;
  somente `INVALID` bloqueia a atividade.
- Sessão, distância, calorias e BPM permanecem operações independentes. Ausência
  de complemento afeta somente a operação correspondente existente.
- A observação é global e não usa a janela aberta do calendário nem `syncedAt`
  como data do aggregate.
- `WorkoutDetailDao.observeHeartRateHistoryBetween()` e
  `RoomRepositories.observeHistory()` permanecem inalterados. Amostras reais
  continuam em `workout_heart_rate_samples`, sem cópia para
  `heart_rate_measurements` e sem dependência da fila.

### Decisões e desvios

- Revisão encerrada sem achado novo e materialmente diferente; nenhuma segunda
  rodada de código foi necessária.
- Nenhum desvio de escopo: writer, provider, BLE, importadores, UI, entities,
  schema, migrações e testes permaneceram inalterados.
- Evidência foi registrada como inspeção estática, sem declarar validação manual
  inexistente.

### Comandos executados e resultados

- `git diff --check`: sucesso, sem erros.
- `./gradlew lint assembleDebug assembleRelease`: `BUILD SUCCESSFUL`; 103 tasks,
  3 executadas e 100 `UP-TO-DATE`.
- `git diff --name-only HEAD^ HEAD -- app/src/test app/src/androidTest app/schemas app/src/main/java/br/com/mwconsulting/championcompanion/core/database/ChampionDatabase.kt app/src/main/java/br/com/mwconsulting/championcompanion/core/healthconnect/HealthRecordMapper.kt app/src/main/java/br/com/mwconsulting/championcompanion/core/model/HeartRateAndSync.kt app/src/main/java/br/com/mwconsulting/championcompanion/core/model/RepositoryContracts.kt app/src/main/java/br/com/mwconsulting/championcompanion/data/LocalCalendarRepository.kt`: sem saída.
- Nenhum teste automatizado foi criado, alterado ou executado, conforme spec,
  subtarefa e `AGENTS.md`.

### Riscos residuais

- Índices globais recalculam o volume local completo a cada invalidação observada;
  custo esperado permanece linear.
- Predicados read-only duplicam parte das regras do writer e podem exigir revisão
  futura se os contratos de mapeamento mudarem.
- A inspeção estática não substitui reprodução manual de cada estado; nenhum
  estado manual não reproduzido foi declarado como validado.
