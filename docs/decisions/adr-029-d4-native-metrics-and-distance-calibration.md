# ADR-029 — Métricas nativas D4 e calibração proporcional de distância

- **Status:** accepted
- **Data:** 2026-09-08
- **Relacionado:** Specs 021, 023, 027, 032 e 033

## Contexto

O Champion Companion historicamente derivou distância de treinos `WATCH_D3_D4`
por `decodedSteps × stepLengthMeters`. Essa decisão foi tomada quando os campos
D4 de distância e calorias ainda não tinham layout confirmado.

Inspeção posterior do APK Champion Smartstyle e validação física do Champion C001
confirmaram que o Sport Model fixo transmite por sample BPM, `sportValue`, passos
`u16`, calorias raw `u16`, distância raw `u16` e pausa. O header transmite totais
e metadados independentes. A distância temporal observada acompanha variações de
intensidade e não deve ser descartada em favor de uma reconstrução linear por
passada quando a série nativa é elegível.

A Spec 021 também mostrou que duração oficial e quantidade de samples são
conceitos independentes; sua normalização `T/N` continua válida e não é revertida.

## Decisão

1. Introduzir `parserVersion = d4-v4` para novas importações quando todo o fluxo
   estiver coerente.
2. Preservar os fatos nativos D4 em campos semanticamente nomeados; não continuar
   sobrecarregando o packing legado `rawMotionValue/rawDistanceValue/rawCaloriesValue`.
3. Manter `startedAt/endedAt` como fonte temporal autoritativa e distribuir `T`
   sobre `N` samples elegíveis como na Spec 021.
4. Usar passos por intervalo para passos/cadência e manter total de header apenas
   como consistência/diagnóstico.
5. Para RUNNING/WALKING com distância D4 elegível, usar distância nativa como base
   da distância canônica antes do fallback por comprimento de passo.
   A série D4 íntegra também pode ser materializada como `WATCH_NATIVE` antes da
   confirmação do tipo, sem inferir contexto; a revisão reavalia o fator de
   RUNNING/WALKING e mantém as políticas próprias dos demais tipos.
6. Distância externa manual corrige proporcionalmente a série nativa por fator
   `reference/watch`, preservando sua forma temporal.
7. Aprender fatores contextuais separados para RUNNING e WALKING pela razão
   ponderada `Σreference / Σwatch`, sem média simples escondida.
8. Manter comprimento de passo como fallback e preservar calibrações legacy sem
   conversão silenciosa.
9. `CYCLING` mantém sua política própria de distância manual.
10. `sportValue`, `pauseState` e calorias raw são preservados mesmo quando sua
    semântica final não estiver promovida a métrica de produto.
11. Reprocessamento de atividades antigas é explícito, local e somente quando
    houver fatos suficientes; atividades sincronizadas não são alteradas
    silenciosamente.
12. Health Connect permanece write-only e explícito, com payload congelado e
    guards existentes.

## Consequências

### Positivas

- parser alinhado ao layout observado do firmware;
- menor dependência de comprimento de passo para treinos com distância nativa;
- preservação da dinâmica temporal de corrida/caminhada;
- calibração externa corrige viés global sem achatar variações por intervalo;
- melhor provenance entre fatos do relógio e valores derivados.

### Custos

- mudança de schema provável para novos fatos por intervalo e provenance de
  derivação/calibração;
- coexistência de `d4-v1/v2/v3/v4` e estratégias antigas/novas;
- consumidores precisam distinguir distância nativa, calibrada e fallback;
- integração tardia com `RoomRepositories` e UI pode conflitar com a Spec 031.

## Validação e fechamento — 2026-09-11

A decisão foi validada fisicamente no Champion C001 com conferência por dumps
sanitizados do banco. A validação cobriu parser v4, coerência header/série,
timeline `T/N`, distância nativa, calibração proporcional, perfis contextuais,
fallback, review-before-sync, Health Connect e preservação de Daily Summary,
cadência e `CYCLING`.

Evidência:
`docs/integration/032-08-physical-validation.md`.

Limitações permanecem para semântica operacional de `pauseStateRaw` não zero,
unidade funcional de `sportValue`, campos reservados ou desconhecidos, política
de reprocessamento de históricos, tolerâncias para divergências futuras,
cobertura de samples acima de 255 passos, retenção de payload diagnóstico e
variações de firmware não observadas. A referência externa manual permanece
sujeita a erro e serve somente como referência de calibração. Essas limitações
não invalidam decisão para layout fixo validado.

## Evolução de política — 2026-09-14

Validação física da entrega 033 confirmou distância nativa D4 v4 íntegra em
`FREE_WORKOUT`. A política passa a aceitar `FREE_WORKOUT` e `TREADMILL` como
tipos de distância nativa, sem reutilizar comprimento de passo. `TREADMILL`
permanece `PENDING_CALIBRATION` até distância externa ser informada; o fator
`referência / distância D4` é aplicado proporcionalmente aos intervalos e
aprendido em perfil separado de `RUNNING` e `WALKING`. `FREE_WORKOUT` não gera
evidência de calibração nativa. `CYCLING`, `TOTAL_CALORIES_BURNED`, fatos raw,
atividades `SYNCED` e snapshots históricos permanecem inalterados.

## Alternativas rejeitadas

### Continuar sempre com `steps × stepLength`

Rejeitada porque ignora uma série de distância nativa fisicamente coerente e
remove variações temporais que o firmware já calculou.

### Substituir toda distância pela referência externa

Rejeitada porque a referência é manual e pode conter erro; ela deve calibrar, não
apagar provenance nem virar medição absoluta implícita.

### Usar média simples dos fatores de calibração

Rejeitada porque dá o mesmo peso a atividades de tamanhos muito diferentes e
introduz viés arbitrário.

### Voltar a `60 s` rígidos por sample

Rejeitada pela evidência física já documentada na Spec 021: duração oficial e
`recordCount` não mantêm relação rígida `N × 60 s`.
