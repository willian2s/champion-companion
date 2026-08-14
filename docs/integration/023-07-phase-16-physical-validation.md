# Evidência física 023-07 — Validação da Fase 16

- **Data da validação técnica:** 2026-08-14
- **Aplicativo:** `1.0-dev`, instalado por `./gradlew installDebug`
- **Dispositivo:** Galaxy S22 `SM-S901E`, Android 16, API 36
- **Escopo:** validação técnica e registro dos treinos informados para a
  calibração de corrida
- **Fonte dos dados físicos:** informação fornecida pelo usuário; passos,
  estado Room e telas não foram inventados quando não observados
- **Health Connect:** a corrida de 14/08 não deve ser sincronizada; nenhuma ação
  explícita de sincronização foi solicitada para ela

## Banco preservado observado

- `PRAGMA user_version = 13`.
- `PRAGMA integrity_check` retornou `ok`.
- `PRAGMA foreign_key_check` não retornou linhas.
- `step_length_calibrations` contém `2` linhas atuais: uma `RUNNING` e uma
  `WALKING`; a calibração da corrida 14/08 foi removida junto com a atividade.
- Há `1` registro com `manualDistanceMeters` e `4` medições de peso locais.
- Após nova importação e reprocessamento explícito, há `11` atividades, `11`
  snapshots ativos (`16` snapshots históricos) e `220` operações Health Connect
  históricas no
  banco preservado.
- A corrida de 12/08 possui `33` operações Health Connect sincronizadas; a corrida
  de 14/08 foi removida antes de qualquer operação remota.
- As `6` atividades históricas de 07/08 a 11/08 permanecem presentes; há `11`
  snapshots ativos para `11` atividades, sem duplicidade de snapshot ativo.
- Consultas explícitas de órfãos retornaram `0` calibrações sem atividade, `0`
  snapshots sem atividade e `0` operações HC de treino sem atividade.
- A inspeção adicional foi feita por cópia somente leitura com
  `adb exec-out run-as ... cat`; o dispositivo não possui `sqlite3`. Consultas
  locais confirmaram `0` calibrações duplicadas por atividade e `0` snapshots
  ativos duplicados. O payload calibrado RUNNING preserva
  `frozenPayloadVersion=2`, `metricProvenance=EXTERNAL_REFERENCE_CALIBRATION` e
  `originVersion=watch-d3-d4-v3`.
- A migration `12 -> 13` no código é aditiva e cria somente tabela/índices de
  calibração, sem inserts. Não existe cópia física pré-upgrade nesta rodada para
  provar que nenhum snapshot/operação HC foi criado durante o upgrade; esse
  critério permanece parcial.

## Resultado técnico

| Verificação | Resultado | Evidência |
|---|---|---|
| `git diff --check` | `PASS` | Nenhum erro de whitespace antes da documentação |
| `./gradlew lint assembleDebug assembleRelease --no-daemon` | `PASS` | Build completo concluído |
| `./gradlew installDebug --no-daemon` | `PASS` | APK instalado em um dispositivo |

## Estado observado no dispositivo

- Importação inicial exibida como concluída, com `3` inseridos, `0` duplicados,
  `0` inválidos e `0` ausentes.
- Resumo atual: `1` aguardando revisão, `0` aguardando calibração, `6` prontos e
  `4` sincronizados. A pendência é a corrida reimportada de 14/08.
- Após `am force-stop` e reinício do app, a tela Exercícios preservou o resumo
  da última importação (`Concluída`, `3` inseridos, `0` duplicados, `0`
  inválidos, `0` ausentes).
- Nova importação explícita em 14/08 inseriu `3` atividades: corrida de 12/08,
  treino livre de 13/08 e corrida de 14/08. As três ficaram `PENDING_REVIEW`.
- Reimportação posterior exibiu `2` inseridos e `1` pulado; trouxe novo treino
  livre de 14/08 e reapresentou corrida 14/08. Referência de caminhada carregada
  anteriormente foi corrigida de `700 m` para `950 m` pela UI, sem dupla contagem.
- A corrida de 12/08 foi sincronizada por ação explícita. Suas `33` operações
  Health Connect ficaram `SYNCED`, com filhos de distância, sessão, BPM,
  velocidade e calorias em `SENT`/`AFTER_REMOTE_EFFECT`.
- A corrida de 14/08 original foi removida pela UI sem operação remota; nova
  importação a apresentou novamente como `PENDING_REVIEW`, sem tipo confirmado
  e sem operação Health Connect. Permanece sem sync.
- O treino livre reimportado de 13/08 foi revisado pela UI como `WALKING`, ficou
  `READY_TO_SYNC`, preservou `936` passos e não criou operação Health Connect.
- Novo treino importado em 14/08 às 09:36 foi revisado como `WALKING` e
  reprocessado explicitamente sem distância externa; passou a usar perfil
  contextual aprendido, ficou `READY_TO_SYNC` e não criou operação Health
  Connect.
- Novo treino importado em 14/08 às 10:47 foi revisado como `TREADMILL`, ficou
  `READY_TO_SYNC`, usou base manual `0,882 m` e não criou operação HC.
- Antes da calibração, a corrida de 14/08 exibiu distância canônica de `3,14 km`;
  referências externas informadas são `2,90 km` em 12/08 e `3,00 km` em 14/08.
- Não foi acionado `Sincronizar com Health Connect` para a corrida de 14/08.

## Treinos informados

| Data | Tipo | Distância externa | Uso nesta validação | Health Connect |
|---|---|---:|---|---|
| 12/08 | Corrida | 2,90 km | Distância calibrada e sincronizada explicitamente | `SYNCED`, 33 operações |
| 14/08 | Corrida | 3,00 km | Calibrada e depois removida pela UI | 0 operações |
| 13/08 | Treino livre → caminhada | 950 m | Distância calibrada; sync não acionado | 0 operações |
| 14/08 09:36 | Treino livre → caminhada | N/A | Perfil WALKING aprendido aplicado | 0 operações |
| 14/08 10:47 | Treino livre → TREADMILL | N/A | BASE/manual preservado | 0 operações |

## Estado dos critérios físicos

- Foram identificadas duas corridas e um treino livre no relógio.
- As referências externas registradas são `2,90 km` em 12/08 e `3,00 km` em
  14/08.
- A corrida de 14/08 foi excluída permanentemente antes de qualquer operação
  remota, conforme autorização explícita do usuário.
- Passos D4 observados: `3.538` na corrida de 12/08 e `936` no treino de
  13/08, revisado como `WALKING` elegível. Os `3.555` passos da corrida removida
  foram usados somente na evidência pré-remoção.
- Corrida de 12/08 calibrada com `2,90 km`: comprimento observado `0,819672 m`,
  snapshot `watch-d3-d4-v3`, source `REFERENCE_DISTANCE_CALIBRATION` e distância
  canônica `2.900 m`.
- Corrida de 14/08 foi calibrada com `3,00 km` antes da remoção; snapshot e
  calibração foram removidos junto com a atividade.
- Antes da remoção, aprendizado RUNNING foi `5.900 m / 7.093 passos = 0,831806 m`;
  após remoção, perfil RUNNING foi recomposto para `2.900 m / 3.538 passos =
  0,819672 m`.
- O treino livre reimportado foi calibrado como `WALKING` com referência externa
  `950 m`: comprimento observado `1,014957 m`, snapshot `watch-d3-d4-v3`,
  source `REFERENCE_DISTANCE_CALIBRATION` e distância canônica `950 m`.
- Perfil WALKING aprendido observado: `950 m / 936 passos = 1,014957 m`.
- Novo treino WALKING de 14/08 09:36 usou `1,014957 m` com source
  `CONTEXTUAL_LEARNED_PROFILE`, derivação `watch-d3-d4-v3`, `138` passos e
  distância canônica `140,064 m`. Reprocessamento foi explícito; nenhuma
  operação Health Connect foi criada.
- TREADMILL de 14/08 10:47 usou `0,882 m`, source `MANUAL`, derivação
  `watch-d3-d4-v2`, `87` passos e distância canônica `76,734 m`; não recebeu
  perfil aprendido nem operação Health Connect.
- Perfil RUNNING preservou a calibração da corrida 12/08; a corrida 14/08 não
  deixou calibração residual. O treino de 13/08 não criou operação Health
  Connect.
- Persistência de Perfil e resumo de Exercícios após reinício foi observada;
  TREADMILL, CYCLING, Daily Summary e parser `d4-v3` foram observados nesta
  rodada; fixtures finais cobriram guards e fallback BASE.
- O fechamento canônico, pace, velocidade e splits foram validados na rodada;
  esta síntese não reproduz cada valor intermediário. A correção/remoção foi
  validada com preservação dos snapshots históricos. Altura/meta foram
  persistidas e a sugestão não foi aceita automaticamente.

## Limites e riscos residuais

- Registro anterior: não marcar a Fase 16 com a evidência parcial então
  disponível. A continuação final validou os gates restantes.
- Corridas elegíveis, passos D4, fechamento canônico e fórmula ponderada foram
  confirmados nesta rodada.
- Formulário de revisão/calibração foi localizado após rolagem completa do
  detalhe. As duas corridas foram revisadas como `RUNNING` e calibradas pela UI.
- Nos cenários de usuário, nenhuma mutação foi feita por SQL ou por ação remota.
  A corrida de 12/08 foi enviada somente depois de tocar em `Sincronizar com
  Health Connect`; a corrida 14/08 foi removida pela UI após confirmação
  irreversível. Fixtures finais usaram cópias temporárias locais e foram
  removidas antes da restauração.
- O treino livre reimportado em 13/08 foi classificado como `WALKING` e calibrado
  com `950 m`; nenhuma sincronização automática foi iniciada.
- O novo treino de 14/08 às 09:36 usou o perfil WALKING aprendido após
  reprocessamento explícito; nenhuma sincronização automática foi iniciada.

- Não houve readback de Health Connect, conforme contrato write-only. Os guards
  físicos de `SYNCING`/efeito remoto parcial foram exercitados em fixtures
  locais; a migration foi validada em fixture v12 reconstruída, sem arquivo
  histórico v12 original.

## Perfil, peso e regressões observados

- Tela Perfil exibiu altura `175 cm`, meta diária `1.000 passos` e comprimento
  base manual `0,882 m` antes e depois de reiniciar o app.
- Altura foi salva como `175 cm` e meta diária como `1.000 passos` pela UI; após
  reinício, ambos permaneceram persistidos e o comprimento base manual `0,882 m`
  não foi aceito automaticamente como sugestão nova.
- Usuário confirmou que Home, Saúde e Calendário exibem a meta diária `1.000`;
  Home também exibiu resumo `5.366` e `100% da meta` após reinício.
- Tela Saúde recebeu medições manuais de `95 kg`, `96 kg` e novo `95 kg` em
  14/08. Histórico após reinício preservou as novas medições e a anterior de
  `96 kg`; último peso exibido: `95 kg`.
- Com `WRITE_WEIGHT` temporariamente revogado, novo `95 kg` foi salvo localmente;
  histórico persistiu e operação HC ficou temporariamente `FAILED`. Permissão foi
  restaurada e retry posterior deixou `4` operações `WEIGHT` `SYNCED`, sem readback.
- Parser das três atividades reimportadas é `d4-v3`; atividades legadas
  preservadas continuam identificáveis separadamente.
- Há duas atividades `CYCLING`: uma sincronizada com distância manual `3.460 m`
  e uma pronta sem distância manual e sem operação HC. Nenhuma usa perfil
  aprendido de corrida/caminhada.
- Na UI, CYCLING sem distância manual exibiu `Distância indisponível` e não
  exibiu distância derivada por passos; registro manual `3.460 m` permanece
  preservado no outro CYCLING.
- Detalhe da corrida 12/08 `SYNCED` exibiu somente estado de escritas locais
  aceitas, sem ação de reprocessamento/calibração disponível e sem readback HC;
  nenhuma reescrita silenciosa foi feita.
- A inspeção após novo lançamento confirmou as mesmas invariantes locais e
  permissões WRITE do Health Connect concedidas. Guard físico de `SYNCED` foi
  observado pela ausência de ação de reprocessamento. O registro inicial não
  tinha fixture para `SYNCING`/efeito remoto parcial; ambas foram exercitadas na
  continuação final. Nenhuma leitura de records remotos foi executada.
- TREADMILL foi validado com BASE/manual. Usuário confirmou Daily Summary
  completo; confirmação registrada como evidência física.
- Inventário HC atual: `1` operação de altura `SYNCED`, `4` operações de peso
  `SYNCED`, `8` resumos diários `SYNCED` e
  operações históricas de treinos,
  frequência cardíaca e projeções filhas.

## Estado de encerramento

- Antes desta continuação, a subtarefa permanecia `in_progress` e o overview em
  `6/7`. Os gates físicos restantes foram exercitados na seção seguinte; o
  fechamento documental foi aplicado em spec, baseline, roadmap e overview. A
  migration foi validada por fixture reconstruída, não por arquivo histórico v12
  original; limitação permanece explícita.

## Continuação final da validação

- O banco original foi preservado localmente, e cada fixture temporária foi
  removida antes da restauração. A restauração final confirmou novamente
  `user_version=13`, `integrity_check=ok`, `foreign_key_check` vazio, `11`
  atividades, `16` snapshots (`11` ativos), `2` calibrações, `4` pesos, `1`
  distância manual e `220` operações Health Connect.
- A migration foi exercitada em fixture local derivada do banco preservado:
  tabela/índices de calibração foram removidos, `user_version` foi reduzido para
  `12`, e o app foi iniciado para executar `12 -> 13`. Após o upgrade, o banco
  retornou `user_version=13`, integridade válida, tabela criada com `0` linhas,
  `11` atividades, `16` snapshots, `220` operações HC e distância manual
  preservadas. Desvio: não havia arquivo histórico v12 original; fixture foi
  reconstruída a partir do v13 preservado e o limite permanece registrado.
- Fallback WALKING foi exercitado em fixture sem calibrações: reprocessamento
  explícito pela UI usou `MANUAL`, `0,882 m`, `watch-d3-d4-v3`, distância
  canônica `121,716 m` para `138` passos e zero operações HC. Fallback RUNNING
  também foi exercitado sem calibrações: reprocessamento explícito usou
  `MANUAL`, `0,882 m`, `watch-d3-d4-v3`, distância canônica `3.135,510 m` e zero
  operações HC.
- Guard `SYNCING` foi exercitado em fixture local: resumo exibiu
  `Sincronizando 1` e o detalhe exibiu `Sincronização em andamento no Health
  Connect` sem CTA de reprocessamento. Guard de efeito remoto parcial foi
  exercitado com operação local `FAILED/SENT/AFTER_REMOTE_EFFECT`; o detalhe
  não exibiu calibração/correção para mutação local. Nenhuma fixture acionou
  provider ou efeito remoto real.
- A integração Health Connect foi desligada pela UI: `Sincronizar agora` ficou
  desabilitado; depois foi ligada novamente. Nenhuma operação adicional foi
  criada. Estado original foi restaurado e reconfirmado no banco.
