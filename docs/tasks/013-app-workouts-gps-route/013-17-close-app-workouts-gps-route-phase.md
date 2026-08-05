# 013-17 — Encerrar a Fase 013 após descontinuação dos treinos iniciados pelo celular

- **Ticker:** `013`
- **Número:** `17`
- **Status:** in_progress
- **Outcome previsto:** `partially_delivered`
- **Motivo do encerramento:** `product_reorientation`

## Objetivo e resultado esperado

Reconciliar documentação com estado entregue e registrar o encerramento da
direção de treinos iniciados pelo celular sem declarar a Fase 013 concluída nem
iniciar a Fase 014. Resultado esperado desta subtarefa: spec, overview, roadmap,
baselines, ADR, resumo de integração e regras operacionais refletem o resultado
`partially_delivered`, os cancelamentos e os limites reais.

## Contexto

A decisão de produto descontinuou novos treinos iniciados pelo celular. Os
registros de execução, bloqueios e evidências físicas permanecem históricos e
não podem ser convertidos em aceite por ausência de continuidade. Fase 12 deve
permanecer concluída sem rota; a Fase 014 será redefinida para refinamento de
treinos iniciados pelo relógio, com WorkManager somente como detalhe técnico de
tarefas finitas e adiáveis.

## Documentos obrigatórios

Spec/overview e todas tasks 013; evidências existentes de 013-08 e 013-11;
roadmap; baselines; ADR-004/012/014/016; ADR-017; resumo de integração;
protocolos afetados; spec/overview 015.

## Dependências

Documentação da Fase 013 disponível para reconciliação. `013-16` não foi
executada nem aceita; seu status `cancelled` deve permanecer explícito.

## Arquivos permitidos

Documentação autoritativa, tasks/overview 013, ADRs, roadmap, baselines, resumo
de integração e registros de evidência para fechamento factual.

## Arquivos e símbolos prováveis

Status da spec/overview, Fases 12-14 do roadmap, baseline, G-13/G-14,
mapeamento Health Connect, ADR-004/012/014/016/017 e referências D0/F002/D3/D4
quando afetadas.

## Arquivos proibidos

Código, Gradle, manifesto, schemas e testes.

## Causa do escalonamento confirmada

- A causa não é falha isolada de um gate: é reorientação de produto para
  encerrar novos treinos iniciados pelo celular.
- Os registros de `013-08`, `013-10` e `013-11` preservam implementação parcial,
  bloqueios físicos e evidências sanitizadas; `013-12` a `013-16` não foram
  executadas ou concluídas.
- O encerramento documental não autoriza corrigir código, ampliar GPS/rota,
  `ExerciseRoute` ou BLE live, nem declarar pontos de entrada phone já
  desativados.

## Requisitos cobertos

Fechamento documental; fronteiras; preservação de migration/schema/dados;
ADR; riscos aceitos; evidências; status/progresso; nenhum WorkManager/teste.

## Escopo

- atualizar status da spec/overview, tasks e links sem renumerar;
- reconciliar baseline/roadmap sem apagar histórico;
- registrar infraestrutura v6, FGS, rota, correlação e Health Connect como
  preservada, parcial ou cancelada conforme evidência;
- registrar limitações do BPM esportivo e riscos residuais;
- confirmar dívida do guard antigo sem alegar teste alterado;
- preservar dados e evidências e redefinir a direção futura para treinos do
  relógio.

## Fora do escopo

Implementação corretiva, desativação operacional em código, Fase 014,
WorkManager, novos recursos, testes e qualquer gate físico novo.

## Plano de fechamento futuro

### A — Encerramento documental

Estado: em execução nesta subtarefa. Consolidar status `closing`, resultado
`partially_delivered`, motivo `product_reorientation`, matriz de 17 itens,
spec, overview, roadmap, baselines, ADR-017 e resumo de integração. Manter
`013-17` como `in_progress`; nenhum item do checklist pode ser marcado por este
registro.

### B — Desativação operacional dos pontos de entrada phone

Estado: pendente e bloqueado para execução nesta etapa documental. A operação
deve definir e evidenciar a desativação dos pontos de entrada de treinos phone,
sem afirmar que o código atual já foi desativado. Não criar correção de produção
nem nova task neste trabalho.

### C — Preservação de sessões e dados existentes

Estado: regra vigente, sem remoção. Migrations, schemas, sessões, dados,
evidências físicas, contratos e infraestrutura existentes devem permanecer
preservados até autorização explícita de remoção. Cancelamento de direção não
é deleção, downgrade, limpeza ou conversão de evidência inconclusiva em sucesso.

### D — Encerramento final `partially_delivered`

Estado: futuro, dependente de A, B e C. Registrar a Fase 013 como
`partially_delivered` somente quando a desativação operacional estiver
documentada com evidência própria e a preservação estiver verificada. Até lá,
Fase 013 permanece `closing` e esta subtarefa permanece `in_progress`.

## Passos de implementação

1. Conferir 17 tasks, statuses, evidências e overview.
2. Atualizar spec somente com fatos entregues e histórico preservado.
3. Atualizar baseline/feature inventory/gaps/HC mapping/source-map.
4. Atualizar roadmap: F13 em encerramento e F14 redefinida para treinos do
   relógio.
5. Inspecionar ADR-004/012/014/016 e criar ADR-017 sem inventar implementação.
6. Criar resumo de integração e registrar riscos residuais.
7. Não alterar protocolos `013-08`/`013-11` nem executar gates físicos.
8. Registrar somente verificações documentais permitidas nesta execução.
9. Manter `013-17` em `in_progress` e o checklist sem conclusão.

## Critérios de aceite

- status/progresso/checklist correspondem a `8/8/1` (`completed/cancelled/in_progress`);
- F12 não é reescrita como route-capable;
- F14 é redefinida para refinamento de treinos do relógio; WorkManager fica
  somente como detalhe técnico finito/adiável;
- baseline descreve entrega real, preservando histórico;
- ADR/migration/riscos/evidências são rastreáveis;
- critérios finais não exigem novos treinos phone;
- não se afirma desativação phone já realizada no código;
- nenhum teste automatizado é alegado;
- nenhum commit/push.

## Validação manual

Revisão documental de links, nomes, ticker, estados, matriz de encerramento,
rastreabilidade e `git status`. Não executar Gradle, testes ou gates físicos.

## Comandos

Nenhum Gradle adicional se evidências válidas de 013-16 existirem. Não executar testes.

## Evidências

Diff documental, verificação de links/checklist, matriz de encerramento, ADR-017,
resumo de riscos e `git status` final.

## Screenshots

Referenciar screenshots da 013-16; não criar evidência nova sem execução.

## Definição de pronto

Bloco A reconciliado documentalmente, overview com 17 itens e `013-17` ainda
`in_progress`; blocos B, C e D continuam claramente pendentes ou em preservação.

## Riscos e cuidados

Não converter risco aceito em sucesso. Não apagar dívida do guard/protocolo,
histórico, migration, schema, dados ou evidência. Não iniciar F14 nem afirmar
desativação phone já realizada.

## Rollback

Reverter somente fechamento documental se evidência for invalidada; reabrir
status das tasks afetadas honestamente. Nunca remover dados ou fazer downgrade
de migration como rollback documental.

Não criar, alterar ou executar testes automatizados.

## Registro anterior desta execução — verificações documentais

### Resultado

- A causa do escalonamento foi confirmada como `product_reorientation`.
- O fechamento documental foi tratado como Bloco A; não houve implementação de
  desativação operacional, limpeza de sessões/dados ou gate físico.
- O status desta subtarefa permanece `in_progress`; a Fase 013 permanece
  `closing`; o resultado previsto permanece `partially_delivered`.
- Histórico, riscos, bloqueios e evidências existentes foram preservados. A
  ausência de execução de `013-12` a `013-16` não foi convertida em sucesso.

### Comandos e resultados

Verificações documentais executadas após a edição:

```text
git diff --check — PASS; nenhuma irregularidade de whitespace
git status --short — PASS; somente alterações documentais desta execução
```

Nenhum Gradle, teste, comando de dispositivo, gate físico, commit ou push foi
executado nesta etapa.

### Arquivos alterados

- `AGENTS.md`;
- `docs/specs/013-app-workouts-gps-route.md`;
- `docs/tasks/013-app-workouts-gps-route/013-00-overview.md`;
- tasks `013-01` a `013-04`, `013-08`, `013-10`, `013-11`, `013-12`, `013-13`,
  `013-14`, `013-15`, `013-16` e esta task;
- `docs/roadmap/champion-companion-roadmap.md`;
- `docs/baseline/current-baseline.md`;
  `feature-inventory.md`; `gaps-and-open-questions.md`;
  `health-connect-mapping.md`; `source-map.md`;
- `docs/decisions/adr-017-watch-started-workouts-and-step-derived-distance.md`;
- `docs/integration/013-closure-summary.md`.

Nenhum arquivo em `docs/integration/013-08-*` ou `docs/integration/013-11-*`
foi alterado. Nenhum arquivo de produção, Gradle, manifesto, migration, schema
ou teste foi alterado.

### Decisões e desvios

- O encerramento normal dependente de `013-16` foi substituído por encerramento
  intermediário motivado por decisão de produto; `013-16` permanece `cancelled`.
- O próximo ADR disponível era `ADR-017`, após `ADR-016`; a nova ADR registra a
  direção de treinos iniciados pelo relógio sem alegar implementação.
- O fechamento documental encerra novos gates físicos phone, mas não executa a
  desativação operacional, não remove pontos de entrada e não limpa dados.
- Histórico de requisitos, execuções, bloqueios e evidências foi preservado;
  cancelamento não foi usado para reclassificar ausência de evidência como
  sucesso.

### Revisão independente

- Veredito: `PASS`, sem achados acionáveis.
- A revisão confirmou ticker, checklist único com 17 itens, classificação
  `8/8/1`, status `in_progress` desta task, preservação das evidências e ausência
  de alterações em código, Gradle ou testes.
- A revisão também confirmou que registros históricos de `013-08` e políticas
  phone/GPS nas baselines estão explicitamente contextualizados como não
  vigentes, sem apagar evidência.

### Riscos e bloqueios restantes

- Bloco B foi implementado e validado parcialmente; a revisão independente final
  ainda precisa confirmar guards de domínio, cleanup e consumidores históricos.
- Bloco C exige preservação contínua de migrations, schemas, sessões, dados e
  evidências até autorização explícita de remoção.
- Bloco D não pode ser concluído enquanto B não possuir evidência operacional e
  C não estiver verificado; `partially_delivered` é resultado previsto, não
  conclusão atual.
- Bloqueios físicos históricos de `013-08`/`013-11` e ausência de execução de
  `013-12` a `013-16` permanecem honestamente registrados.

## Registro desta execução — desativação operacional

### Resultado

- O guard central `PhoneWorkoutPolicy`, com estado
  `PhoneWorkoutAvailability.DISCONTINUED`, foi introduzido e reutilizado nas
  fronteiras de UI, navegação, repository, foreground service, ownership,
  localização e participação BLE.
- A entrada visível de treino phone foi removida da tela Exercícios. Seleção,
  preparação, countdown, sessão ativa, recovery operacional e intents antigos
  agora redirecionam ou falham fechados para Exercícios/recurso descontinuado.
- A criação de nova `AppWorkoutSession` e as mutações operacionais phone são
  recusadas antes de persistência. Leituras históricas permanecem disponíveis.
- `WorkoutRecordingService` recusa `START`, `RESUME`, `RETRY_WATCH` e ações de
  recuperação sob a política descontinuada. O shutdown aguarda cleanup de
  ownership/BLE quando possível e possui fallback idempotente em `onDestroy`.
- Sessões históricas abertas permanecem preservadas. `CREATED`, `ACTIVE`,
  `PAUSED` e `AWAITING_RECOVERY` podem ser descartadas somente após confirmação
  e shutdown confirmado; `FINISHING` permanece sem ação e preservada.
- Nenhuma tabela, coluna, migration, schema, atividade, rota, evidência,
  parser D3/D4 ou política de distância foi removida ou alterada.

### Arquivos alterados

- Código: `MainActivity.kt`, `ChampionDaos.kt`,
  `core/model/PhoneWorkoutAvailability.kt`, `LocationModule.kt`,
  `WorkoutLocationSource.kt`, `AndroidWorkoutLocationSource.kt`,
  `ChampionNavigation.kt`, `RoomRepositories.kt`, `ExercisesScreen.kt`,
  `ExercisesViewModel.kt`, `WatchWorkoutParticipationCoordinator.kt`,
  `WorkoutRecordingService.kt`, `WorkoutSessionActivity.kt`,
  `WorkoutSessionOwnershipCoordinator.kt`, `WorkoutShells.kt` e
  `HomeViewModel.kt`.
- Documentação: este arquivo, `013-00-overview.md` e
  `docs/integration/013-closure-summary.md`.
- Nenhum teste automatizado foi criado, alterado ou executado.

### Comandos e resultados

```text
git diff --check — PASS
./gradlew lint assembleDebug assembleRelease — PASS
./gradlew installDebug — PASS; instalado em 1 dispositivo
adb devices — PASS; dispositivo Galaxy S22 disponível
```

### Gates manuais

- Gate A — PASS: Exercícios não exibe CTA phone; `Buscar treinos no relógio`
  permanece acessível.
- Gate B — PASS parcial: Activity antiga não exportada rejeitou intent externo;
  destinos restaurados são convertidos para recovery/Exercícios; countdown não
  apareceu. Retorno de back stack real não foi reproduzido.
- Gate C — PASS: tentativas de `ACTION_START` direto não produziram foreground
  service, notificação de treino, request ativo de localização ou owner visível.
  O `ServiceRecord` do shell permaneceu sem processo/foreground e foi tratado
  como evidência de tentativa externa, não runtime ativo.
- Gate D — PASS/N/A: não havia sessão phone histórica elegível no dispositivo;
  o cenário é impossível de produzir com segurança sem manipular o banco.
  Nenhum banco foi manipulado e nenhuma backdoor foi criada.
- Gate E — PASS: Champion associado; busca no relógio importou um treino pronto,
  registrando `inseridos 1`, `parciais 1`, `inválidos 0`, com atividade pendente
  de revisão. Nenhum foreground service, GPS ou sessão phone foi iniciado.

### Status, decisões e riscos

- `013-17` permanece `in_progress`; item do checklist permanece `[ ]` até revisão
  independente confirmar os critérios. Fase 013 permanece `closing`.
- Classes, modelos e estruturas phone legadas permanecem deliberadamente no
  código para leitura, compatibilidade e preservação histórica.
- Risco residual: gates D e parte de E não possuem evidência física nesta rodada;
  consumidores históricos do serviço continuam presentes, mas protegidos pelo
  guard central.
