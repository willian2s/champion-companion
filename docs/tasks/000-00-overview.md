# Task 000-00 - Overview Operacional Da Fase 0

## Objetivo

Carregar o contexto operacional da Fase 0, manter o checklist de execucao e orientar as tasks seguintes em sessoes independentes do Codex.

## Contexto

Champion Companion sera um app Android nativo para o Champion C001. A Fase 0 documenta o baseline funcional do MotionLog, mas nao implementa Android e nao altera `motionlog-app`.

## Documentos Obrigatorios Para Leitura

- `docs/specs/000-phase-zero-baseline-handoff.md`
- `docs/roadmap/champion-companion-roadmap.md`
- `docs/decisions/adr-001-native-rewrite.md`
- `docs/specs/_inputs/phase-zero-original-requirements.md`

## Baseline Utilizada

- repositorio: `../motionlog-app`
- branch esperada: `feat/015-watch-workouts-d3-d4`
- commit observado no planejamento: `0a2a2d64f8eb88cec9b1fcdf84fe0e46ef704edb`

## Dependencias

- Nenhuma. Esta task deve ser lida antes de qualquer outra task da Fase 0.

## Arquivos Ou Areas De Entrada

- `docs/specs/000-phase-zero-baseline-handoff.md`
- `docs/roadmap/champion-companion-roadmap.md`
- `docs/decisions/adr-001-native-rewrite.md`
- `docs/specs/_inputs/phase-zero-original-requirements.md`
- `docs/tasks/`

## Arquivos Exatos De Saida

- `docs/tasks/000-00-overview.md`

## Escopo

- Registrar contexto operacional da Fase 0.
- Registrar baseline usada.
- Registrar regras globais.
- Listar artefatos esperados.
- Listar tasks, dependencias e paralelizacao.
- Manter checklist e status de execucao.

## Fora Do Escopo

- Executar analise do MotionLog.
- Criar artefatos finais de baseline/protocolo.
- Implementar codigo Android.
- Alterar documentos autoritativos.

## Regras Globais

- `motionlog-app` e somente leitura.
- Roadmap, ADR e requisitos originais nao podem ser alterados.
- Nao fazer commit.
- Nao fazer push.
- Nao implementar codigo Android.
- Nao criar projeto Gradle, Kotlin, Compose, Room ou Health Connect.
- Inferencias nao podem ser apresentadas como fatos.
- Duvidas devem ser persistidas em documentos, nao apenas na resposta da sessao.
- Cada task deve funcionar sem contexto de conversa anterior.

## Regras De Seguranca

- `motionlog-app` e somente leitura.
- Roadmap e ADR nao podem ser alterados.
- Requisitos originais nao podem ser alterados.
- Nao deve haver commit ou push.
- Codigo Android ainda nao deve ser implementado.
- Inferencias nao podem ser apresentadas como fatos.
- Duvidas devem ser persistidas em documentos, nao apenas na resposta da sessao.

## Artefatos Esperados

```text
docs/baseline/current-baseline.md
docs/baseline/feature-inventory.md
docs/baseline/feature-parity-checklist.md
docs/baseline/source-map.md
docs/baseline/database-reference.md
docs/baseline/health-connect-mapping.md
docs/baseline/gaps-and-open-questions.md
docs/protocol/champion-c001.md
docs/protocol/ble-transport.md
docs/protocol/ble-command-mapping.md
docs/protocol/handshake-a1.md
docs/protocol/heart-rate-d0-f002.md
docs/protocol/history-d1.md
docs/protocol/workouts-d3-d4.md
docs/protocol/diagnostics-d8-a8.md
docs/protocol/sample-packets/README.md
```

## Lista Ordenada De Tasks

1. `000-00-overview.md`
2. `000-01-verify-baseline.md`
3. `000-02-create-documentation-scaffolding.md`
4. `000-03-inventory-application.md`
5. `000-04-document-ble-transport.md`
6. `000-05-document-handshake-and-manual-heart-rate.md`
7. `000-06-document-d1-history.md`
8. `000-07-document-d3-d4-workouts.md`
9. `000-08-document-activities-and-treadmill-calibration.md`
10. `000-09-document-phone-workouts-and-background.md`
11. `000-10-document-database-reference.md`
12. `000-11-document-health-connect-mapping.md`
13. `000-12-create-source-map.md`
14. `000-13-create-parity-checklist.md`
15. `000-14-consolidate-gaps-and-open-questions.md`
16. `000-15-validate-phase-zero.md`

## Dependencias Operacionais

- `000-00` deve ser lida antes de qualquer outra task.
- `000-01` deve confirmar baseline antes de analise de codigo.
- `000-02` deve criar estrutura documental antes de tasks que gravam docs finais.
- `000-04` deve preceder `000-05`, `000-06` e `000-07`.
- `000-03` e `000-10` devem preceder ou orientar `000-08`, `000-09`, `000-11`, `000-12` e `000-13`.
- `000-14` deve ocorrer apos os documentos principais.
- `000-15` deve ser a ultima task.

## Tasks Paralelizaveis

- `000-01` e `000-02` podem rodar em paralelo depois de `000-00`.
- `000-03`, `000-04` e `000-10` podem rodar em paralelo depois de `000-01` e `000-02`.
- `000-05`, `000-06` e `000-07` podem rodar em paralelo depois de `000-04`.
- `000-08`, `000-09` e `000-11` podem rodar em paralelo se houver cuidado com edicoes no mesmo arquivo.
- `000-12` e `000-13` podem rodar em paralelo apos docs principais.

## Checklist

- [x] `000-00` Overview operacional criado e atualizado
- [x] `000-01` Baseline verificada
- [x] `000-02` Scaffolding documental criado
- [x] `000-03` Aplicacao inventariada
- [x] `000-04` Transporte BLE documentado
- [x] `000-05` Handshake e BPM manual documentados
- [x] `000-06` Historico D1 documentado
- [x] `000-07` Treinos D3/D4 documentados
- [ ] `000-08` Atividades e esteira documentadas
- [ ] `000-09` Exercicios pelo celular e background documentados
- [ ] `000-10` Banco atual documentado
- [ ] `000-11` Health Connect documentado
- [ ] `000-12` Source map criado
- [ ] `000-13` Checklist de paridade criado
- [ ] `000-14` Lacunas consolidadas
- [ ] `000-15` Fase 0 validada

## Status Inicial

| Task | Status |
| --- | --- |
| `000-00` | Atualizada |
| `000-01` | Concluida |
| `000-02` | Concluida |
| `000-03` | Concluida |
| `000-04` | Concluida |
| `000-05` | Concluida |
| `000-06` | Concluida |
| `000-07` | Concluida |
| `000-08` | Pendente |
| `000-09` | Pendente |
| `000-10` | Pendente |
| `000-11` | Pendente |
| `000-12` | Pendente |
| `000-13` | Pendente |
| `000-14` | Pendente |
| `000-15` | Pendente |

## Criterios Para Concluir A Fase 0

- Todos os artefatos esperados existem.
- Todas as tasks estao marcadas como concluidas.
- Requisitos originais possuem task responsavel.
- Lacunas estao centralizadas em `docs/baseline/gaps-and-open-questions.md`.
- `motionlog-app` permanece sem alteracoes.
- Roadmap, ADR e requisitos originais permanecem sem alteracoes.
- Nao houve commit nem push.
- Nao houve implementacao Android.

## Instrucao De Atualizacao

Apos cada execucao, atualizar este checklist e o status da task correspondente. Persistir duvidas e lacunas nos documentos da Fase 0.

## Passos De Execucao

1. Ler esta task antes de iniciar qualquer outra task da Fase 0.
2. Confirmar documentos autoritativos e spec.
3. Seguir dependencias e ordem recomendada.
4. Atualizar checklist e status apos cada task concluida.
5. Persistir duvidas nos artefatos da Fase 0.

## Criterios De Aceite

- Overview contem contexto suficiente para nova sessao.
- Checklist lista todas as tasks.
- Dependencias e paralelizacao estao explicitas.
- Regras globais e seguranca estao explicitas.

## Validacao Final

- Confirmar que `000-00` foi lida antes da task executada.
- Confirmar que checklist foi atualizado apos execucao.
- Confirmar que `motionlog-app` nao foi alterado.
