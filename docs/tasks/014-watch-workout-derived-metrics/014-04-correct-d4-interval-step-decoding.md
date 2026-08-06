# 014-04 — Corrigir parser D4 e incrementar `parserVersion`

- **Ticker:** `014`
- **Número:** `04`
- **Status:** completed
- **Concluída em:** 2026-08-06

## Objetivo e resultado esperado

Corrigir a decodificação de passos temporais, preservar os valores raw e publicar
uma nova versão de parser, inicialmente `d4-v2`.

Resultado: novas importações distinguem claramente o parser corrigido do legado
`d4-v1`, e a sequência observada soma `3.737` passos sem atribuir semântica aos
bits inferiores.

## Dependências

- `014-03` concluída.

## Arquivos permitidos

- `core/bluetooth/ChampionD4WorkoutParser.kt`;
- contratos D4 estritamente necessários;
- coordinator/importação para selecionar `d4-v2`;
- mappers/repository para gravar raw e measured;
- `docs/protocol/workouts-d3-d4.md`;
- overview.

## Arquivos proibidos

- UI, perfil e Health Connect;
- Gradle e manifesto;
- migrations/schemas após `014-03`, salvo bloqueio documentalmente revisado;
- infraestrutura phone/Fase 013.

## Escopo

```kotlin
val rawMotionValue = bytes.readUInt24LittleEndian(7)
val decodedSteps = rawMotionValue ushr 16
```

- preservar `rawMotionValue`;
- persistir `decodedSteps` separadamente;
- preservar raw de distância e calorias temporais;
- interromper promoção desses campos raw a métricas semânticas;
- manter BPM, ordenação, duplicatas e parcialidade existentes;
- usar `parserVersion = d4-v2` em novas importações/reimportações corrigidas.

## Fora do escopo

- duração efetiva e plausibilidade;
- distância canônica;
- interpretação dos 16 bits inferiores;
- backfill automático de atividades antigas;
- testes automatizados.

## Passos de implementação

1. Ler u24 uma única vez como `rawMotionValue`.
2. Derivar passos pelo deslocamento de 16 bits.
3. Persistir raw e measured nos contratos de `014-02/03`.
4. Preservar distância/calorias temporais somente como raw.
5. Incrementar a versão do parser para `d4-v2` em todos os pontos de criação.
6. Garantir que `d4-v1` continue identificável em dados legados.
7. Não substituir silenciosamente os passos totais do pacote 2.
8. Atualizar protocolo e overview.

## Critérios de aceite

- `0x0064002B` produz raw `6.553.643` e `100` passos.
- A sequência de referência soma exatamente `3.737`.
- Os 16 bits inferiores permanecem desconhecidos.
- Valores raw sobrevivem ao round-trip Room.
- Novas importações registram `d4-v2`.
- Dados `d4-v1` não são reclassificados sem reprocessamento explícito.
- Totais da atividade não são silenciosamente substituídos.

## Validação

```bash
./gradlew lint assembleDebug assembleRelease
./gradlew installDebug
git diff --check
git status --short
```

Não criar nem executar testes automatizados.

## Evidências

- tabela sanitizada índice → raw → passos;
- soma de referência;
- parser version antes/depois;
- builds e smoke de importação.

## Implementação concluída

- `ChampionD4WorkoutParser` lê `bytes[7..9]` uma única vez como
  `rawMotionValue` e deriva `decodedSteps` somente para `d4-v2`.
- `WorkoutInterval` transporta `rawMotionValue`, `rawDistanceValue`,
  `rawCaloriesValue` e `decodedSteps`, com validação por versão.
- `d4-v1` e `d4-v2` usam `DomainValue.Absent` para distância e calorias
  temporais; os valores raw seguem separados. O total de passos do pacote 2
  continua em `WorkoutMetrics.steps`.
- `ChampionWatchWorkoutImportCoordinator` passou a criar novas importações com
  `parserVersion = d4-v2`.
- Mappers não promovem distância/calorias temporais de nenhuma versão D4 a
  colunas semânticas em novas persistências; preservam `d4-v1` sem
  `decodedSteps`, mantêm valores temporais legados já persistidos identificáveis
  e transportam os fatos raw para as colunas Room v9 existentes. Nenhuma
  migration ou schema foi alterado.
- Lookup de retomada carrega `parserVersion`; `canSkipOnResume` exige `VALID` e
  versão D4 conhecida. Versões desconhecidas ou ausentes falham fechadas e são
  reimportadas.
- Protocolo documenta o comportamento implementado e a compatibilidade legada.

Tabela sanitizada da evidência de referência:

| índice | rawMotionValue | decodedSteps |
|---:|---:|---:|
| 0 | `0x0064002B` = `6.553.643` | `100` |

Os 16 bits inferiores são `0x002B` (`43`) e permanecem desconhecidos. O comando
de soma da sequência documentada retornou `28` amostras e `3.737` passos.

## Arquivos alterados

- `app/src/main/java/br/com/mwconsulting/championcompanion/core/bluetooth/ChampionD4WorkoutParser.kt`;
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/database/ChampionDaos.kt`;
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/model/WatchWorkoutActivity.kt`;
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/model/RepositoryContracts.kt`;
- `app/src/main/java/br/com/mwconsulting/championcompanion/data/DatabaseMappers.kt`;
- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/exercises/ChampionWatchWorkoutImportCoordinator.kt`;
- `docs/protocol/workouts-d3-d4.md`;
- este arquivo e `014-00-overview.md`.

## Decisões e desvios

- Campos raw/measured foram adicionados com valores padrão ao contrato
  `WorkoutInterval`, preservando callers existentes.
- Versões D4 desconhecidas falham fechadas na criação da atividade; somente
  `d4-v1` e `d4-v2` são aceitas.
- O lookup de retomada preserva `parserVersion` como dado opcional para manter
  compatibilidade estrutural; `canSkipOnResume` permanece falso quando a versão
  está ausente ou não é `d4-v1`/`d4-v2`.
- A remoção da promoção temporal vale para os caminhos explícitos `d4-v1` e
  `d4-v2`; linhas legadas já persistidas continuam preservadas e identificáveis,
  sem alteração de migration ou schema.
- Não houve desvio de escopo: Room v9, migrations, UI, Health Connect e
  infraestrutura phone permaneceram intocados.

## Comandos executados e resultados

- `./gradlew lint assembleDebug assembleRelease` — PASS; warnings preexistentes
  de override deprecated em `RoomRepositories.kt` e recomendação preexistente
  de índice para `associatedDeviceId`.
- `./gradlew installDebug` — PASS; instalado no Galaxy S22 `SM-S901E`.
- `git diff --check` — PASS.
- Revisão independente após correções — APROVADA, sem bloqueadores.
- `git status --short` — somente arquivos desta subtarefa, contratos de retomada,
  spec e protocolo; nenhuma migration/schema/Gradle/UI foi alterada.
- Testes automatizados — não criados nem executados, conforme escopo explícito.

## Riscos residuais

- Não houve importação física D4 neste ambiente; `installDebug` comprova apenas
  instalação do APK. Smoke de importação física permanece para `014-15`.
- Round-trip Room foi validado por schema v9, mappers e compilação; execução de
  teste automatizado foi proibida pela task.
- Registros legados com parser ausente ou desconhecido não são pulados na
  retomada; exigem reimportação explícita, que permanece dependente de BLE quando
  os fatos raw não estiverem persistidos.
- Unidade e semântica dos 16 bits inferiores, distância temporal e calorias
  temporais continuam desconhecidas por decisão de privacidade e proveniência.
- `docs/integration/014-01-d4-layout-and-consumer-audit.md` ainda registra nomes
  internos anteriores (`SampleData.stepsDelta`) e parser padrão `d4-v1`; revisão
  recomendou atualização futura, fora do escopo permitido desta subtarefa.

## Definição de pronto

Parseres `d4-v1` e `d4-v2` persistem fatos raw/measured corretamente, preservam
legado identificável, retomada falha fechada para versões desconhecidas e item
`014-04` está marcado.

## Rollback

Interromper uso de `d4-v2` sem apagar campos raw já persistidos. Não voltar a
tratar o u24 completo como passos canônicos.
