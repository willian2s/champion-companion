# ADR-038 — Recuperação de comparabilidade histórica de corridas

- **Status:** accepted for planning; execução condicionada ao Gate A
- **Data:** 2026-09-29
- **Relacionada:** [Spec 038](../specs/038-historical-running-comparability.md)
- **Base:** Spec 037, ADR-018, ADR-019, ADR-022, ADR-029 e Room v20

## Contexto

A Spec 037 encontrou 21 corridas `RUNNING` na janela de 56 dias: 5 com
lineage nativa comparável e 16 com `STEP_LENGTH_FALLBACK`. A evidência sanitizada
não informa quais fallbacks preservam parser, header, série nativa ou payload
diagnóstico suficiente.

O risco central é melhorar a quantidade de pontos misturando fontes ou
reescrevendo fatos históricos. O segundo risco é alterar uma atividade local
cujo payload Health Connect já foi aceito, sem readback, IDs/versionamento
reconciliável ou autorização de republicação.

## Decisão

### Escopo e regra de comparação

038 investiga somente `WATCH_D3_D4`, `confirmedType == RUNNING`,
`workflowState != DISCARDED` e a população relevante à janela `[asOf - 56 dias,
asOf)` da 037. `WALKING`, `TREADMILL`, `FREE_WORKOUT`, `CYCLING` e phone
workouts permanecem fora.

Uma corrida só ganha comparabilidade nativa se a fonte autorizada provar,
deterministicamente:

- identidade única de device/importação, CRC, bloco e `startedAt`;
- parser original `d4-v4`, header v4 e série completa; interpretação legacy
  exige decisão de provenance separada antes de qualquer promoção;
- `recordCount`, índices, ordem, quantidade e fatos raw/measured coerentes;
- consistência de passos, distância, calorias, `sportValue`, pausa e BPM
  disponível, sem preencher ausência;
- temporalidade atual `ELIGIBLE`, duração positiva e janelas válidas;
- distância nativa derivada da série, nunca de comprimento de passo ou total
  legacy;
- plausibilidade sem indicador bloqueador;
- `confirmedType == RUNNING` confirmado, sem inferência a partir de
  `sportTypeRaw`.

`WATCH_NATIVE` e `WATCH_NATIVE_CONTEXT_CALIBRATED` continuam sendo as únicas
proveniências nativas aceitas pela 037, com `watch-d3-d4-v4`, escalares válidos,
temporalidade elegível e plausibilidade válida. `STEP_LENGTH_FALLBACK` não entra
nos gráficos.

### Taxonomia

O Gate A deve distinguir:

- `NATIVO_ATUAL`: já atende 037; nenhuma recuperação;
- `V4_RECUPERAVEL_LOCALMENTE`: parser original v4 e facts v4 fecham, ou uma
  interpretação legacy foi autorizada com provenance distinta e todos os
  invariantes fecham;
- `LEGACY_COM_DIAGNOSTICO_POTENCIALMENTE_RECUPERAVEL`: payload legacy existe,
  mas prova integral ou provenance de interpretação ainda falta;
- `LEGACY_SEM_FONTE_NATIVA`: não há fonte suficiente;
- `ESTRUTURALMENTE_INELEGIVEL`: fonte inspecionada falha integridade,
  temporalidade ou plausibilidade;
- `BLOQUEADO_POR_ESTADO_REMOTO`: fonte recuperável foi provada, porém estado
  remoto impede mutação canônica.

`BLOQUEADO_POR_ESTADO_REMOTO` é resultado de fonte + mutação, não substituto
para uma fonte inexistente. A classificação deve preservar assessment de fonte,
workflow e disposition de mutação separadamente.

### Parser legacy e diagnostic payload

`WatchWorkoutImport.parserVersion` é fato histórico e não pode ser alterado.
`d4-v1`, `d4-v2` ou `d4-v3` não viram `d4-v4` por relabeling.

Diagnostic payload legacy pode ser reparseado somente em memória para auditar
uma interpretação v4 candidata. A interpretação só é segura se fechar device,
CRC, bloco, início, quantidade, índices, passos, BPM compatível,
duração/header, comparação com facts persistidos e nenhuma alteração nos bytes.

Até prova integral e provenance explícita, payload presente classifica legacy
como potencial, não como recuperável e não como ponto 037.

### Atividades `SYNCED`

Adota-se a alternativa A: recuperação canônica continua bloqueada para
`SYNCED`, `SYNCING` e projection children inseguros. Não se altera o guard atual.

A alternativa B, derivar localmente e deixar Health Connect com payload antigo,
é rejeitada porque cria duas representações históricas sem readback, sem
reconciliação segura e sem republicação automática autorizada. A alternativa C,
snapshot/read model efêmero user-facing, é rejeitada por criar segunda verdade,
duplicar pipeline e depender da retenção diagnóstica. Leitura em memória para o
Gate A é permitida apenas como auditoria e nunca alimenta 037.

### Snapshots e calibração

Snapshots são append-only: source/raw, measured, derived snapshot e projection
Health Connect continuam camadas distintas. Snapshot anterior não é editado
para parecer nativo. Uma recuperação futura aprovada deve criar snapshot novo,
se tornar ativo somente por transação explícita/idempotente e manter derived
intervals/splits vinculados ao snapshot novo.

Recuperar facts nativos não aplica automaticamente perfil aprendido ou fator
contextual atual. O resultado preferido é `WATCH_NATIVE`. Só evidence válida já
associada à atividade pode sustentar `WATCH_NATIVE_CONTEXT_CALIBRATED`; nenhuma
evidence antiga é reescrita e nenhuma outra atividade é recalibrada.

## Health Connect

Qualquer implementação futura deve preservar:

- zero chamadas remotas causadas por auditoria, preflight ou recuperação;
- zero novas operações Health Connect automaticamente;
- zero alteração de operações `SYNCED` ou payloads congelados;
- zero reset de reconciliation;
- zero delete remoto, retry remoto ou readback;
- zero mudança de `clientRecordId`/`clientRecordVersion` por analytics;
- zero reabertura silenciosa de workflow para reenviar atividade.

## Room e reversibilidade

Room permanece v20, sem nova migration nesta spec, tabela, coluna, índice,
cache comparável ou snapshot analítico. Se a recuperação precisar persistir
provenance de interpretação que não esteja representável com segurança no
modelo vigente, a implementação fica bloqueada até nova decisão/migration.

## Consequências

- Evita falsificação de lineage e mistura de fallback com nativo.
- Torna “recuperável” predicado reproduzível e auditável.
- Mantém `SYNCED` localmente/remotamente coerente por bloqueio conservador.
- Permite `GO`, `PARTIAL-GO`, `NO-GO` ou estado inconclusivo sem meta artificial.
- Parte ou todo histórico pode permanecer fallback.
- Diagnostic payload presente pode continuar somente potencial.

## Alternativas rejeitadas

- Alterar `parserVersion` para obter `d4-v4`.
- Usar header sozinho, total agregado, distância original ou passos calibrados
  como distância nativa.
- Reprocessar todos os históricos por batch automático.
- Criar segunda derivação user-facing para `SYNCED`.
- Ler Health Connect para confirmar payload.
- Criar persistência `comparable = true` sem fonte que a sustente.
- Aplicar perfil contextual atual silenciosamente durante recuperação.

## Gate e validação

- **GO:** existe classe de fonte provada e caminho local compatível sem efeitos
  remotos.
- **PARTIAL-GO:** somente subconjunto provado; demais classes ficam fora.
- **NO-GO:** nenhum candidato pode ser promovido sem inventar fato, falsificar
  provenance ou romper contrato remoto.
- **INCONCLUSIVO:** banco físico ou fatos necessários indisponíveis; não
  autoriza implementação.

Gate A registra somente agregados sanitizados. Activity ID, MAC, CRC individual,
payload, bytes, BPM individual, timestamp completo, título, notas e localização
ficam proibidos.

## Limites

Esta ADR não implementa preflight, recuperação, UI, migration, parser, BLE,
Health Connect ou alteração na 037. Também não reabre fases macro nem autoriza
as Specs 039–041.
