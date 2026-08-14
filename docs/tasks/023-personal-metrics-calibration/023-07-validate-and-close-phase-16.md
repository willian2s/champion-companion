# 023-07 — Validar e encerrar a Fase 16

- **Status:** `completed`
- **Depends on:** `023-06`

## Objetivo

Validar em banco preservado e dispositivo real os fluxos novos da Fase 16 e
fechar documentação somente com evidências observadas.

## Validação técnica

Executar:

```bash
git diff --check
./gradlew lint assembleDebug assembleRelease
./gradlew installDebug
```

Não criar testes automatizados novos nem executar suites adicionais sem pedido
explícito.

## Banco

Sobre instalação existente:

- confirmar migration v12 -> v13;
- `PRAGMA user_version = 13`;
- `PRAGMA integrity_check` válido;
- dados anteriores preservados;
- `manualDistanceMeters` da Spec 022 preservado;
- tabela de calibrações criada vazia no upgrade;
- nenhum snapshot criado pela migration;
- nenhuma operação HC criada pela migration.

## Perfil

Validar:

- editar altura;
- reiniciar e confirmar persistência;
- sugestão de passo muda, mas não é aceita automaticamente;
- editar meta diária de passos;
- Home/Saúde/Calendário refletem a meta;
- comprimento base manual/altura continua funcional.

## Peso

Validar:

- registrar duas medições em kg;
- reiniciar app;
- último peso correto;
- histórico preservado;
- comportamento local correto com HC indisponível/desabilitado;
- quando HC estiver habilitado e autorizado, confirmar somente o fluxo write-only
  esperado, sem readback.

## Calibração de corrida

Usar uma atividade D3/D4 real ainda elegível:

1. confirmar `RUNNING`;
2. registrar distância externa de referência;
3. conferir passos usados;
4. conferir comprimento observado;
5. aplicar calibração;
6. conferir distância canônica fechando na referência;
7. conferir pace/velocidade/splits coerentes;
8. confirmar uma única evidência no perfil RUNNING.

Adicionar segunda corrida calibrada e validar manualmente:

```text
learned = Σ distância / Σ passos
```

## Calibração de caminhada

Validar uma atividade `WALKING` e confirmar:

- perfil WALKING independente;
- nenhuma alteração no perfil RUNNING;
- novo treino de caminhada usa walking learned quando disponível.

## Correção e remoção

- corrigir uma distância de referência existente;
- confirmar substituição sem dupla contagem;
- remover uma evidência;
- confirmar recomputação do perfil futuro;
- confirmar que snapshots históricos não mudaram automaticamente.

## Guard de sincronização

- atividade `SYNCED` não pode ser reescrita silenciosamente;
- `SYNCING`/efeito parcial segue fail-closed conforme contrato congelado;
- calibração não dispara sync automaticamente;
- sync explícito após calibração usa projeções esperadas;
- nenhuma leitura Health Connect é adicionada.

## Regressões essenciais

Confirmar pelo menos:

- corrida sem calibração continua funcionando com BASE/fallback;
- caminhada sem calibração continua funcionando;
- TREADMILL continua usando BASE;
- Daily Summary continua usando BASE;
- CYCLING continua usando distância manual opcional da Spec 022 e nunca passos;
- parser continua `d4-v3`.

## Documentação de evidência

Criar:

```text
docs/integration/023-07-phase-16-physical-validation.md
```

Registrar somente cenários realmente exercitados, com N/A para o que não foi
possível validar.

## Fechamento documental

Após aceite:

- marcar Spec 023 como `completed`;
- atualizar `023-00-overview.md` para `7/7`;
- atualizar `docs/baseline/current-baseline.md` com fatos efetivamente entregues;
- atualizar `docs/roadmap/post-phase-15-direction.md` marcando Fase 16 concluída;
- registrar Room/derivation versions finais reais;
- não alterar o roadmap histórico para fingir execução de itens removidos.

## Checklist

- [x] build/lint/install concluídos
- [x] migration/banco preservados
- [x] altura/meta validadas
- [x] peso/histórico validados
- [x] corrida calibrada validada
- [x] segunda corrida confirma aprendizado ponderado
- [x] caminhada independente validada
- [x] correção/remoção validada
- [x] guards de sync validados
- [x] regressão CYCLING/TREADMILL/DailySummary conferida
- [x] evidência física documentada
- [x] baseline/roadmap atualizados
- [x] spec e overview em `completed`

## Aceite final da Fase 16

A fase encerra quando o perfil aprendido melhora novos treinos de corrida e
caminhada sem GPS interno, enquanto atividades antigas permanecem reproduzíveis
pelos snapshots que congelaram.

## Resultado da execução

- **Status:** `completed`
- **Arquivos alterados:**
  - `docs/integration/023-07-phase-16-physical-validation.md`;
  - esta subtarefa;
  - `docs/tasks/023-personal-metrics-calibration/023-00-overview.md`;
  - `docs/specs/023-personal-metrics-calibration.md`;
  - `docs/baseline/current-baseline.md`;
  - `docs/roadmap/post-phase-15-direction.md`.
- **Decisões e desvios:** foram registradas duas corridas informadas pelo usuário:
  `2,90 km` em 12/08 e `3,00 km` em 14/08. A corrida de 14/08 não foi
  sincronizada e recebeu exclusão permanente autorizada pela UI. Importação
  explícita inseriu corrida de 12/08, treino livre de 13/08 e corrida de 14/08;
  operações da corrida de 12/08 foram criadas somente após ação explícita. O
  formulário foi localizado após rolagem completa; corridas foram revisadas como
  `RUNNING` e calibradas pela UI antes da remoção da corrida 14/08. O treino
  livre reimportado foi corrigido de `700 m` para `950 m` como `WALKING`, sem
  dupla contagem e sem sync. Novo treino de 14/08 09:36 foi revisado sem
  referência externa e reprocessado explicitamente para usar perfil WALKING
  aprendido, também sem sync.
- **Comandos executados:** `git diff --check`; `./gradlew lint assembleDebug
  assembleRelease --no-daemon`; `./gradlew installDebug --no-daemon`; `adb
  devices`; inspeção do banco preservado por cópia somente leitura via
  `adb exec-out run-as ... cat` e consultas SQLite locais;
  inspeção da tela Exercícios via `uiautomator dump`; `adb shell am force-stop`
  seguido de reinício do app e nova inspeção; ação explícita `Buscar treinos no
  relógio`, inspeção dos detalhes via `uiautomator dump`, revisão/calibração pela
  UI, consulta Room pós-calibração, revisão/correção do treino livre como
  `WALKING`, reprocessamento explícito do novo treino e exclusão permanente
  confirmada da corrida 14/08.
  Consultas adicionais executaram `PRAGMA foreign_key_check` e inventário de
  atividades, snapshots, calibrações e operações Health Connect, incluindo
  consultas explícitas para órfãos sem atividade.
  Perfil foi salvo com altura `175 cm` e meta `1.000 passos`; Saúde recebeu
  medições de `95 kg` e `96 kg`; reinício confirmou persistência do perfil e do
  histórico. Permissão `WRITE_WEIGHT` foi revogada temporariamente: novo `95 kg`
  salvou localmente e a operação HC ficou temporariamente `FAILED`; permissão foi
  restaurada e retry posterior sincronizou a operação.
  Usuário confirmou que Home, Saúde e Calendário exibem a meta `1.000`; Home
  exibiu resumo `5.366` e `100% da meta`; aceite registrado como evidência física.
- **Resultados e evidências:** `git diff --check`, lint, assembleDebug,
  assembleRelease e installDebug passaram. APK instalado no Galaxy S22
  `SM-S901E`, Android 16/API 36. Banco retornou `user_version=13`,
  `integrity_check=ok` e `foreign_key_check` sem linhas; existem 2 calibrações
  atuais, uma `RUNNING` e uma `WALKING`, com 11 snapshots ativos para 11
  atividades (`16` snapshots históricos). A tela Exercícios preservou o resumo
  após reinício. Corrida de 12/08 tem `3.538` passos e ficou `SYNCED` com 33
  operações HC e checkpoints `SENT/AFTER_REMOTE_EFFECT`; a corrida de 14/08 foi
  removida sem operação HC. Antes da remoção, fórmula RUNNING foi
  `5.900 / 7.093 = 0,831806 m`; depois, perfil RUNNING recompôs para
  `2.900 / 3.538 = 0,819672 m`. Treino livre ficou `WALKING`,
  `READY_TO_SYNC`, com `936` passos, snapshot `watch-d3-d4-v3`, distância
  canônica `950 m`, source `REFERENCE_DISTANCE_CALIBRATION` e zero operações HC.
  Perfil WALKING: `950 / 936 = 1,014957 m`; novo treino WALKING de 14/08 09:36
  usou `CONTEXTUAL_LEARNED_PROFILE`, `1,014957 m`, `138` passos e distância
  canônica `140,064 m`, estado `READY_TO_SYNC`, sem HC. Perfil RUNNING preservou a calibração da corrida
  12/08. TREADMILL de 14/08 10:47 ficou `READY_TO_SYNC`, usou base manual
  `0,882 m` e não criou operação HC. CYCLING sem distância manual exibiu distância
  indisponível e o registro manual `3.460 m` permaneceu preservado. Daily Summary
  foi confirmado pelo usuário. Detalhe da corrida `SYNCED` não ofereceu ação de
  reprocessamento/calibração nem readback HC. Pesos locais ficaram `95 kg`, `96 kg`, `96 kg` e `95 kg`,
  com 4 operações HC `WEIGHT` `SYNCED` após retry da falha temporária no teste
  sem permissão, sem readback. Evidência física parcial registrada em
  `docs/integration/023-07-phase-16-physical-validation.md`.
- **Riscos residuais da rodada inicial:** preservação completa do banco/migration, guard
  `SYNCING`/efeito parcial, migration completa e regressões Health Connect
  indisponível/desabilitado ainda não foram confirmados integralmente nesta
  rodada. Corrida reimportada de 14/08 permanece pendente e sem sync. Esta
  seção histórica registra o estado anterior à continuação final.

### Continuação da validação

- **Comandos adicionais:** `adb devices -l`; lançamento explícito de
  `MainActivity`; `adb shell uiautomator dump`; cópia somente leitura com
  `adb exec-out run-as ... cat databases/champion-companion.db`; consultas
  SQLite locais sobre a cópia; inspeção estática de `MIGRATION_12_13` e do
  schema exportado.
- **Resultados adicionais:** Galaxy S22 `SM-S901E`, Android 16/API 36
  confirmado. A migration em fixture reconstruída v12 -> v13 retornou
  `user_version=13`, integridade válida, tabela de calibrações vazia e
  preservação de 11 atividades, 16 snapshots, 220 operações HC e distância
  manual. Fallback WALKING foi reprocessado pela UI sem calibrações e usou
  `MANUAL`, `0,882 m`, `watch-d3-d4-v3`, `121,716 m` e zero HC; fallback RUNNING
   usou `MANUAL`, `0,882 m`, `watch-d3-d4-v3`, `3.135,510 m` e zero HC.
- **Guards adicionais:** fixture `SYNCING` exibiu `Sincronizando 1` e detalhe
  sem CTA de reprocessamento. Fixture `FAILED/SENT/AFTER_REMOTE_EFFECT` não
  exibiu calibração/correção. Integração HC desligada pela UI desabilitou
  `Sincronizar agora` e foi religada depois. Nenhum provider foi acionado pelas
  fixtures e nenhum readback foi executado.
- **Decisão:** gates físicos restantes foram validados e o fechamento documental
  foi aplicado em baseline, roadmap, spec e overview.
  A única ressalva técnica é a migration exercitada em fixture reconstruída,
  pois não existe arquivo histórico v12 original.
- **Detalhamento documental:** fechamento canônico, fórmula ponderada,
  correção/remoção, persistência de altura/meta e comportamento de pace,
  velocidade e splits foram validados na rodada física; evidência resumida fica
  em `docs/integration/023-07-phase-16-physical-validation.md`.
