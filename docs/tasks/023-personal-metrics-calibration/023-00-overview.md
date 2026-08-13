# 023 — Overview: Perfil, métricas pessoais e calibração

- **Fase:** 16
- **Spec:** `docs/specs/023-personal-metrics-calibration.md`
- **Status:** `in_progress`
- **Progresso:** `5/7`
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
- [ ] `023-06-integrate-contextual-step-length-and-health-projections.md` — derivação contextual e projeções integradas
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
- `023-06` selecionada explicitamente para implementar provenance do frozen
  payload Health Connect; item permanece desmarcado enquanto estiver em curso.
- Recorte autorizado de `023-06` implementou frozen payload Health Connect v2,
  `metricProvenance` e decoder compatível com v1; item permanece desmarcado por
  não concluir integração contextual, consumers ou validação física.
- `023-04` implementada; validação física de restart e Health Connect permanece
  bloqueada para `023-07`.
- `023-05` revisada e corrigida: preview canônica, provenance contextual,
  persistência transacional, guards e correção/remoção. Build completo passou;
  validação física permanece em `023-07`.
- `023-05` concluída após guards fail-closed, CTA seguro, preview finita e
  labels alinhados. Validação física Health Connect permanece em `023-07`.

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
