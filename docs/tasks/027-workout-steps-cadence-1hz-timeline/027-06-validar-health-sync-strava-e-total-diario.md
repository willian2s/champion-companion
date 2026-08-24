# 027-06 — Validar Health Sync, Strava e total diário

- **Ticker:** `027`
- **Número:** `06`
- **Status:** `pending`
- **Depends on:** `027-01` a `027-05`

## Objetivo

Executar o Gate A em hardware/contas reais e registrar somente comportamento observado.

## Cenário principal — atividade primeiro

1. obter/importar atividade real com passos D4 válidos;
2. confirmar total local da atividade e cadência por intervalos;
3. sincronizar explicitamente a atividade com Health Connect;
4. confirmar `StepsRecord` aceito;
5. confirmar `StepsCadenceRecord` aceito;
6. confirmar crédito local ativo uma única vez;
7. obter novo `DailySummary` cumulativo que contenha os passos da atividade;
8. sincronizar Daily;
9. confirmar residual calculado sem inflação;
10. confirmar total diário final coerente com o acumulado do relógio;
11. executar Health Sync para Strava;
12. verificar o que realmente chegou ao Strava.

## Cenário inverso — Daily primeiro

1. sincronizar Daily contendo a atividade;
2. sincronizar depois a atividade;
3. confirmar que `StepsRecord` da atividade é bloqueado por cobertura anterior;
4. confirmar que `StepsCadenceRecord` continua elegível;
5. confirmar ausência de inflação do total diário.

## Retry

Exercitar, quando reproduzível sem manipulação insegura:

- retry de atividade;
- retry de Daily;
- nova sincronização da mesma atividade;
- confirmação de que não existe segundo crédito.

## Health Sync -> Strava

Registrar separadamente:

- total de passos da atividade aparece ou não;
- cadência média aparece ou não;
- cadência máxima aparece ou não;
- gráfico temporal aparece ou não;
- granularidade observada;
- qualquer transformação aparente feita pelo Health Sync.

Não inferir suporte se o campo não estiver visível.

## Classificação

- `go`: integridade diária passa e interoperabilidade atende objetivo;
- `go_with_adjustments`: integridade diária passa, mas Health Sync/Strava preserva somente parte dos dados;
- `no_go`: risco de duplicidade/subcontagem ou comportamento remoto inseguro.

`no_go` de interoperabilidade não autoriza quebrar a regra de passos diários.

## Evidência

Criar documento sanitizado em `docs/integration/` com:

- dispositivo/versão relevantes;
- ordem dos syncs;
- totais antes/depois;
- estados locais;
- resultado Health Connect;
- resultado Health Sync/Strava;
- limitações e casos não reproduzidos.

Não registrar dados sensíveis desnecessários.

## Intercorrências observadas durante o Gate A

A primeira execução física importou a atividade normalmente, mas a ação explícita
de sincronizar encerrou o app por exceção não tratada. A validação foi interrompida
antes de afirmar aceite de `StepsRecord`, `StepsCadenceRecord`, crédito ou
interoperabilidade.

A primeira correção adicionou uma boundary defensiva no mapper para converter
falhas dos construtores estritos do SDK em `INVALID_LOCAL_DATA`. O reteste com
essa APK não crashou no mesmo ponto, porém revelou duas regressões adicionais:

1. o calendário passou a exibir `Dados parciais` porque a validação de
   `projectionChildKey` passou a rejeitar globalmente `STEPS` sem filho, incluindo
   o caso legítimo `DailySummary + STEPS`;
2. a sincronização da atividade retornou erro de projeção porque os payloads de
   `WorkoutSteps` e `StepsCadenceSeries` eram criados como payload base e somente
   depois completados por `.copy(...)`. O `init` de
   `HealthSyncFrozenProjectionPayload` exige esses campos no construtor, então a
   instância falhava antes do `.copy()`.

## Correções aplicadas antes do próximo reteste

- `DailySummary + STEPS` voltou a aceitar `projectionChildKey = null`; a
  obrigatoriedade de `WorkoutSteps`/`StepsCadenceSeries` fica restrita ao
  agregado `WorkoutActivity`;
- `WorkoutSteps` e `StepsCadenceSeries` agora são congelados atomicamente já com
  `steps` ou `stepsCadenceSamples` preenchidos no construtor;
- limites públicos usados pelo SDK permanecem validados antes da projeção:
  `StepsRecord` de `1` a `1.000.000` e cadência de `0` a `10.000` spm;
- funções de elegibilidade não transformam mais exceção em lista/conjunto vazio;
- a boundary global `SafeHealthConnectSyncCoordinator` foi removida; a proteção
  permanece no mapper, junto da operação que pode lançar por contrato do SDK,
  evitando esconder estado inconsistente de fila.

### Verificação da correção atual

GitHub Actions run `32680327755` executou com sucesso:

```bash
./gradlew lint assembleDebug assembleRelease
```

O mesmo run publicou `champion-companion-027-debug` como APK de validação.
Nenhum teste automatizado foi criado ou executado.

A task permanece `pending`: é obrigatório repetir primeiro a abertura do
calendário e depois a sincronização física da atividade com esta nova APK antes
de classificar o Gate A ou iniciar `027-07`.

## Saída

Gate A fechado antes de iniciar `027-07`.
