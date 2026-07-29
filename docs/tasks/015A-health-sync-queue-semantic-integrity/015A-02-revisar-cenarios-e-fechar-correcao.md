# Task 015A-02 - Revisar cenários e fechar correção

## Status

pending

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
