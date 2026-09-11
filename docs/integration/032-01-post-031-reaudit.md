# 032-01 — Reauditoria pós-031

- **Task original:** `032-01`
- **Spec:** `docs/specs/032-d4-native-workout-metrics.md`
- **Branch auditada:** `feat/032-d4-native-workout-metrics`
- **Data da reauditoria:** 2026-09-10
- **Baseline comum confirmada:** `main` em `6025fbaaf119727fbb95c2f51e6f16946ef646ea`
- **Resultado:** contrato D3/D4 v4 permanece válido; `032-02` e `032-03` continuam liberadas
- **Mudança de runtime nesta reauditoria:** nenhuma

## Motivo

A auditoria original de `032-01` foi executada enquanto a Spec 031 ainda estava em
branch paralela. Depois da conclusão e integração da 031, a branch 032 foi
atualizada sobre a nova `main`. Esta reauditoria verifica se o redesign ou seus
ajustes de read model alteraram algum pressuposto necessário para parser,
persistência, derivação ou projeção D4 v4.

## Estado da branch

A comparação `main...feat/032-d4-native-workout-metrics` após a atualização da
branch mostrou:

```text
status: ahead
ahead_by: 19
behind_by: 0
merge-base: 6025fbaaf119727fbb95c2f51e6f16946ef646ea
```

Os arquivos próprios da 032 continuam documentais neste ponto. Nenhuma mudança de
runtime da 032 foi iniciada antes desta reauditoria.

## Baseline pós-031 confirmado

A Spec 031 está concluída e integrada. O produto atual possui Home e Saúde no shell
principal, com demais rotas contextuais. A Home preserva `PullToRefreshBox` e o
refresh diário existente; Exercícios e Saúde foram redesenhados sem redefinir o
protocolo D3/D4.

Room está efetivamente em **v15** em `ChampionDatabase`, portanto o ponto de partida
planejado para `032-03` permanece `15 -> 16`, sujeito à auditoria normal da própria
task antes de qualquer migration.

## Revalidação do parser atual

### D3

`ChampionD3WorkoutIndexProtocol.IndexCollector` continua descartando `crc == 0`
antes de incrementar `nextOriginalBlockIndex`.

Consequência: o bug congelado pela auditoria original permanece real. `032-02`
deve continuar preservando a posição física de todo slot D3, inclusive slot com
CRC zero, e somente depois decidir se haverá solicitação D4.

### D4

`ChampionD4WorkoutParser` continua limitado a `d4-v1/v2/v3` e ainda interpreta o
sample legado por `rawMotionValue`, `rawDistanceValue` e `rawCaloriesValue`, com
`decodedSteps` derivado por shift do packing histórico. O parser v4 ainda não foi
implementado.

Portanto, permanecem válidos sem alteração os offsets congelados em `032-01`:

```text
byte  6      BPM raw
bytes 7..8   sportValue u16 LE
bytes 9..10  steps u16 LE
bytes 11..12 caloriesRaw u16 LE
bytes 13..14 distanceMeters u16 LE
byte  15     pauseStateRaw
```

Também permanecem válidos o header reconstruído em 42 bytes, as unidades já
confirmadas e a regra de que BPM inválido não descarta os demais fatos do sample.

## Persistência e reimportação

A 031 alterou `RoomRepositories` para necessidades de apresentação/read model, mas
o caminho `persistImported(...)` de atividades do relógio continua presente com a
mesma responsabilidade de deduplicação/reimportação. Não foi introduzido novo
significado para os campos raw legados de D4.

Logo, a fronteira de `032-03` permanece:

- novos fatos v4 usam campos semanticamente novos;
- `rawMotionValue`, `rawDistanceValue` e `rawCaloriesValue` mantêm significado
  histórico de `d4-v1/v2/v3`;
- fatos do header permanecem ligados à importação;
- migration não cria derivação, calibração, workflow ou efeito Health Connect.

## Delta de consumidores após a 031

O principal delta funcional encontrado está nos consumidores de leitura, não no
protocolo.

### Saúde / frequência cardíaca

O histórico de Saúde agora inclui explicitamente amostras de
`HeartRateSource.WATCH_WORKOUT_ACTIVITY` vindas de
`workout_heart_rate_samples`. Assim, BPM D4 válido não alimenta apenas o detalhe do
workout: ele também participa das análises e gráficos de frequência cardíaca.

Implicação para `032-02` e `032-04`:

- preservar `sourceSampleIndex` e timestamp efetivo normalizado;
- BPM fora da faixa aceita fica ausente para FC;
- a invalidez do BPM nunca elimina passos, distância, calorias, `sportValue` ou
  pausa do mesmo sample;
- adapters v4 não podem quebrar a origem `WATCH_WORKOUT_ACTIVITY` usada por Saúde.

### Exercícios

`ExercisesScreen` foi amplamente redesenhada, mas continua sendo consumidora de
read models/métricas canônicas. A UI não deve conhecer offsets nem packing raw D4.
A nova provenance de distância deve chegar por contratos de domínio/read model e
ser integrada apenas na task de produto correspondente, preservando o redesign.

### Calendário e demais históricos

Calendário e históricos continuam consumidores de dados semânticos/canônicos.
Não foi encontrada necessidade de expor fatos raw v4 diretamente a essas
superfícies.

### Health Connect

A 031 alterou `HealthConnectPlatform` na camada de disponibilidade/navegação do
provider, mas não alterou `HealthRecordMapper`, `HealthProjectionContracts` ou o
coordenador de sync de workout. Permanecem válidos os contratos da Spec 027 para
passos/crédito e as projeções congeladas de distância, velocidade, cadência e FC.

A 032 continua proibida de recalcular ou republicar silenciosamente records já
aceitos.

## Impacto sobre as tasks seguintes

### `032-02`

Sem mudança de escopo. Pode começar imediatamente sobre a branch atualizada.

### `032-03`

Sem mudança de escopo. Room v15 foi reconfirmado como baseline real.

### `032-04` a `032-06`

Continuam dependentes do parser/schema coerentes. A inclusão de FC de workout nos
históricos de Saúde deve ser considerada nos adapters, sem mudar a regra temporal
`T/N`.

### `032-07`

A antiga pré-condição de atualizar a branch para uma `main` contendo a 031 já está
**satisfeita**. `RoomRepositories.kt` e `ExercisesScreen.kt` continuam arquivos
compartilhados importantes, mas não são mais pontos de conflito entre branches
paralelas; devem ser editados sobre o baseline pós-031 já incorporado.

## Decisões preservadas

A reauditoria não altera:

- posição física D3;
- framing/header D4;
- layout do sample v4;
- metros como unidade de distância fixa observada;
- `caloriesRaw / 1000.0 = kcal` como escala confirmada;
- `sportValueRaw` sem significado fisiológico inventado;
- pausa preservada raw;
- `T/N` da Spec 021;
- série de passos por intervalos como fato de workout;
- header como validação/diagnóstico, não substituto da série;
- distância nativa elegível como base da derivação prevista pela 032;
- compatibilidade e provenance de `d4-v1/v2/v3`;
- Health Connect write-only, local-first e explícito;
- ausência de GPS/rota de produção;
- proibição de testes automatizados sem solicitação explícita.

## Conclusão

A atualização para a `main` pós-031 **não invalida nenhuma decisão técnica da
032-01**. Ela apenas muda a situação de integração: a 031 deixou de ser trabalho
paralelo e passou a ser baseline obrigatório já incorporado.

`032-02` e `032-03` permanecem liberadas e agora podem ser implementadas sem uma
segunda etapa de merge com a 031. A integração tardia deve preservar os novos read
models e o redesign já presentes na branch.