# Fase 13 — Exercícios iniciados pelo Champion Companion com GPS e rota

- **Ticker:** `013`
- **Status:** pending
- **Base:** Fases 12 e 15 concluídas; Fase 14 posterior

## Contexto

Champion Companion já importa treinos D3/D4, revisa e calibra atividades, mantém histórico local e sincroniza records granulares com Health Connect. Fase 13 adicionará caminhada, corrida, treino livre e esteira iniciados explicitamente pelo celular. Sessão ativa será local-first, persistida, recuperável e mantida por foreground service.

Decisão de produto desta fase substitui política histórica de descarte de coordenadas por persistência controlada de rota para caminhada e corrida iniciadas pelo usuário:

```text
capturar GPS durante sessão explícita
→ filtrar pontos
→ persistir rota local
→ calcular métricas
→ criar atividade revisável
→ sincronizar a mesma rota com o Health Connect
```

Fase 12 permanece concluída sem `ExerciseRoute`. Fase 15 permanece concluída. Fase 14 continua responsável por `WorkManager`, tarefas adiáveis e expansões de background não necessárias à sessão ativa. Foreground service mínimo de sessão ativa pertence à Fase 13.

## Objetivo

Entregar exercícios iniciados pelo celular com:

- preparação explícita e countdown persistido;
- GPS preciso e rota persistida somente para caminhada/corrida;
- treino livre e esteira sem GPS;
- sessão persistida, controlada por foreground service e recuperável após morte de processo;
- participação opcional do Champion C001 para BPM e passos;
- generalização de `WorkoutActivity` como abstração única;
- correlação segura com importação D3/D4;
- revisão/calibração existentes;
- sincronização idempotente da mesma `ExerciseSessionRecord`, incluindo `ExerciseRoute` quando autorizado.

## Documentos autoritativos

- `AGENTS.md`;
- `docs/baseline/current-baseline.md`;
- `docs/baseline/feature-inventory.md`;
- `docs/baseline/gaps-and-open-questions.md`;
- `docs/baseline/health-connect-mapping.md`;
- `docs/roadmap/champion-companion-roadmap.md`;
- ADRs vigentes em `docs/decisions/`, especialmente ADR-003, ADR-004, ADR-008, ADR-011, ADR-012 e ADR-013;
- `docs/specs/010-watch-workouts-d3-d4.md`;
- `docs/specs/011-activity-review-calibration.md`;
- `docs/specs/012-native-health-connect.md`;
- `docs/specs/015-calendar-consolidated-history.md`;
- `docs/tasks/015-calendar-consolidated-history/015-00-overview.md`;
- `docs/specs/017-alinhamento-visual-margens-cabecalhos.md` para o refinamento
  visual independente das superfícies compartilhadas;
- `docs/protocol/heart-rate-d0-f002.md`;
- `docs/protocol/workouts-d3-d4.md`;
- `docs/protocol/ble-transport.md`;
- `docs/protocol/ble-command-mapping.md`.

## Comportamento atual encontrado

### Domínio e persistência

- `ChampionDatabase` está em `v5` e exporta schemas `1.json` a `5.json`.
- `AppWorkoutSession` já modela `CREATED`, `ACTIVE`, `PAUSED`, `AWAITING_RECOVERY`, `FINISHED` e `DISCARDED`, mas comentário atual proíbe localização no domínio.
- `AppWorkoutSessionRepository` expõe somente `observeRecoverable`, `recover` e `finish`.
- `AppWorkoutSessionEntity` já persiste tempos, agregados e vínculo opcional com atividade.
- `AppWorkoutSessionEntity.toDomain()` perde estado, tempos e vínculo; recria sessão como `CREATED`.
- `WorkoutActivity` exige `deviceId`, `WatchWorkoutImportKey`, parser, import timestamp e diagnóstico, logo ainda é orientada a D3/D4.
- consultas de BPM e candidatos de atividade usam `INNER JOIN watch_workout_imports`, excluindo futuras atividades do celular.
- `HeartRateSource.APP_WORKOUT_SESSION` e deduplicação por sessão/timestamp já existem.
- não existem tabelas de rota nem persistência incremental de pontos.

### BLE

- gate foreground possui `DAILY_STEPS_REFRESH`, `MANUAL_HEART_RATE`, `WATCH_D1_HISTORY` e `WATCH_WORKOUT_IMPORT`; falta owner de sessão do celular.
- D0/F002 está implementado como medição manual. Documentação MotionLog mostra uso em exercício, mas protocolo do modo esporte não está confirmado no app nativo.
- D3/D4 representa importação posterior de treino gravado, não stream esportivo live.
- A8/D8 não têm contrato funcional confirmado.
- desconexão encerra sessão de transporte; não há reconexão de treino ativa pronta.

### Android, navegação e UI

- manifesto não declara permissões modernas de localização de treino, notificação ou foreground service.
- não existe foreground service, runtime de localização ou dependência específica de localização.
- DataStore `champion_preferences` não possui countdown.
- Navigation3 usa back stack manual e não possui seleção, preparação, sessão ativa ou recuperação.
- Exercícios possui importação D3/D4, filtros, detalhe, revisão, calibração e sync explícito; não possui CTA `Iniciar treino`.

### Health Connect

- SDK atual: `androidx.health.connect:connect-client:1.1.0`.
- fila é granular por `EXERCISE_SESSION`, `DISTANCE`, `TOTAL_CALORIES_BURNED` e `HEART_RATE`.
- IDs usam identidade determinística por variante e versões locais.
- `ExerciseRoute` e `WRITE_EXERCISE_ROUTE` não existem no app.
- atividade só é totalmente `SYNCED` quando componentes elegíveis concluem.
- índice Room da fila é único por `(aggregateId, recordType)`, enquanto contrato lógico também distingue `aggregateType`; migration deve reconciliar essa inconsistência se nova projeção da sessão exigir.

## Requisitos funcionais e critérios de aceite

### Política documental e privacidade

1. `013-02` atualiza primeiro roadmap, baseline, ADR-004 ou sucessora, ADR-014 sequencial e `AGENTS.md`.
2. Documentos removem proibição absoluta de rota somente nos escopos autorizados desta fase.
3. Fase 12 continua descrita como concluída sem rota.
4. Fase 13 recebe foreground service mínimo de sessão; Fase 14 mantém `WorkManager` e tarefas adiáveis.
5. `Phase003PrivacyGuardTest` é registrado como incompatível e dívida conhecida, sem alteração ou execução nesta fase.
6. Não usar nomes obscuros para contornar guard antigo.
7. Rota existe somente para caminhada/corrida iniciadas explicitamente pelo usuário.
8. Sem rastreamento passivo, servidor, backup, device transfer, exportação automática, GPX, TCX ou FIT.
9. Descarte anterior à atividade remove rota e amostras transitórias.
10. Logs nunca contêm coordenadas, altitude, rota, BPM, payload BLE, identificador pessoal, título ou observações.

### Preparação e countdown

11. Navegação terá destinos equivalentes a seleção, preparação, sessão ativa e recuperação.
12. Preparação mostra tipo, iniciar, countdown, relógio, GPS aplicável, permissões e serviços desativados.
13. Voltar da preparação não cria atividade, serviço ou rota.
14. DataStore aceita countdown `0`, `3`, `5`, `10`; padrão `5`; última escolha permanece.
15. Countdown começa por toque explícito, é visível, acessível, cancelável e não cria atividade antes de zero.
16. Em zero, sessão é iniciada e foreground service começa enquanto app ainda está visível.
17. Caminhada/corrida voltam à espera quando GPS perde prontidão durante countdown.

### GPS e qualidade

18. Caminhada/corrida exigem localização precisa e `ACCESS_FINE_LOCATION`; localização aproximada é detectada e insuficiente.
19. Preparação aquece GPS sem persistir pontos.
20. Readiness usa principalmente fix recente, idade, precisão horizontal, ausência de mock, permissão e serviços.
21. `GnssStatus` complementa com satélites/C/N0 quando disponível, sem ser requisito exclusivo.
22. Direção inicial: forte `<=10 m`; bom/pronto `>10 e <=20 m`; fraco `>20 e <=50 m`; não utilizável `>50 m`, ausente ou antigo; fix recente `<=10 s`.
23. Iniciar caminhada/corrida só fica habilitado em bom ou forte.
24. UI distingue permissão necessária, localização desativada, procurando, fraco, bom, forte, pronto, perdido e indisponível.
25. Texto usa “precisão aproximada” quando C/N0 não existir, nunca chama precisão de força de rádio.

### Rota e métricas

26. Cada ponto aceito representa sequência, timestamp, latitude, longitude, altitude opcional, precisões horizontal/vertical, velocidade opcional, bearing opcional e segmento.
27. Filtro rejeita ordem inválida, ponto antigo, precisão insuficiente, coordenada inválida, salto incompatível com velocidade humana, duplicata, mock e deslocamento abaixo do ruído.
28. Distância usa somente pontos aceitos.
29. Pausa, lacuna prolongada, recuperação de GPS e recuperação de processo iniciam novo segmento; gaps nunca viram linha reta.
30. Pontos são persistidos incrementalmente ou em lotes pequenos.
31. Uma rota pertence inicialmente a uma sessão e, pela associação sessão→atividade, a uma única atividade final.
32. Ordenação é determinística; distância acumulada e segmentos são persistidos; ausência de GPS é segura.

### Generalização e sessão

33. `WorkoutActivity` suporta `WATCH_D3_D4`, `PHONE_GPS`, `PHONE_TREADMILL`, `PHONE_FREE_WORKOUT` sem duplicar modelo completo.
34. Metadados de importação permanecem em `WatchWorkoutImport`; atividade do celular não exige CRC, índice, parser, diagnóstico, import key ou dispositivo.
35. `deviceId` é opcional em atividade do celular.
36. D3/D4 existentes continuam compatíveis e preservados.
37. Repository de sessão cobre criar, iniciar, observar ativa, atualizar agregados, pausar, retomar, interromper, recuperar, finalizar, descartar e associar atividade.
38. Estado sobrevive a rotação, background, tela apagada, recriação da Activity e morte recuperável de processo.
39. Reboot não reinicia serviço nem sessão automaticamente; usuário decide após abrir app.

### Foreground service

40. Serviço mínimo da sessão fica na Fase 13; sem `WorkManager`.
41. Serviço começa ao final do countdown com app visível.
42. Tipo `location` é usado para caminhada/corrida; `connectedDevice` somente com BLE contínuo; combinação quando ambos.
43. Não solicitar `ACCESS_BACKGROUND_LOCATION` por padrão.
44. Serviço não exportado, sem boot receiver e preferencialmente `START_NOT_STICKY`.
45. Notificação persistente mostra tipo, duração, pausa/atividade, GPS, relógio, abrir app e ações idempotentes de pausar, retomar e finalizar.
46. Ações de finalizar/descarte exigem confirmação segura na UI; serviço encerra produtores e notificação corretamente.

### Sessão ativa e tipos sem GPS

47. Tela ativa mostra tipo, duração ativa/pausada, distância, ritmos, velocidade, GPS, relógio, BPM atual/médio, passos, pausa e ações.
48. Treino livre/esteira não pedem GPS, não mostram GPS como erro e funcionam sem relógio.
49. Ausência de BPM/passos permanece ausente.
50. Esteira finaliza em fluxo de revisão e depois `PENDING_CALIBRATION`; não inventa distância real.
51. Caminhada, corrida e treino livre criam atividade revisável em `PENDING_REVIEW` conforme requisito de produto; revisão existente conduz a pronto para sync.

### Relógio e prioridade

52. Sessão funciona sem relógio.
53. Relógio conectado mostra status e pode fornecer BPM real do modo esporte somente quando protocolo observado permitir associação segura.
54. Amostras são `APP_WORKOUT_SESSION`, vinculadas à sessão e deduplicadas por sessão/timestamp.
55. App não inicia modo esporte, não executa D0 como substituto automático e não envia A8/D8.
56. Desconexão não encerra treino; reconexão é controlada e não apaga amostras.
57. Novo owner `APP_WORKOUT_SESSION` bloqueia D1, D3/D4, medição manual e refresh conflitantes.
58. UI orienta usuário a iniciar treino no relógio se quiser dados do Champion C001.
59. Fonte prioritária segue tabela da solicitação: celular para sessão/tempo/tipo/GPS/rota/distância/ritmo/velocidade; relógio para BPM/passos e calorias válidas; revisão para título/notas.

### Correlação D3/D4

60. Antes de criar nova atividade D3/D4, procura atividade do celular com `abs(startedAt)` `<=60 s`, sobreposição relevante, candidato único e mesmo dispositivo quando associado.
61. Correlação segura mantém atividade do celular, vincula `WatchWorkoutImport`, preserva CRC/metadata, agrega BPM/passos/calorias elegíveis e nunca substitui rota/métricas GPS.
62. Sem correlação segura, importação normal cria atividade independente.
63. Ambiguidade cria atividade do relógio pendente de revisão e diagnóstico sanitizado.
64. Reimportação do mesmo CRC permanece idempotente e não cria segunda atividade.

### Finalização

65. Ordem: parar captura, persistir lote final, consolidar, criar atividade, associar rota/amostras, finalizar sessão, parar serviço e abrir revisão.
66. Operação local final é transacional; falha deixa estado recuperável sem duplicar atividade.
67. Descarte antes da atividade apaga rota e amostras transitórias.

### Health Connect e `ExerciseRoute`

68. Manifesto inclui `android.permission.health.WRITE_EXERCISE_ROUTE`.
69. Rota é parte de `ExerciseSessionRecord`; nunca segundo record de sessão.
70. Sessão base e atualização com rota usam o mesmo `clientRecordId` canônico de `EXERCISE_SESSION`.
71. `clientRecordVersion` é monotônico, persistido e maior em mudança visível ao Health Connect.
72. Sem permissão de rota, sessão e complementos autorizados sincronizam; rota local permanece pendente.
73. Concessão posterior atualiza a mesma sessão; não duplica.
74. Retry da rota não apaga sessão, métricas ou rota já sincronizada.
75. Todo upsert posterior da sessão inclui rota local elegível já existente, evitando downgrade route-less.
76. Operação interna equivalente a rota representa projeção/enriquecimento da mesma `ExerciseSessionRecord`.
77. Ordem remota: sessão base primeiro; rota e complementos granulares depois; falha de rota não bloqueia complementos independentes.
78. Atividade só vira `SYNCED` quando todos os componentes elegíveis, inclusive rota autorizada, concluírem.
79. Metadata da sessão/rota/distância usa telefone; BPM pode manter proveniência do relógio.

### Superfícies e acessibilidade

80. Exercícios ganha `Iniciar treino`, separado de `Buscar treinos no relógio`.
81. Detalhe, Calendário, dia, histórico BPM, feedback de sync e Settings aceitam atividade do celular/correlacionada sem duplicação.
82. Detalhe mostra presença de rota, pontos, segmentos, precisão média, distância GPS e status de sync; sem mapa/tiles/API key.
83. UI cobre descrições acessíveis de GPS, countdown anunciado sem excesso, botões grandes, contraste, fonte aumentada, landscape, confirmações e estados de falha requeridos.
84. Mudanças visuais produzem screenshots na execução das subtarefas correspondentes.

O alinhamento de margem e cabeçalho das superfícies já existentes é tratado
separadamente pela Spec 017. A subtarefa `013-15` permanece responsável pela
integração funcional de atividades phone/correlacionadas, estados, histórico,
sincronização e acessibilidade específica da Fase 13; não absorve esse pacote
visual nem tem suas dependências alteradas por ele.

## Abordagem escolhida

### Abstração única com proveniências combináveis

`WorkoutActivity` será raiz comum. `AppWorkoutSession` representa gravação operacional; `WatchWorkoutImport` representa proveniência D3/D4. Uma atividade pode possuir somente sessão, somente importação ou ambos após correlação. Queries deixam de depender de `INNER JOIN watch_workout_imports`.

### Modelo de rota

Migration manual `v5 → v6` criará estruturas equivalentes:

```text
app_workout_sessions 1 ─── 0..1 workout_routes
workout_routes       1 ─── N    workout_route_points
app_workout_sessions 0..1 ─── 1 workout_activities após finalização
```

`workout_routes` mantém ID, sessão única, estado, contagens, distância acumulada, timestamps e revisão. `workout_route_points` mantém rota, sequência, timestamp, coordenadas, altitude/precisões/velocidade/bearing opcionais e segmento. Pontos usam cascata ao apagar rota. Não usar JSON, blob ou polyline opaca. Índices somente para constraints/consultas concretas: sessão única e `(routeId, sequence)` único; índice temporal adicional somente se consulta real exigir.

### Localização em fronteiras

Separar:

- readiness/permissão/serviços;
- source Android de localização precisa;
- complemento GNSS opcional;
- filtro puro e categorizado;
- acumulador de segmentos/métricas;
- repository de persistência de pontos aceitos.

Callbacks carregam sessão e geração de runtime; callback tardio não persiste.

### Serviço e recuperação

Foreground service é owner operacional da sessão ativa. UI envia intents; repository e estado persistido são fonte de recuperação. Checkpoints persistem agregados e tempos. Processo novo converte sessão sem runtime vivo para `AWAITING_RECOVERY`. Retomar cria nova geração e segmento. Sem `BOOT_COMPLETED`, start sticky ou restauração automática após reboot.

### BLE conservador

Owner `APP_WORKOUT_SESSION` arbitra conexão durante sessão. D0 continua exclusivo da medição manual enquanto protocolo esportivo não for comprovado; app não inicia modo esporte nem A8/D8. A integração aceita somente BPM/passos cuja origem temporal e transporte estejam seguros. Incerteza física não bloqueia treino sem relógio; ausência permanece explícita.

### Correlação

Ordem: deduplicação por CRC existente; depois busca candidato phone por janela de 60 segundos, device quando disponível, sobreposição e unicidade. Merge transacional adiciona import e dados permitidos. Ambiguidade nunca mescla silenciosamente.

### `ExerciseRoute`

Usar projeção interna de enriquecimento da operação `EXERCISE_SESSION`, mas manter mesmo `clientRecordId`. Base pode ser enviada sem rota. Rota posterior usa versão maior e mesma identidade. Qualquer upsert futuro da sessão inclui rota elegível já local. Provider fica fora da transação Room; retry repete ID/versão.

## Alternativas descartadas

- modelo completo separado para atividade do celular: duplicaria revisão, calendário e sync;
- dispositivo/importação fictícios: corromperia proveniência;
- rota somente em memória: impediria recuperação e retry;
- JSON/blob/polyline: prejudicaria inserção incremental e consultas seguras;
- vínculo duplo direto da rota à sessão e atividade: criaria owners divergentes;
- FGS controlado por ViewModel: morreria com UI;
- `START_STICKY`, boot receiver ou `WorkManager`: retomada sem consentimento ou mecanismo inadequado;
- `ACCESS_BACKGROUND_LOCATION`: desnecessária quando FGS inicia visivelmente;
- D0/A8 automático: protocolo não confirmado e contrário ao requisito;
- correlação só por horário ou escolha arbitrária: risco de falso merge;
- segundo `ExerciseSessionRecord` para rota: duplicação remota;
- delete remoto seguido de insert: risco de perda;
- versão somente por relógio de parede: colisão/regressão;
- mapa externo: fora do escopo.

## Arquivos, módulos e contratos afetados

### Documentação

- `AGENTS.md`;
- roadmap e baselines listadas;
- ADR-004 e nova ADR-014, após confirmar sequência;
- documentação Health Connect e protocolo quando houver evidência física.

### Código provável

- `core/model/AppWorkoutSession.kt`, `WatchWorkoutActivity.kt`, `HeartRateAndSync.kt`, `RepositoryContracts.kt`, `DomainMetadata.kt`;
- novos contratos/modelos em `core/model` para rota, GPS, qualidade, serviço e correlação;
- `core/database/ChampionDatabase.kt`, `ChampionDaos.kt`, schema `6.json` e migration `5→6`;
- `data/DatabaseMappers.kt`, `RoomRepositories.kt`, `RepositoryModule.kt`;
- novos pacotes `core/location` e `core/notifications` ou equivalentes reais aprovados;
- `core/bluetooth` e coordinators das features para owner/conflitos;
- `core/healthconnect/HealthRecordMapper.kt`, gateway, platform e coordinator;
- `core/preferences/AppPreferencesStore.kt` e módulo;
- `core/navigation/AppDestination.kt`, `ChampionNavigation.kt`;
- `feature/exercises`, `feature/calendar`, `feature/health`, `feature/settings`;
- `AndroidManifest.xml`, `app/build.gradle.kts`, `gradle/libs.versions.toml`.

## Riscos e mitigação

- **Guard antigo incompatível:** formalizar ADR/política antes de código; não contornar; dívida fica explícita.
- **Sensibilidade do banco:** sandbox privado, backup/device transfer negados, sem export/rede/log e deleção física no descarte pré-atividade.
- **Migration complexa:** manual, sem destructive migration, preservar IDs D3/D4 e schema exportado; rollback operacional por restauração do artefato anterior antes de abrir v6, pois downgrade após abertura não é suportado.
- **Mapper atual perde sessão:** corrigir antes de serviço/captura.
- **Android/OEM interrompe localização:** FGS visível, checkpoints pequenos e recuperação explícita.
- **GPS ruidoso:** thresholds conservadores, segmentos e validação física ajustável.
- **BPM esportivo não comprovado:** integração opcional e passiva; sem D0/A8 automático; ausência segura.
- **Falso merge D3/D4:** candidate único, device/sobreposição, fallback independente.
- **Upsert de rota:** mesma identidade e versão monotônica; validação física real no Health Connect.
- **Falha remota após sucesso:** retry com mesmo ID/versão.
- **Rotas grandes:** pequenos lotes locais; avaliar limites reais do provider na validação, sem simplificação opaca antecipada.
- **Hotspots compartilhados:** serializar contratos/repositories; paralelizar somente após `013-08` conforme grafo.

## Estratégia de validação

Não criar, alterar, executar ou recomendar testes automatizados. `Phase003PrivacyGuardTest` permanece dívida conhecida e fora da validação desta fase.

Comandos únicos permitidos nas subtarefas aplicáveis:

```bash
./gradlew lint assembleDebug assembleRelease
./gradlew installDebug
```

Validação manual no Galaxy S22 cobre todos cenários informados na solicitação: countdowns, permissões/localização, qualidade GPS, caminhada/corrida, pausa/gaps, background/tela apagada/process death, descarte, tipos sem GPS, relógio opcional e desconexão, correlação/ambiguidade/CRC, calendário sem duplicata, Health Connect sem/depois com permissão de rota, retry e ausência de coordenadas no Logcat. Mudanças visuais exigem screenshots.

## Ordem das subtarefas

```text
013-01 → 013-02 → 013-03 → 013-04 → 013-05 → 013-06 → 013-07 → 013-08 [base]
                                                                  ├→ 013-09 ─┐
                                                                  ├→ 013-10 ─┤
                                                                  └→ 013-11 ─┤
                                                                              └→ 013-08 [aceite integrado] → 013-12
                                                                                                      ├→ 013-13
                                                                                                      └→ 013-14
                                                                                                          ↓
                                                                                                   013-15
                                                                                                      ↓
                                                                                                   013-16
                                                                                                      ↓
                                                                                                   013-17
```

`013-08` possui dois marcos de planejamento: sua base operacional já permite
iniciar `013-09`, `013-10` e `013-11`; aceite integrado de `013-08` aguarda os
handoffs de GPS/BLE e suas evidências. As três frentes podem avançar em paralelo,
desde que não alterem simultaneamente contratos ou `RoomRepositories`. `013-13`
e `013-14` podem avançar em paralelo após `013-12`; integração final em `013-15`
exige ambas.

## Matriz de rastreabilidade

| Requisito | Subtarefas |
|---|---|
| Auditoria factual e guard antigo | 013-01, 013-02 |
| Roadmap, baseline, ADR, AGENTS e fronteira F13/F14 | 013-02, 013-17 |
| Generalização da atividade, sessão, rota, GPS e correlação | 013-03 |
| Permissões, dependências, countdown e navegação | 013-04 |
| Migration v6, rota, atividade phone, cascatas e recuperação | 013-05 |
| Readiness, GNSS, thresholds, filtro e segmentos | 013-06, 013-09 |
| Preparação, countdown, permissão e acessibilidade | 013-07 |
| FGS, notificação, lifecycle e recuperação | 013-08 |
| Caminhada/corrida, captura incremental e métricas | 013-09 |
| Treino livre, esteira e calibração | 013-10, 013-12 |
| Owner BLE, BPM/passos, desconexão e orientação | 013-11 |
| Finalização transacional e revisão unificada | 013-12 |
| Correlação D3/D4 e deduplicação | 013-13 |
| ExerciseRoute, identidade, versão e falha parcial | 013-14 |
| Exercícios, detalhe, calendário, BPM, Settings e a11y | 013-15 |
| Galaxy S22, GPS, relógio, HC e Logcat | 013-16 |
| Fechamento e riscos residuais | 013-17 |

## Premissas explícitas

- Fases 12 e 15 permanecem concluídas; Fase 13 é próxima; Fase 14 continua posterior.
- DB atual confirmado em v5; próxima versão planejada é v6.
- ADR sequencial confirmada como ADR-014 pelo inventário atual até ADR-013.
- SDK 1.1.0 será mantido se suportar APIs necessárias; upgrade exige necessidade comprovada.
- Android Location APIs nativas são preferência inicial; dependência externa só entra com justificativa concreta.
- BPM/passos live do relógio podem permanecer ausentes se protocolo esportivo não for validado fisicamente.
- Sessão do celular não inicia automaticamente modo esporte no relógio.
- Sessão recuperável após reboot significa visível para decisão quando app abrir, nunca reinício automático.
- Rota histórica não será criada para atividades da Fase 12 ou D3/D4 sem sessão phone correlacionada.
- Nenhuma task cria, altera ou executa testes automatizados.

## Definition of Done da fase

- documentação autoritativa substitui política antiga sem reescrever entrega da Fase 12;
- ADR-014 aceita e ADR-004 alinhada/sucedida;
- DB v6 e schema exportado preservam histórico existente;
- quatro tipos iniciados pelo celular funcionam conforme política de GPS;
- sessão ativa usa FGS e recuperação explícita;
- atividade única abastece revisão, calendário e histórico;
- correlação não duplica atividades seguras;
- uma única sessão Health Connect recebe rota idempotente;
- validação manual e screenshots possuem evidências;
- nenhum teste automatizado é criado, alterado ou executado;
- overview e subtarefas refletem estado real.
