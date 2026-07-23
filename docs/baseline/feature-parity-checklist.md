# Checklist De Paridade Funcional

## Objetivo

Validar, em fases futuras, paridade funcional segura entre Champion Companion e baseline MotionLog. Este documento nao declara paridade atingida nem exige criacao ou execucao de testes.

## Baseline E Uso

- Baseline: MotionLog `feat/015-watch-workouts-d3-d4`, commit `0a2a2d64f8eb88cec9b1fcdf84fe0e46ef704edb`.
- Status de todos itens: pendente de implementacao e validacao futura.
- Evidencia `Confirmado pelo codigo` descreve baseline. Nao confirma comportamento do Champion Companion nem validacao no dispositivo.
- Itens `Ainda nao confirmado` exigem decisao ou validacao fisica antes de aceitacao; nao devem ser inferidos como requisito implementado.
- Validacao deve registrar versao Champion Companion, firmware/dispositivo, dados de entrada, resultado esperado, resultado obtido e evidencia.

## Criterio Global De Aceite

- [ ] Cada item aplicavel abaixo tem implementacao, validacao reproduzivel e evidencia registrada.
- [ ] Fluxos com Champion C001 sao validados em dispositivo fisico quando protocolo ou comportamento BLE participa.
- [ ] Dados persistidos, enviados ao Health Connect e exibidos ao usuario conferem com regra declarada; falhas permanecem rastreaveis.
- [ ] Nenhuma validacao introduz latitude, longitude, coordenadas brutas, polyline, rota, mapa, GPX, TCX, FIT ou `ExerciseRoute`.

## Dispositivo E BLE

Fontes: `feature-inventory.md`, `ble-transport.md`, `champion-c001.md`, `ble-command-mapping.md`, `handshake-a1.md`, `diagnostics-d8-a8.md`.

- [ ] Permissoes BLE, scan e filtro identificam Champion C001 conforme contrato documentado.
- [ ] Conexao descobre services/characteristics documentadas e assina notifications necessarias antes de comandos.
- [ ] Operacoes GATT passam por fila serial; escrita, notification, timeout, cleanup e desconexao nao deixam operacao concorrente.
- [ ] Reconexao, timeout e retry seguem decisoes futuras documentadas, sem BLE permanente ou retry infinito.
- [ ] Handshake A1 envia payload e reconhece resposta confirmada pelo codigo antes de leituras dependentes.
- [ ] FEA1 atualiza passos somente para payload valido; dados invalidos nao corrompem resumo diario.
- [ ] D8 e A8 ficam apenas diagnosticos ate finalidade, payload/resposta e fluxo serem confirmados. Nenhuma paridade funcional e assumida para eles.

## Dados Diarios

Fontes: `feature-inventory.md`, `database-reference.md`, `health-connect-mapping.md`, `ble-command-mapping.md`.

- [ ] Passos FEA1 validos consolidam resumo por dia local com deduplicacao previsivel.
- [ ] Home e detalhe diario exibem passos, meta, distancia estimada e calorias estimadas quando dados existirem.
- [ ] Estimativas usam dados de perfil e regras documentadas; ausencia de dados produz estado tratavel, sem valor inventado.
- [ ] Estados loading, erro e vazio permitem entender ausencia, falha e dados disponiveis.
- [ ] Sync de `Steps` so ocorre por acao de usuario; intervalo, unidade e metadados sao verificaveis no Health Connect.

## Frequencia Cardiaca

Fontes: `heart-rate-d0-f002.md`, `history-d1.md`, `health-connect-mapping.md`, `database-reference.md`.

Estado Champion Companion apos Fase 09:

- medicao manual D0/F002 entregue e persistencia final ativa;
- historico D1 local-first entregue com importacao explicita, deduplicacao, historico consolidado, agregacoes e grafico local;
- escrita real no Health Connect para BPM ainda nao faz parte do estado atual e segue para fase posterior.

- [ ] Medicao manual envia D0 start/stop, processa D0/F002 e mostra estado de medicao, erro e resultado final.
- [ ] BPM em tempo real e resultado final aplicam faixas e fallback documentados; sinais invalidos nao viram medicao valida.
- [ ] Historico D1 coleta pacotes multipart, respeita conclusao, timeout e tratamento de pacote invalido documentados.
- [ ] Registros D1 sao deduplicados por dispositivo, instante e origem; reimportacao nao cria duplicatas.
- [ ] Todos pontos D1 validos ainda nao sincronizados seguem para Health Connect sem selecao ponto a ponto.
- [ ] Falha parcial de persistencia ou Health Connect mantem somente itens afetados pendentes ou falhos para nova tentativa.

## Treinos Importados Do Relogio

Fontes: `workouts-d3-d4.md`, `feature-inventory.md`, `database-reference.md`, `health-connect-mapping.md`.

- [ ] D3 obtém indice de CRCs; D4 baixa blocos sequencialmente, com timeout, cleanup e sem retry interno nao documentado.
- [ ] Parser D4 produz data, horario, duracao, passos, distancia total/por intervalo, calorias, BPM total/por intervalo e min/media/max somente de valores validos.
- [ ] Dados incompletos preservam status de validacao; `partial` e `invalid` bloqueiam envio ate regra definida.
- [ ] Importacoes guardam payload bruto e deduplicam por `device_id`, CRC e origem.
- [ ] Treino importado nasce `pending_review`, nunca sincroniza automaticamente e preserva resultado de revisao em reimportacao.
- [ ] Consulta e sincronizacao partem de acao do usuario; abrir app nao inicia D3/D4 automaticamente.

## Revisao De Atividades

Fontes: `feature-inventory.md`, `database-reference.md`, `health-connect-mapping.md`.

- [ ] Lista mostra atividades pendentes, revisadas, invalidas e estados de sync aplicaveis.
- [ ] Sugestao automatica de tipo permanece revisavel; usuario confirma ou altera tipo antes de enviar.
- [ ] Revisao permite tipos confirmados `walking`, `running`, `treadmill` e `free`; regras adicionais so entram apos decisao documentada.
- [ ] Edicao de distancia respeita limite confirmado: esteira revisada. Edicao generica para outros tipos nao e assumida.
- [ ] Descarte remove atividade e dependentes de forma consistente, sem aparentar sync concluido.
- [ ] Sync manual exige revisao, validacao valida, sessao vinculada e calibracao de esteira quando aplicavel.
- [ ] Falha de sync preserva estado e erro para tentativa manual posterior; sucesso evita novo envio indevido.

## Exercicios Iniciados Pelo Celular

Fontes: `feature-inventory.md`, `health-connect-mapping.md`, `database-reference.md`.

- [ ] Inicio oferece caminhada, corrida, treino livre e esteira, com pre-condicoes de dispositivo/GPS confirmadas para cada tipo.
- [ ] Sessao ativa mostra duracao, passos quando disponiveis, conexao e BPM; finalizacao persiste metricas e amostras validas.
- [ ] Caminhada e corrida usam GPS somente em memoria para metricas. Coordenadas nunca sao persistidas ou enviadas.
- [ ] Esteira exige calibracao antes da persistencia/sync; descarte de rascunho nao cria atividade persistida.
- [ ] Pausa, retomada e recuperacao apos encerramento so sao aceitas se decisao futura documentar contrato; baseline analisada nao as confirmou.
- [ ] Continuidade/background de exercicio segue decisao futura compatível com roadmap; nao herda monitoramento BLE continuo do MotionLog.

## Calibracao De Esteira

Fontes: `feature-inventory.md`, `health-connect-mapping.md`.

- [ ] Usuario informa tempo e distancia validos do painel; app recalcula fim, distancia, ritmo, velocidade e calorias segundo regra escolhida.
- [ ] Distancia calibrada, em metros, torna-se unica distancia usada no envio Health Connect de esteira.
- [ ] Treino importado revisado como esteira fica bloqueado de sync ate calibracao concluida.
- [ ] Fator de calibracao separado nao e exigido nem declarado atingido: baseline nao o confirmou. Decisao futura deve definir calculo/persistencia, se necessario.

## Health Connect

Fontes: `health-connect-mapping.md`, `feature-inventory.md`, `database-reference.md`.

- [ ] Permissoes e disponibilidade bloqueiam escrita de modo explicavel; permissoes opcionais nao invalidam registros independentes.
- [ ] `Steps`, `HeartRate`, `ExerciseSession`, `Distance`, `TotalCaloriesBurned`, `Weight` e `Height` usam campos, unidades e tempos documentados quando aplicaveis.
- [ ] IDs estaveis proprios, versoes e estrategia de update/retry sao decididos e validados; prefixos `motionlog:` nao sao contrato do novo app.
- [ ] Fila local registra pendente, sucesso, falha e dados invalidos por entidade/record; escrita parcial nao mascara records restantes.
- [ ] D3/D4 somente enviam apos revisao manual; `free` nao envia distancia; esteira usa distancia calibrada.
- [ ] Nenhum fluxo grava ou envia `ExerciseRoute`, coordenadas, rotas ou polylines.

## Dados, Backup, Restauracao E Limpeza

Fontes: `database-reference.md`, `feature-inventory.md`, `000-phase-zero-baseline-handoff.md`.

- [ ] Banco proprio inicia vazio; nao le `motionlog.db`, nao migra schema, nao importa nem transfere dados MotionLog.
- [ ] Modelo novo exclui dados proibidos: latitude, longitude, rotas, polylines e coordenadas brutas.
- [ ] Dados locais operacionais suportam deduplicacao, revisao e fila Health Connect sem depender de banco MotionLog.
- [ ] Backup proprio, se implementado, cobre somente dados Champion Companion e possui restauracao verificavel.
- [ ] Backup/restauracao nao exige compatibilidade, importacao ou exportacao do MotionLog; baseline atual de Firebase nao foi confirmada no codigo.
- [ ] Limpeza de dados define alvo, efeito em fila Health Connect e confirmacao de usuario antes de execucao; nao remove dados externos do Health Connect sem regra futura explicitada.

## Evidencia E Pendencias

- [ ] Fixtures progressivas sao adicionadas por fluxo e cenario conforme implementacao e validacao fisica; ausencia atual nao prova nem bloqueia paridade.
- [ ] Cada divergencia MotionLog/Champion Companion possui decisao ou lacuna registrada em `docs/baseline/gaps-and-open-questions.md` antes de ser aceita.
- [ ] Validacao final confirma que este checklist continua sem marcar paridade por inferencia ou apenas por leitura de codigo.

## Limites

Este checklist nao cria requisitos alem dos documentos autoritativos. Decisoes pendentes, em especial D8/A8, fator de calibracao, pausa/retomada, recuperacao de sessao, retry e backup, exigem consolidacao futura antes de virarem criterio fechado.
