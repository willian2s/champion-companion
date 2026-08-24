# 027-11 — Validar e fechar a Spec 027

- **Ticker:** `027`
- **Número:** `11`
- **Status:** `blocked`
- **Depends on:** `027-10`

## Objetivo

Executar validação final integrada e fechar a spec com baseline factual.

## Validar integridade de passos

- atividade antes do Daily;
- Daily antes da atividade;
- retry sem duplicidade;
- múltiplas atividades quando reproduzível;
- mudança de dia/reset conforme política final;
- total diário final não excede total observado por efeito desta spec;
- crédito não é consumido antes de efeito remoto seguro.

## Validar cadência

- cadência por intervalo local coerente;
- `StepsCadenceRecord` aceito quando elegível;
- resultado Health Sync/Strava documentado;
- comportamento quando passos da atividade estão bloqueados por cobertura diária.

## Validar timeline 1 Hz

- BPM e cadência compartilham a mesma grade temporal;
- gaps permanecem ausentes;
- pontos interpolados não substituem SOURCE;
- início/fim da sessão corretos;
- série longa não causa regressão visual evidente;
- versão do algoritmo permanece identificável;
- estatísticas históricas não mudam silenciosamente.

## Health Connect

Confirmar:

- identities estáveis;
- retry determinístico;
- nenhuma duplicação de `StepsRecord`;
- decisão final bucket vs 1 Hz para `StepsCadenceRecord`;
- sync continua explícito e write-only;
- nenhuma leitura HC introduzida.

## Documentação

Atualizar:

- `docs/specs/027-workout-steps-cadence-1hz-timeline.md`;
- `027-00-overview.md`;
- `docs/baseline/current-baseline.md`;
- ADR correspondente;
- docs de integração/evidência;
- `AGENTS.md` somente se houver nova regra operacional duradoura.

Não apagar histórico da Spec 016 nem reclassificar specs anteriores.

## Classificação final

Registrar separadamente:

- integridade de passos;
- Health Connect;
- Health Sync/Strava;
- timeline local 1 Hz.

É permitido fechar como `completed` com limitações externas claramente documentadas, desde que integridade local/remota de passos esteja preservada.

## Verificação

Para código/Gradle:

```bash
./gradlew lint assembleDebug assembleRelease
```

Não criar nem executar testes automatizados sem pedido explícito.

## Resultado desta execução

- **Status:** `blocked` após validação física concluída; revisão independente
  encontrou lacunas no contrato/implementação da timeline 1 Hz.
- **Arquivos alterados:**
  - `docs/integration/027-11-final-validation.md`;
  - `docs/specs/027-workout-steps-cadence-1hz-timeline.md`;
  - `docs/tasks/027-workout-steps-cadence-1hz-timeline/027-00-overview.md`;
  - `docs/baseline/current-baseline.md`;
  - `docs/decisions/adr-026-workout-1hz-derived-timeline.md`;
  - este arquivo.
- **Decisões:** GATE A permanece `go_with_adjustments`, baseado na evidência
  física de `027-06`. Após recarga, duas atividades foram importadas, revisadas
  e sincronizadas individualmente: longa `32:17`/`4.605` passos e curta
  `05:30`/`639` passos; ambas terminaram `SYNCED`. Timeline local 1 Hz permanece
  somente local e por demanda; `StepsCadenceRecord` permanece por buckets D4.
- **Desvios:** tentativa inicial falhou com `Operação Bluetooth expirou` porque
  o relógio estava descarregado. O reteste após recarga passou; Health Connect
  não solicitou autorização e não foi lido, portanto `SYNCED` é evidência de
  aceite local da escrita, não readback do provider. Health Sync/Strava não foi
  repetido para essas duas atividades; evidência do Gate A permanece em `027-06`.
- **Comandos executados:** `adb devices -l`; `adb shell getprop`; `adb shell pm
  path br.com.mwconsulting.championcompanion.dev`; `./gradlew installDebug`;
  `./gradlew lint assembleDebug assembleRelease`; abertura de
  `br.com.mwconsulting.championcompanion.dev/br.com.mwconsulting.championcompanion.MainActivity`;
  ação UI **Buscar treinos no relógio**; revisão das duas atividades; sync
  individual explícito de cada detalhe; `git diff --check`; verificação
  equivalente de whitespace do arquivo de evidência não rastreado.
- **Resultados e evidências:** dispositivo `SM-S901E`, Android 16, SDK 36;
  `installDebug` e build completo passaram. Reteste observou `Inseridos 2,
  duplicados 0, inválidos 0, ausentes 0`; longa `32:17`/`4.605` passos com
  cadência aproximada por intervalos `137/161/162/131/54 spm` e curta
  `05:30`/`639` passos com `116 passos/min`. Ambas terminaram `SYNCED`.
  Evidência sanitizada em `docs/integration/027-11-final-validation.md`.
- **Revisão independente:** reprovou fechamento integral; confirmou checklist
  SDD e validação física, mas encontrou três lacunas em `Workout1HzTimeline.kt`:
  gap estrutural de cadência falha série inteira, BPM aceita parser `PARTIAL` e
  fallback BPM aceita valor acima de `90 s`.
- **Testes automatizados:** não criados nem executados, conforme regra da Spec
  027 e `AGENTS.md`.
- **Riscos residuais:** mudança de dia/reset continua não exercitada; aceite
  Health Connect continua significando efeito local aceito, sem readback; Health
  Sync/Strava não foi repetido para estas duas atividades. A revisão independente
  encontrou: cadência 1 Hz falha a série inteira diante de intervalo estrutural
  inválido em vez de preservar gap; BPM aceita parser `PARTIAL`; parâmetros BPM
  aceitam `fallbackGapSeconds > 90`. Esses achados bloqueiam fechamento do Gate B
  e exigem subtarefa corretiva planejada.

## Bloqueio de fechamento

Validação física das duas atividades concluída, mas esta subtarefa permanece
`blocked` até correção explícita das lacunas em
`app/src/main/java/br/com/mwconsulting/championcompanion/core/model/Workout1HzTimeline.kt`.
Não implementar essa correção nesta subtarefa nem avançar automaticamente.

## Correção planejada

O plano aditivo da Spec 027 criou `027-12`, `027-13` e `027-14` para corrigir
os bloqueadores, avaliar achatamento da curva BPM e revalidar o fechamento.
Este registro histórico permanece `blocked` até essas subtarefas concluírem.
