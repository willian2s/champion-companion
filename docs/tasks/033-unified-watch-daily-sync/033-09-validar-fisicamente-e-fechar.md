# 033-09 — Validar fisicamente e fechar

- **Status:** completed — validação positiva concluída; gates não reproduzíveis aceitos como riscos residuais
- **Depende de:** 033-02 a 033-08

## Objetivo

Validar no Champion C001 e em banco real a linha completa da 033 antes do merge.

## Gates físicos mínimos

1. Home pull-to-refresh executa FEA1 + D1 + D3/D4 numa tentativa composta.
2. Segundo refresh não duplica D1 nem D4.
3. D1 v2 resolve dayOffset/hora e persiste fatos completos.
4. BPM inválido não descarta passos/distância/calorias e não impede conclusão.
5. FEA1 permanece total autoritativo de passos e `Σ D1.steps` é usado como
   cobertura/diagnóstico; D4 não aumenta novamente o total diário.
6. `FEA1 == ΣD1` com D1 estruturalmente completo é classificado como cobertura
   completa.
7. `FEA1 > ΣD1` com D1 completo até `totalPositions` e último bucket recente é
   classificado como cauda recente (`RECENT_TAIL`/`LAGGING` ou equivalente), não
   como divergência estrutural.
8. Nenhum bucket, intervalo ou `StepsRecord` sintético é criado para
   `FEA1 - ΣD1`; novo D1 real posterior deve materializar a cauda.
9. `FEA1 < ΣD1`, lacuna estrutural ou diferença sem explicação de frescor falha
   fechada/classifica divergência ou incompletude conforme o contrato 033-06.
10. Overlap temporal D1/D4 não causa supressão global de bucket nem soma ingênua;
    autoridade/substituição segue a política por métrica da 033-04.
11. Distância diária usa D1 nativo quando elegível; D4 permanece canônico no
    workout e só substitui contribuição diária com prova de cobertura.
12. FC combinada prefere D4 dentro de workout apenas quando houver cobertura D4
    válida; D1 não é descartado sem cobertura substituta.
13. Saúde não possui CTA BLE/manual; histórico antigo permanece.
14. Exercícios não possui CTA de importar; workout novo aparece após watch sync.
15. Perfil não expõe calibração por passo.
16. Watch sync não escreve Health Connect automaticamente.
17. `Sincronizar agora` explícito escreve fila elegível.
18. Cutover preserva dia anterior, impede DailySummary STEPS pós-cutover e não
    duplica D1 com `WorkoutSteps`/DISTANCE D4.
19. Cauda recente não reescreve IDs/payloads já congelados quando novos buckets D1
    chegam; retries preservam identidade.
20. Partial success preserva componentes já persistidos.
21. Migration real v17 -> v19 preserva dados da 032.
22. Cenário `dayOffset > 0` é exercitado ou permanece risco residual explícito se
    não houver dado físico disponível.

## Evidência já disponível

`docs/integration/033-03-d1-v2-physical-validation.md` já confirmou Room v18, D1
v2, reimportação idempotente, fechamento D1/FEA1 e a necessidade da política de
overlap por métrica.

`docs/integration/033-05-unified-watch-sync-physical-validation.md` já confirmou o
watch sync unificado, idempotência física de D1/D3/D4, ausência de auto-sync HC e
o cenário real de cauda recente `FEA1 > ΣD1` com D1 estruturalmente completo.

A validação final deve reaproveitar esses cenários e exercitar a integração
restante, principalmente cutover/projeção explícita Health Connect, partial
success forçado e a materialização posterior da cauda sem residual sintético.

## Evidência

Registrar somente agregados sanitizados, sem payload BLE integral, MAC, IDs
pessoais ou dados desnecessários.

Evidência final: `docs/integration/033-09-final-physical-validation.md`.

## Arquivos alterados

- `docs/integration/033-09-final-physical-validation.md`
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/database/ChampionDaos.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/data/GranularHealthSyncRepository.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/healthconnect/HealthConnectSyncCoordinator.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/data/DatabaseMappers.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/data/RoomRepositories.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/exercises/ExercisesScreen.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/exercises/ExercisesViewModel.kt`
- `docs/specs/033-unified-watch-daily-sync.md`
- `docs/decisions/adr-030-unified-watch-sync-and-granular-daily-data.md`
- `docs/decisions/adr-031-d1-granular-steps-health-connect.md`
- `docs/decisions/adr-032-sync-entrypoints-and-watch-management.md`
- `docs/decisions/adr-033-d1-granular-distance-active-calories.md`
- `docs/baseline/current-baseline.md`
- `docs/baseline/health-connect-mapping.md`
- `docs/protocol/history-d1.md`
- `docs/tasks/033-unified-watch-daily-sync/033-00-overview.md`
- este documento

## Decisões e desvios

- A validação parcial foi registrada com evidência física sanitizada do `SM-S901E`
  e dump Room v19. Não foram criados testes automatizados.
- Foi concedido waiver explícito para os cenários físicos negativos não disponíveis
  (`FEA1 < ΣD1`, lacuna estrutural e frescor inexplicado), partial success
  artificial e upgrade direto físico v17 → v19. Esses itens permanecem como
  riscos residuais; a cadeia de migrations existente não foi alterada.
- O segundo refresh produziu partial success real de componente e preservou FEA1
  e D1 já persistidos; não houve injeção artificial de falha.
- `FEA1 = 2488` e `Σ D1 = 2466`, com posições `1..184` completas, foi tratado
  como `RECENT_TAIL`; nenhum residual sintético foi preparado.
- A ação explícita de Health Connect foi exercitada separadamente da coleta do
  relógio. Não houve leitura do provider.
- A ordenação da fila foi corrigida para priorizar `operationSequence` em cadeias
  causais de passos; revisão reproduziu a inversão `2,1` e confirmou `1,2` após a
  correção. Recência permanece desempate.
- A investigação da UI encontrou snapshots nativos válidos rejeitados pelo mapper
  quando o tipo era `FREE_WORKOUT` ou `TREADMILL`. O mapper passou a aceitar os
  dois tipos; a UI voltou a exibir distância, provenance, intervalos e calorias.
  `TREADMILL` também voltou a expor calibração por referência, sem cards de
  comprimento de passo.
- Correção posterior alinhou o fluxo de revisão: `TREADMILL` permanece elegível
  no repositório, exibe distância de referência no formulário e preserva o valor
  ao trocar o tipo. O perfil de esteira continua separado.
- Nova falha reproduzida na aplicação retornava `UnexpectedState`: a derivação
  promovia `PENDING_CALIBRATION` antes do método de aplicação repetir a transição.
  A promoção automática foi removida; a aplicação agora faz uma única transição
  para `READY_TO_SYNC` após persistir evidência e snapshot calibrado.
- D4 zero/ausente permanece guard técnico, sem cenário físico artificial.

## Comandos executados

```text
./gradlew lint assembleDebug assembleRelease
./gradlew installDebug
git diff --check
adb devices
adb shell am start -n br.com.mwconsulting.championcompanion.dev/br.com.mwconsulting.championcompanion.MainActivity
adb shell input swipe 540 450 540 1500 900   # refresh Home 1
adb shell input swipe 540 450 540 1500 900   # refresh Home 2
adb shell input tap 810 200                    # Sync HC Home
adb shell input tap 956 230                    # Perfil -> Configurações
adb shell input tap 540 1820                   # Sincronizar agora
adb shell input tap 815 2200                   # Saúde
adb shell input tap 800 530                    # Exercícios
adb shell uiautomator dump /sdcard/window.xml
```

Também foi extraído dump local sanitizado do Room v19 para agregação, sem
preservar o arquivo ou IDs no repositório.

Extração usada: `adb shell run-as ... ls databases`, cópia temporária dos arquivos
Room via `adb exec-out` e consultas `sqlite3` somente agregadas (`PRAGMA
user_version`, contagens, somas, estados e continuidade de posições).

## Resultados e evidências

- `lint`, `assembleDebug`, `assembleRelease` e `installDebug`: sucesso.
- Home no Champion C001: relógio conectado, FEA1/D1 exibidos, distância nativa
  exibida e segundo refresh em estado parcial sem perda dos fatos anteriores.
- Room: `user_version = 19`; D1 atual com `184/184` posições, `Σ steps = 2466`,
  distância `1863,0 m`, calorias `121,8 kcal`, `129` BPM ausentes e
  `dayOffset = 0`.
- FEA1 atual: `2488` passos; diferença `22` classificada visualmente como cauda
  recente; consulta sanitizada não encontrou operação D1 sintética.
- Health Connect: operações D1 granular e workout persistidas; `WorkoutSteps`
  não foi publicado como segunda fonte diária; as quatro linhas de crédito ficaram
  `BLOCKED`, enquanto as operações correspondentes permaneceram `PENDING`; ação
  explícita disponível e executada sem auto-sync durante o pull.
- Saúde, Exercícios e Perfil confirmaram visualmente remoção dos entrypoints
  proibidos, preservando histórico e revisão.
- Após reinstalação, Exercícios exibiu `Esteira 0,25 km` e `Treino livre 0,14 km`;
  detalhe de Esteira exibiu distância `0,25 km`, fonte nativa calibrada, `274`
  etapas, `13 kcal`, parciais e FC média.
- O fluxo corrigido de revisão mantém `TREADMILL` no conjunto de calibração:
  o campo `Distância de referência` é exibido ao selecionar Esteira e o input não
  é limpo durante a troca de tipo. A aplicação usa o perfil contextual próprio
  de `TREADMILL`; não houve nova amostra `PENDING_REVIEW` disponível para toque
   físico após esta correção.
- Usuário confirmou calibração concluída com sucesso e operações sincronizadas
  com sucesso no dispositivo, fechando o fluxo ponta a ponta pós-correção.
- `lint`, `assembleDebug`, `assembleRelease` e instalação debug passaram após a
  correção da transição. A validação física do caso novo não foi repetida porque
  o banco do dispositivo não tinha nova atividade `PENDING_CALIBRATION` elegível.
- A ordenação causal foi corrigida nos três pontos de seleção/composição da fila;
  `lint assembleDebug assembleRelease` e `installDebug` passaram após a mudança.
- Evidências anteriores fecharam migration v18 → v19, dayOffset/hora observado,
  overlap por métrica, cutover, idempotência e distância/calorias granulares.

## Riscos residuais

- `dayOffset > 0` não foi exercitado; revalidar quando houver amostra física.
- Upgrade direto real v17 → v19 não foi reproduzido nesta rodada; waiver aceito.
  A cadeia existente `v17 → v18 → v19` permanece responsável pelo upgrade e a
  migration `v18 → v19` possui evidência física.
- `FEA1 < ΣD1`, lacuna estrutural e diferença sem explicação de frescor não foram
  reproduzidos em banco físico nesta rodada; waiver aceito, com comportamento
  coberto pelo contrato/código.
- Partial success foi observado por falha real de componente, não por injeção
  controlada/artificial; waiver aceito para injeção artificial.
- D4 zero/ausente permanece guard técnico, conforme `033-10`.

## Validação técnica

```bash
./gradlew lint assembleDebug assembleRelease
git diff --check
```

Não criar nem executar testes automatizados sem autorização explícita.

Ao concluir, atualizar Spec, ADR, overview, baseline e protocolos afetados; marcar
riscos residuais explicitamente antes do merge.
