# 021 — Normalização temporal D4 e integridade do calendário

- **Status geral:** `in_progress`
- **Spec:** [021-d4-timeline-normalization-and-calendar-integrity.md](../../specs/021-d4-timeline-normalization-and-calendar-integrity.md)
- **ADR:** [ADR-019](../../decisions/adr-019-authoritative-workout-duration-and-normalized-d4-timeline.md)
- **Base:** `feat/014-watch-workout-derived-metrics`
- **Progresso:** `2/4`

## Objetivo

Corrigir duas falhas independentes observadas após a Fase 014:

1. usar a duração oficial do treino como eixo temporal e normalizar a série D4
   sem perder samples, passos ou BPM;
2. reconstruir candidates de Health Connect com derivação ativa para eliminar
   falso `UNRESOLVED_AGGREGATE` no calendário.

## Checklist

- [x] [021-01-correct-d4-authoritative-duration.md](021-01-correct-d4-authoritative-duration.md)
- [x] [021-02-normalize-d4-samples-to-workout-duration.md](021-02-normalize-d4-samples-to-workout-duration.md)
- [ ] [021-03-restore-health-sync-semantic-candidate-derivation.md](021-03-restore-health-sync-semantic-candidate-derivation.md)
- [ ] [021-04-validate-existing-data-and-close.md](021-04-validate-existing-data-and-close.md)

## Dependências

```text
021-01 ──→ 021-02 ──┐
                    ├──→ 021-04
021-03 ──────────────┘
```

`021-03` é independente do parser/timeline e pode ser executada em paralelo com
`021-01/02`, mas `021-04` exige as duas frentes concluídas.

## Decisões congeladas pelo planejamento

- duração oficial da atividade é fonte de verdade temporal;
- `packet2[6]` do layout preferido representa `endSecond` quando `0..59`;
- `60 s` é granularidade nominal observada, não duração obrigatória do sample;
- sample D4 preserva ordem, passos, BPM e raw; timestamps/durações individuais
  são derivados pela timeline normalizada;
- nova semântica usa `d4-v3` e derivação `watch-d3-d4-v2`;
- nenhuma atividade antiga é reprocessada silenciosamente;
- a atividade física `PENDING_REVIEW` com 32 samples deve ser recuperável in-place
  por ação explícita, sem wipe e sem nova leitura BLE;
- operações Health Connect existentes não são reparadas durante leitura do
  calendário;
- candidate semântico deve carregar snapshot ativo e série canônica;
- GPS, rota, `ExerciseRoute`, treino phone e leitura ampla do Health Connect
  permanecem fora do escopo.

## Evidência de entrada sanitizada

- banco local íntegro em schema v10;
- quatro atividades D3/D4 preservadas;
- três durações do app oficial fecham exatamente quando `packet2[6]` é usado como
  segundo final;
- atividade oficial `29:15` possui 32 samples e zero suspensões;
- sequência temporal problemática é estruturalmente íntegra e seus passos fecham
  com o total da atividade;
- fila possui 84 operações `SYNCED`, sem erro ou identidade ausente;
- falso estado parcial decorre da reconstrução sem derivação, não de falha
  persistida da fila.

Não registrar IDs, payloads integrais, BPM, comprimento de passo ou outras
métricas pessoais nas evidências da task.

## Estado inicial

| Task | Status |
| --- | --- |
| 021-01 | `completed` |
| 021-02 | `completed` |
| 021-03 | `pending` |
| 021-04 | `pending` |

## Regras de execução

- seguir `AGENTS.md` e baseline atual;
- não criar nem executar testes automatizados salvo pedido explícito do usuário;
- mudança de schema deve exportar schema Room e registrar migration;
- mudanças funcionais devem preservar banco existente;
- não executar wipe, `clear data`, exclusão de atividade ou ressync automático;
- manter documentação, comandos, evidências e riscos atualizados na própria task;
- solicitar revisão independente antes de marcar cada subtarefa concluída.

## Observações

- `021-02` implementada e validada por `git diff --check` e build completo.
- Validação física com banco preservado, reprocessamento do cenário de 32 samples
  e instalação no dispositivo permanecem em `021-04`.

## Fechamento esperado

A 021 só pode ser fechada quando:

- nova importação usa `d4-v3` completo;
- timeline normalizada cobre exatamente a duração oficial;
- atividade de 32 samples é reprocessada localmente sem apagar dados;
- calendário resolve a fila existente sem falso `UNRESOLVED_AGGREGATE`;
- degradações reais continuam observáveis;
- build e validação física possuem evidência factual.
