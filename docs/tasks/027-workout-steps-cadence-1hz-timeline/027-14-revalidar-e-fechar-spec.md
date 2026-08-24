# 027-14 — Revalidar e fechar a Spec 027

- **Ticker:** `027`
- **Número:** `14`
- **Status:** `cancelled`
- **Depends on:** `027-11`, `027-12`, `027-13`

## Objetivo

Revalidar Gate B após correções e fechar a Spec 027 somente com baseline factual.

## Escopo

- validar timeline local em atividade existente;
- confirmar grade comum, início/fim, gaps e provenance;
- confirmar estatísticas históricas inalteradas;
- confirmar identidade/versionamento e cache;
- repetir lint e builds;
- instalar debug somente se necessário para evidência visual;
- atualizar documentação append-only.

Health Connect, Health Sync e Strava não serão reexecutados nesta subtarefa.
Evidências `027-06` e `027-11` permanecem fontes do Gate A e das duas atividades
sincronizadas. Não declarar readback onde ele não existe.

## Documentação

Atualizar, quando aplicável:

- `docs/specs/027-workout-steps-cadence-1hz-timeline.md`;
- `docs/tasks/027-workout-steps-cadence-1hz-timeline/027-00-overview.md`;
- `docs/tasks/027-workout-steps-cadence-1hz-timeline/027-11-validar-e-fechar-spec.md`;
- `docs/baseline/current-baseline.md`;
- `docs/decisions/adr-026-workout-1hz-derived-timeline.md`;
- nova evidência sanitizada, se houver validação visual.

Preservar histórico bloqueado de `027-11`; adicionar resolução posterior sem
apagar resultado anterior.

## Critérios de aceite

- timeline preserva gaps e distingue ausência de valor zero;
- BPM e cadência compartilham grade válida;
- pico/fundo observados têm comportamento documentado após suavização;
- SOURCE, snapshots, estatísticas e Health Connect não são reescritos;
- Gate A continua `go_with_adjustments` sem regressão;
- `./gradlew lint assembleDebug assembleRelease` passa;
- nenhuma nova sincronização remota é inferida sem evidência.

## Verificação

```bash
git diff --check
./gradlew lint assembleDebug assembleRelease
```

Não criar nem executar testes automatizados sem pedido explícito.

## Definição de pronto

Revisão independente aprovada, checklist atualizado de forma coerente, Spec
fechada ou bloqueio final explicitado. Não iniciar outra subtarefa
automaticamente.

## Riscos

Validação visual limitada a atividades disponíveis e não substitui cobertura de
todos os padrões D4. Mudança de dia/reset continua limitação conhecida.

## Encerramento do plano

Cancelada por decisão de produto. Revalidação final será feita por `027-16` após
remoção do Gate B; não haverá fechamento da timeline 1 Hz.
