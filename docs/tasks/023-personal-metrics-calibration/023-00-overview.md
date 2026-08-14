# 023 — Overview: Perfil, métricas pessoais e calibração

- **Fase:** 16
- **Spec:** `docs/specs/023-personal-metrics-calibration.md`
- **Status:** `in_progress`
- **Progresso:** `6/7`
- **Branch:** `feat/023-personal-metrics-calibration`

## Objetivo

Concluir a Fase 16 com um perfil pessoal enxuto e funcional, incluindo altura,
meta diária de passos, peso e calibração progressiva do comprimento do passo de
corrida/caminhada por distância externa de referência.

## Sequência

```text
023-01 Auditoria e contratos
   ↓
023-02 Persistência de calibrações / perfis aprendidos
   ↓
023-03 Altura e meta de passos no Perfil
   ↓
023-04 Peso manual e histórico
   ↓
023-05 Calibração por distância de referência
   ↓
023-06 Derivação contextual + consumidores + Health Connect
   ↓
023-07 Validação física e fechamento
```

## Dependências normativas

- `docs/specs/_inputs/phase-16-personal-metrics-direction.md`
- `docs/decisions/adr-022-contextual-step-length-calibration.md`
- ADR-017 — direção watch-first;
- ADR-018 — raw/measured/derived/projection;
- ADR-019 — timeline D4 normalizada;
- ADR-021 — bicicleta e distância manual;
- `docs/specs/014-watch-workout-derived-metrics.md`;
- `docs/specs/022-cycling-workouts.md`.

## Guard rails

- não adicionar GPS, rota, mapa ou coleta de localização;
- não criar integração com Adidas/Strava;
- não reescrever snapshots históricos automaticamente;
- não alterar atividade sincronizada silenciosamente;
- não misturar calibração de corrida e caminhada;
- não aplicar perfil aprendido a `CYCLING`;
- não transformar `TREADMILL` em corrida por suposição;
- não recriar fila Health Connect para altura/peso;
- não adicionar nome, nascimento, sexo, meta de peso, unidades ou zonas de FC;
- não criar testes novos nem executar suites sem pedido explícito;
- mudanças de Room devem preservar dados e schemas existentes.

## Checklist

- [x] `023-01-audit-personal-metrics-and-freeze-phase-boundaries.md` — auditoria concluída e contratos congelados
- [x] `023-02-persist-step-length-calibrations-and-learned-profiles.md` — calibrações persistidas e perfis aprendidos disponíveis
- [x] `023-03-complete-profile-height-and-daily-steps-goal.md` — altura/meta de passos editáveis
- [x] `023-04-implement-manual-weight-and-local-history.md` — peso manual/histórico funcional
- [x] `023-05-implement-reference-distance-calibration.md` — calibração de corrida/caminhada funcional
- [x] `023-06-integrate-contextual-step-length-and-health-projections.md` — derivação contextual e projeções integradas
- [ ] `023-07-validate-and-close-phase-16.md` — validação física e fechamento documental concluídos

## Observações

- `023-01` concluída. Auditoria e decisões congeladas em
  `docs/integration/023-01-personal-metrics-and-calibration-audit.md`.
- Sem bloqueios para `023-02`; riscos residuais estão registrados na subtarefa e
  no documento de integração.
- `023-02` corrigida após revisão independente: fonte canônica D4, perfil atual,
  guards fail-closed de filhos HC e bloqueio de `CYCLING` validados. Room v13 e
  schema exportado permanecem.
- Revisão final corrigida: metadata de filhos validada por mapper estrito e
  agregação exclui evidências inválidas antes da fórmula. Evidências atuais:
  clean compileDebugKotlin, lintDebug e `git diff --check` passaram.
- Bloqueador final corrigido: operações legacy `WATCH_D3_D4` são rejeitadas
  fail-closed sem criar reconciliação. Risco de migration: linhas legacy
  preservadas continuam bloqueando calibração até decisão futura explícita.
- `exportRoomSchema` não existe neste projeto.
- `023-03` concluída com altura/meta editáveis no Perfil, fila Health Connect
  existente preservada e estado vazio para perfis aprendidos.
- Validação concluída com compileDebugKotlin, lintDebug, lint, assembleDebug,
  assembleRelease e `git diff --check`. Persistência/restart e Health Connect
  real permanecem validação física de `023-07`.
- `023-06` foi selecionada explicitamente para implementar provenance do frozen
  payload Health Connect e integração contextual.
- Recorte de `023-06` implementou frozen payload Health Connect v2,
  `metricProvenance`, decoder compatível com v1, resolução contextual, derivação
  `v3`, consumers por snapshot e guards fail-closed.
- `023-04` implementada; validação física de restart e Health Connect permanece
  bloqueada para `023-07`.
- `023-05` revisada e corrigida: preview canônica, provenance contextual,
  persistência transacional, guards e correção/remoção. Build completo passou;
  validação física permanece em `023-07`.
- `023-05` concluída após guards fail-closed, CTA seguro, preview finita e
  labels alinhados. Validação física Health Connect permanece em `023-07`.
- Correção de uso: revisão de corrida/caminhada agora oferece distância de
  referência já no estado `PENDING_REVIEW`, preservando valor até aceite.
- Splits recalculados agora exibem trecho final parcial e atividades abaixo de
  1 km, sem descartar distância residual.
- Calibração preserva timestamps/BPM persistidos e alinha preview à duração
  canônica da série.
- Exclusão permanente passou a cobrir atividades locais elegíveis não
  sincronizadas, mantendo bloqueios para sync, efeitos remotos e sessões phone.
- Guard de exclusão rejeita origens desconhecidas e valida import compatível.
- Exclusão permanente de atividade agora possui CTA fail-closed no detalhe,
  confirmação irreversível com data/tipo e fechamento após sucesso; integração
  contextual concluída; validação física permanece pendente.
- `023-06` concluída: resolução contextual centralizada, derivação `v3`,
  consumidores baseados em snapshot e guards fail-closed de projeção, incluindo
  bloqueio de filhos em retry com payload congelado, integrados; Health Connect
  permanece explícito e write-only.
- Validação física de payload v2, retry e Health Connect real permanece escopo de
  `023-07`; nenhum efeito remoto é criado durante calibração ou rederivação.
- `023-07` em andamento: build, lint e instalação no Galaxy S22 passaram. Foram
  informadas corridas de `2,90 km` em 12/08 e `3,00 km` em 14/08; a segunda não
  deve ser sincronizada com Health Connect. Importação explícita encontrou as
  duas corridas e um treino livre; ambas as corridas foram calibradas e fecharam
  em `2.900 m` e `3.000 m` antes da remoção autorizada da corrida 14/08. Corrida
  12/08 sincronizada explicitamente com 33 operações HC; corrida 14/08 removida
  sem operação HC. Treino livre revisado como `WALKING` e corrigido para `950 m`,
  gerando `950 / 936 = 1,014957 m` sem operação HC. Após remoção, RUNNING ficou
  em `2.900 / 3.538 = 0,819672 m`; `user_version=13` e `integrity_check=ok`.
  Altura `175 cm`, meta `1.000 passos` e pesos `95/96 kg` foram persistidos após
  reinício. Teste sem permissão WRITE_WEIGHT salvou `95 kg` localmente e deixou
  operação HC falhou temporariamente; permissão foi restaurada e retry sincronizou
  a operação. Novo treino WALKING de 14/08 usou
  `CONTEXTUAL_LEARNED_PROFILE` após reprocessamento explícito, sem operação HC.
  TREADMILL validado com BASE/manual sem operação HC; CYCLING, Daily Summary e
  consumidores da meta conferidos. HC indisponível/desabilitado, guards,
  regressões restantes e demais gates ainda pendentes; corrida
  reimportada de 14/08 permanece pendente sem sync; progresso permanece `6/7`.
- Continuação da inspeção confirmou, por cópia somente leitura do banco, 11
  atividades, 16 snapshots (11 ativos), 2 calibrações, 4 pesos, 1 distância
  manual de bicicleta, 220 operações HC e zero órfãos nas consultas de
  calibrações/snapshots/operações de treino, sem duplicidade de calibração ou
  snapshot ativo. Payload
  calibrado RUNNING preserva frozen payload v2, provenance externa e
  `watch-d3-d4-v3`; a migration `12 -> 13` é aditiva no código. Guard físico de
  `SYNCED` foi observado. Sem fixture segura, `SYNCING`, efeito remoto parcial,
  Health Connect indisponível/desabilitado e prova física sobre banco v12
  anterior permanecem `N/A`; status continua `in_progress` e progresso `6/7`.

## Estado esperado ao final

```text
Perfil
├── altura editável
├── meta diária de passos
├── peso + histórico
├── comprimento do passo base
├── corrida: perfil aprendido
└── caminhada: perfil aprendido

Treino RUNNING/WALKING
├── D3/D4 + decodedSteps
├── distância estimada pelo perfil vigente
├── distância externa opcional para calibração
├── snapshot imutável por atividade
└── Health Connect explícito
```

## Fechamento

A Fase 16 só deve ser marcada como concluída em `023-07`, depois de evidência em
banco preservado e dispositivo real. Concluir tasks isoladas não conclui a fase.
