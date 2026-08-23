# 027-05 — Publicar cadência por intervalos D4

- **Ticker:** `027`
- **Número:** `05`
- **Status:** `pending`
- **Depends on:** `027-01`, `027-02`, `027-03`, `027-04`

## Objetivo

Adicionar `StepsCadenceRecord` à projeção Health Connect da atividade usando somente intervalos D4 válidos, sem depender da timeline de 1 Hz.

## Derivação

Para cada intervalo elegível:

```text
cadenceSpm = steps / durationSeconds * 60
```

Usar duração normalizada já persistida/derivada pela atividade.

## Elegibilidade da amostra

Não produzir amostra quando:

- duração <= 0;
- passos ausentes/inválidos;
- intervalo fora da janela da sessão;
- tipo de atividade não admite cadência de passos;
- dados de origem estiverem em estado que a política atual considera não utilizável.

## Timestamp

Usar convenção determinística coerente com a timeline D4 atual, preferencialmente midpoint do intervalo quando confirmado na auditoria.

Não apresentar midpoint como timestamp medido pelo sensor.

## Health Connect

Criar projeção filha estável para `StepsCadenceRecord`:

- uma identidade por série/versão;
- múltiplas samples no record quando API atual permitir;
- retry com payload congelado;
- permissão explícita de cadência;
- metadata coerente com origem do relógio.

## Independência dos passos

Cadência:

- não gera crédito;
- não consome crédito;
- continua elegível quando `StepsRecord` da atividade estiver `ALREADY_COVERED_BY_DAILY`;
- não altera `DailySummary`.

## Métricas locais

Pode expor média/mínimo/máximo derivados para UI somente se isso já se encaixar no modelo atual sem inventar estatística incompatível. O foco desta task é a série Health Connect.

## Fora de escopo

- suavização;
- interpolação 1 Hz;
- mudança em BPM;
- validação Strava — `027-06`.

## Verificação

```bash
./gradlew lint assembleDebug assembleRelease
```

Não criar nem executar testes automatizados sem pedido explícito.
