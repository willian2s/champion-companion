# Lacunas E Questoes Abertas

## Reorientação atual após Fase 013

- Fase 013 está `closed`, com `partially_delivered` por
  `product_reorientation`.
- Treinos iniciados pelo celular, GPS, rota, `ExerciseRoute` e BLE live não são
  backlog ativo. A desativação operacional dos pontos de entrada phone foi
  implementada em `013-17`; aceite final foi confirmado por revisão independente.
- Migrations, schemas, sessões, dados e evidências devem ser preservados até
  autorização explícita de remoção; lacunas físicas históricas não viram
  sucesso por cancelamento.
- Fase 014 está `completed`, com progresso `17/17`. A entrega cobre treinos
  iniciados pelo relógio: importação D3/D4, derivação, revisão e Health Connect.
  WorkManager permanece limitado a trabalho finito e adiável.
- A distância canônica será derivada de passos válidos e `stepLengthMeters`. A
  distância original do relógio será somente diagnóstica.

## Estado factual final da Fase 014

- Parser novo usa `d4-v2`; `d4-v1` permanece legado identificável, sem
  `decodedSteps` criado automaticamente pela migration.
- `MIGRATION_8_9` levou o banco ao Room v9 e `MIGRATION_9_10` levou o banco ao
  schema v10. Ambas são aditivas e não executam efeitos remotos.
- A projeção Health Connect usa operações filhas com payload, ID e versão
  congelados, retry foreground explícito e reconciliação por ID exato. A
  integração permanece write-only.
- A validação física cobriu somente cenários exercitados: Gate A passou, Gates B,
  C, D, E e G ficaram `N/A`, e Gate F passou para autorização, falha parcial e
  retry observados após `014-17`.
- Permanecem riscos de fixtures físicos não exercitados, midpoint,
  delete-before-insert, round-trip completo e variações de firmware. Esses riscos
  não reabrem a fase nem autorizam evidência ausente.

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
| G-13 | Privacidade de GPS/rota histórica | Alta | Preservação documental | Não |
| G-14 | Background e notificações históricas | Alta | Preservação documental | Sim |
| G-15 | Paridade de telas e estados | Media | Implementacao UI/validacao | Nao inicialmente |
| G-16 | Arquitetura/nomenclatura Android | Media | Inicio implementacao Android | Nao |

## Atualizacao Factual Pos-Fase 12

- Fase 11 foi concluida no codigo com revisao, calibracao, descarte logico e atividades locais em `READY_TO_SYNC`.
- Documentacao atrasada da Fase 11 foi reconciliada em `012-01` sem reabrir implementacao funcional.
- Backlog local herdado para Health Connect passou a ser reconciliado e processado por fila persistida foreground.
- `SettingsScreen` deixou de ser placeholder, `healthSyncEnabled` participa de fluxo funcional e `ChampionDatabase` esta em `v5`.
- G-08, G-09 e G-10 deixaram de ser lacunas centrais de implementacao da Fase 12 e passam a riscos residuais apenas para evolucoes futuras, regressao e validacoes adicionais de longo prazo.
- A validação física de fechamento da Fase 11 confirmou telas, filtros, detalhe, métricas, gráfico, intervalos e persistência no Galaxy S22 `SM-S901E`, mas não reproduziu review, calibração, descarte, `PARTIAL` e banner de pendências por ausência desses estados no dispositivo.
- Health Connect funcional observado no dispositivo pertence à Fase 12; a ausência registrada para Fase 11 é ausência de operação de sync própria, não ausência do provider no produto final.

## Fechamento Factual Da Fase 12

- Gate C físico foi concluído em 2026-07-28 no Galaxy S22 com Health Connect real, conforme registro de `012-13` e confirmação manual integral adicional do usuário.
- G-08, G-09 e G-10 ficam encerradas como lacunas de entrega da Fase 12; permanecem somente riscos de regressão, evolução futura e validações de longo prazo.
- Na Fase 12 continuam fora do escopo: leitura remota, `WorkManager`, foreground service, sincronização periódica, backup, rotas e dados de localização.

## Política documental da Fase 13

- A ADR-014 autorizou rota somente em caminhada/corrida iniciada explicitamente
  pelo usuário; implementação, migration e validação física continuam abertas
  para as subtarefas seguintes.
- Treino livre e esteira não usam localização. Não há rastreamento passivo,
  servidor, exportação, GPX, TCX, FIT, backup ou device transfer de rota.
- Foreground service mínimo da sessão ativa pertence à Fase 13. WorkManager e
  tarefas adiáveis permanecem na Fase 14.
- `Phase003PrivacyGuardTest` é dívida conhecida incompatível com a exceção;
  não deve ser alterado, contornado ou executado nesta fase.

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
- Evidencia: `workouts-d3-d4.md` confirma heuristicas de layout pelo codigo; validacao fisica da Fase 10 em Galaxy S22 com Champion C001 confirmou fluxo funcional de importacao, deduplicacao, cancelamento e retomada sem divergencia bloqueadora. Permanecem sem confirmacao exaustiva offsets/semantica de bytes desconhecidos, unidades de todos campos, relacao CRC D3/D4 em todos firmwares e politica final de fixture versionada.
- Motivo da duvida: validacao fisica confirmou fluxo de produto, mas ainda faltam fixtures progressivas vazio, multipacote, CRC repetido, completo, timeout, fora de ordem e duplicado para cobertura documental e regressao futura.
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
- Evidência histórica: a baseline MotionLog registrava IDs `motionlog:` como legado, `insertRecords` sem update explícito e `Steps` por delete de intervalo seguido de insert. O Champion Companion usa identidade própria por `applicationId`, `clientRecordVersion` e update seguro, validados no Gate C.
- Motivo da dúvida: resolvido para a Fase 12; permanece somente risco de regressão futura do provider.
- Fase: resolvida na implementação Health Connect da Fase 12.
- Dispositivo fisico: Sim.
- Validação concluída no Gate C: contrato de ID local/remoto e versão, insert repetido, update e retry foram exercitados manualmente; impacto sobre registros de terceiros permanece fora do escopo write-only.

## G-09 — Permissoes, Escrita Parcial E Retry Health Connect

- Impacto: Alta. Sucesso parcial pode ocultar records pendentes; permissao indisponivel pode gerar perda de fila.
- Evidência histórica: a baseline não comprovava permissões, provider, mensagens reais, retry ou recuperação de records complementares. O Champion Companion confirmou esses cenários no Gate C.
- Motivo da dúvida: resolvido para a Fase 12; permanece somente risco de regressão futura do provider.
- Fase: resolvida na implementação Health Connect da Fase 12.
- Dispositivo fisico: Sim, Android com Health Connect disponivel.
- Validação concluída no Gate C: ausência, concessão e revogação de permissão, disponibilidade, falha parcial, fila por entidade/record, retry e erro seguro foram confirmados manualmente.

## G-10 — Steps, Perfil, Peso/Altura E Tempo Health Connect

- Impacto: Media. Intervalo ou timestamp errado altera agregacao e historico externo.
- Evidência histórica: a baseline deixava pendentes timezone, virada de dia, política de peso/altura, frequência de envio e timestamps fallback. O Champion Companion validou os cenários elegíveis no Gate C.
- Motivo da dúvida: resolvido para a Fase 12; timezone futuro e evolução de política permanecem riscos de longo prazo.
- Fase: resolvida na implementação Health Connect da Fase 12; evolução futura permanece aberta.
- Dispositivo fisico: Sim para escrita/consulta; Nao para definir regra.
- Validação concluída no Gate C para dados elegíveis: passos, peso, altura, timestamps e medidas repetidas foram confirmados manualmente; ausência de dados pessoais permanece tratável.

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

## G-13 — Privacidade De GPS, Rotas E Dados Proibidos — registro histórico

G-13 preserva a política e os riscos da execução histórica da Fase 013. Não é
backlog ativo de GPS/rota phone; a desativação operacional foi concluída em
`013-17` e a fase está fechada.

- Impacto: Alta. Violacao direta de requisito autoritativo.
- Evidencia: ADR-004 e ADR-014 distinguem a proibição geral da autorização
  controlada; MotionLog ainda possui campos/tabelas legados e o código
  Champion Companion ainda não implementa rota.
- Motivo da duvida: a política está decidida, mas persistência, deleção, logs,
  Health Connect e validação física ainda precisam ser entregues sem ampliar o
  escopo.
- Fase histórica: `013-02` para decisão; `013-03` a `013-16` para implementação
  e validação; fechamento em `013-17`. Não abrir novos gates phone.
- Dispositivo fisico: Nao.
- Validacao sugerida: validar schema, payload, retenção, deleção, export/backup,
  logs e rede; procurar campos proibidos fora da sessão F13 autorizada antes de
  cada release.

## G-14 — Background, Scheduler E Notificacoes — registro histórico

- Impacto: Alta. Pode conflitar com limites de BLE e comprometer continuidade/consumo.
- Evidencia: `feature-inventory.md` descreve Foreground Service MotionLog
  com reconnect e D1 cíclico; ADR-014 define o serviço mínimo da sessão F13;
  roadmap reserva WorkManager para tarefas adiáveis F14.
- Motivo da duvida: continuidade da sessão, notificação, recuperação e limites
  de retry ainda exigem implementação nativa; baseline não é cópia direta.
- Fase histórica: implementação background/notificações. A direção futura é
  refinamento de treinos do relógio; WorkManager fica limitado a trabalho finito
  e adiável, sem objetivo funcional na Fase 014.
- Dispositivo fisico: Sim.
- Validacao sugerida: definir gatilhos iniciados pelo usuario, duracao, cancelamento, notificacao, WorkManager e limites de retry. Testar app fechado, perda BLE, restricao de bateria e recuperacao sem iniciar leitura proibida automaticamente.

## G-15 — Paridade De Telas, Graficos E Estados Operacionais

- Impacto: Media. Fluxos podem existir sem comportamento compreensivel em loading, vazio, erro ou diagnostico.
- Evidencia: `feature-inventory.md` cataloga telas/estados; `source-map.md` deixa pendentes politica visual, graficos, calendario, estados vazios, notificacoes e diagnosticos.
- Motivo da duvida: Fase 0 mapeia comportamento atual, nao define navegacao, composicao visual ou cobertura de interface Android.
- Atualizacao apos Fase 09: detalhe de frequencia cardiaca agora possui acao D1 explicita, grafico local, agregacoes, historico paginado e estados operacionais principais. Permanecem abertas apenas lacunas de semantica clinica das faixas visuais, screenshots formais de PR e paridade futura de outras areas nao cobertas pela fase.
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

## Ordem Histórica Recomendada

1. G-13 e G-11: limites de dados, privacidade e persistencia.
2. G-16, G-05, G-14: contratos Android, BLE e background antes de serviços.
3. G-01 a G-03: fixtures e validacao fisica dos fluxos BLE principais.
4. G-06 e G-07: contratos de exercicio e esteira.
5. G-08 a G-10: Health Connect apos dados locais e fluxos estaveis.
6. G-12 e G-15: backup/limpeza e validacao de interface conforme escopo aprovado.

## Limites

As lacunas G-08, G-09 e G-10 foram resolvidas para a Fase 12 pelo código e pelo Gate C físico. As demais lacunas continuam abertas conforme seus registros individuais. `Confirmado pelo código` permanece evidência da baseline; não substitui validação física futura fora do escopo desta fase.
