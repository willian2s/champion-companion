# 022-01 — Auditar consumers e congelar contratos de bicicleta

- **Status:** `completed`
- **Depende de:** nenhuma
- **Desbloqueia:** `022-02`

## Objetivo

Mapear todos os pontos que dependem de `WorkoutType`, distância, velocidade,
pace, cadência, splits e projeções Health Connect antes de alterar o domínio.
Congelar a matriz de comportamento de `CYCLING` para evitar regressão silenciosa
em running/walking/treadmill.

## Escopo

Auditar pelo menos:

- `WorkoutType` e mapeamentos de persistência;
- formulário e transições da revisão;
- política atual de `PENDING_CALIBRATION`;
- consumidores de `WorkoutDistanceMetrics`;
- derivação/canonical series por passos;
- detalhe/lista de exercícios;
- calendário e detalhe diário;
- históricos/aggregates locais de distância;
- `HealthProjectionChildKey`;
- manifesto/reconciliação de projeções filhas;
- frozen payload e mapper Health Connect;
- identidade/versionamento de operações.

## Matriz que deve ser registrada

```text
                    WALK/RUN/TREADMILL   CYCLING
steps -> distance          vigente          nunca
manual distance            não              opcional
pace                        vigente          ausente
step cadence                vigente          ausente
km splits                   vigente          ausente
avg speed                   vigente          manual/duração
DistanceInterval HC         vigente          nunca
SpeedSeries HC              vigente          nunca
ManualDistance HC           não              opcional
HR/Calories HC              vigente          vigente
```

A matriz real deve respeitar exceções existentes de treadmill e demais guards;
o relatório não deve simplificar comportamento que o código atual trate de
forma diferente.

## Entregável

Criar relatório sanitizado em:

```text
docs/integration/022-01-cycling-consumer-audit.md
```

O relatório deve listar arquivos/consumers concretos, riscos e decisões de
implementação necessárias para `022-02..04`.

## Execução

- **Status:** `completed`
- **Arquivos alterados:**
  - `docs/integration/022-01-cycling-consumer-audit.md`
  - `docs/tasks/022-cycling-workouts/022-00-overview.md`
  - este arquivo
- **Decisões:**
  - distância manual será limpa no mesmo salvamento quando revisão confirmar
    tipo diferente de `CYCLING`;
  - snapshots raw/measured/derived permanecem preservados, mas consumers falham
    fechados para `CYCLING` quando tentariam usar série por passos;
  - `CYCLING` permanece exclusivo do fluxo watch-first e não entra no fluxo phone;
  - payload congelado Health Connect v1, identidade determinística e versão
    monotônica serão reutilizados nas próximas subtarefas.
- **Desvios:** nenhum. Escopo permaneceu documental; nenhum código funcional,
  migration ou teste foi criado.
- **Comandos executados:**
  - `git diff --check`
  - `git diff --no-index --check /dev/null docs/integration/022-01-cycling-consumer-audit.md`
- **Resultados e evidências:** ambos os comandos não encontraram erro de
  whitespace; relatório sanitizado criado no caminho do entregável. Revisão
  independente aprovou cobertura, referências e ausência de código funcional
  alterado em 2026-08-11.
- **Testes/lint/typecheck/build:** não executados; subtarefa somente documental,
  sem alteração de código ou Gradle, conforme escopo e regras do repositório.
- **Riscos residuais:** `022-02..04` ainda precisam transportar o campo manual
  no Room/read model, aplicar limpeza atomicamente, filtrar consumers locais e
  ajustar manifesto/reconciliação Health Connect sem gerar distância/speed
  derivados para bicicleta.

## Fora do escopo

- alterar código funcional;
- migration;
- UI nova;
- Health Connect write;
- GPS/rota;
- testes automatizados.

## Critérios de aceite

- [x] todos os switches/mappings de `WorkoutType` relevantes foram localizados;
- [x] todos os consumers de distância/velocidade/pace/splits foram mapeados;
- [x] política atual de revisão/calibração está documentada;
- [x] pontos de criação e retry de filhos Health Connect estão documentados;
- [x] decisão sobre limpeza vs. fail-closed de distância manual ao trocar tipo foi congelada;
- [x] nenhum código funcional foi alterado;
- [x] revisão independente confirma que o mapa cobre os consumers necessários.
