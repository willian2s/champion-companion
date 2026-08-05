# 014-02 — Definir contratos canônicos e temporais

- **Ticker:** `014`
- **Número:** `02`
- **Status:** pending

## Objetivo e resultado esperado

Criar contratos Android-free que separem raw, measured, snapshot derivado,
elegibilidade temporal, plausibilidade, métricas, splits e identidade conceitual
de projeções filhas.

Resultado: domínio compilável e decisão fechada para a migration local de
`014-03`, sem dependência de Room, Compose, BLE Android ou Health Connect SDK.

## Dependências

- `014-01` concluída e revisada.

## Arquivos permitidos

- `core/model/WatchWorkoutActivity.kt`;
- `core/model/DeviceProfileDailySummary.kt`;
- `core/model/HeartRateAndSync.kt`;
- `core/model/RepositoryContracts.kt`;
- novos arquivos em `core/model`;
- spec, ADR-018 e overview.

Símbolos prováveis:

- `StepLengthSource`;
- `WorkoutDerivationMethod`;
- `MeasuredWorkoutInterval`;
- `WorkoutDerivationSnapshot`;
- `DerivedWorkoutInterval`;
- `TemporalSeriesEligibility` e motivos tipados;
- `WorkoutSeriesPlausibility`;
- `WorkoutSplit`;
- `HealthProjectionChildKey` conceitual;
- `HealthSyncRecordType.SPEED` sem habilitar writer.

## Arquivos proibidos

- `core/bluetooth`, `core/database`, `data`, `feature` e `core/healthconnect`;
- Gradle, manifesto, migrations, schemas e Fase 013.

## Decisões obrigatórias

### Série e distância

```text
canonicalIntervalDistance = decodedSteps × stepLengthMetersUsed
canonicalDistance = soma exata dos intervalos canônicos
```

- Série inelegível implica distância canônica ausente.
- `totalSteps × stepLengthMetersUsed` é somente estimativa agregada diagnóstica.
- Original, calibrada histórica e raw temporal não são fallback.

### Índices

Série V1 exige índices únicos e contínuos `0..N-1`. Lacuna, duplicata, primeiro
índice não zero ou ordem inconsistente recebe motivo explícito de inelegibilidade.

### Duração e plausibilidade

- Modelar os quatro casos `T/N/60 s` da spec.
- Modelar `LAST_INTERVAL_RESIDUAL` separadamente de plausibilidade.
- Definir motivos tipados, incluindo cadência/velocidade implausível.
- Registrar limiar adotado ou manter política inelegível sem limiar até evidência;
  não esconder a decisão em código posterior.

### BPM por split

- Janelas semiabertas `[start, end)`.
- Sample-and-hold: amostra `i` cobre `[sampledAt_i, sampledAt_i+1)`, limitada
  pela sessão.
- Média ponderada pela duração de sobreposição.

### SpeedRecord

A política estável é ponto médio do intervalo:

```text
sampledAt = intervalStart + effectiveDuration / 2
```

### Esteira

Para `WATCH_D3_D4`, `TREADMILL` pode avançar para `READY_TO_SYNC` quando existir
série canônica elegível, sem exigir calibração histórica. Contratos phone
permanecem legados.

### Projeções filhas

Definir conceitualmente uma operação por filho:

```text
aggregate + recordType + projectionChildKey
```

Não desenhar schema ainda, mas impedir contrato que volte a uma operação com
subfila interna de vários records.

## Passos de implementação

1. Definir raw/measured/derived sem reutilizar nomes legados com nova semântica.
2. Modelar `stepLengthMeters` positivo, origem e snapshot imutável.
3. Modelar série canônica única e estimativa diagnóstica separada.
4. Modelar índices, quatro casos temporais e plausibilidade.
5. Modelar métricas por intervalo, acumulado e split.
6. Formalizar BPM temporal e ponto médio da velocidade.
7. Formalizar workflow `TREADMILL` para origem relógio.
8. Adicionar `SPEED` ao enum sem tornar atividade elegível antes de `014-13`.
9. Modelar chave conceitual de filho Health Connect.
10. Manter adapters compatíveis para consumidores ainda não migrados.

## Critérios de aceite

- Contratos representam todas as decisões sem tipos Android.
- Não existe API que permita duas distâncias canônicas.
- Estimativa agregada é nomeada como diagnóstica.
- Lacunas e duplicatas são representáveis sem exceção silenciosa.
- Plausibilidade não modifica duração nem passos.
- BPM temporal e midpoint de velocidade são determinísticos.
- Workflow de esteira diferencia `WATCH_D3_D4` de phone histórico.
- `SPEED` não habilita writer prematuramente.
- A chave conceitual suporta uma operação por filho.

## Validação

```bash
./gradlew lint assembleDebug assembleRelease
git diff --check
git status --short
```

Não criar nem executar testes automatizados.

## Evidências

- tabela requisito → tipo/invariante;
- matriz de elegibilidade e plausibilidade;
- exemplos de split/BPM e midpoint;
- resultado dos builds e revisão de arquitetura.

## Definição de pronto

Contratos compilam, schema lógico local está fechado para `014-03` e item
`014-02` está marcado no overview.

## Rollback

Remover novos contratos/adapters e restaurar enum sem `SPEED`; sem efeito em
dados persistidos.
