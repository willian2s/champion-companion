# 027-06 — Validar Health Sync, Strava e total diário

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

## Saída

Gate A fechado antes de iniciar `027-07`.
