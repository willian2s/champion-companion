# Task 007-09 — Executar Validação Manual Do Resumo Diário

## Status

Pendente.

## Objetivo

Executar gate reduzido e smoke test manual físico da Fase 07 no Galaxy S22 com Champion C001.

## Contexto

Fase 07 depende de comportamento físico BLE para FEA1. Build verde não substitui smoke manual.

## Documentos Obrigatórios

- `AGENTS.md`
- `docs/specs/007-daily-summary-home.md`
- `docs/tasks/007-daily-summary-home/007-00-overview.md`
- tasks `007-01` a `007-08`

## Dependências

- `007-08`

## Entradas

- App implementado até `007-08`.
- Galaxy S22.
- Champion C001.

## Arquivos Exatos Permitidos

- `docs/specs/007-daily-summary-home.md`
- `docs/tasks/007-daily-summary-home/007-00-overview.md`
- `docs/tasks/007-daily-summary-home/007-09-run-manual-daily-summary-validation.md`
- Arquivos Kotlin apenas para correções diretas de regressões encontradas nesta validação.

## Arquivos Proibidos

- Roadmap.
- ADRs.
- Specs concluídas anteriores.
- Tasks concluídas anteriores.
- Refatorações não relacionadas.
- Gradle/manifest salvo bloqueio direto introduzido pela fase.

## Escopo

- Rodar comandos obrigatórios.
- Rodar comandos informativos quando útil.
- Executar smoke manual completo.
- Documentar falhas antigas não relacionadas.
- Corrigir regressões introduzidas pela Fase 07.

## Fora Do Escopo

- Campanha ampla de testes.
- Correção geral da suíte antiga.
- Novas features.

## Passos

1. Executar `./gradlew assembleDebug`.
2. Executar `./gradlew assembleRelease`.
3. Executar `./gradlew installDebug`.
4. Executar informativos se cabível: `./gradlew lint`, `./gradlew testDebugUnitTest`.
5. Executar smoke test de 35 passos do overview.
6. Registrar resultado, falhas e evidências.
7. Atualizar overview.

## Critérios De Aceite

- Gate obrigatório registrado.
- Smoke manual registrado.
- Regressões da Fase 07 corrigidas ou bloqueio explícito documentado.
- Falhas antigas não relacionadas separadas.

## Validação Mínima

```bash
./gradlew assembleDebug
./gradlew assembleRelease
./gradlew installDebug
```

## Evidência Manual

- Resultado do roteiro de 35 passos.
- Build/install usados.
- Estado do dispositivo e app.

## Atualização Do Overview

- Marcar `007-09` como concluída se gate e smoke passarem.

## Validação Final

- Confirmar ausência de Health Connect, D1, D3 e D4.
