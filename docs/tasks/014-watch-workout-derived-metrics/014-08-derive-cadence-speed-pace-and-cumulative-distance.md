# 014-08 — Derivar cadência, velocidade, ritmo e distância acumulada

- **Ticker:** `014`
- **Número:** `08`
- **Status:** pending

## Objetivo e resultado esperado

Completar série V1 sem suavização usando distância canônica e duração efetiva.
Resultado: métricas totais/temporais e acumulado consistentes, com zero passos e
ausências tratados explicitamente.

## Contexto

`014-07` fornece distância canônica congelada. Consumidores atuais ainda usam
ritmo/velocidade importados da distância original.

## Requisitos cobertos

- R5 e R6.

## Dependências

- `014-07` concluída.

## Arquivos permitidos

- políticas/contratos derivados em `core/model`;
- mappers/repository de snapshots;
- read models estritamente necessários;
- overview.

Símbolos prováveis: métricas de `DerivedWorkoutInterval`, acumulador determinístico
e campos canônicos de `WorkoutMetrics`/read model.

## Arquivos proibidos

- parser D4, perfil, UI final, splits e Health Connect;
- Gradle, manifesto, migrations e schemas;
- dados raw para sobrescrita.

## Escopo

- cadência em passos/minuto;
- velocidade em m/s;
- ritmo em s/km;
- distância acumulada;
- agregados da atividade pela mesma base canônica.

## Fora do escopo

- suavização/interpolação de velocidade;
- zonas, VO2, calorias ou passada variável;
- splits de quilômetro;
- testes automatizados.

## Passos de implementação

1. Calcular métricas com `Double` sem arredondar persistência prematuramente.
2. Aplicar fórmulas V1 por intervalo válido.
3. Tratar zero passos como distância/cadência/velocidade zero e ritmo ausente.
4. Acumular somente deltas canônicos em ordem estável.
5. Calcular totais pela duração e distância canônica total, sem média de médias.
6. Impedir fallback para campos importados/calibrados em `WATCH_D3_D4`.
7. Persistir/fornecer read model reproduzível.
8. Atualizar overview.

## Critérios de aceite

- Fórmulas e unidades correspondem à spec.
- Acumulado é monotônico e termina na soma temporal, quando série elegível.
- Intervalo zero segue contrato completo.
- Série ausente não produz números `NaN`, infinito ou placeholders.
- Métricas antigas pela distância original deixam de alimentar consumidores
  canônicos.

## Validação manual

- Conferir planilha/cálculo independente para atividade de referência.
- Verificar intervalo zero e último parcial.
- Comparar total pela fórmula, não pela média dos intervalos.

## Comandos permitidos

```bash
./gradlew lint assembleDebug assembleRelease
./gradlew installDebug
git diff --check
git status --short
```

Não criar nem executar testes automatizados.

## Evidências

- tabela sanitizada intervalo → duração/passos/distância/cadência/velocidade/ritmo;
- acumulado final;
- builds e smoke local.

## Definição de pronto

Série V1 completa e consistente, sem suavização ou fallback, com item `014-08`
marcado.

## Riscos e cuidados

- Duração parcial altera cadência/velocidade do último intervalo.
- Arredondamento somente na apresentação.
- Total canônico e soma temporal podem divergir; manter diagnóstico explícito.

## Rollback

Retornar métricas novas como ausentes e preservar snapshots; não restaurar
distância original como fallback canônico.

## Atualização obrigatória do overview

Atualizar `014-00-overview.md`, marcar `014-08` e registrar unidades/precisão.
