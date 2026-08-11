# 022-01 — Auditar consumers e congelar contratos de bicicleta

- **Status:** `pending`
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

## Fora do escopo

- alterar código funcional;
- migration;
- UI nova;
- Health Connect write;
- GPS/rota;
- testes automatizados.

## Critérios de aceite

- [ ] todos os switches/mappings de `WorkoutType` relevantes foram localizados;
- [ ] todos os consumers de distância/velocidade/pace/splits foram mapeados;
- [ ] política atual de revisão/calibração está documentada;
- [ ] pontos de criação e retry de filhos Health Connect estão documentados;
- [ ] decisão sobre limpeza vs. fail-closed de distância manual ao trocar tipo foi congelada;
- [ ] nenhum código funcional foi alterado;
- [ ] revisão independente confirma que o mapa cobre os consumers necessários.
