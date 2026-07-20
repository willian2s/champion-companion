# Task 000-05 - Documentar Handshake E Frequencia Cardiaca Manual

## Objetivo

Documentar handshake A1, comando D0, notificacoes F002, medicao manual e stream de BPM.

## Contexto

Fases futuras precisam reproduzir conexao minima, handshake e BPM manual antes de D1/D3/D4.

## Documentos Obrigatorios Para Leitura

- `docs/tasks/000-00-overview.md`
- `docs/specs/000-phase-zero-baseline-handoff.md`
- `docs/baseline/current-baseline.md`
- `docs/protocol/ble-transport.md`
- `docs/protocol/ble-command-mapping.md`

## Dependencias

- `000-01`
- `000-02`
- `000-04`

## Arquivos Ou Areas De Entrada

- Codigo de handshake, D0, F002 e medicao manual em `../motionlog-app`

## Arquivos Exatos De Saida

- `docs/protocol/handshake-a1.md`
- `docs/protocol/heart-rate-d0-f002.md`
- Atualizacao do checklist em `docs/tasks/000-00-overview.md`

## Escopo

- Finalidade, fluxo, characteristic, payloads, offsets, endianess, escalas, conclusao, timeout, retry, cancelamento, erro, objeto produzido e arquivos de origem.
- Requisitos originais 21, 22, 28 e 34-36.

## Fora Do Escopo

- D1 historico.
- D3/D4.
- Implementacao Kotlin.
- Captura fisica obrigatoria.

## Regras De Seguranca

- `motionlog-app` e somente leitura.
- Roadmap e ADR nao podem ser alterados.
- Requisitos originais nao podem ser alterados.
- Nao deve haver commit ou push.
- Codigo Android ainda nao deve ser implementado.
- Inferencias nao podem ser apresentadas como fatos.
- Duvidas devem ser persistidas em documentos, nao apenas na resposta da sessao.

## Passos De Execucao

1. Localizar arquivos de A1, D0 e F002.
2. Registrar contrato do handshake.
3. Registrar contrato da medicao manual.
4. Separar valor em tempo real de valor final.
5. Registrar cancelamento, desconexao e erro.
6. Classificar cada detalhe por nivel de confirmacao.
7. Registrar lacunas para `000-14`.
8. Atualizar checklist do overview.

## Criterios De Aceite

- A1 documentado como fluxo completo.
- D0/F002 documentados como contrato de parser/stream.
- Valor final identificado ou lacuna registrada.

## Validacao Final

- Confirmar que inferencias estao marcadas.
- Confirmar que `motionlog-app` nao foi alterado.
