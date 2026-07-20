# Task 000-08 - Documentar Atividades E Calibracao De Esteira

## Objetivo

Documentar estados, revisao, edicao, descarte, sincronizacao manual e calibracao de esteira.

## Contexto

Atividades importadas do relogio devem ser revisadas antes de envio. Esteira deve usar distancia calibrada.

## Documentos Obrigatorios Para Leitura

- `docs/tasks/000-00-overview.md`
- `docs/specs/000-phase-zero-baseline-handoff.md`
- `docs/baseline/current-baseline.md`
- `docs/baseline/feature-inventory.md`
- `docs/protocol/workouts-d3-d4.md`

## Dependencias

- `000-01`
- `000-02`
- `000-03`
- `000-07`

## Arquivos Ou Areas De Entrada

- Codigo de atividades, revisao, esteira e sincronizacao manual em `../motionlog-app`

## Arquivos Exatos De Saida

- Atualizacao de `docs/baseline/feature-inventory.md`
- Atualizacao do checklist em `docs/tasks/000-00-overview.md`

## Escopo

- Requisitos originais 50-64.
- Estados, pendencias, sugestao de tipo, edicao, revisao, descarte e sincronizacao manual.
- Esteira: criacao, distancia estimada, distancia do usuario, fator, recalculos e valor final.

## Fora Do Escopo

- Mapeamento completo do Health Connect.
- Implementacao de UI Android.
- Criar novo modelo de banco.

## Regras De Seguranca

- `motionlog-app` e somente leitura.
- Roadmap e ADR nao podem ser alterados.
- Requisitos originais nao podem ser alterados.
- Nao deve haver commit ou push.
- Codigo Android ainda nao deve ser implementado.
- Inferencias nao podem ser apresentadas como fatos.
- Duvidas devem ser persistidas em documentos, nao apenas na resposta da sessao.

## Passos De Execucao

1. Localizar codigo de atividades e revisao.
2. Registrar estados e transicoes.
3. Registrar edicoes permitidas.
4. Registrar fluxo de descarte.
5. Registrar fluxo de calibracao de esteira.
6. Registrar recalculos evidenciados.
7. Registrar lacunas para `000-14`.
8. Atualizar checklist do overview.

## Criterios De Aceite

- Fluxo de revisao antes do envio esta documentado.
- Calibracao de esteira esta documentada.
- Distancia calibrada aparece como valor para envio futuro.

## Validacao Final

- Confirmar que nenhuma regra nova de produto foi criada.
- Confirmar que `motionlog-app` nao foi alterado.
