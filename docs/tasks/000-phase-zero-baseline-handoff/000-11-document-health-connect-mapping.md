# Task 000-11 - Documentar Mapeamento Health Connect

## Objetivo

Documentar integracao atual com Health Connect e regras obrigatorias para Champion Companion.

## Contexto

Health Connect sera destino de sincronizacao. O banco local do Champion Companion sera fonte operacional.

## Documentos Obrigatorios Para Leitura

- `docs/tasks/000-00-overview.md`
- `docs/specs/000-phase-zero-baseline-handoff.md`
- `docs/baseline/current-baseline.md`
- `docs/baseline/database-reference.md`
- `docs/roadmap/champion-companion-roadmap.md`

## Dependencias

- `000-01`
- `000-02`
- `000-10` recomendado

## Arquivos Ou Areas De Entrada

- Codigo Health Connect, sincronizacao, permisssoes, mapeadores e tratamento de erros em `../motionlog-app`

## Arquivos Exatos De Saida

- `docs/baseline/health-connect-mapping.md`
- Atualizacao do checklist em `docs/tasks/000-00-overview.md`

## Escopo

- Requisitos originais 77-90.
- Registros, campos, unidades, timestamps, IDs, versoes, permissoes, insert, update, retry, deduplicacao e erro.
- Passos, BPM, exercicios, peso e altura.
- Regras obrigatorias: BPM D1 integral, atividades D3/D4 revisadas, esteira calibrada, sem rota.

## Fora Do Escopo

- Implementar Health Connect SDK.
- Criar fila real de sincronizacao.
- Decidir novos modelos nao aprovados.

## Regras De Seguranca

- `motionlog-app` e somente leitura.
- Roadmap e ADR nao podem ser alterados.
- Requisitos originais nao podem ser alterados.
- Nao deve haver commit ou push.
- Codigo Android ainda nao deve ser implementado.
- Inferencias nao podem ser apresentadas como fatos.
- Duvidas devem ser persistidas em documentos, nao apenas na resposta da sessao.

## Passos De Execucao

1. Localizar integracao Health Connect.
2. Registrar permissoes.
3. Registrar tipos de registros.
4. Registrar campos, unidades e timestamps.
5. Registrar IDs, versoes, insert/update/retry.
6. Registrar prevencao de duplicacao e tratamento de erro.
7. Registrar explicitamente ausencia de `ExerciseRoute`.
8. Registrar lacunas para `000-14`.
9. Atualizar checklist do overview.

## Criterios De Aceite

- Mapeamento cobre todos os tipos exigidos.
- Regras obrigatorias estao explicitas.
- Coordenadas e rotas estao proibidas.

## Validacao Final

- Confirmar que nenhuma rota foi definida para envio.
- Confirmar que `motionlog-app` nao foi alterado.
