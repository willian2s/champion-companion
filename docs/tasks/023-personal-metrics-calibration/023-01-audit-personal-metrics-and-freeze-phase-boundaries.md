# 023-01 — Auditar métricas pessoais e congelar fronteiras da Fase 16

- **Status:** `completed`
- **Spec:** `023-personal-metrics-calibration`

## Objetivo

Confirmar no código da branch o estado real de perfil, peso, meta de passos,
derivação D3/D4 e Health Connect antes de qualquer implementação, transformando
as decisões da spec/ADR em contratos executáveis sem reimplementar o que já
existe.

## Escopo

Auditar pelo menos:

- `UserProfile` / `UserProfileEntity`;
- `PersonalMetricsSnapshot` / `PersonalMetricsRepository`;
- `ProfileScreen` / `ProfileViewModel`;
- `AppPreferences.dailyStepsGoal` e consumers;
- `WeightMeasurementEntity` e surfaces de Saúde/Peso;
- mapeamento atual de `HeightRecord` e `WeightRecord`;
- `StepLengthProfile`, `StepLengthSource` e sugestão por altura;
- resolução atual de step length na derivação D3/D4;
- fluxo de review/reprocessamento de `RUNNING` e `WALKING`;
- snapshots `watch-d3-d4-v2` e contratos para nova versão;
- projeções congeladas Health Connect pós-Spec 022;
- guards de `SYNCING`, `SYNCED`, `SYNC_FAILED` e efeitos remotos parciais;
- consumers de distância/pace/velocidade em Exercícios, Calendário e Saúde.

## Decisões a congelar

Registrar em documento de integração da task:

1. onde a nova tabela de calibração será ligada ao perfil/atividade;
2. política de uma evidência por atividade;
3. forma de calcular perfis aprendidos sem estado duplicado;
4. momento exato em que `confirmedType` passa a selecionar perfil contextual;
5. quais workflow states permitem rederivar a própria atividade calibrada;
6. comportamento para atividade já sincronizada;
7. provenance mínima necessária para filhos HC de atividade diretamente
   calibrada;
8. ponto de enqueue já existente para novas alturas/pesos;
9. consumers que precisam de alteração e os que permanecem intactos.

## Fora do escopo

- implementar migration;
- alterar UI;
- criar calibração;
- mudar algoritmo de derivação;
- criar testes;
- alterar GPS/rota legado.

## Entregável

Criar:

```text
docs/integration/023-01-personal-metrics-and-calibration-audit.md
```

com mapa de arquivos, contratos congelados, riscos e decisões finais de
implementação.

## Checklist

- [x] perfil atual auditado
- [x] peso/meta de passos auditados
- [x] derivação e workflow auditados
- [x] Health Connect auditado
- [x] consumers listados
- [x] guards de sync congelados
- [x] provenance decidida
- [x] documento de integração criado
- [x] spec/overview atualizados com resultado da auditoria

## Aceite

A task termina somente quando `023-02` puder implementar schema/domínio sem
decisões arquiteturais abertas.

## Resultado da execução

- **Status:** `completed`
- **Arquivos alterados:**
  - `docs/integration/023-01-personal-metrics-and-calibration-audit.md`;
  - esta subtarefa;
  - `docs/tasks/023-personal-metrics-calibration/023-00-overview.md`;
  - `docs/specs/023-personal-metrics-calibration.md`.
- **Decisões e desvios:** contratos congelados conforme Spec 023 e ADR-022. A
  calibração não usará FK rígida para atividade; atividades sincronizadas ou com
  efeitos remotos parciais não serão evidência nesta fase; provenance direta
  exigirá campo opcional retrocompatível no payload Health Connect v2; a
  identidade vigente permanece `originId = activity.id`. `DailySummary` mantém
  semântica BASE atual, sem perfis aprendidos. Nenhuma alteração de código foi
  feita.
- **Comandos executados:** buscas e leituras direcionadas do código/documentação;
  `git diff --check`.
- **Resultados e evidências:** documento de integração registra mapa de arquivos,
  linhas auditadas, nove decisões de integração, consumers preservados, guards de
  sync e riscos. `git diff --check` passou.
- **Riscos residuais:** mutation de altura/peso, migration/domínio de calibração,
  resolver contextual e payload v2 permanecem para subtarefas seguintes. Todos
  os entrypoints de rederivação deverão usar whitelist comum; o guard de
  `SYNC_FAILED` deverá inspecionar operações filhas antes de liberar
  rederivação.
