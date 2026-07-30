# 013-14 — Sincronizar rota e métricas phone com Health Connect

- **Ticker:** `013`
- **Número:** `14`
- **Status:** blocked

## Objetivo e resultado esperado

Adicionar `ExerciseRoute` à mesma `ExerciseSessionRecord` com identidade canônica, versão monotônica e retry granular. Resultado: sessão pode sincronizar sem permissão de rota e ser enriquecida depois sem duplicação/downgrade.

## Contexto

Fila atual separa sessão/distância/calorias/BPM. Rota é campo da sessão, não record independente. SDK 1.1.0 deve ser validado antes de upgrade. Metadata phone/watch deve preservar proveniência.

## Documentos obrigatórios

Spec 013; ADR-012/014; spec 012; `013-03/04/05/12`; documentação oficial Health Connect da versão usada.

## Dependências

`013-12` concluída. Pode avançar em paralelo com 013-13 se hotspots serializados.

## Arquivos permitidos

`core/healthconnect/**`; `HeartRateAndSync.kt`; fila/repository/mappers/DAOs necessários; manifesto/rationale/Settings permission models; Gradle somente se upgrade comprovado; task/overview.

## Arquivos e símbolos prováveis

`HealthRecordMapper`, `HealthConnectGateway`, `HealthConnectSyncCoordinator`, `HealthSyncOperation`, `HealthSyncRecordType.EXERCISE_SESSION`, nova projeção de route enrichment, `SettingsViewModel` e rationale Activity.

## Arquivos proibidos

GPS capture, FGS, D3/D4 protocol, WorkManager, leitura HC, testes.

## Requisitos cobertos

WRITE_EXERCISE_ROUTE; mesma sessão/ID; versão; permissão ausente/posterior; retry; ordem/atomicidade; proveniência; fully synced.

## Escopo

- capability/permissão de rota separada;
- projeção interna de route enrichment da mesma sessão;
- canonical ID de `EXERCISE_SESSION` sem sufixo route;
- versão persistida monotônica;
- mapper de route points ordenados/segmentados para API;
- upsert completo sempre inclui rota local elegível já existente;
- workflow e falha parcial.

## Fora do escopo

Mapa, leitura remota, delete amplo, servidor, WorkManager, sync automático de atividade.

## Passos de implementação

1. Confirmar classes/limites do SDK 1.1.0; atualizar somente se API necessária faltar.
2. Expor permissão `WRITE_EXERCISE_ROUTE` no gateway/rationale/Settings.
3. Modelar projeção/dependência route sem segundo `ExerciseSessionRecord` identity.
4. Seedar versão sem regredir versões v5 e incrementar transacionalmente em mudança HC-visível.
5. Mapear pontos finais elegíveis, ordenados, dentro da sessão e sem mock.
6. Enviar base primeiro; sem route permission, manter enrichment pendente/falha `PERMISSION_REQUIRED`.
7. Após concessão, upsert mesma ID com versão maior e rota.
8. Garantir que todo upsert posterior inclua rota já elegível.
9. Processar distância/calorias/BPM granularmente; BPM watch com metadata de watch, sessão/rota/distância phone.
10. Marcar atividade SYNCED só quando elegíveis concluem.
11. Tratar sucesso remoto+falha local por retry mesma ID/versão.
12. Atualizar overview.

## Critérios de aceite

- Health Connect contém uma sessão por atividade;
- sem permissão route, sessão/métricas autorizadas sincronizam;
- rota fica pendente local;
- concessão posterior atualiza mesma sessão;
- retry não duplica nem apaga rota;
- falha de route não apaga outros sucessos;
- route nunca vira record independente.

## Validação manual

Provider real: sem permissão route; sync base; conceder; retry; visualizar rota; retry posterior; editar/upsert posterior e confirmar rota preservada; falhas parciais; uma única sessão.

## Comandos

```bash
./gradlew lint assembleDebug assembleRelease
./gradlew installDebug
```

## Evidências

Permissões, IDs canônicos sanitizados/parcialmente redigidos, versões, estados da fila, contagem remota de sessão, rota visível, comandos e screenshots.

### Registro parcial — correção de offsets derivados de `WorkoutActivity`

- `HealthRecordMapper` passou a resolver `operation.timeZoneId` uma vez por
  atividade, com fallback seguro para `ZoneId.systemDefault()` quando o valor
  estiver ausente ou inválido.
- Sessão, distância e calorias preservam os mesmos `Instant` e calculam offsets
  com `zoneId.offsetAt(start/end)`; BPM de treino usa os instantes reais da
  primeira e da última amostra.
- `RoomRepositories` reutiliza `health_sync_operations.timeZoneId`, já presente
  no schema v6. A primeira operação de atividade congela o `ZoneId` vigente e
  operações complementares reutilizam metadata válida do mesmo agregado. Não
  houve nova coluna, migration, fonte paralela, resync ou delete remoto.
- Operações antigas sem timezone só recebem metadata quando passam novamente
  pelo fluxo explícito de `ensureOperation`; records remotos não são alterados
  automaticamente. `clientRecordId`, `clientRecordVersion`, proveniência de
  dispositivo e `Instant` permaneceram intactos.
- Testes não foram criados, alterados ou executados, conforme restrição da fase.
  A validação incluiu inspeção estática, diff, lint e builds debug/release.
- Capturar timezone exatamente no início da sessão phone exigiria persistência
  no fluxo ainda pendente de `013-08`/`013-12`; adicionar campo a sessão ou
  atividade demandaria nova migration. Esta correção escolhe menor solução
  compatível com schema atual: congelamento na criação da primeira operação de
  sync, com fallback explícito.
- Subtarefa continua bloqueada por `013-12` e pelos itens próprios ainda não
  implementados: `ExerciseRoute`, permissão, versão de enriquecimento, retry,
  workflow e validação física no provider.
- A elegibilidade legada de `HealthRecordMapper.sessionWindowOrNull()` ainda
  depende de `parserValidity` e rejeita atividade phone sem importação D3/D4.
  Correção desse workflow não pertence ao recorte de offsets solicitado e
  permanece para implementação integral de `013-14` após `013-12`.

### Arquivos alterados nesta execução

- `app/src/main/java/br/com/mwconsulting/championcompanion/core/healthconnect/HealthRecordMapper.kt`;
- `app/src/main/java/br/com/mwconsulting/championcompanion/data/RoomRepositories.kt`;
- esta subtarefa e `013-00-overview.md`.

### Comandos executados e evidências

- `git diff --check` — passou;
- `./gradlew lint assembleDebug assembleRelease` — passou na segunda execução;
  a primeira execução excedeu o timeout de 120 s sem erro de compilação, e a
  segunda concluiu com `BUILD SUCCESSFUL` em 58 s;
- nenhum teste automatizado foi criado, alterado ou executado.

### Decisões e desvios

- O timezone foi congelado na primeira operação criada explicitamente para a
  atividade, reutilizando `health_sync_operations.timeZoneId`; operações
  complementares reutilizam esse valor válido.
- Não foi criada migration ou coluna adicional, porque a fila v6 já possui o
  campo necessário. Captura no instante exato de criação/início da sessão phone
  permanece dependente da finalização de `013-08`/`013-12`, ainda ausente nesta
  branch.
- O item permanece `blocked`, não concluído: a mudança entregue é somente a
  correção de offsets solicitada, não toda implementação de rota, permissão,
  versão de enriquecimento, retry e workflow previstos em `013-14`.

### Riscos residuais

- Uma atividade phone criada em fluxo ainda não integrado pode ter timezone
  congelado somente quando a primeira operação explícita de sync for criada;
  se o aparelho viajar antes disso, o offset histórico pode não refletir o
  fuso do início.
- Atividades phone continuam sujeitas à elegibilidade legada dependente de
  `parserValidity` até a integração de `013-12`/`013-14`.
- Validação física do provider, rota e atualização da mesma sessão permanecem
  pendentes.

## Screenshots

Obrigatórias: Settings/capacidade de rota, falha parcial/status e Health Connect mostrando sessão/rota; ocultar coordenadas pessoais quando evidência sair do ambiente privado.

## Definição de pronto

Upsert route idempotente comprovado fisicamente; overview atualizado.

## Riscos e cuidados

Sem atomicidade Room/provider. Limites de pontos e comportamento do provider exigem evidência física. Nunca fazer write route-less com versão superior após rota synced.

## Rollback

Parar de criar novos enrichments sem apagar records já enviados; manter rota local e operações pendentes. Não remover permissão/mapper se isso causar downgrade remoto; preferir forward fix.

Não criar, alterar ou executar testes automatizados.
