# Task 000-07 - Documentar Treinos D3/D4

## Objetivo

Documentar consulta D3, blocos, CRCs, solicitacao D4, remontagem e parser de treinos do relogio.

## Contexto

Treinos D3/D4 devem virar atividades pendentes para revisao antes de qualquer envio.

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

- Codigo D3, D4, parser de treinos e armazenamento de payload bruto em `../motionlog-app`

## Arquivos Exatos De Saida

- `docs/protocol/workouts-d3-d4.md`
- Atualizacao do checklist em `docs/tasks/000-00-overview.md`

## Escopo

- Requisitos originais 24, 25 e 40-49.
- Contratos de parsers D3/D4.
- Campos: data, horario, duracao, passos, distancia, distancia por intervalo, calorias, BPM, BPM por intervalo, BPM medio, minimo, maximo e tipo.
- Dados incompletos, duplicatas, payload bruto, D8/A8 quando relacionado.

## Fora Do Escopo

- Revisao visual de atividades.
- Health Connect detalhado.
- Implementacao Kotlin.
- Capturar todos os pacotes reais.

## Regras De Seguranca

- `motionlog-app` e somente leitura.
- Roadmap e ADR nao podem ser alterados.
- Requisitos originais nao podem ser alterados.
- Nao deve haver commit ou push.
- Codigo Android ainda nao deve ser implementado.
- Inferencias nao podem ser apresentadas como fatos.
- Duvidas devem ser persistidas em documentos, nao apenas na resposta da sessao.

## Passos De Execucao

1. Localizar fluxos D3/D4.
2. Registrar fluxo D3 de indice/blocos/CRCs.
3. Registrar fluxo D4 de requisicao/remontagem.
4. Documentar parser como contrato.
5. Registrar regra de calorias `calories = caloriesRaw / 1000` somente se confirmada nos documentos/codigo.
6. Registrar sugestao de tipo conforme roadmap quando aplicavel.
7. Registrar payload bruto e limpeza quando evidenciado.
8. Registrar lacunas para `000-14`.
9. Atualizar checklist do overview.

## Criterios De Aceite

- D3 e D4 documentados separadamente.
- Parser documentado por contrato.
- Atividade pendente e sem envio automatico aparecem como regra.

## Validacao Final

- Confirmar que dados nao confirmados foram marcados como lacuna.
- Confirmar que `motionlog-app` nao foi alterado.
