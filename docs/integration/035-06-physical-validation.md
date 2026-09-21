# 035-06 — Validação física e fechamento

- **Data:** 2026-09-21
- **Dispositivo:** Champion C001 real, validado pelo app no `SM-S901E`
- **Privacidade:** evidência sanitizada; sem BPM individual, samples, payload BLE,
  IDs pessoais ou identificadores de atividade
- **Status:** `completed`, com N/A explícito para cenários que não existiam
  naturalmente no banco físico

## Validação técnica

```text
git diff --check                              -> passou
./gradlew lint assembleDebug assembleRelease  -> BUILD SUCCESSFUL
./gradlew installDebug                       -> instalado em 1 dispositivo
```

Nenhum teste automatizado novo foi criado ou executado. A instalação ocorreu
sobre os dados existentes, sem limpar o armazenamento.

## Banco físico e schema

O banco físico confirmou Room v20 antes da validação e após recomposição e
reabertura:

```text
Room user_version:              20
schema exportado:               20.json
workout_activities:             8
workout_heart_rate_samples:    165
workout_derivation_snapshots:  11
health_sync_operations:       780
workflow:                      2 PENDING_REVIEW, 6 SYNCED
origem analítica:              8 WATCH_D3_D4
```

O schema v20 não possui entidade de carga cardiovascular. Não houve migration,
índice, tabela, coluna, snapshot ou operação remota criada pela Spec 035.

## Cálculo independente

O cálculo externo usou as atividades existentes, a janela canônica
`[startedAt, startedAt + duration)`, sample-and-hold da policy 034, a FC máxima
pessoal já configurada e o `ZoneId` do dispositivo. A identificação abaixo é
ordinal sanitizada, ordenada por início, sem expor IDs ou timestamps.

| Atividade | Duração (s) | Samples na janela | Carga | Cobertura |
| --- | ---: | ---: | ---: | ---: |
| T1 | 1625,0 | 27 | 22,0679 | 0,981482 |
| T2 | 2063,0 | 35 | 137,0422 | 0,985714 |
| T3 | 659,0 | 11 | 0,9985 | 0,954546 |
| T4 | 393,0 | 6 | 6,5500 | 0,916667 |
| T5 | 63,0 | 2 | 1,3125 | 0,750000 |
| T6 | 385,0 | 8 | 7,6198 | 0,937501 |
| T7 | 2594,0 | 44 | 180,3026 | 0,988636 |
| T8 | 1873,0 | 32 | 149,7425 | 0,984375 |

Fechamento independente das janelas:

| Período | Treinos | Analisados | Duração total (s) | Duração coberta (s) | Cobertura | Carga |
| --- | ---: | ---: | ---: | ---: | ---: | ---: |
| current7 | 5 | 5 | 5308,0 | 5176,696 | 0,975263 | 345,5274 |
| previous7 | 3 | 3 | 4347,0 | 4257,483 | 0,979407 | 160,1086 |
| current28 | 8 | 8 | 9655,0 | 9434,179 | 0,977129 | 505,6360 |
| previous28 | 0 | 0 | 0,0 | 0,000 | 0,000000 | 0,0000 |

O delta absoluto de `current7` foi `185,4189`; o percentual independente foi
aproximadamente `115,8%`, apresentado pela UI como `116% maior`. `previous28`
teve baseline zero e a UI mostrou `Sem base anterior`, sem infinito ou `NaN`.

Somas independentes fecharam:

- buckets diários de `current28`: `505,6360`, igual à carga do período;
- grupos por tipo: `RUNNING 489,1552`, `FREE_WORKOUT 2,3110` e
  `TREADMILL 14,1698`, total `505,6360`;
- `8 de 8` treinos analisados e cobertura UI `97,7%`;
- a UI mostrou `505,6 pontos`, `02:37:14` cobertos e `Sem base anterior`.

## Recomposição e efeitos colaterais

A FC máxima foi alterada temporariamente por Perfil para outro valor válido e
restaurada imediatamente ao valor original. A seção recompôs sem alterar os
workouts:

```text
estado original:    current7 345,5; current28 505,6
configuração válida: current7 355,0; current28 528,4
após restauração:   current7 345,5; current28 505,6
```

Comparações antes/depois e após force-stop/reopen mantiveram assinaturas e
contagens:

```text
workout_activities:             8 linhas, assinatura igual
workout_derivation_snapshots:  11 linhas, assinatura igual
health_sync_operations:       780 linhas, assinatura igual
```

Reabrir Exercícios recompôs a seção em memória. Nenhuma tabela analítica foi
criada e nenhum analytics histórico apareceu no banco.

## Screenshot

Screenshot sanitizada da seção histórica:

- [`035-06-training-load-history.png`](035-06-training-load-history.png)

O recorte mostra somente resumo de 28 dias, comparação, contadores, cobertura,
gráfico diário e título de contribuição por tipo. Não contém dados pessoais,
BPM individual, samples ou identificadores.

## Cenários N/A e riscos residuais

1. Todas as 8 atividades existentes tinham samples; workout sem análise por
   ausência de samples ficou `N/A`. Nenhum treino ou sample foi criado.
2. `NO_WORKOUTS` no período principal não surgiu naturalmente porque
   `current28` tinha atividades. `previous28` vazio foi exercitado pela
   comparação e produziu `Sem base anterior`.
3. Falha de leitura, configuração ausente e workout com samples inválidos não
   foram fabricados; estados permanecem cobertos pelos contratos tipados e
   ficaram `N/A` físicos.
4. O plano temporal continua sem índice composto e sem medição em dataset de
   escala; risco já registrado em `035-03`, sem migration especulativa.

Nenhuma limitação restante afeta fechamentos numéricos exercitados, preservação
de fatos, workflow, snapshots, fila Health Connect ou ausência de persistência
analítica.

## Conclusão

Validação física independente, recomposição por FC máxima, restauração,
preservação do banco e reabertura foram concluídas. A Spec 035 está pronta para
fechamento documental e revisão independente.
