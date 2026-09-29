# 038-01 — Auditar recuperabilidade e congelar Gate A

- **Ticker:** `038`
- **Número:** `01`
- **Status:** `pending`

## Objetivo

Fechar contrato documental e auditoria read-only da recuperabilidade histórica
de corridas `WATCH_D3_D4` confirmadas `RUNNING`, incluindo decisão
`GO`/`PARTIAL-GO`/`NO-GO` ou `INCONCLUSIVO`.

## Resultado esperado

Matriz sanitizada por agregados, taxonomia congelada, política legacy,
`SYNCED`, snapshots, calibração, Room e Health Connect documentadas. Nenhum
fato físico é inventado quando banco real ou dump read-only não estiver
disponível.

## Requisitos cobertos

- Definição objetiva de recuperável para comparabilidade nativa.
- Classes `NATIVO_ATUAL`, `V4_RECUPERAVEL_LOCALMENTE`,
  `LEGACY_COM_DIAGNOSTICO_POTENCIALMENTE_RECUPERAVEL`,
  `LEGACY_SEM_FONTE_NATIVA`, `ESTRUTURALMENTE_INELEGIVEL` e
  `BLOQUEADO_POR_ESTADO_REMOTO`.
- Parser v1/v2/v3/v4, diagnostic payload, header/série, CRC, identidade,
  facts raw/measured, temporalidade, plausibilidade e workflow.
- Política A para `SYNCED`: bloquear recuperação canônica.
- Invariantes Health Connect, Room v20 sem nova migration, snapshots append-only e
  calibração sem efeitos colaterais.
- Gate físico real, sem reimportação, mutação ou dados artificiais.

## Escopo incluído

- Ler a população 037 em banco real somente se dump/ambiente read-only existir.
- Contar parser, método/versão, workflow, payload, header, facts nativos,
  recuperabilidade, fallback, bloqueio remoto e inelegibilidade.
- Comparar payload candidato com import identity, CRC, bloco, início,
  quantidade, índices, passos, BPM compatível, duração/header e facts
  persistidos, sem preservar conteúdo sensível.
- Registrar cenários ausentes como `N/A`.
- Atualizar somente documentação da 038 e ADR vinculada.

## Escopo excluído

- Código funcional, preflight executável ou reprocessamento.
- Qualquer escrita em Room, snapshot, workflow, payload ou Health Connect.
- Alterar parser version, bytes, CRC, import identity ou evidência histórica.
- Criar fixture, corrida, sample, estado artificial ou teste automatizado.

## Dependências

- Spec/ADR/evidência 037 e banco físico/dump read-only, quando disponível.
- Specs 014, 021, 023, 032; ADR-018, ADR-019, ADR-022, ADR-029, ADR-037.
- Código atual de derivação, reprocessamento, persistência, Exercícios e HC.

## Arquivos e símbolos prováveis

- `docs/specs/038-historical-running-comparability.md`;
  `docs/decisions/adr-038-historical-running-comparability.md`.
- `docs/integration/037-06-physical-validation.md` como referência, sem
  sobrescrever evidência anterior.
- `RoomRepositories.reprocessWatchWorkoutInternal` e
  `recoverAuthoritativeDurationFromDiagnostic`.
- `WatchWorkoutNativeDistancePolicy.isEligible` e
  `WatchWorkoutDerivedMetrics`.
- Entidades/DAOs de `workout_activities`, `watch_workout_imports`,
  `workout_intervals`, `watch_workout_d4_v4_headers`, snapshots, derivados,
  splits, samples, calibration evidence e `health_sync_operations`.

## Passos de implementação/auditoria

1. Confirmar população, janela e filtro da 037 sem usar `suggestedType`.
2. Auditar presença e retenção de fontes por atividade, sem exportar payload.
3. Separar fonte recuperável de disposição de mutação.
4. Aplicar invariantes header/série/CRC/identity/temporalidade/plausibility.
5. Classificar legacy com payload apenas como potencial até prova integral.
6. Avaliar cada workflow e projection child sem remover guards de `SYNCED`.
7. Registrar agregados sanitizados e limitações físicas.
8. Emitir decisão Gate A. Sem banco físico, emitir `INCONCLUSIVO` e deixar
   procedimento pronto para primeira execução autorizada.

## Testes e comandos de validação

- Revisão independente dos documentos e das citações de código.
- `git diff --check` após documentação.
- Auditoria física read-only futura, sem comandos de escrita, sem wipe e sem
  reprocessamento.
- Nenhum teste automatizado criado ou executado.

## Definição de pronto

- Taxonomia, predicado e precedência documentados.
- Contagens sanitizadas registradas quando fisicamente disponíveis.
- Ausências naturais e limitações declaradas como `N/A`.
- Política legacy, `SYNCED`, HC, snapshots, calibração e Room congelada.
- Gate A em `GO`, `PARTIAL-GO`, `NO-GO` ou `INCONCLUSIVO`, sem ambiguidade.
- Nenhum arquivo funcional, banco, schema, migration, parser ou HC alterado.

## Riscos e cuidados

- Não transformar presença de payload em prova v4.
- Não publicar IDs, MAC, CRC individual, bytes, BPM individual ou timestamps
  completos.
- Não confundir atividade nativa atual com candidata a recuperação.
- Não classificar bloqueio remoto quando fonte ainda não foi provada.
- Não converter ausência de banco em `NO-GO`; usar `INCONCLUSIVO`.
