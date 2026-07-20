# Task 000-01 - Verificar Baseline

## Objetivo

Confirmar a baseline funcional do MotionLog antes de qualquer analise de codigo.

## Contexto

A Fase 0 usa `motionlog-app` como referencia funcional. A branch esperada e `feat/015-watch-workouts-d3-d4`.

## Documentos Obrigatorios Para Leitura

- `docs/tasks/000-00-overview.md`
- `docs/specs/000-phase-zero-baseline-handoff.md`
- `docs/roadmap/champion-companion-roadmap.md`
- `docs/decisions/adr-001-native-rewrite.md`
- `docs/specs/_inputs/phase-zero-original-requirements.md`

## Dependencias

- `000-00` lida.

## Arquivos Ou Areas De Entrada

- `../motionlog-app/.git`
- `champion-companion/.git`
- MotionLog instalado ou APK baseline, se disponivel.

## Arquivos Exatos De Saida

- `docs/baseline/current-baseline.md`
- Atualizacao do checklist em `docs/tasks/000-00-overview.md`

## Escopo

- Registrar branch, commit, data da analise, comandos usados e status dos repositorios.
- Confirmar se a baseline pode ser usada.
- Registrar APK e fluxos funcionais confirmados quando possivel.
- Registrar limitacoes e fixtures progressivas.

## Fora Do Escopo

- Analisar profundamente codigo do MotionLog.
- Alterar qualquer repositorio.
- Criar artefatos de protocolo.

## Regras De Seguranca

- `motionlog-app` e somente leitura.
- Roadmap e ADR nao podem ser alterados.
- Requisitos originais nao podem ser alterados.
- Nao deve haver commit ou push.
- Codigo Android ainda nao deve ser implementado.
- Inferencias nao podem ser apresentadas como fatos.
- Duvidas devem ser persistidas em documentos, nao apenas na resposta da sessao.

## Passos De Execucao

1. Executar `git -C ../motionlog-app branch --show-current`.
2. Se a branch nao for `feat/015-watch-workouts-d3-d4`, interromper e registrar bloqueio.
3. Executar `git -C ../motionlog-app rev-parse HEAD`.
4. Executar `git -C ../motionlog-app status --short`.
5. Executar `git status --short` em `champion-companion`.
6. Registrar comandos e saidas em `current-baseline.md`.
7. Registrar funcionalidades confirmadas somente quando houver evidencia.
8. Atualizar checklist do overview.

## Criterios De Aceite

- Branch esperada confirmada ou bloqueio documentado.
- Commit registrado.
- Status dos dois repositorios registrado.
- Regra de somente leitura registrada.
- Referencias ao roadmap e ADR incluidas.

## Validacao Final

- Mostrar status Git dos dois repositorios.
- Confirmar que nenhum arquivo do `motionlog-app` foi alterado.
