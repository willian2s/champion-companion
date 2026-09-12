# 033-07 — Remover entrypoints BLE e calibração por passo da UI

- **Status:** pending
- **Depende de:** 033-05

## Objetivo

Alinhar a experiência do usuário ao novo ownership sem apagar contratos legacy.

## Saúde

Remover da UI/contrato de produto:

- medir frequência cardíaca manualmente;
- buscar D1;
- cancel/retry/progresso BLE desses fluxos.

Preservar histórico manual/D1 antigo como leitura.

## Exercícios

Remover da UI de produto:

- buscar treinos no relógio;
- cancel/retry/progresso de importação própria.

Preservar review, referência de distância, treadmill, reprocessamento de manutenção
e sync explícito.

## Perfil

Remover affordances de:

- step length manual;
- sugestão por altura;
- perfis/histórico/correção de calibração por passo.

Preservar internamente legacy/fallback. Calibração contextual de distância D4
continua no exercício.

## Guard rails

- não apagar dados históricos;
- não redesenhar superfícies além do necessário;
- preservar baseline visual pós-031;
- screenshot no PR por ser mudança visual.

## Validação

```bash
./gradlew lint assembleDebug assembleRelease
git diff --check
```
