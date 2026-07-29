# Task 015A-01 - Implementar integridade semântica por operação

## Status

concluída em 29/07/2026.

## Objetivo

Substituir a resolução estrutural global da fila por validação semântica de cada
operação válida, usando `aggregate.type + aggregate.id + recordType`.

## Resultado esperado

- operação malformada sinaliza somente `hasInvalidOperation`;
- aggregate inexistente ou inelegível para o record sinaliza
  `hasUnresolvedAggregate`;
- operação válida fora da janela do calendário não vira inconsistência;
- `PARTIAL`, granularidade e histórico dedicado de FC permanecem inalterados.

## Requisitos cobertos

- definição semântica de aggregate resolvido;
- regras específicas de todos os pares aggregate/record;
- separação entre operação inválida e aggregate não resolvido;
- consultas reativas em lote, sem N+1;
- preservação de leitura local e UI atual.

## Documentos obrigatórios

- `docs/specs/015A-health-sync-queue-semantic-integrity.md`;
- spec 015 e overview 015;
- tasks `015-04`, `015-05`, `015-11`, `015-14` e `012-07`;
- ADR-011;
- baseline atual.

## Escopo incluído

- validação de integridade em `RoomRepositories`;
- queries Room globais, reativas e somente leitura estritamente necessárias;
- mapeamento mínimo para formar candidatos semanticamente avaliáveis;
- comentário ou contrato mínimo de repository, somente se necessário;
- documentação desta task e overview `015A`.

## Escopo excluído

- UI, navegação e read model visual já implementados;
- writer, provider, coordinator ou permissões do Health Connect;
- BLE e importadores D1, D3 e D4;
- entities, schema, migration, índice e versão do banco;
- alteração de workflow;
- histórico dedicado de FC;
- testes automatizados;
- tasks `015-12`, `015-13` e qualquer `015-15`.

## Dependências

- nenhuma subtarefa `015A` anterior;
- contratos existentes das Fases 11, 12 e 15 permanecem vigentes.

## Arquivos e símbolos prováveis

- `app/src/main/java/br/com/mwconsulting/championcompanion/data/RoomRepositories.kt`;
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/database/ChampionDaos.kt`;
- `app/src/main/java/br/com/mwconsulting/championcompanion/data/DatabaseMappers.kt`;
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/model/RepositoryContracts.kt`, somente se necessário;
- `RoomRepositories.observeForAggregatesWithIntegrity()`;
- substituto nomeado de `observeStructurallyValidHealthSyncAggregates()`;
- predicados nomeados de elegibilidade por aggregate e record.

Arquivos e símbolos de consulta obrigatória, sem alteração funcional esperada:

- `core/healthconnect/HealthRecordMapper.kt`;
- `core/model/HeartRateAndSync.kt`;
- `HealthRecordMapper.findEligibleWorkoutRecordTypes()`;
- `WorkoutActivity.sessionWindowOrNull()`;
- `WorkoutDetailDao.observeHeartRateHistoryBetween()`;
- `RoomRepositories.observeHistory()`;
- `HeartRateHistoryQuery`;
- `HeartRateHistoryEntry`.

## Passos de implementação

1. Ler integralmente documentos obrigatórios e confirmar worktree antes de editar.
2. Mapear operações globais com `HealthSyncOperationEntity.toDomain()`.
3. Manter operações que falham nesse mapper fora da resolução de aggregate e
   sinalizá-las somente como inválidas.
4. Construir índices globais de resumos, medições de BPM, atividades, pesos e
   perfis por tipo e ID, usando consultas em lote.
5. Para atividades, observar importações e amostras necessárias sem consulta por
   atividade; agrupar em memória e mapear domínio completo sem carregar
   intervalos desnecessários.
6. Criar resolvedor semântico nomeado baseado em `HealthSyncOperationKey`.
7. Criar predicados nomeados para `DailySummary + STEPS` e
   `HeartRateMeasurement + HEART_RATE`.
8. Criar regras base nomeadas para atividade e predicados específicos de sessão,
   distância, calorias e BPM.
9. Criar predicados nomeados para peso e altura.
10. Comparar cada operação válida com resolvedor semântico; não usar conjunto
    global sem `recordType`.
11. Preservar filtro de operações retornadas pelos aggregates solicitados e
    diagnóstico global de integridade.
12. Confirmar que `LocalCalendarRepository` e UI existente não exigem mudança.
13. Revisar diff contra proibições antes de entregar para `015A-02`.

## Regras por par

- `DailySummary + STEPS`: domínio válido, validade `VALID`, origem de resumo do
  relógio, dispositivo e janela de steps elegíveis.
- `HeartRateMeasurement + HEART_RATE`: domínio válido, validade `VALID`,
  timestamp e origem manual ou D1.
- `WorkoutActivity + EXERCISE_SESSION`: importação e sessão válidas, parser não
  `INVALID`, tipo confirmado e workflow compatível.
- `WorkoutActivity + DISTANCE`: regras base mais distância positiva e finita na
  prioridade calibrada, original e estimada.
- `WorkoutActivity + TOTAL_CALORIES_BURNED`: regras base mais calorias positivas
  e finitas.
- `WorkoutActivity + HEART_RATE`: regras base mais amostras reais, válidas e
  dentro da sessão.
- `WeightMeasurement + WEIGHT`: domínio válido e peso convertido positivo e
  finito.
- `UserProfileHeight + HEIGHT`: perfil válido e altura existente, positiva e
  finita.

Qualquer combinação não suportada é classificada como operação inválida por uma
validação explícita anterior ao resolvedor semântico da
operação, não aggregate não resolvido.

## Preservações obrigatórias

- não exigir `ParserValidity.VALID` globalmente;
- aceitar `ParserValidity.PARTIAL`;
- não bloquear sessão por ausência de distância, calorias ou BPM;
- não exigir todos os records complementares;
- não inventar dados;
- não alterar `HealthRecordMapper.findEligibleWorkoutRecordTypes()` sem
  necessidade demonstrada;
- não alterar query ou composição do histórico dedicado de FC;
- não copiar amostras para `heart_rate_measurements`;
- não vincular histórico de FC à fila;
- não usar `syncedAt` como data do aggregate.

## Testes e comandos de validação

Não criar, alterar ou executar testes automatizados.

Após implementação:

```bash
git diff --check
./gradlew lint assembleDebug assembleRelease
```

Registrar resultados reais. Não marcar validação manual não reproduzida.

## Definição de pronto

- integridade usa chave completa da operação;
- predicados semânticos são nomeados e centralizados;
- nenhuma consulta N+1 foi introduzida;
- flags de operação inválida e aggregate não resolvido são exclusivas quando
  aplicável;
- API pública, calendário e UI permanecem compatíveis;
- `PARTIAL`, granularidade e histórico dedicado de FC estão preservados por
  inspeção;
- comandos permitidos foram registrados;
- `015A-01` e overview atualizados sem iniciar `015A-02` automaticamente.

## Riscos e cuidados

- não usar `findEligibleWorkoutRecordTypes()` diretamente para operações já
  `SYNCED`, pois função governa criação/início e não todo ciclo persistido;
- aceitar workflows transitórios e terminais coerentes com operação existente;
- não confundir `HEART_RATE_MEASUREMENT` com `WORKOUT_ACTIVITY`;
- converter falha estrutural ou temporal do aggregate em não resolvido sem
  derrubar Flow;
- manter custo linear e observar todas as tabelas necessárias para reatividade;
- evitar predicado SQL extenso ou duplicação dos mappers do writer.

## Relatório obrigatório da execução

Registrar:

- causa raiz;
- arquivos alterados;
- definição implementada para aggregate resolvido;
- regras implementadas por par;
- confirmação de `PARTIAL`;
- confirmação de granularidade;
- confirmação de preservação do histórico dedicado de FC;
- comandos executados e resultados;
- riscos residuais.

## Execução

### Causa raiz

`RoomRepositories.observeForAggregatesWithIntegrity()` resolvia aggregate por
presença estrutural global, sem considerar `recordType`. Com isso, registro
mapeável, mas inelegível para operação específica, era tratado como resolvido.
Atividades também não carregavam importação e amostras em lote no caminho de
integridade.

### Arquivos alterados

- `app/src/main/java/br/com/mwconsulting/championcompanion/core/database/ChampionDaos.kt`;
- `app/src/main/java/br/com/mwconsulting/championcompanion/data/DatabaseMappers.kt`;
- `app/src/main/java/br/com/mwconsulting/championcompanion/data/RoomRepositories.kt`;
- este arquivo;
- `docs/tasks/015A-health-sync-queue-semantic-integrity/015A-00-overview.md`.

### Definição implementada

Operação válida possui aggregate resolvido somente quando a chave completa
`aggregate.type + aggregate.id + recordType` encontra candidato local mapeável e
elegível pelo predicado nomeado do par. Índices globais são carregados por
consultas Room reativas em lote. Falha em `HealthSyncOperationEntity.toDomain()`
ou combinação não suportada sinaliza somente `hasInvalidOperation`; operação
válida sem candidato semântico sinaliza `hasUnresolvedAggregate`.

### Regras implementadas por par

- `DailySummary + STEPS`: domínio mapeável, `VALID`, origem
  `WATCH_DAILY_SUMMARY`, dispositivo, steps não negativos e janela compatível
  com `StepsRecord`.
- `HeartRateMeasurement + HEART_RATE`: domínio mapeável, `VALID`, timestamp,
  BPM válido e origem manual ou D1; demais origens permanecem inelegíveis.
- `WorkoutActivity + EXERCISE_SESSION`: importação relacionada válida, parser
  diferente de `INVALID`, workflow compatível, tipo confirmado, duração e
  janela válidas.
- `WorkoutActivity + DISTANCE`: regras base mais distância positiva e finita na
  prioridade calibrada, original e estimada.
- `WorkoutActivity + TOTAL_CALORIES_BURNED`: regras base mais calorias positivas
  e finitas.
- `WorkoutActivity + HEART_RATE`: regras base mais amostras reais dentro da
  janela inclusiva, lista não vazia e BPM válido em todas as amostras avaliadas.
- `WeightMeasurement + WEIGHT`: domínio mapeável e conversão para quilogramas
  positiva e finita.
- `UserProfileHeight + HEIGHT`: perfil mapeável com altura positiva e finita.

Atividades usam importação e amostras globais agrupadas em memória, sem
intervalos e sem consulta por atividade. `PARTIAL` permanece aceito; estados
de workflow `READY_TO_SYNC`, `SYNCING`, `SYNCED` e `SYNC_FAILED` são avaliados
com matriz explícita de compatibilidade com estado da operação. Complementos
continuam granulares e não exigem distância, calorias ou BPM para resolver a
sessão.

### Decisões e desvios

- A API pública de observação foi preservada.
- O conjunto estrutural global foi substituído por índice semântico tipado e
  predicados nomeados por aggregate e record.
- Foi adicionada consulta global reativa de candidatos de atividade com
  importação relacionada e consulta global reativa de amostras de BPM.
- Nenhum desvio de escopo foi aprovado. `HealthRecordMapper`, histórico
  dedicado de FC, writer, UI, entidades, schema e migrações permaneceram
  inalterados.

### Comandos executados

- `git diff --check`;
- `./gradlew app:compileDebugKotlin`;
- `./gradlew lint assembleDebug assembleRelease`.

Testes automatizados não foram criados, alterados ou executados, conforme
spec, subtarefa e `AGENTS.md`.

### Resultados e evidências

- `git diff --check`: sucesso, sem erros.
- `./gradlew app:compileDebugKotlin`: `BUILD SUCCESSFUL` após correção dos
  predicados de BPM e workflow.
- `./gradlew lint assembleDebug assembleRelease`: `BUILD SUCCESSFUL`; 103
  tasks, 28 executadas e 75 `UP-TO-DATE`.
- Revisão independente final: aprovada com ressalvas; confirmou chave completa,
  separação das flags, `PARTIAL`, granularidade, consultas em lote reativas,
  ausência de N+1 e preservação do histórico dedicado de FC.
- Diff não contém alterações em entities, `ChampionDatabase.kt`, schema,
  testes, UI, Health Connect writer ou histórico dedicado de FC.

### Preservações confirmadas

- `ParserValidity.PARTIAL` continua elegível quando demais regras do par passam.
- Sessão, distância, calorias e BPM permanecem operações independentes.
- Histórico dedicado de FC continua lendo amostras de
  `workout_heart_rate_samples`, sem cópia para `heart_rate_measurements` e sem
  dependência da fila.
- Resolução usa candidatos globais e não usa a janela aberta do calendário nem
  `syncedAt` como data.

### Riscos residuais

- Índices globais recalculam volume local completo a cada invalidação observada;
  custo esperado permanece linear e não houve adição de índice preventivo.
- Predicados read-only duplicam parte das regras do writer e podem exigir revisão
  futura se os contratos de mapeamento mudarem.
- Validação dos cenários foi estática; testes automatizados permanecem proibidos
  e nenhum estado manual não reproduzido foi declarado como validado.
