# Task 000-15 - Validar Fase 0

## Objetivo

Validar que a Fase 0 foi concluida, todos os requisitos foram cobertos e nenhuma restricao foi violada.

## Contexto

Esta e a ultima task da Fase 0. Ela verifica documentos, rastreabilidade, status Git e ausencia de implementacao Android.

## Documentos Obrigatorios Para Leitura

- `docs/tasks/000-00-overview.md`
- `docs/specs/000-phase-zero-baseline-handoff.md`
- `docs/specs/_inputs/phase-zero-original-requirements.md`
- todos os artefatos em `docs/baseline/`
- todos os artefatos em `docs/protocol/`

## Dependencias

- Todas as tasks `000-00` a `000-14`.

## Arquivos Ou Areas De Entrada

- `docs/`
- `../motionlog-app/.git`
- `champion-companion/.git`

## Arquivos Exatos De Saida

- Atualizacao do checklist em `docs/tasks/000-00-overview.md`
- Ajustes documentais necessarios nos artefatos da Fase 0, sem criar codigo Android

## Escopo

- Conferir existencia dos artefatos.
- Conferir matriz de rastreabilidade.
- Conferir lacunas.
- Conferir status Git.
- Confirmar restricoes.

## Fora Do Escopo

- Implementar qualquer fase futura.
- Resolver lacunas por codigo.
- Fazer commit ou push.

## Regras De Seguranca

- `motionlog-app` e somente leitura.
- Roadmap e ADR nao podem ser alterados.
- Requisitos originais nao podem ser alterados.
- Nao deve haver commit ou push.
- Codigo Android ainda nao deve ser implementado.
- Inferencias nao podem ser apresentadas como fatos.
- Duvidas devem ser persistidas em documentos, nao apenas na resposta da sessao.

## Passos De Execucao

1. Listar arquivos criados/modificados no `champion-companion`.
2. Confirmar branch e commit da baseline.
3. Conferir todos os artefatos esperados.
4. Conferir cobertura dos requisitos originais.
5. Conferir parsers documentados e parciais.
6. Conferir lacunas com e sem dispositivo fisico.
7. Executar `git -C ../motionlog-app status --short`.
8. Executar `git status --short`.
9. Confirmar roadmap, ADR e requisitos originais sem alteracao.
10. Confirmar ausencia de implementacao Android.
11. Atualizar checklist do overview.

## Criterios De Aceite

- Todos os artefatos esperados existem.
- Todos os requisitos originais possuem task responsavel.
- Roadmap, ADR e requisitos originais permanecem sem alteracao.
- `motionlog-app` permanece sem alteracao.
- Fase 0 nao criou codigo Android.
- Sem commit e sem push.

## Validacao Final

Responder com:

1. spec criada;
2. tasks criadas;
3. ordem recomendada;
4. dependencias;
5. tasks paralelizaveis;
6. ambiguidades encontradas;
7. confirmacao de cobertura dos requisitos;
8. confirmacao de que Fase 0 nao foi executada alem da documentacao;
9. confirmacao de que nao houve implementacao Android;
10. confirmacao de que MotionLog nao foi alterado;
11. confirmacao de que roadmap, ADR e requisitos originais nao foram alterados;
12. status Git do `champion-companion`;
13. confirmacao sem commit;
14. confirmacao sem push.
