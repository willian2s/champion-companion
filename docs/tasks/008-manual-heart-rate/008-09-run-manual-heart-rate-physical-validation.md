# Task 008-09 - Executar validacao fisica da medicao manual

## Status

Planejada.

## Objetivo

Executar gate reduzido e smoke fisico da Fase 08 estendida no Galaxy S22 com Champion C001.

## Contexto

Fluxo manual de BPM e novo hub de Saude dependem de comportamento fisico do relogio, lifecycle BLE real e navegacao real entre cards e telas de metricas. Build verde nao substitui smoke manual.

## Documentos Obrigatorios

- `AGENTS.md`
- `docs/specs/008-manual-heart-rate.md`
- `docs/tasks/008-manual-heart-rate/008-00-overview.md`
- tasks `008-01` a `008-08B`

## Dependencias

- `008-08B`

## Entradas

- App implementado ate `008-08B`.
- Galaxy S22.
- Champion C001.

## Arquivos Exatos Permitidos

- `docs/specs/008-manual-heart-rate.md`
- `docs/tasks/008-manual-heart-rate/008-00-overview.md`
- `docs/tasks/008-manual-heart-rate/008-09-run-manual-heart-rate-physical-validation.md`
- Arquivos Kotlin apenas para correcoes diretas de regressao encontradas nesta validacao.

## Arquivos Proibidos

- Roadmap.
- ADRs.
- Specs concluidas anteriores.
- Tasks concluidas anteriores.
- Refatoracoes nao relacionadas.
- Gradle/manifesto salvo bloqueio direto introduzido pela fase.

## Escopo

- Rodar comandos obrigatorios.
- Rodar comandos informativos quando util.
- Executar smoke fisico da Fase 08 estendida.
- Documentar falhas antigas nao relacionadas.
- Corrigir regressao introduzida pela Fase 08.

## Fora Do Escopo

- Campanha ampla de testes.
- Correcao geral da suite antiga.
- Novas features.

## Passos

1. Executar `./gradlew assembleDebug`.
2. Executar `./gradlew assembleRelease`.
3. Executar `./gradlew installDebug`.
4. Rodar informativos se cabivel: `./gradlew lint`, `./gradlew testDebugUnitTest`.
5. Executar roteiro manual da spec e overview, incluindo hub de Saude e telas de metricas.
6. Registrar resultado, falhas, evidencias e riscos aceitos.
7. Atualizar overview.

## Criterios De Aceite

- Gate obrigatorio registrado.
- Smoke fisico registrado.
- Regressao da Fase 08 corrigida ou bloqueio explicito documentado.
- Hub de Saude, navegacao de metricas e medicao manual movida/composita validados juntos.
- Ausencia de Health Connect e ausencia de logs de BPM/F002 confirmadas.

## Testes Pequenos Aplicaveis

- Nenhum exclusivo desta task.

## Validacao Minima

```bash
./gradlew assembleDebug
./gradlew assembleRelease
./gradlew installDebug
```

## Evidencia Manual

- Resultado dos comandos.
- Resultado do smoke fisico item a item ou por agrupamento claro.

## Atualizacao Do Overview

- Marcar `008-09` como concluida.
- Consolidar comandos e riscos residuais aceitos.

## Validacao Final

- Confirmar ausencia de Health Connect, D1, persistencia de stream e dados simulados.
