# Task 008-09 - Executar validacao fisica da medicao manual

## Status

Planejada.

## Objetivo

Executar gate reduzido e smoke fisico da Fase 08 no Galaxy S22 com Champion C001.

## Contexto

Fluxo manual de BPM depende de comportamento fisico do relogio e de lifecycle BLE real. Build verde nao substitui smoke manual.

## Documentos Obrigatorios

- `AGENTS.md`
- `docs/specs/008-manual-heart-rate.md`
- `docs/tasks/008-manual-heart-rate/008-00-overview.md`
- tasks `008-01` a `008-08`

## Dependencias

- `008-08`

## Entradas

- App implementado ate `008-08`.
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
- Executar smoke fisico da Fase 08.
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
5. Executar roteiro manual da spec e overview.
6. Registrar resultado, falhas, evidencias e riscos aceitos.
7. Atualizar overview.

## Criterios De Aceite

- Gate obrigatorio registrado.
- Smoke fisico registrado.
- Regressao da Fase 08 corrigida ou bloqueio explicito documentado.
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

- Confirmar ausencia de Health Connect, D1 e persistencia de stream.
