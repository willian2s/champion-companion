# 034-03 — Persistir FC máxima pessoal

- **Ticker:** `034`
- **Número:** `03`
- **Status:** `pending`

## Objetivo

Adicionar FC máxima manual ao perfil pessoal e atualizar Room de v19 para v20
sem perder dados nem produzir efeitos remotos.

## Resultado esperado

Perfil, snapshot, repository e editor de Perfil leem, validam, salvam, limpam e
preservam `maxHeartRateBpm`; bancos v19 são migrados aditivamente para v20 com
schema exportado atualizado.

## Requisitos cobertos

- R1, R7 e critérios de aceite 1 a 4 e 15 a 17 da Spec 034;
- consequência de FC máxima como métrica pessoal, não preferência;
- Health Connect sem mudanças funcionais.

## Escopo incluído

- `UserProfile`, `UserProfileEntity` e mappers;
- `PersonalMetricsSnapshot` e `PersonalMetricsRepository`;
- input/result e atualização local da métrica;
- preservação em `updateHeight` e `updateStepLength`;
- estado, intents, mensagens, parser inteiro e UI do Perfil;
- `MIGRATION_19_20`, registro na base e `20.json`;
- supporting text explicativo.

## Escopo excluído

- zonas/carga e policy;
- read model do detalhe de Exercícios;
- sincronização de FC máxima com Health Connect;
- mudança de altura/meta/peso além da preservação necessária;
- alteração de tabelas de activities, samples ou snapshots;
- testes automatizados novos.

## Dependências

- `034-01` concluída;
- `034-02` pode estar em paralelo, mas não é necessária para a migration;
- Room v19 e schema exportado 19;
- fluxo atual de `ProfileViewModel` e `PersonalMetricsRepository`.

## Arquivos e símbolos prováveis

- `core/model/DeviceProfileDailySummary.kt` — `UserProfile`;
- `core/model/RepositoryContracts.kt` — snapshot/input/repository;
- `core/database/ChampionDatabase.kt` — `UserProfileEntity`, `version`;
- `core/database/ChampionMigrations.kt` — `MIGRATION_19_20`;
- `core/database/DatabaseModule.kt` — registro;
- `data/DatabaseMappers.kt` e `data/RoomRepositories.kt`;
- `feature/profile/ProfileFeatureContract.kt`;
- `feature/profile/ProfileViewModel.kt`;
- `feature/profile/ProfileScreen.kt`;
- `app/src/main/res/values/strings.xml`;
- `app/schemas/.../20.json`.

## Passos de implementação

1. Adicionar campo opcional ao domínio com validação `30..220`.
2. Adicionar coluna nullable `INTEGER` na entidade.
3. Atualizar mappers e snapshot.
4. Criar input/método de repository que não chame Health Connect.
5. Garantir que updates de altura e passada preservem FC máxima.
6. Criar migration aditiva `19 -> 20` sem backfill.
7. Registrar migration e alterar versão da base.
8. Gerar e revisar schema exportado 20.
9. Adicionar editor no Perfil, limpeza obrigatória para `NULL` e mensagens de
   validação.
10. Confirmar que o fluxo de altura continua com seu contrato vigente, sem
    reutilização pelo novo fluxo.

## Testes e comandos de validação

- inspeção do SQL da migration e schema 20;
- conferência de mapeamento após reinício;
- `git diff --check`;
- não criar ou executar testes automatizados nesta task.

## Definição de pronto

- banco v19 abre em v20 sem wipe;
- perfis existentes iniciam com `NULL`;
- valores válidos persistem e valores fora de faixa são rejeitados;
- limpar valor retorna a estado não configurado;
- altura/passada não apagam FC máxima;
- nenhuma operação Health Connect é criada/alterada ao salvar FC máxima.

## Riscos e cuidados

- Não reutilizar `maximumBpm` de atividade.
- Não adicionar idade ou sugestão automática.
- Não registrar FC máxima em DataStore.
- Não alterar IDs, payloads ou fila Health Connect por conveniência do fluxo de
  Perfil.
- Não esquecer schema exportado 20 nem registrar migration fora da ordem.

## Registro da execução

- **Status:** `pending` — aguardando implementação após congelamento de contrato.
- **Arquivos alterados:** nenhum arquivo de produto nesta fase de planejamento.
- **Decisões e desvios:** limpeza de FC máxima é obrigatória; valor vazio/ação
  explícita deve persistir `NULL` sem sync remoto.
- **Comandos executados:** inspeção de entidades, migration v19, mappers,
  repository e Profile; build não executado por escopo documental.
- **Resultados e evidências:** Room v19, schema 19, `UserProfile` e todos os
  reconstrutores de perfil foram confirmados.
- **Riscos residuais:** update de altura/passada pode perder campo novo se algum
  reconstrutor não for atualizado; validar round-trip após reinício.
