# Lacunas E Questoes Abertas

## Objetivo E Leitura

Centralizar lacunas da Fase 0. Documento nao decide requisitos novos, nao confirma comportamento fisico e nao altera documentos autoritativos.

Baseline consultada: MotionLog `feat/015-watch-workouts-d3-d4`, commit `0a2a2d64f8eb88cec9b1fcdf84fe0e46ef704edb`.

Classificacao:

- `Alta`: pode invalidar parser, dado de saude, integracao ou privacidade.
- `Media`: afeta previsibilidade operacional, recuperacao ou experiencia.
- `Baixa`: diagnostico ou detalhe sem fluxo funcional confirmado.
- `Sim`: requer Champion C001 fisico.
- `Nao`: decisao, validacao local/manual ou revisao de contrato basta inicialmente.

## Resumo De Dependencias

| ID | Assunto | Impacto | Fase de validacao | Dispositivo fisico |
| --- | --- | --- | --- | --- |
| G-01 | GATT, scan e notifications | Alta | Implementacao BLE | Sim |
| G-02 | A1, D0/F002 e D1 | Alta | Implementacao BLE/BPM | Sim |
| G-03 | D3/D4 e fixtures de treino | Alta | Implementacao importacao treinos | Sim |
| G-04 | D8, A8 e comandos sem parser | Baixa | Diagnostico/MVP posterior | Sim, se mantidos |
| G-05 | Timeout, retry e reconexao BLE | Alta | Implementacao BLE/background | Sim |
| G-06 | Ciclo de vida exercicio manual | Alta | Implementacao exercicios | Nao inicialmente |
| G-07 | Calibracao e fator esteira | Media | Implementacao/revisao esteira | Nao inicialmente |
| G-08 | Health Connect: IDs e idempotencia | Alta | Implementacao Health Connect | Sim |
| G-09 | Health Connect: permissao e escrita parcial | Alta | Implementacao Health Connect | Sim |
| G-10 | Health Connect: Steps, perfil e tempo | Media | Implementacao Health Connect | Sim |
| G-11 | Banco: identidade, exclusao e retencao | Alta | Modelagem banco proprio | Nao |
| G-12 | Backup, restauracao e limpeza | Alta | Definicao produto/dados | Nao |
| G-13 | Privacidade de GPS/rota | Alta | Modelagem, validacao e revisao | Nao |
| G-14 | Background e notificacoes | Alta | Implementacao background | Sim |
| G-15 | Paridade de telas e estados | Media | Implementacao UI/validacao | Nao inicialmente |
| G-16 | Arquitetura/nomenclatura Android | Media | Inicio implementacao Android | Nao |

## G-01 — GATT, Scan E Notifications

- Impacto: Alta. UUID ou propriedade GATT incorreta impede conexao, notificacao ou leitura.
- Evidencia: `champion-c001.md` confirma FEA1/F008/F002 pelo codigo; FEA2/FEC8, uso de escrita F002 e propriedades GATT reais permanecem sem confirmacao. `ble-transport.md` e `source-map.md` registram validacao fisica pendente.
- Motivo da duvida: baseline mostra chamadas e constantes, nao descoberta GATT capturada no Champion C001 nem variacao por firmware.
- Fase: implementacao BLE, antes de liberar fluxos de saude ou importacao.
- Dispositivo fisico: Sim.
- Validacao sugerida: registrar scan, services, characteristics, propriedades e subscribe efetivo; comparar UUIDs e testar FEA1/F008/F002. Testar FEA2/FEC8 somente se houver necessidade diagnostica.

## G-02 — Contratos A1, D0/F002 E D1

- Impacto: Alta. Afeta autenticacao, medicao manual e historico de BPM.
- Evidencia: `handshake-a1.md` nao confirma resposta A1 completa ou senha invalida; `heart-rate-d0-f002.md` nao confirma escala F002, resposta D0 final ou emissao fora de sessao; `history-d1.md` nao define bytes 6-13/15-18, data/hora do relogio, checksum, completude multipart ou D1 sem `deviceId`.
- Motivo da duvida: parsers atuais usam somente parte dos bytes e nao ha fixtures fisicas.
- Fase: implementacao BLE/BPM; D1 antes de sync real de BPM.
- Dispositivo fisico: Sim.
- Validacao sugerida: criar fixtures progressivas A1, D0 start/stop, stream F002 e D1 unico/multipart/duplicado/fora de ordem/faltante/timeout. Registrar firmware, payload raw, resposta e resultado do parser. Definir politica nativa somente apos evidencia e documentos autoritativos.

## G-03 — D3/D4, CRCs E Layout De Treinos

- Impacto: Alta. Parser incorreto produz treino, metricas ou deduplicacao incorretos.
- Evidencia: `workouts-d3-d4.md` confirma heuristicas de layout pelo codigo, mas nao offsets/semantica no dispositivo, unidades, relacao CRC D3/D4, `sportCountRaw`, `pauseRaw`, assinatura `data` ou politica para bloco `partial`.
- Motivo da duvida: faltam fixtures vazio, multipacote, CRC repetido, completo, timeout, fora de ordem e duplicado.
- Fase: implementacao importacao/revisao de treinos.
- Dispositivo fisico: Sim.
- Validacao sugerida: capturar cenarios listados; comparar bloco raw, parser MotionLog e parser novo. Testar divergencia CRC e validar tipo apenas como sugestao revisavel.

## G-04 — D8, A8 E Comandos Sem Fluxo Funcional

- Impacto: Baixa. Sem evidencia de fluxo principal, mas envio especulativo pode gerar comportamento desconhecido.
- Evidencia: `diagnostics-d8-a8.md` nao encontra payload D8, parser ou fluxo; A8 existe como constante `A8 00`, sem resposta/uso confirmado. `ble-command-mapping.md` tambem lista A0, DF e E0 sem parser atual.
- Motivo da duvida: constantes e prototipo nao provam finalidade, resposta ou necessidade MVP.
- Fase: diagnostico ou MVP posterior, somente se documentos autoritativos mantiverem fluxo.
- Dispositivo fisico: Sim, se comando for mantido.
- Validacao sugerida: nao incluir no fluxo principal. Capturar resposta com ambiente controlado; registrar comando, payload, efeito e risco antes de qualquer interface diagnostica.

## G-05 — Timeout, Retry, Reconexao E Sequenciamento BLE

- Impacto: Alta. Politica agressiva conflita com roadmap; politica fraca pode deixar operacao sem recuperacao.
- Evidencia: `ble-transport.md`, `handshake-a1.md`, `heart-rate-d0-f002.md` e `history-d1.md` registram valores MotionLog, mas timeout/retry finais nativos estao pendentes. `feature-inventory.md` descreve reconnect e D1 ciclicos do MotionLog; roadmap proibe BLE permanente, D1 ao abrir e retry infinito.
- Motivo da duvida: baseline e referencia, nao contrato de scheduler Champion Companion.
- Fase: implementacao BLE e background.
- Dispositivo fisico: Sim para comportamento GATT; Nao para decisao inicial de politica.
- Validacao sugerida: documentar matriz por fluxo manual com limite, cancelamento, mensagem e nova tentativa do usuario. Testar desconexao durante A1, D0, D1, D3 e D4; verificar fila serial e ausencia de reconnect infinito.

## G-06 — Pausa, Retomada, Recuperacao E Continuidade De Exercicio

- Impacto: Alta. Pode perder sessao/metricas ou prometer controle ausente na baseline.
- Evidencia: `feature-inventory.md` nao encontra pausa, retomada ou recuperacao apos morte do processo; sessao manual e rascunho esteira ficam em memoria. Background atual MotionLog nao se vincula a esta sessao.
- Motivo da duvida: requisitos listam pausa, retomada e recuperacao, enquanto comportamento baseline analisado nao os confirma.
- Fase: implementacao exercicios e definicao de continuidade.
- Dispositivo fisico: Nao inicialmente.
- Validacao sugerida: reconciliar requisito autoritativo com contrato de sessao; definir estados, persistencia, encerramento, descarte e notificacao. Cobrir morte de processo, navegacao, permissao GPS/BLE perdida e finalizacao parcial com validacao manual e observacao no dispositivo.

## G-07 — Calibracao E Fator De Esteira

- Impacto: Media. Distancia enviada e metricas derivadas podem divergir.
- Evidencia: `feature-inventory.md` confirma substituicao pela distancia informada e recalculo; nao encontra fator separado `distancia_usuario / distancia_estimada` em codigo, schema ou tela.
- Motivo da duvida: requisito menciona fator, baseline atual nao o implementa como dado separado.
- Fase: implementacao/revisao de esteira.
- Dispositivo fisico: Nao inicialmente.
- Validacao sugerida: preservar regra confirmada de distancia calibrada enquanto decisao pendente. Antes de criar fator persistido, definir fonte, formula, reutilizacao e efeito em metricas/Health Connect nos documentos autoritativos.

## G-08 — IDs, Versoes E Idempotencia Health Connect

- Impacto: Alta. Pode duplicar dados remotos ou sobrescrever dados fora de escopo.
- Evidencia: `health-connect-mapping.md` registra IDs `motionlog:` como legado, `insertRecords` sem update explicito e `Steps` por delete de intervalo seguido de insert; resultado de reinsert/conflito nao foi validado.
- Motivo da duvida: Champion Companion precisa IDs proprios, `clientRecordVersion` para records mutaveis e atualizacao segura.
- Fase: implementacao Health Connect, antes de liberar sync.
- Dispositivo fisico: Sim.
- Validacao sugerida: definir contrato de ID local/remoto e versao; testar insert repetido, update, conflito, retry apos falha e impacto do intervalo Steps com registros de outros apps.

## G-09 — Permissoes, Escrita Parcial E Retry Health Connect

- Impacto: Alta. Sucesso parcial pode ocultar records pendentes; permissao indisponivel pode gerar perda de fila.
- Evidencia: `health-connect-mapping.md` confirma fila local e falha parcial nao granular em grupo de exercicio. Permissoes, provider, mensagens reais, unidade de retry e recuperacao de records complementares nao foram exercitados.
- Motivo da duvida: chamadas no codigo nao comprovam comportamento da plataforma nem politica segura do novo app.
- Fase: implementacao Health Connect.
- Dispositivo fisico: Sim, Android com Health Connect disponivel.
- Validacao sugerida: testar ausencia/concessao/revogacao de permissao, provider indisponivel e falha entre records de sessao. Verificar fila por entidade/record, tentativa posterior e erro exibido sem marcar lote inteiro como sincronizado.

## G-10 — Steps, Perfil, Peso/Altura E Tempo Health Connect

- Impacto: Media. Intervalo ou timestamp errado altera agregacao e historico externo.
- Evidencia: `health-connect-mapping.md` deixa pendentes timezone/virada de dia, politica de peso/altura, frequencia de envio e timestamps fallback. `database-reference.md` tambem aponta coexistencia de `synced_at` diario e cursor especifico Steps.
- Motivo da duvida: comportamento MotionLog nao prova regra futura em mudanca de horario, perfil atualizado ou medidas historicas.
- Fase: implementacao Health Connect e banco proprio.
- Dispositivo fisico: Sim para escrita/consulta; Nao para definir regra.
- Validacao sugerida: definir semantica temporal e fonte de perfil/historico; testar dia corrente, dia fechado, meia-noite, mudanca de horario, perfil sem `updatedAt` e medidas repetidas.

## G-11 — Banco Proprio: Identidade, Exclusao E Retencao

- Impacto: Alta. Sem regra, dados podem duplicar, permanecer apos exclusao ou reter payload sensivel sem necessidade.
- Evidencia: `database-reference.md` aponta BPM sem `device_id` com unicidade indefinida, exclusao de sessao sem regra clara para itens Health Connect relacionados, retencao de `raw_hex`/IDs brutos/logs/erros e necessidade de amostras completas versus agregados. `source-map.md` confirma banco novo vazio, sem schema final.
- Motivo da duvida: schema MotionLog e referencia; Champion Companion nao tera migracao nem compatibilidade de schema.
- Fase: modelagem banco proprio, antes de implementacao persistente.
- Dispositivo fisico: Nao.
- Validacao sugerida: escrever contratos de chave, cascata/limpeza, retencao, acesso diagnostico e expurgo. Testar reimportacao, exclusao, falha de sync, limpeza e ausencia de `deviceId` com banco isolado.

## G-12 — Backup, Restauracao E Limpeza

- Impacto: Alta. Escopo indefinido pode virar migracao proibida ou limpeza inconsistente.
- Evidencia: `feature-inventory.md` encontra apenas plano Firebase, sem codigo atual confirmado. Spec define banco Champion Companion vazio, sem migracao, schema compativel, importador ou transferencia MotionLog. `feature-parity-checklist.md` registra backup proprio como condicional e limpeza ainda sem contrato.
- Motivo da duvida: backup/restauracao atual nao esta confirmado; produto novo ainda nao define conteudo, destino, seguranca, restauracao ou efeito sobre fila.
- Fase: definicao produto/dados antes de implementar backup ou limpeza.
- Dispositivo fisico: Nao.
- Validacao sugerida: definir dados incluidos/excluidos, autenticacao/criptografia se aplicavel, restauracao, conflitos, limpeza e confirmacao. Confirmar explicitamente ausencia de interoperabilidade MotionLog.

## G-13 — Privacidade De GPS, Rotas E Dados Proibidos

- Impacto: Alta. Violacao direta de requisito autoritativo.
- Evidencia: spec, roadmap, `feature-inventory.md`, `database-reference.md` e `health-connect-mapping.md` proíbem latitude, longitude, rota, polyline e `ExerciseRoute`; MotionLog ainda possui campos/tabelas legados.
- Motivo da duvida: nao ha duvida sobre proibicao; lacuna e prevenir reintroducao em modelo, log, backup, diagnostico e Health Connect.
- Fase: modelagem banco, exercicios, Health Connect, backup e revisao final.
- Dispositivo fisico: Nao.
- Validacao sugerida: validacao de schema, payload, export/backup, logs e rede; usar GPS somente em memoria para metricas e procurar campos proibidos antes de cada release.

## G-14 — Background, Scheduler E Notificacoes

- Impacto: Alta. Pode conflitar com limites de BLE e comprometer continuidade/consumo.
- Evidencia: `feature-inventory.md` descreve Foreground Service MotionLog com reconnect e D1 ciclico; `source-map.md` e `database-reference.md` registram scheduler/entidades finais pendentes. Roadmap define Foreground Services/WorkManager futuros, sem BLE permanente, sync agressivo, BPM continuo, D1 ao abrir ou retry infinito.
- Motivo da duvida: funcionalidades baseline e restricoes Champion Companion exigem reconciliacao, nao copia direta.
- Fase: implementacao background/notificacoes.
- Dispositivo fisico: Sim.
- Validacao sugerida: definir gatilhos iniciados pelo usuario, duracao, cancelamento, notificacao, WorkManager e limites de retry. Testar app fechado, perda BLE, restricao de bateria e recuperacao sem iniciar leitura proibida automaticamente.

## G-15 — Paridade De Telas, Graficos E Estados Operacionais

- Impacto: Media. Fluxos podem existir sem comportamento compreensivel em loading, vazio, erro ou diagnostico.
- Evidencia: `feature-inventory.md` cataloga telas/estados; `source-map.md` deixa pendentes politica visual, graficos, calendario, estados vazios, notificacoes e diagnosticos.
- Motivo da duvida: Fase 0 mapeia comportamento atual, nao define navegacao, composicao visual ou cobertura de interface Android.
- Fase: implementacao UI e validacao de paridade.
- Dispositivo fisico: Nao inicialmente.
- Validacao sugerida: criar matriz tela/acao/estado esperado; testar loading, erro, vazio, pendencia de revisao, sync falho e dados parciais. Avaliar diagnosticos somente apos G-04.

## G-16 — Arquitetura E Nomes Android

- Impacto: Media. Contratos inconsistentes dificultam evolucao, mas Fase 0 deliberadamente nao fixa classes/pacotes.
- Evidencia: `source-map.md` apresenta areas conceituais e declara arquitetura, contratos e nomes finais pendentes. Spec proibe criar projeto Android nesta fase.
- Motivo da duvida: destino conceitual nao e desenho Kotlin definitivo.
- Fase: inicio da implementacao Android.
- Dispositivo fisico: Nao.
- Validacao sugerida: definir contratos entre BLE, banco, Health Connect, background e UI antes de cada modulo; manter separacao de transporte, protocolo, dominio e interface exigida pelo roadmap.

## Ordem Recomendada

1. G-13 e G-11: limites de dados, privacidade e persistencia.
2. G-16, G-05, G-14: contratos Android, BLE e background antes de serviços.
3. G-01 a G-03: fixtures e validacao fisica dos fluxos BLE principais.
4. G-06 e G-07: contratos de exercicio e esteira.
5. G-08 a G-10: Health Connect apos dados locais e fluxos estaveis.
6. G-12 e G-15: backup/limpeza e validacao de interface conforme escopo aprovado.

## Limites

Nenhuma lacuna acima foi resolvida por esta task. `Confirmado pelo codigo` permanece evidencia da baseline; nao equivale a validacao no Champion Companion ou no Champion C001 fisico.
