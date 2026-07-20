# Task 000-04 - Documentar Transporte BLE

## Objetivo

Documentar BLE geral do MotionLog e mapa de comandos do Champion C001.

## Contexto

Champion Companion deve usar APIs nativas Android BLE com fila GATT serial e comportamento previsivel.

## Documentos Obrigatorios Para Leitura

- `docs/tasks/000-00-overview.md`
- `docs/specs/000-phase-zero-baseline-handoff.md`
- `docs/baseline/current-baseline.md`
- `docs/roadmap/champion-companion-roadmap.md`

## Dependencias

- `000-01`
- `000-02`

## Arquivos Ou Areas De Entrada

- Codigo BLE de `../motionlog-app`
- Permissoes, scan, conexao, services, characteristics, notifications, indications, fila, timeouts, retries

## Arquivos Exatos De Saida

- `docs/protocol/champion-c001.md`
- `docs/protocol/ble-transport.md`
- `docs/protocol/ble-command-mapping.md`
- `docs/protocol/diagnostics-d8-a8.md`
- Atualizacao do checklist em `docs/tasks/000-00-overview.md`

## Escopo

- Requisitos BLE 9-20.
- UUIDs, services, characteristics.
- Comandos D8, A8 e FEA1.
- Mapa inicial de D0, D1, D3, D4, A1 e F002 sem detalhar parsers.
- Classificacao: confirmado pelo codigo, confirmado no uso, inferido ou nao confirmado.

## Fora Do Escopo

- Documentar profundamente A1, D0, D1, D3, D4 e F002.
- Implementar BLE nativo.
- Capturar pacotes fisicos.

## Regras De Seguranca

- `motionlog-app` e somente leitura.
- Roadmap e ADR nao podem ser alterados.
- Requisitos originais nao podem ser alterados.
- Nao deve haver commit ou push.
- Codigo Android ainda nao deve ser implementado.
- Inferencias nao podem ser apresentadas como fatos.
- Duvidas devem ser persistidas em documentos, nao apenas na resposta da sessao.

## Passos De Execucao

1. Localizar codigo BLE e permissoes.
2. Registrar fluxo de scan e conexao.
3. Registrar discovery e characteristics.
4. Registrar notifications/indications.
5. Registrar sequenciamento, timeouts e retries.
6. Documentar FEA1, D8 e A8 conforme evidencia.
7. Criar mapa de comandos.
8. Registrar lacunas para `000-14`.
9. Atualizar checklist do overview.

## Criterios De Aceite

- Transporte BLE documentado com caminhos de origem.
- UUIDs e characteristics catalogados sem inventar dados.
- D8/A8 separados como diagnostico ou fallback quando aplicavel.

## Validacao Final

- Confirmar que nenhum acesso direto futuro a GATT por telas foi definido como aceitavel.
- Confirmar que `motionlog-app` nao foi alterado.
