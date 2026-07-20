# Spec 000 - Fase 0 Baseline e Handoff

## Contexto

Champion Companion sera um novo aplicativo Android nativo para o relogio Champion C001. O produto deve atingir paridade funcional com o MotionLog mobile atual antes de substituir o uso diario.

Documentos autoritativos:

- `docs/roadmap/champion-companion-roadmap.md`
- `docs/decisions/adr-001-native-rewrite.md`
- `docs/specs/_inputs/phase-zero-original-requirements.md`

Esta spec consolida os requisitos da Fase 0. Ela nao substitui os documentos autoritativos e nao pode remover requisitos do documento original.

## Objetivo

Planejar e executar, em tarefas pequenas, a captura documental do baseline funcional do MotionLog para orientar a reimplementacao futura em Kotlin, Jetpack Compose, Room, BLE nativo, Health Connect SDK, Foreground Services e WorkManager.

A Fase 0 deve produzir conhecimento tecnico e funcional persistido. Ela nao deve implementar o app Android nativo.

## Repositorios Envolvidos

- `motionlog-app`: aplicativo React Native/Expo atual, fonte funcional de referencia.
- `champion-companion`: novo repositorio Android nativo, unico local onde arquivos podem ser criados ou alterados durante a Fase 0.

Baseline funcional:

- branch: `feat/015-watch-workouts-d3-d4`
- commit observado no planejamento: `0a2a2d64f8eb88cec9b1fcdf84fe0e46ef704edb`
- caminho esperado no workspace: `../motionlog-app`

## Pre-condicoes

Antes de analisar codigo do MotionLog, a task `000-01` deve confirmar:

- `motionlog-app` esta na branch `feat/015-watch-workouts-d3-d4`;
- commit atual com `git -C ../motionlog-app rev-parse HEAD`;
- estado dos dois repositorios com `git -C ../motionlog-app status --short` e `git status --short`;
- caso a branch esteja diferente, interromper a Fase 0 e registrar o bloqueio.

Nao trocar branch automaticamente. Nao descartar alteracoes existentes.

## Regra De Somente Leitura Do MotionLog

`motionlog-app` e estritamente somente leitura.

Proibido em `motionlog-app`:

- alterar, formatar, mover ou excluir arquivos;
- gerar arquivos;
- instalar dependencias;
- executar migrations;
- aplicar correcoes;
- adicionar logs temporarios;
- fazer commit, tag ou push.

Todos os artefatos produzidos devem ficar em `champion-companion`.

## Baseline Funcional

O baseline representa o comportamento funcional usado diariamente no MotionLog. A Fase 0 deve documentar o que precisa ser reproduzido, onde esta implementado, quais contratos os parsers devem atender, como o protocolo BLE funciona, como os dados sao persistidos, como sao enviados ao Health Connect e quais pontos exigem validacao fisica posterior.

## Escopo Funcional

Documentar:

- estrutura de diretorios, arquitetura, telas, navegacao, hooks, services, repositories, stores e modelos de dominio;
- estados vazios, loading e erro;
- acoes manuais e automaticas;
- permissoes;
- passos, resumo diario, distancia estimada e calorias estimadas;
- BPM manual, stream de BPM, valor final e historico automatico D1;
- treinos D3/D4, revisao, descarte, edicao e sincronizacao manual;
- caminhada, corrida, treino livre e esteira iniciados pelo celular;
- comportamento em segundo plano;
- backup e restauracao atuais apenas como referencia funcional.

## Escopo BLE

Documentar pelo codigo atual:

- permissoes BLE;
- scan e filtros do Champion C001;
- conexao, descoberta de services e characteristics;
- notifications e indications;
- fila ou sequenciamento de operacoes;
- timeouts, retries, desconexao e reconexao;
- UUIDs, services e characteristics usados;
- comandos D0, D1, D3, D4, D8, A1, A8, F002 e FEA1.

## Comandos E Parsers

Para cada comando ou fluxo, documentar:

- finalidade;
- characteristic;
- payload enviado;
- payload recebido;
- resposta esperada;
- condicao de conclusao;
- timeout e retry;
- offsets;
- endianess;
- escalas;
- checksum ou CRC;
- valores invalidos;
- objeto produzido;
- arquivos responsaveis;
- lacunas.

Classificar cada informacao como:

- `Confirmado pelo codigo`
- `Confirmado no uso funcional atual`
- `Inferido a partir do codigo`
- `Ainda nao confirmado`

Inferencias nao podem ser apresentadas como fatos confirmados.

## Persistencia

Documentar o banco SQLite atual apenas como referencia conceitual:

- tabelas, colunas, tipos, relacionamentos e indices;
- estados;
- regras de deduplicacao;
- transacoes importantes;
- dados temporarios;
- dados necessarios no novo modelo;
- dados desnecessarios no novo modelo.

Champion Companion tera banco proprio, iniciara vazio e nao tera migracao, compatibilidade de schema, importador do banco antigo ou transferencia de dados do MotionLog.

Dados proibidos no novo modelo:

- latitude;
- longitude;
- rotas;
- polylines;
- coordenadas brutas.

## Health Connect

Documentar:

- permissoes;
- tipos de registros enviados;
- campos, unidades e timestamps;
- IDs estaveis;
- `clientRecordId` e `clientRecordVersion`, quando aplicavel;
- regras de insert, update, retry e prevencao de duplicacao;
- tratamento de erro;
- sincronizacao de passos, BPM, exercicios, peso e altura;
- ausencia de rotas.

Regras obrigatorias:

- todos os registros validos de BPM obtidos por D1 devem ser enviados ao Health Connect;
- nao havera selecao manual registro por registro;
- falhas parciais permanecem pendentes;
- atividades D3/D4 exigem revisao antes do envio;
- atividades de esteira usam distancia calibrada;
- nao usar `ExerciseRoute`;
- nao armazenar ou enviar coordenadas.

## Atividades

Documentar:

- estados das atividades;
- atividades pendentes;
- sugestao automatica de tipo;
- edicao de tipo e distancia;
- revisao;
- descarte;
- sincronizacao manual;
- deduplicacao de treinos.

Atividades importadas do relogio devem nascer pendentes e nao devem ser enviadas automaticamente.

## Calibracao De Esteira

Documentar:

- criacao da atividade de esteira;
- distancia estimada;
- distancia informada pelo usuario;
- fator de calibracao;
- recalculo de distancia, ritmo, velocidade e metricas derivadas;
- valor enviado ao Health Connect.

## Exercicios Pelo Celular

Documentar:

- caminhada;
- corrida;
- treino livre;
- esteira;
- sessao ativa;
- pausa e retomada;
- recuperacao apos encerramento;
- GPS;
- BPM durante exercicio;
- persistencia da sessao;
- finalizacao.

Coordenadas usadas em caminhada/corrida devem existir apenas em memoria durante a sessao. Nao documentar rota como requisito do novo app.

## Background

Documentar servicos em segundo plano do MotionLog como referencia funcional, incluindo quando ha sessao ativa, GPS, BPM durante exercicio, importacoes longas e recuperacao.

Para Champion Companion, o roadmap define uso futuro de Foreground Services e WorkManager, sem BLE permanente, sincronizacao agressiva, BPM continuo, D1 ao abrir ou retry infinito.

## Fixtures Progressivas

Nao exigir todas as amostras fisicas na Fase 0.

Criar estrutura futura:

```text
docs/protocol/sample-packets/
```

Padrao de nomes:

```text
<fluxo>-<cenario>.hex
<fluxo>-<cenario>-expected.json
<fluxo>-<cenario>-notes.md
```

As fixtures devem ser adicionadas progressivamente durante fases futuras, conforme comandos/parsers forem implementados e comparados com o dispositivo fisico e o MotionLog.

## Artefatos Esperados Da Fase 0

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

## Fora Do Escopo

- executar implementacao Android;
- criar projeto Gradle;
- configurar Kotlin, Compose, Room, Hilt ou Health Connect SDK;
- criar testes Kotlin;
- copiar dependencias Expo;
- copiar componentes React Native;
- criar banco Room;
- criar plano de migracao;
- criar transferencia de dados;
- capturar todas as amostras fisicas;
- alterar `motionlog-app`;
- alterar roadmap, ADR ou requisitos originais;
- fazer commit;
- fazer push.

## Criterios De Aceite

- branch e commit funcionais registrados;
- estado dos repositorios registrado;
- principais fluxos documentados;
- comandos BLE catalogados;
- parsers documentados como contratos, nao como traducao mecanica;
- banco atual documentado como referencia conceitual;
- Health Connect mapeado;
- checklist de paridade criado;
- source map criado;
- lacunas centralizadas;
- fixtures progressivas preparadas;
- todas as informacoes classificadas por nivel de confirmacao;
- todos os requisitos originais possuem task responsavel;
- nenhuma decisao de produto ou arquitetura nova foi tomada sem autorizacao.

## Validacao Final

A task `000-15` deve:

1. listar arquivos criados/modificados no `champion-companion`;
2. informar branch e commit da baseline;
3. resumir funcionalidades identificadas;
4. resumir protocolo identificado;
5. listar lacunas que exigem dispositivo fisico;
6. listar lacunas resolviveis por codigo;
7. informar parsers totalmente documentados;
8. informar parsers parciais;
9. confirmar roadmap sem alteracao;
10. confirmar ADR sem alteracao;
11. confirmar requisitos originais sem alteracao;
12. confirmar `motionlog-app` sem alteracao;
13. mostrar `git -C ../motionlog-app status --short`;
14. mostrar `git status --short`;
15. confirmar sem commit e sem push.

## Definition Of Done Da Fase 0

A Fase 0 esta concluida quando todos os artefatos esperados existem, todos os requisitos originais estao rastreados, lacunas estao centralizadas, fixtures progressivas estao preparadas, a baseline esta registrada, e as fases seguintes podem iniciar o app Android nativo sem depender desta conversa.

## Matriz De Rastreabilidade

| Requisito | Task responsavel | Artefato esperado |
| --- | --- | --- |
| Contexto operacional e checklist da Fase 0 | `000-00` | `docs/tasks/000-00-overview.md` |
| Verificar branch e commit | `000-01` | `docs/baseline/current-baseline.md` |
| Verificar status dos repositorios | `000-01` | `docs/baseline/current-baseline.md` |
| Confirmar APK e fluxos baseline | `000-01` | `docs/baseline/current-baseline.md` |
| Criar scaffolding documental | `000-02` | `docs/protocol/sample-packets/README.md` |
| Estrutura de diretorios | `000-03`, `000-12` | `feature-inventory.md`, `source-map.md` |
| Arquitetura atual | `000-03`, `000-12` | `feature-inventory.md`, `source-map.md` |
| Telas e navegacao | `000-03` | `feature-inventory.md` |
| Hooks, services, repositories, stores | `000-03`, `000-12` | `feature-inventory.md`, `source-map.md` |
| Modelos de dominio | `000-03`, `000-10` | `feature-inventory.md`, `database-reference.md` |
| Persistencia local | `000-10` | `database-reference.md` |
| Servicos em segundo plano | `000-09` | `feature-inventory.md` |
| Permissoes BLE, scan e filtros | `000-04` | `ble-transport.md` |
| Conexao, descoberta, notifications, indications | `000-04` | `ble-transport.md` |
| Fila, timeouts, retries, desconexao, reconexao | `000-04` | `ble-transport.md` |
| UUIDs e characteristics | `000-04` | `champion-c001.md`, `ble-command-mapping.md` |
| Handshake A1 | `000-05` | `handshake-a1.md` |
| D0 | `000-05` | `heart-rate-d0-f002.md` |
| D1 | `000-06` | `history-d1.md` |
| D3 | `000-07` | `workouts-d3-d4.md` |
| D4 | `000-07` | `workouts-d3-d4.md` |
| D8 e A8 | `000-04` | `diagnostics-d8-a8.md` |
| F002 | `000-05` | `heart-rate-d0-f002.md` |
| FEA1 | `000-04` | `ble-command-mapping.md` |
| Payload, resposta, conclusao, offsets, endianess, escalas, invalidos | `000-05`, `000-06`, `000-07` | documentos de protocolo |
| Passos e resumo diario | `000-03`, `000-04` | `feature-inventory.md`, `ble-command-mapping.md` |
| Distancia e calorias estimadas | `000-03`, `000-10` | `feature-inventory.md`, `database-reference.md` |
| BPM manual, stream e valor final | `000-05` | `heart-rate-d0-f002.md` |
| Historico D1, validacao e deduplicacao BPM | `000-06` | `history-d1.md` |
| Consulta D3, blocos e CRCs | `000-07` | `workouts-d3-d4.md` |
| Solicitacao D4 e remontagem | `000-07` | `workouts-d3-d4.md` |
| Parser de treinos e campos extraidos | `000-07` | `workouts-d3-d4.md` |
| Dados incompletos, deduplicacao, payload bruto | `000-07` | `workouts-d3-d4.md` |
| Atividades, pendencias, revisao, descarte, sincronizacao | `000-08` | `feature-inventory.md` |
| Sugestao de tipo e edicao | `000-08` | `feature-inventory.md` |
| Esteira, calibracao, fator e recalculos | `000-08` | `feature-inventory.md` |
| Valor calibrado no Health Connect | `000-11` | `health-connect-mapping.md` |
| Caminhada, corrida, treino livre, esteira pelo celular | `000-09` | `feature-inventory.md` |
| Sessao ativa, pausa, retomada, recuperacao | `000-09` | `feature-inventory.md` |
| GPS e BPM durante exercicio | `000-09` | `feature-inventory.md` |
| Health Connect permissoes, tipos, campos, unidades, timestamps | `000-11` | `health-connect-mapping.md` |
| IDs estaveis, insert, update, retry, deduplicacao, erros | `000-11` | `health-connect-mapping.md` |
| Passos, BPM, exercicios, peso, altura no Health Connect | `000-11` | `health-connect-mapping.md` |
| Ausencia de rotas | `000-11`, `000-10`, `000-09` | `health-connect-mapping.md`, `database-reference.md`, `feature-inventory.md` |
| SQLite, tabelas, colunas, relacionamentos, indices | `000-10` | `database-reference.md` |
| Estados, regras de deduplicacao, temporarios | `000-10` | `database-reference.md` |
| Dados que nao devem existir no novo modelo | `000-10` | `database-reference.md` |
| Backup/restauracao atuais como referencia | `000-03`, `000-13` | `feature-inventory.md`, `feature-parity-checklist.md` |
| Source map | `000-12` | `source-map.md` |
| Checklist de paridade | `000-13` | `feature-parity-checklist.md` |
| Lacunas e questoes abertas | `000-14` | `gaps-and-open-questions.md` |
| Validacao final | `000-15` | artefatos revisados e status Git |

## Questões Identificadas Durante O Planejamento

- `000-00` e uma subtask real e deve ser executada primeiro para carregar contexto, checklist e status.
- Backup/restauracao nao tem task isolada. Cobertura planejada: inventario funcional em `000-03` e checklist de paridade em `000-13`.
- Passos/resumo diario nao tem task propria. Cobertura planejada: inventario funcional em `000-03` e comando FEA1 em `000-04`.
- `000-08` e `000-09` podem atualizar o mesmo `feature-inventory.md`. Nao executar em paralelo sem revisao posterior.
- Ausencia de amostras fisicas completas nao bloqueia Fase 0. Deve virar lacuna documentada e fixture progressiva.
