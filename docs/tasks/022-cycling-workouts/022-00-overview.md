# 022 — Suporte a treinos de bicicleta

- **Status geral:** `in_progress`
- **Spec:** [022-cycling-workouts.md](../../specs/022-cycling-workouts.md)
- **ADR:** [ADR-021](../../decisions/adr-021-cycling-distance-and-health-projection.md)
- **Base:** `main`
- **Branch:** `feat/022-cycling-workouts`
- **Progresso:** `4/5`

## Objetivo

Permitir revisar uma atividade D3/D4 como `Bicicleta`, com distância manual
opcional, mantendo duração/BPM/calorias do relógio e impedindo que passos sejam
usados como distância, velocidade, pace ou cadência de ciclismo.

## Checklist

- [x] [022-01-audit-cycling-consumers-and-freeze-contracts.md](022-01-audit-cycling-consumers-and-freeze-contracts.md)
- [x] [022-02-add-cycling-type-manual-distance-and-persistence.md](022-02-add-cycling-type-manual-distance-and-persistence.md)
- [x] [022-03-adapt-cycling-local-metrics-and-ui.md](022-03-adapt-cycling-local-metrics-and-ui.md)
- [x] [022-04-add-cycling-health-connect-projections.md](022-04-add-cycling-health-connect-projections.md)
- [ ] [022-05-validate-cycling-workflow-and-close.md](022-05-validate-cycling-workflow-and-close.md)

## Dependências

```text
022-01
   ↓
022-02
   ↓
022-03
   ↓
022-04
   ↓
022-05
```

## Decisões congeladas

- `WorkoutType.CYCLING` com label `Bicicleta`;
- um único tipo para academia e rua;
- seleção manual durante revisão, sem heurística automática;
- distância total opcional e digitada pelo usuário;
- distância persistida separadamente de original/estimada/calibrada;
- passos D4 preservados, mas nunca convertidos em distância de bicicleta;
- sem pace, cadência de passos, splits por km ou velocidade granular derivada;
- velocidade média local somente quando existir distância manual;
- Health Connect usa `EXERCISE_TYPE_BIKING`;
- distância manual gera no máximo um `DistanceRecord` total;
- distância manual usa provenance `manualEntry`, não `autoRecorded` do relógio;
- BPM e calorias do relógio continuam elegíveis;
- ao salvar revisão com tipo diferente de `CYCLING`, distância manual é limpa no
  mesmo write;
- sem GPS, rota, mapa, FTMS, RPM ou potência;
- sem edição/reconciliação nova de atividade já `SYNCED`.

## Persistência planejada

Room atual: `v11`.

Mudança esperada:

```text
v11 → v12
workout_activities.manualDistanceMeters REAL NULL
```

Migration aditiva, sem wipe e sem efeito remoto.

## Regras de execução

- seguir `AGENTS.md`;
- não criar nem executar testes automatizados salvo pedido explícito;
- manter parser `d4-v3` e derivação `watch-d3-d4-v2` inalterados;
- não reutilizar calibração de esteira para bicicleta;
- não introduzir GPS/rota;
- mudanças de schema devem atualizar schema Room exportado;
- solicitar revisão independente antes de fechar cada subtarefa;
- manter spec e overview atualizados durante execução.

## Fechamento esperado

A spec só pode ser marcada `completed` quando:

- bicicleta funciona com e sem distância;
- o banco existente atualiza para v12 sem wipe;
- consumidores locais não usam a derivação por passos como métrica de ciclismo;
- Health Connect recebe sessão genérica de biking e provenance correta da
distância manual;
- nenhuma projeção granular falsa de distância/velocidade é criada;
- build e validação física possuem evidência factual.

## Observações

- `022-01` concluída com relatório sanitizado em
  `docs/integration/022-01-cycling-consumer-audit.md`.
- Revisão independente aprovada em 2026-08-11.
- `022-02` concluída com `CYCLING`, distância manual opcional, migration Room
  v11→v12 e formulário de revisão; build/lint aprovados em 2026-08-11.
- `022-03` concluída com revisão independente aprovada com ressalva não
  bloqueadora: calibração histórica permanece visível somente como diagnóstico.
- `022-04` concluída com mapping genérico de biking, projeção total manual e
  reconciliação sem `DistanceInterval`/`SpeedSeries` para cycling; lint, builds e
  revisão independente aprovados em 2026-08-11.
- Riscos pendentes: inspeção manual de cycling com e sem distância, upgrade físico
  v11→v12 e validação final do fluxo Health Connect permanecem em `022-05`.
- Próxima subtarefa elegível: `022-05`; não iniciada neste trabalho.
