# Task 000-13 - Criar Checklist De Paridade

## Objetivo

Criar checklist para validar futura paridade funcional entre Champion Companion e baseline MotionLog.

## Contexto

Checklist sera usado em fases posteriores para confirmar substituicao segura do app atual.

## Documentos Obrigatorios Para Leitura

- `docs/tasks/000-00-overview.md`
- `docs/specs/000-phase-zero-baseline-handoff.md`
- `docs/baseline/current-baseline.md`
- `docs/baseline/feature-inventory.md`
- `docs/roadmap/champion-companion-roadmap.md`

## Dependencias

- `000-03`
- `000-04`
- `000-05`
- `000-06`
- `000-07`
- `000-08`
- `000-09`
- `000-10`
- `000-11`

## Arquivos Ou Areas De Entrada

- Artefatos de baseline/protocolo criados
- Roadmap
- Requisitos originais

## Arquivos Exatos De Saida

- `docs/baseline/feature-parity-checklist.md`
- Atualizacao do checklist em `docs/tasks/000-00-overview.md`

## Escopo

Checklist deve cobrir:

- dispositivo;
- dados diarios;
- frequencia cardiaca;
- treinos;
- revisao;
- exercicios;
- Health Connect;
- dados, backup proprio, restauracao, limpeza e sem migracao.

## Fora Do Escopo

- Executar testes de paridade.
- Implementar funcionalidades.
- Criar criterios novos fora dos documentos autoritativos.

## Regras De Seguranca

- `motionlog-app` e somente leitura.
- Roadmap e ADR nao podem ser alterados.
- Requisitos originais nao podem ser alterados.
- Nao deve haver commit ou push.
- Codigo Android ainda nao deve ser implementado.
- Inferencias nao podem ser apresentadas como fatos.
- Duvidas devem ser persistidas em documentos, nao apenas na resposta da sessao.

## Passos De Execucao

1. Ler inventario e docs de protocolo.
2. Criar checklist agrupado pelas categorias exigidas.
3. Marcar itens como pendentes para implementacao futura.
4. Incluir ausencia de rota.
5. Incluir backup proprio sem compatibilidade com MotionLog.
6. Atualizar checklist do overview.

## Criterios De Aceite

- Checklist cobre categorias obrigatorias.
- Itens sao verificaveis futuramente.
- Nao ha execucao de paridade nesta task.

## Validacao Final

- Confirmar que checklist nao declara paridade ja atingida.
- Confirmar que `motionlog-app` nao foi alterado.
