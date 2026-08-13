# 023-06 — Integrar comprimento contextual à derivação e às projeções

- **Status:** `in_progress`

## Escopo desta execução

Implementar evolução retrocompatível do frozen payload Health Connect para
identificar `WATCH_DERIVED` e `EXTERNAL_REFERENCE_CALIBRATION`, sem alterar
identidade de operações, criar efeitos remotos ou concluir a integração inteira.
- **Depends on:** `023-05`

## Objetivo

Fazer novos treinos de corrida/caminhada usarem o perfil aprendido correto sem
alterar silenciosamente atividades históricas e alinhar consumers locais/Health
Connect com a nova provenance de calibração.

## Nova versão de derivação

Introduzir versão identificável para a política contextual:

```text
watch-d3-d4-v3
```

O parser permanece `d4-v3`.

Snapshots antigos `watch-d3-d4-v2` continuam legíveis e não são migrados.

## Resolver perfil efetivo

Implementar resolução central, fora da UI:

```text
RUNNING -> learned RUNNING ?: BASE
WALKING -> learned WALKING ?: BASE
TREADMILL -> BASE
FREE_WORKOUT -> comportamento vigente
CYCLING -> sem distância por passos
DailySummary -> BASE
```

A resolução precisa ocorrer de forma coerente com o momento em que o tipo foi
confirmado, conforme decisão congelada em `023-01`.

## Importação e revisão

Evitar usar perfil contextual errado antes de `confirmedType` existir.

Direção preferida:

- importação inicial mantém derivação/base atual quando necessário;
- revisão de RUNNING/WALKING pode rederivar explicitamente com perfil contextual
  antes do sync;
- nova atividade após perfil aprendido deve terminar com snapshot que identifique
  o valor/contexto realmente usado;
- mudança de tipo não reaproveita snapshot semântico incompatível.

## Consumers locais

Auditar/adaptar:

- lista de Exercícios;
- detalhe da atividade;
- Calendário/detalhe diário;
- históricos de distância;
- splits/pace/velocidade;
- qualquer read model que assuma um único `stepLengthMeters` atual.

Todos devem consumir o snapshot congelado da atividade, nunca o perfil pessoal
corrente para recalcular histórico em leitura.

## Perfil UI

Conectar os perfis aprendidos à tela Perfil:

```text
Corrida
0,846 m por passo
2 calibrações • 8,36 km de referência

Caminhada
0,721 m por passo
1 calibração • 3,12 km de referência
```

Sem evidência:

```text
Ainda sem calibrações de corrida.
Será usado o comprimento de passo base.
```

Mostrar acesso ao histórico/correção de evidências.

## Health Connect

Auditar o contrato pós-Spec 022 para atividades diretamente calibradas por
referência.

Requisitos:

- sessão/BPM/calorias mantêm provenance vigente;
- distância/velocidade diretamente baseadas em referência digitada não podem ser
  apresentadas como GPS coletado pelo app;
- preservar IDs determinísticos, versões monotônicas e frozen payloads;
- não duplicar `DistanceRecord`;
- não quebrar operações antigas;
- não criar efeitos remotos durante rederivação/calibração;
- sync continua explícito.

Se for necessário adicionar provenance ao frozen payload, preferir evolução
retrocompatível e documentar versão/decoder. Não aumentar schema por conveniência.

Treino futuro que apenas usa perfil aprendido continua seguindo a projeção
canônica automática vigente, pois a distância daquele treino não foi digitada.

## Daily Summary

Confirmar explicitamente que a estimativa diária continua usando `BASE`.

Não substituir por walking/running learned profile porque passos do dia não têm
contexto confiável.

## Compatibilidade 022

Confirmar:

- `CYCLING` continua com `manualDistanceMeters` opcional;
- nenhum learned step profile é consumido por bicicleta;
- nenhuma `DistanceInterval`/`SpeedSeries` por passos retorna para CYCLING.

## Checklist

- [ ] derivation version nova introduzida
- [ ] resolver contextual centralizado
- [ ] RUNNING usa learned/fallback corretamente
- [ ] WALKING usa learned/fallback corretamente
- [ ] TREADMILL continua BASE
- [ ] DailySummary continua BASE
- [ ] CYCLING permanece isolado
- [ ] consumers usam snapshot congelado
- [ ] Perfil mostra learned profiles/evidências
- [x] HC provenance auditada e implementada
- [x] payloads antigos continuam legíveis
- [ ] sync continua explícito
- [ ] spec/overview atualizados

## Aceite

Depois de duas calibrações de corrida, um novo treino RUNNING sem distância de
referência deve usar o perfil aprendido corrente e congelá-lo em seu próprio
snapshot, permanecendo imutável quando calibrações futuras alterarem o perfil.

## Resultado da execução

- **Status:** `in_progress`
- **Escopo executado:** recorte autorizado de evolução do frozen payload Health
  Connect para schema v2 com `metricProvenance`.
- **Arquivos alterados nesta execução:**
  - `app/src/main/java/br/com/mwconsulting/championcompanion/core/model/HealthProjectionContracts.kt`;
  - `app/src/main/java/br/com/mwconsulting/championcompanion/core/healthconnect/HealthRecordMapper.kt`;
  - esta subtarefa;
  - `docs/tasks/023-personal-metrics-calibration/023-00-overview.md`.
- **Decisões e desvios:** payloads novos usam schema 2; payloads schema 1
  ausentes de `metricProvenance` assumem `WATCH_DERIVED`; schema desconhecido,
  provenance desconhecida ou combinação inválida falham fechados. Distância e
  velocidade usam `WATCH_DERIVED` para perfil base/perfil contextual e
  `EXTERNAL_REFERENCE_CALIBRATION` somente para
  `REFERENCE_DISTANCE_CALIBRATION`. Sessão, BPM, calorias e `ManualDistance` de
  `CYCLING` não recebem provenance externa. Nenhum desvio.
- **Compatibilidade preservada:** chave da operação, IDs, versionamento
  monotônico, `originId = activity.id` e `originVersion` do snapshot continuam
  sob contratos existentes; enqueue, reconciliação e sync automático não foram
  alterados.
- **Comandos executados:** `./gradlew :app:compileDebugKotlin --no-daemon`,
  `./gradlew :app:lintDebug --no-daemon`, `git diff --check`.
- **Resultados e evidências:** compilação Kotlin, lint debug e verificação de
  whitespace passaram. Nenhum teste automatizado novo foi criado ou executado.
- **Revisão independente:** decoder v1 corrigido para aplicar `WATCH_DERIVED`
  somente a distância/velocidade; filhos de sessão, BPM, calorias e
  `ManualDistance` preservam ausência de provenance. Schema/progresso da spec
  alinhados em `4/7`.
- **Riscos e bloqueios:** validação física de payload v2, retry e Health Connect
  permanece bloqueada para `023-07`; `023-05` está concluída. Resolver
  contextual, consumers, Perfil e fechamento integral de `023-06` permanecem
  fora deste recorte e não autorizam marcar a subtarefa como concluída.
- **Correção operacional:** o detalhe de Exercícios agora expõe exclusão
  permanente somente após prévia fail-closed do repository. A ação usa intent
  confirmável, dialog irreversível com data/tipo, fecha o detalhe após sucesso
  e recarrega a lista pela observação existente. Esta correção não conclui a
  integração contextual nem a validação física.
- **Validação da correção:** `./gradlew :app:compileDebugKotlin --no-daemon`,
  `./gradlew lint assembleDebug assembleRelease --no-daemon` e
  `git diff --check` passaram. Nenhum teste automatizado foi criado ou
  executado; validação física permanece bloqueada para `023-07`.
