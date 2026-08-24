# 027-12 — Corrigir bloqueadores da timeline 1 Hz

- **Ticker:** `027`
- **Número:** `12`
- **Status:** `completed`
- **Depends on:** `027-10`; achados documentados em `027-11`

## Objetivo

Corrigir bloqueadores de contrato identificados na revisão final da Spec 027,
sem alterar SOURCE, snapshots, métricas históricas, Gate A ou projeções Health
Connect.

## Escopo

- preservar gaps e segmentos quando intervalo estrutural de cadência for
  inválido;
- exigir `ParserValidity.VALID` para derivação BPM;
- limitar `fallbackGapSeconds` de BPM ao intervalo `1..90`;
- manter `CYCLING` sem cadência baseada em passos;
- preservar proveniência `OBSERVED_DERIVED`, `INTERPOLATED` e `ABSENT`.

Intervalo inválido não pode gerar suporte nem ser atravessado por interpolação.
Grade válida continua completa em `[startedAt, endedAt)`, com buckets ausentes
representados por ausência, nunca zero.

## Fora de escopo

- calibração da suavização BPM, pertencente a `027-13`;
- mudança de fórmula/interpolador;
- persistência, migration ou Health Connect;
- nova sincronização física;
- criação ou execução de testes automatizados sem pedido explícito.

## Arquivos e símbolos prováveis

- `app/src/main/java/br/com/mwconsulting/championcompanion/core/model/Workout1HzTimeline.kt`;
- `deriveCadence1Hz`;
- `deriveBpm1Hz`;
- `Workout1HzBpmParameterSet`;
- `buildCadenceSourceSegments` e continuidade estrutural relacionada.

## Critérios de aceite

- intervalo inválido no meio não elimina valores válidos antes/depois;
- gaps permanecem `null`/`ABSENT`, sem zero sintético;
- BPM com parser `PARTIAL` falha fechadamente;
- fallback BPM aceita `1` e `90`, rejeita `0` e `91+`;
- cadência continua independente de crédito, Daily e Health Connect;
- SOURCE, snapshots, métricas históricas e `CYCLING` permanecem intactos;
- lint e builds pertinentes passam.

## Verificação

```bash
git diff --check
./gradlew lint assembleDebug assembleRelease
```

Não criar nem executar testes automatizados sem pedido explícito.

## Definição de pronto

Código corrigido, documentação da subtarefa atualizada com evidências e revisão
independente solicitada. Não avançar automaticamente para `027-13`.

## Resultado desta execução

- **Status:** `completed`; três bloqueadores corrigidos sem reabrir Gate A.
- **Arquivos alterados:**
  - `app/src/main/java/br/com/mwconsulting/championcompanion/core/model/Workout1HzTimeline.kt`;
  - `docs/decisions/adr-026-workout-1hz-derived-timeline.md`;
  - este arquivo.
- **Decisões:** intervalos inválidos permanecem marcadores estruturais de ruptura
  durante a segmentação; não geram suporte e não permitem interpolação entre as
  regiões válidas. Intervalos válidos antes e depois continuam na mesma grade
  `[startedAt, endedAt)`, com ausência por canal preservada. BPM agora exige
  `ParserValidity.VALID`. `fallbackGapSeconds` de BPM usa o limite inclusivo
  `1..90`, alinhado ao contrato já aplicado à cadência.
- **Desvios:** nenhum. SOURCE, snapshots, proveniência, identidade, cache,
  `CYCLING`, projeções Health Connect e crédito de passos não foram alterados.
- **Comandos executados e resultados:**
  - `git diff --check` — passou, sem erros de whitespace;
  - `./gradlew lint assembleDebug assembleRelease` — primeira execução excedeu
    o timeout operacional de 120 s; repetição com timeout de 300 s terminou em
    `BUILD SUCCESSFUL`;
  - testes automatizados — não criados nem executados, conforme solicitação.
- **Evidências:** validação estática do fluxo confirma que intervalo com janela
  nula, duração inválida ou inconsistência encerra segmento sem suporte; a grade
  mantém buckets ausentes como `null`/`ABSENT`. A guarda de parser BPM rejeita
  `PARTIAL`/`INVALID`, e a construção dos parâmetros rejeita `0` e `91+`,
  aceitando `1` e `90`.
- **Revisão independente:** aprovada com ressalvas; nenhum bloqueador funcional
  foi encontrado. A revisão confirmou segmentação de gaps, guarda `VALID`,
  limite `1..90`, preservação de SOURCE/Health Connect e checklist SDD. Escopo
  global continua bloqueado por `027-13` e `027-14`.

## Riscos residuais e bloqueios

- `027-13` ainda precisa avaliar calibração da suavização BPM sem alterar SOURCE,
  snapshots ou versionamento.
- `027-14` permanece responsável pela revalidação e fechamento do Gate B; esta
  subtarefa não autoriza marcar o overview ou a Spec 027 como fechados.
- revisão independente aprovou a correção com ressalvas; integração no checklist
  foi feita pelo agente principal. Validação física não pertence a esta
  subtarefa.

## Riscos

Segmentação incorreta pode interpolar através de perda estrutural. A correção
deve preferir ausência a valor inventado e manter identidade/versionamento
existentes.
