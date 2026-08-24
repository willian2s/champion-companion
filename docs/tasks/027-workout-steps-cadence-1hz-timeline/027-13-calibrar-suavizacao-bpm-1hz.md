# 027-13 — Calibrar suavização BPM 1 Hz

- **Ticker:** `027`
- **Número:** `13`
- **Status:** `cancelled`
- **Depends on:** `027-12`

## Objetivo

Avaliar e, somente se houver benefício comprovado, reduzir achatamento de picos
e fundos do BPM derivado em 1 Hz sem substituir BPM SOURCE nem alterar
estatísticas históricas.

## Escopo

Comparar separadamente:

1. BPM raw;
2. amostras aceitas após faixa/Hampel;
3. suportes EMA;
4. pontos interpolados;
5. downsample de renderização;
6. gráfico final.

Avaliar inicialmente `tau=5s` contra candidato menos suavizado, como `tau=1s`.
EMA continua aplicada somente a observações-fonte; pontos interpolados não
entram na EMA. Proveniência, gaps e segmentos permanecem inalterados.

## Decisão de versionamento

Se somente `tau` mudar:

- manter algoritmo `watch-d3-d4-1hz-v1`;
- criar novo conjunto de parâmetros BPM, por exemplo
  `workout-1hz-params-v2`;
- preservar `workout-1hz-params-v1`/`tau=5s` para reprocessamento explícito;
- gerar fingerprint distinto;
- manter parâmetros de cadência em `v1`.

Mudança de fórmula, interpolador ou semântica exige atualização de algoritmo e
ADR-026; não usar alteração silenciosa.

## Fora de escopo

- alterar BPM raw, `averageBpm`, `minimumBpm` ou `maximumBpm`;
- preencher gaps;
- usar interpolados como fonte;
- alterar cadência Health Connect;
- nova sincronização física;
- testes automatizados sem pedido explícito.

## Arquivos e símbolos prováveis

- `app/src/main/java/br/com/mwconsulting/championcompanion/core/model/Workout1HzTimeline.kt`;
- parâmetros `Workout1HzBpmParameterSet`;
- `docs/decisions/adr-026-workout-1hz-derived-timeline.md`;
- Spec 027 e evidência visual, se UI mudar.

## Critérios de aceite

- pico e fundo não outlier continuam distinguíveis nos suportes derivados;
- nenhum extremo novo é criado fora do suporte aceito;
- gaps, segmentos e provenance permanecem corretos;
- identidade/fingerprint mudam quando parâmetros mudam;
- cache é invalidado pela identidade nova;
- decisão registra alteração ou retenção de `tau=5s` com evidência factual;
- screenshot novo é criado se houver mudança visual, sem sobrescrever evidência
  anterior.

## Verificação

```bash
git diff --check
./gradlew lint assembleDebug assembleRelease
```

Não criar nem executar testes automatizados sem pedido explícito.

## Definição de pronto

Decisão de suavização documentada, código e identidade coerentes, evidência
visual sanitizada quando aplicável e revisão independente solicitada. Não
avançar automaticamente para `027-14`.

## Riscos

Reduzir `tau` pode reintroduzir ruído; manter `tau=5s` pode preservar achatamento.
Nenhum ajuste deve transformar derivação em medição real.

## Encerramento do plano

Cancelada por decisão de produto: Gate B/timeline local 1 Hz será removido em
`027-15`; BPM raw permanece fonte do Health Connect. Nenhum código desta
subtarefa foi implementado.
