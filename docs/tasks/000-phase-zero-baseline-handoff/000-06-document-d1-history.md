# Task 000-06 - Documentar Historico D1

## Objetivo

Documentar historico automatico D1 de frequencia cardiaca, validacao, deduplicacao e regra de envio integral ao Health Connect.

## Contexto

Champion Companion deve enviar todos os BPM validos obtidos por D1, sem selecao manual registro por registro.

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

- Codigo D1, validacao e deduplicacao de BPM em `../motionlog-app`

## Arquivos Exatos De Saida

- `docs/protocol/history-d1.md`
- Atualizacao do checklist em `docs/tasks/000-00-overview.md`

## Escopo

- Requisitos originais 23 e 37-39.
- Parser D1 como contrato: entrada, validacoes, transformacoes, offsets, endianess, escalas, campos opcionais, invalidos, saida e erros.
- Timestamps, timezone quando evidenciado, periodo, deduplicacao e falhas pendentes.

## Fora Do Escopo

- Implementar envio Health Connect.
- Criar fixtures reais.
- Documentar D3/D4.

## Regras De Seguranca

- `motionlog-app` e somente leitura.
- Roadmap e ADR nao podem ser alterados.
- Requisitos originais nao podem ser alterados.
- Nao deve haver commit ou push.
- Codigo Android ainda nao deve ser implementado.
- Inferencias nao podem ser apresentadas como fatos.
- Duvidas devem ser persistidas em documentos, nao apenas na resposta da sessao.

## Passos De Execucao

1. Localizar comando e parser D1.
2. Registrar fluxo completo de consulta.
3. Documentar contrato do parser.
4. Registrar validacoes de BPM.
5. Registrar deduplicacao.
6. Registrar regra obrigatoria de envio de todos os validos.
7. Registrar lacunas para `000-14`.
8. Atualizar checklist do overview.

## Criterios De Aceite

- D1 documentado sem inventar offsets ou escalas.
- Validacao e deduplicacao registradas.
- Falhas parciais e retry descritos quando evidenciados ou marcados como lacuna.

## Validacao Final

- Confirmar que envio integral de BPM valido esta explicito.
- Confirmar que `motionlog-app` nao foi alterado.
