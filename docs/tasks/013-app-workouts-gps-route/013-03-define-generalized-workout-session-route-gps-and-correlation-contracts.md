# 013-03 — Definir contratos generalizados de atividade, sessão, rota, GPS e correlação

- **Ticker:** `013`
- **Número:** `03`
- **Status:** completed

## Objetivo e resultado esperado

Estabilizar contratos Android-free antes de infraestrutura. Resultado: abstração única de atividade, lifecycle completo de sessão, modelos de rota/GPS, prioridade de fontes, correlação e projeção HC definidos sem implementação Android/Room.

## Contexto

`WorkoutActivity` exige metadados D3/D4; `AppWorkoutSessionRepository` é parcial; mapper perde estado; fontes APP já existem. Contratos devem eliminar combinações inválidas e preparar migration v6.

## Documentos obrigatórios

Spec 013; `013-01/02`; ADR-014; ADR-003/008/011/012; specs 010-012/015; protocolos BLE relevantes.

## Dependências

`013-02` concluída.

## Arquivos permitidos

`core/model/AppWorkoutSession.kt`, `WatchWorkoutActivity.kt`, `HeartRateAndSync.kt`, `RepositoryContracts.kt`, `DomainMetadata.kt`; novos modelos Android-free em `core/model`; documentação desta task e overview.

## Arquivos e símbolos prováveis

`WorkoutActivity`, `WatchWorkoutImport`, `AppWorkoutSession`, `AppWorkoutSessionRepository`, `DataOrigin`, `HeartRateSource`, `HealthSyncOperationKey` e novos contratos `WorkoutRoute`, `GpsReadiness` e `WorkoutCorrelation`.

## Arquivos proibidos

Room/data, Android location/service, UI, manifesto, Gradle e testes.

## Requisitos cobertos

Generalização; lifecycle; rota/ponto/segmento; qualidade GPS; filtros; correlação; prioridade; sync da mesma sessão; ausência segura.

## Escopo

- origem `WATCH_D3_D4`, `PHONE_GPS`, `PHONE_TREADMILL`, `PHONE_FREE_WORKOUT`;
- `deviceId` e metadados import-only opcionais/segregados;
- factories separadas para import e sessão phone;
- restauração validada de sessão e estado `FINISHING` se necessário;
- repository completo e operações CAS/revisão;
- modelos `WorkoutRoute`, `WorkoutRoutePoint`, segmentos, readiness e rejeição;
- input/output de correlação e merge;
- revisão monotônica HC e projeção de enriquecimento da sessão.

## Fora do escopo

Implementação Room/Android/UI/BLE/provider.

## Passos de implementação

1. Separar campos comuns de atividade dos metadados exclusivos de `WatchWorkoutImport`.
2. Definir proveniências combináveis sessão/importação.
3. Definir factories e invariantes para atividade phone.
4. Completar state machine de sessão, recuperação, finalização idempotente e descarte.
5. Expandir repository com criar/iniciar/observar/checkpoint/pausar/retomar/interromper/recuperar/finalizar/descartar/associar.
6. Definir rota, ponto completo, sequência, segmento, métricas e revisão.
7. Definir readiness e categorias de filtro sem APIs Android.
8. Definir prioridade de fontes e merge D3/D4 seguro.
9. Definir operação HC de rota como enriquecimento da mesma sessão, não record separado.
10. Revisar todos `when` exaustivos afetados e atualizar overview.

## Critérios de aceite

- atividades phone não exigem CRC/parser/device;
- importações existentes continuam representáveis;
- rota só é válida para WALKING/RUNNING phone;
- repository cobre ciclo completo;
- estados e transições rejeitam ordem temporal/conflitos;
- correlação ambígua não escolhe candidato;
- identidade HC canônica não muda com rota.

## Validação manual

Revisão de API e state machines; compilar consumidores após implementação do contrato.

## Comandos

```bash
./gradlew lint assembleDebug assembleRelease
```

## Evidências

Diff de contratos, diagramas de estado, lista de consumidores quebrados/ajustados e saída dos comandos.

## Screenshots

Não aplicável.

## Definição de pronto

Contratos compilam, são Android-free e desbloqueiam `013-04/05`; overview atualizado.

## Riscos e cuidados

Não tornar todos campos nullable sem invariantes. Não alterar semântica D3/D4, revisão `PARTIAL` ou descarte lógico existente.

## Rollback

Reverter contratos e consumidores juntos; nenhuma migration ocorreu nesta task.

Não criar, alterar ou executar testes automatizados.

## Registro de execução

### Estado e escopo

- Dependências confirmadas: `013-01` e `013-02` concluídas; ticker `013`
  consistente entre spec, pasta, overview e subtarefas.
- Escopo limitado aos contratos Android-free de `core/model`; nenhum arquivo de
  Room, `data`, Android, UI, manifesto, Gradle ou teste foi alterado.

### Decisões e desvios

- `WorkoutActivity` tornou-se agregado comum com proveniências combináveis:
  `WATCH_D3_D4`, `PHONE_GPS`, `PHONE_TREADMILL` e `PHONE_FREE_WORKOUT`.
  Metadados D3/D4 permanecem em `WatchWorkoutImport`; factories de importação e
  sessão phone são separadas.
- `AppWorkoutSession` recebeu lifecycle completo, `FINISHING`, recuperação,
  checkpoints monotônicos, `lastTransitionAt`, revisão CAS e finalização
  idempotente. Defaults legados permanecem fail-closed até migration v6.
- Foram definidos rota, ponto, segmento, métricas, readiness GPS, política de
  rejeição, correlação ambígua segura, prioridade de fontes e merge D3/D4.
- Projeção Health Connect representa enriquecimento da mesma operação
  `EXERCISE_SESSION`, com `clientRecordId` estável e versão monotônica; downgrade
  sem rota conhecida é rejeitado.
- Desvio controlado: bridges internos mantêm consumidores D3/D4 compiláveis;
  mappers, Room e transações persistentes ficam para subtarefas posteriores,
  conforme arquivos proibidos desta subtarefa.

### Arquivos alterados

- `app/src/main/java/br/com/mwconsulting/championcompanion/core/model/AppWorkoutSession.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/model/HeartRateAndSync.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/model/RepositoryContracts.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/model/WatchWorkoutActivity.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/model/WorkoutCorrelation.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/model/WorkoutHealthProjection.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/model/WorkoutRoute.kt`
- este arquivo e `013-00-overview.md`

### Comandos, resultados e evidências

- `git diff --check`: passou.
- `./gradlew lint assembleDebug assembleRelease`: `BUILD SUCCESSFUL`; lint,
  assembleDebug e assembleRelease concluídos. Primeira execução após ajustes
  excedeu timeout operacional de 120 s durante tarefas já concluídas; repetição
  com timeout maior concluiu em 58 s.
- Consumidores existentes compilaram com as bridges de compatibilidade; nenhum
  teste automatizado foi criado, alterado ou executado, conforme regra da fase.
- Revisão independente final: aprovada, sem bloqueadores.

### Riscos residuais

- `RoomRepositories` ainda possui caminhos legados sem CAS e o mapper ainda não
  restaura todos os campos da sessão; implementação pertence a `013-05`.
- Finalização transacional, merge persistente e deduplicação CRC global dependem
  de `013-12/13`.
- Persistência/provider de rota, requeue/versionamento remoto e validação física
  permanecem nas subtarefas posteriores.

### Estado final

- Status: concluída.
- Próxima subtarefa elegível: `013-04`; não iniciada automaticamente.
