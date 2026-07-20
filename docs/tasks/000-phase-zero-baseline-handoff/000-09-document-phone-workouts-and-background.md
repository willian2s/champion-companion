# Task 000-09 - Documentar Exercicios Pelo Celular E Background

## Objetivo

Documentar caminhada, corrida, treino livre, esteira pelo celular, GPS, BPM durante exercicio, recuperacao e segundo plano.

## Contexto

Champion Companion deve permitir exercicios iniciados pelo celular e respeitar a regra de nao persistir rotas.

## Documentos Obrigatorios Para Leitura

- `docs/tasks/000-00-overview.md`
- `docs/specs/000-phase-zero-baseline-handoff.md`
- `docs/baseline/current-baseline.md`
- `docs/baseline/feature-inventory.md`
- `docs/roadmap/champion-companion-roadmap.md`

## Dependencias

- `000-01`
- `000-02`
- `000-03`
- `000-10` recomendado

## Arquivos Ou Areas De Entrada

- Codigo de exercicios, localizacao, servicos, notificacoes e recuperacao em `../motionlog-app`

## Arquivos Exatos De Saida

- Atualizacao de `docs/baseline/feature-inventory.md`
- Atualizacao do checklist em `docs/tasks/000-00-overview.md`

## Escopo

- Requisitos originais 65-76.
- Caminhada, corrida, treino livre, esteira.
- Sessao ativa, pausa, retomada, recuperacao, GPS, BPM, persistencia e finalizacao.
- Background atual como referencia.

## Fora Do Escopo

- Criar Foreground Service Android.
- Criar WorkManager.
- Persistir coordenadas como requisito.

## Regras De Seguranca

- `motionlog-app` e somente leitura.
- Roadmap e ADR nao podem ser alterados.
- Requisitos originais nao podem ser alterados.
- Nao deve haver commit ou push.
- Codigo Android ainda nao deve ser implementado.
- Inferencias nao podem ser apresentadas como fatos.
- Duvidas devem ser persistidas em documentos, nao apenas na resposta da sessao.

## Passos De Execucao

1. Localizar fluxos de exercicios.
2. Registrar comportamento por tipo.
3. Registrar pausa, retomada, finalizacao e descarte.
4. Registrar recuperacao apos encerramento.
5. Registrar uso de GPS e confirmar se coordenadas sao persistidas ou nao.
6. Registrar background/notificacoes atuais.
7. Registrar lacunas para `000-14`.
8. Atualizar checklist do overview.

## Criterios De Aceite

- Exercicios pelo celular documentados.
- Regra de nao armazenar rota esta destacada.
- Background documentado como referencia, nao como implementacao futura.

## Validacao Final

- Confirmar que nao foi criado codigo Android.
- Confirmar que `motionlog-app` nao foi alterado.
