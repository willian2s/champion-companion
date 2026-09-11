# 032 — Overview: D4 v4, métricas nativas e calibração de distância

- **Spec:** `docs/specs/032-d4-native-workout-metrics.md`
- **ADR:** `docs/decisions/adr-029-d4-native-metrics-and-distance-calibration.md`
- **Status:** `in_progress`
- **Progresso:** `7/8`
- **Branch de integração:** `feat/032-d4-native-workout-metrics`

## Objetivo

Corrigir o parser D3/D4 e promover a distância nativa do Champion C001 para a
arquitetura de treino sem perder a normalização temporal já validada, mantendo
legado reproduzível, fallback por comprimento de passo e Health Connect explícito.

## Sequência e paralelismo

```text
032-01 Congelar evidência e contratos       ✓
   ↓
   ├───────────────┐
   ↓               ↓
032-02 Parser    032-03 Persistência/schema
   │               │
   └───────┬───────┘
           ↓
032-04 Domínio + adapters + reprocessamento
           ↓
   ┌───────┴────────┐
   ↓                ↓
032-05 Distância  032-06 Calibração/aprendizado
   └───────┬────────┘
           ↓
032-07 Integração repository/UI/Health Connect
           ↓
032-08 Validação física e fechamento
```

## Baseline pós-031

A Spec 031 está concluída e integrada à `main`. A branch 032 foi atualizada sobre
essa baseline e, na reauditoria pós-031 de `032-01`, a comparação confirmou:

```text
main...feat/032-d4-native-workout-metrics
status: ahead
behind_by: 0
merge-base: 6025fbaaf119727fbb95c2f51e6f16946ef646ea
```

A reauditoria está registrada em:

`docs/integration/032-01-post-031-reaudit.md`.

Conclusões relevantes:

- nenhuma decisão técnica do contrato D3/D4 v4 foi invalidada pela 031;
- Room permanece efetivamente em v15;
- `ChampionD3WorkoutIndexProtocol` mantém o bug de compressão de slot com CRC zero;
- `ChampionD4WorkoutParser` continua em `d4-v1/v2/v3` e ainda não conhece o layout v4;
- `RoomRepositories.persistImported(...)` continua sendo o caminho de persistência/reimportação;
- Saúde agora consome FC de `WATCH_WORKOUT_ACTIVITY`, portanto BPM D4 também alimenta históricos/gráficos de Saúde;
- `HealthRecordMapper`, contratos de projeção e coordenador de sync de workout não foram redefinidos pela 031;
- a pré-condição antiga de integrar a 031 em `032-07` já está satisfeita.

### Ownership inicial da 032

`032-02` e `032-03` continuam independentes e devem evitar mistura desnecessária de
responsabilidades:

- `032-02`: priorizar `core/bluetooth` e contratos de parser;
- `032-03`: priorizar `core/database`, migrations, schema exportado e adapters mínimos de persistência;
- `data/RoomRepositories.kt` só deve entrar antes de `032-04/032-07` se a persistência não puder ser concluída corretamente sem ele;
- `feature/exercises/ExercisesScreen.kt`, design system e navegação permanecem fora de `032-02/032-03`.

A razão agora é separação de escopo, não conflito com uma branch 031 paralela.

### Integração tardia

`032-07` continua sendo o ponto deliberado para:

1. integrar repository/read models/UI sobre a baseline pós-031 já presente;
2. preservar o redesign e os históricos de Saúde que agora incluem FC de workout;
3. atualizar Health Connect sem alterar os guards de passos da Spec 027;
4. evitar que consumidores finais conheçam offsets ou packing raw D4.

Não há nova etapa planejada de merge com a 031.

## Branches/worktrees sugeridos

Depois da `032-01`, parser e persistência estão formalmente independentes e podem
ser executados em paralelo a partir da mesma revisão da branch de integração:

```text
feat/032-d4-native-workout-metrics
├── task/032-02-d4-v4-parser
└── task/032-03-d4-v4-persistence
```

No ambiente local, usar um `git worktree` por branch permite duas sessões/agentes
sem compartilhar working tree:

```bash
git worktree add ../champion-032-parser task/032-02-d4-v4-parser
git worktree add ../champion-032-schema task/032-03-d4-v4-persistence
```

Depois de uma subtarefa revisada, integrar na branch 032; não integrar direto em
`main` enquanto a spec estiver aberta.

## Checklist

- [x] `032-01-auditar-e-congelar-layout-d4-v4.md` — contrato congelado e revalidado sobre a baseline pós-031
- [x] `032-02-implementar-parser-d3-d4-v4.md` — parser v4 e índice D3 físico implementados; na conclusão da subtarefa, runtime efetivo permanecia d4-v3
- [x] `032-03-persistir-fatos-nativos-e-provenance.md` — Room v16, fatos nativos, header D4 v4, provenance e migration aditiva implementados
- [x] `032-04-integrar-dominio-adapters-e-reprocessamento.md` — domínio v4, adapters, reidratação de header e reprocessamento local implementados
- [x] `032-05-derivar-distancia-nativa-e-metricas.md` — distância nativa elegível, fallback explícito e métricas recalculadas sobre T/N implementados
- [x] `032-06-implementar-calibracao-de-distancia-contextual.md` — evidência nativa, perfis contextuais, fator proporcional, provenance e contratos de estado/ação implementados
- [x] `032-07-integrar-produto-health-connect-e-spec-031.md` — runtime d4-v4, review/rederivação, UI de provenance e integração Health Connect preservados sobre baseline pós-031
- [ ] `032-08-validar-fisicamente-e-fechar.md`

## Observações

032-02 concluída com builds e lint passando e revisão independente aprovada. Os
testes focados existentes continuam incompatíveis com fixtures legacy e com a
correção deliberada de slots físicos D3; nenhum teste foi alterado. Validação
física e integração de persistência/derivação permanecem nas subtarefas seguintes.

032-03 concluída com `MIGRATION_15_16`, schema exportado v16, campos nativos por
intervalo, header D4 v4 persistido e mappers compatíveis com legacy. Build, lint e
`git diff --check` passaram; revisão independente foi aprovada. Naquela subtarefa,
header ainda não era reidratado no domínio após reload; responsabilidade planejada
para 032-04 e concluída abaixo.

032-04 concluída com adapters measured/source explícitos para v1/v2/v3/v4,
reidratação estrita do header v4 em leituras locais e Health Sync, reprocessamento
local compatível com fatos nativos e bloqueio fail-closed de reimportação diferente
em atividades `SYNCED`. Na conclusão da subtarefa, runtime permanecia `d4-v3`;
distância nativa canônica e calibração foram entregues nas subtarefas 032-05 e
032-06, e promoção para `d4-v4` foi concluída em 032-07.

032-05 concluída com elegibilidade nativa fail-closed contra header e série,
provenance `WATCH_NATIVE`/`STEP_LENGTH_FALLBACK`, preservação independente de
passos e cadência, métricas derivadas sobre duração efetiva e read path Room v16.
Health Connect ainda precisa mapear provenance nativa em 032-07; validação física
da dinâmica temporal permanece em 032-08.

032-06 concluída com migration aditiva para Room v17, evidência de distância
nativa separada da calibração legacy, agregação ponderada independente para
RUNNING/WALKING, aplicação proporcional por intervalo, fallback por comprimento
de passo, rederivação explícita, remoção sem mutação de snapshot histórico e
provenance compatível com Health Connect. Contratos de estado/ação foram
  ampliados sem redesenho visual. A integração final de produto foi concluída em
  032-07; validação física permanece em 032-08.

032-07 concluída sobre a baseline pós-031. Novas importações selecionam `d4-v4`,
persistência inicial e review materializam derivação nativa quando elegível,
calibração usa passos nativos sem packing legacy e Exercícios exibe provenance,
distância nativa e fator aplicado sem reverter o redesign. Health Connect mantém
projeções congeladas, IDs/versionamento, retry explícito, histórico
`WATCH_WORKOUT_ACTIVITY`, STEPS/STEPS_CADENCE e crédito da Spec 027. Build,
lint e `git diff --check` passaram; validação física permanece em 032-08.

Correção posterior à validação física: o review de nova atividade `d4-v4` agora
considera incompatível qualquer snapshot fallback por comprimento de passo após
confirmação `RUNNING`/`WALKING`, mesmo quando a versão da derivação é
`watch-d3-d4-v3`. A rederivação nativa ou nativa contextual ocorre antes de
`READY_TO_SYNC`; tipos não elegíveis, legacy e `SYNCED` preservam seus guards.
Build, lint e `git diff --check` passaram novamente.

Complemento de produto: série `d4-v4` íntegra agora recebe `WATCH_NATIVE` já na
importação, sem inferir tipo nem aplicar fator contextual. A revisão de
`RUNNING`/`WALKING` reavalia fator aprendido; tipos não elegíveis retornam às suas
políticas próprias e `STEP_LENGTH_FALLBACK` permanece reservado a ausência ou
inelegibilidade da distância nativa.

A linha visual `Fonte da distância` agora trunca valores longos com reticências em
uma linha, preservando descrição semântica completa.

Review v4 sem snapshot permanece fail-closed: somente `TREADMILL` pode aguardar
`PENDING_CALIBRATION`; demais tipos não chegam a `READY_TO_SYNC` sem derivação.

Revisão independente posterior corrigiu a fronteira transacional do review:
falha ou ausência de derivação v4 desfaz a tentativa antes de persistir
`READY_TO_SYNC`. Health Connect também bloqueia explicitamente `d4-v4` sem
snapshot, sem alterar a política de legado ou de outros tipos. Compilação,
lint, builds debug/release e `git diff --check` passaram; não foram criados nem
executados testes automatizados.

## Resultado da 032-01

Contrato autoritativo original:

`docs/integration/032-01-d4-v4-layout-and-consumer-audit.md`.

Revalidação pós-031:

`docs/integration/032-01-post-031-reaudit.md`.

Achados congelados:

- D3 usa posição física de slot; CRC zero não pode comprimir `blockIndex`;
- header fixo D4 é reconstruído em 42 bytes a partir dos pacotes 1–3;
- sample fixo: BPM, `sportValue`, passos u16, calorias raw u16, distância u16 e pausa raw;
- 34 blocos completos fecharam 34/34 em `recordCount`, passos, distância,
  calorias raw, sport count e CRC;
- distância está confirmada em metros para o layout fixo observado;
- calorias usam escala `raw / 1000.0 = kcal`;
- `sportValue` permanece raw sem unidade funcional inventada;
- pausa permanece raw por falta de caso físico com pausa no conjunto auditado;
- timeline `T/N` da Spec 021 permanece autoritativa;
- persistência v4 deve usar campos semanticamente novos sem reutilizar o
  significado legacy de `rawMotionValue/rawDistanceValue/rawCaloriesValue`;
- divergência entre header e série preserva ambos os lados e falha fechada para
  distância nativa canônica;
- FC D4 válida também é consumida pelos históricos de Saúde no baseline pós-031.

`docs/protocol/workouts-d3-d4.md` permanece como contrato de protocolo que registra
`d4-v4` como runtime efetivo de novas importações e preserva `d4-v1/v2/v3` como
histórico identificável.

### Preflight local antes de 032-02/032-03

A branch já contém a `main` pós-031. Antes de começar código em um worktree local,
executar:

```bash
git checkout feat/032-d4-native-workout-metrics
git pull
git diff --check main...HEAD
```

Não há build necessário para a reauditoria de `032-01`, pois nenhum arquivo de
runtime foi alterado por ela.

## Guard rails

- preservar `T/N` da Spec 021;
- não transformar `recordCount` em duração;
- não usar total de passos do header para substituir a série temporal;
- não criar GPS de produção ou integração Adidas/Strava;
- não mudar `CYCLING` implicitamente;
- não converter calibrações antigas de passo para fator de distância;
- não alterar atividades `SYNCED` silenciosamente;
- não criar efeitos Health Connect durante migration/reprocessamento;
- preservar os consumidores de FC de workout introduzidos/ajustados no baseline pós-031;
- não criar testes automatizados sem pedido explícito;
- cada task atualiza este overview.

## Critério de merge da branch 032

A branch só está pronta para `main` quando:

- parser v4, schema, derivação e provenance estão coerentes juntos;
- runtime não publica `d4-v4` parcial;
- integração repository/UI preserva integralmente a baseline pós-031;
- validação física demonstrou pelo menos um treino com distância nativa;
- `./gradlew lint assembleDebug assembleRelease` e `git diff --check` passam.
