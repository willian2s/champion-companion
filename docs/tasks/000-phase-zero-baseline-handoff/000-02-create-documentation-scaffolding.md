# Task 000-02 - Criar Scaffolding De Documentacao

## Objetivo

Criar a estrutura documental esperada da Fase 0 e o guia para fixtures progressivas.

## Contexto

A Fase 0 nao deve depender de todas as amostras BLE reais. Fixtures serao adicionadas progressivamente.

## Documentos Obrigatorios Para Leitura

- `docs/tasks/000-00-overview.md`
- `docs/specs/000-phase-zero-baseline-handoff.md`
- `docs/specs/_inputs/phase-zero-original-requirements.md`

## Dependencias

- `000-00` lida.

## Arquivos Ou Areas De Entrada

- `docs/`

## Arquivos Exatos De Saida

- `docs/baseline/`
- `docs/protocol/`
- `docs/protocol/sample-packets/README.md`
- Atualizacao do checklist em `docs/tasks/000-00-overview.md`

## Escopo

- Criar diretorios ausentes.
- Criar README de fixtures com padrao de nomes.
- Registrar regras para payload bruto, hexadecimal, firmware, origem da captura, dados sensiveis e transformacao futura em testes Kotlin.

## Fora Do Escopo

- Criar fixtures reais.
- Inventariar codigo.
- Documentar protocolo real.

## Regras De Seguranca

- `motionlog-app` e somente leitura.
- Roadmap e ADR nao podem ser alterados.
- Requisitos originais nao podem ser alterados.
- Nao deve haver commit ou push.
- Codigo Android ainda nao deve ser implementado.
- Inferencias nao podem ser apresentadas como fatos.
- Duvidas devem ser persistidas em documentos, nao apenas na resposta da sessao.

## Passos De Execucao

1. Criar `docs/baseline/`.
2. Criar `docs/protocol/sample-packets/`.
3. Criar `sample-packets/README.md`.
4. Incluir padrao `<fluxo>-<cenario>.hex`, `<fluxo>-<cenario>-expected.json`, `<fluxo>-<cenario>-notes.md`.
5. Incluir exemplos para A1, D1, D3 e D4.
6. Atualizar checklist do overview.

## Criterios De Aceite

- Estrutura documental existe.
- README explica fixtures progressivas.
- Nenhuma fixture real e exigida.

## Validacao Final

- Listar diretorios criados.
- Confirmar ausencia de alteracao no `motionlog-app`.
