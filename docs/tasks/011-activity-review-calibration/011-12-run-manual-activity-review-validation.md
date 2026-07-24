# Task 011-12 - Executar validação manual da revisão e calibração

## Status

Planejada em 2026-07-24.

## Objetivo

Executar gate de build e validação manual completa da fase em Galaxy S22 com atividades reais D3/D4.

## Contexto

A fase depende de validação física e manual, não de testes automatizados. O roteiro precisa cobrir Home, lista, detalhe, review, calibração, reimportação, descarte e ausência de Health Connect funcional.

## Documentos obrigatórios

- `AGENTS.md`
- `docs/specs/011-activity-review-calibration.md`
- `docs/tasks/011-activity-review-calibration/011-00-overview.md`
- todas as tasks `011-01` a `011-11`

## Dependências

- `011-11`.

## Entradas

- fase funcional implementada;
- atividades D3/D4 reais já importadas.

## Arquivos exatos permitidos

- `docs/specs/011-activity-review-calibration.md`
- `docs/tasks/011-activity-review-calibration/011-00-overview.md`
- `docs/tasks/011-activity-review-calibration/011-12-run-manual-activity-review-validation.md`

## Arquivos proibidos

- `app/src/test/**`
- `app/src/androidTest/**`
- qualquer arquivo Kotlin, salvo correção inevitável descoberta e tratada em task nova explícita.

## Escopo

- executar gate de build;
- instalar no Galaxy S22;
- validar roteiro manual completo;
- registrar screenshots necessárias;
- registrar achados físicos.

## Fora do escopo

- criar ou rodar testes automatizados;
- ajustar Health Connect.

## Passos

1. Executar `./gradlew lint assembleDebug assembleRelease`.
2. Executar `./gradlew installDebug`.
3. Validar manualmente Home, filtros, detalhe, métricas, gráfico, intervalos, review, calibração, persistência após reinício, reimportação, bloqueio de `PARTIAL`, descarte e ausência de Health Connect funcional.
4. Registrar screenshots e achados.

## Critérios de aceite

- gate executado com sucesso;
- instalação em Galaxy S22 concluída;
- roteiro manual completo executado;
- screenshots registradas;
- ausência de testes novos ou alterados confirmada.

## Validação manual

- esta task é a própria validação manual.

## Comandos de build aplicáveis

```bash
./gradlew lint assembleDebug assembleRelease
./gradlew installDebug
```

## Evidências

- saída dos comandos;
- roteiro manual preenchido;
- screenshots do fluxo.

## Screenshot quando visual

- obrigatória.

## Atualização do overview

- marcar `011-12` como concluída;
- registrar achados físicos e riscos residuais aceitos.

## Validação final

- Não criar, alterar ou executar testes automatizados.
- Task executável em sessão nova, sem depender de contexto anterior.
