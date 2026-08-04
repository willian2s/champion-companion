# 013-09 — Implementar captura de rota e métricas de caminhada/corrida

- **Ticker:** `013`
- **Número:** `09`
- **Status:** completed

## Objetivo e resultado esperado

Conectar fonte GPS ao serviço para persistir pontos aceitos em pequenos lotes, segmentar gaps e calcular métricas live. Resultado: walking/running recuperáveis com rota local íntegra.

## Contexto

Readiness/filtro vêm de `013-06`; schema/repository de `013-05`; FGS de `013-08`. Aquecimento anterior ao zero nunca persiste.

## Documentos obrigatórios

Spec 013; ADR-014; `013-05/06/08`; política de privacidade atualizada.

## Dependências

Base operacional de `013-08` disponível. Pode iniciar antes do aceite integrado de
`013-08` e avançar em paralelo com `013-10`/`013-11`, sem editar
contratos/repositories simultaneamente. Seu handoff de captura GPS é necessário
para o aceite integrado de `013-08`.

## Arquivos permitidos

Serviço/coordinator da sessão; `core/location`; route repository/DAOs somente APIs já definidas; estado/tela ativa de métricas GPS; logging seguro; task/overview.

## Arquivos e símbolos prováveis

`WorkoutRecordingService`, `WorkoutLocationSource`, `RoutePointFilter`, `WorkoutRouteAccumulator`, `WorkoutRouteRepository`, `WorkoutRouteDao` e estado de métricas da sessão ativa.

## Arquivos proibidos

BLE live, tipos sem GPS, finalização em atividade, Health Connect, mapa/export e testes.

## Requisitos cobertos

Captura incremental; campos de ponto; filtros; distância; ritmo/velocidade; segmentos; pause/loss/recovery; falha ao persistir.

## Escopo

- começar persistência só após ACTIVE;
- pequenos lotes/checkpoints;
- sequence/segment determinísticos;
- distância acumulada somente accepted;
- ritmo atual/médio e velocidade;
- signal lost/indisponível sem encerrar sessão;
- novo segmento em resume/gap/process recovery;
- degradação segura em falha de persistência.

## Fora do escopo

Mapa, route simplification, export, sync HC e correlação.

## Passos de implementação

1. Criar rota única ao iniciar walking/running.
2. Iniciar location source pela geração ativa.
3. Filtrar e acumular accepted; contar rejected por categoria segura.
4. Persistir em lote curto com sequence/segment e distância acumulada.
5. Pausa para source/persistência e fecha segmento.
6. Retomada/readiness e recuperação abrem segmento novo.
7. Em loss prolongado, sinalizar UI e não conectar gap.
8. Atualizar agregados/checkpoints e tela ativa.
9. Falha de write pausa captura/mostra erro seguro, sem perder atividade silenciosamente.
10. Atualizar overview.

## Critérios de aceite

- zero ponto de warmup persiste;
- rejected não altera distância;
- gap/pausa nunca forma linha reta;
- processo morto preserva últimos lotes;
- sequência/segmentos sobreviverem à recuperação;
- UI não mostra coordenadas;
- livre/esteira não entram neste caminho.

## Validação manual

Caminhada/corrida externa, pausa/retomada, perda/recuperação GPS, background, tela apagada, process death/recovery e falha de persistência reproduzível; comparar distância com referência aproximada sem tratar precisão como garantia.

## Comandos

```bash
./gradlew lint assembleDebug assembleRelease
./gradlew installDebug
```

## Evidências

Contagens sanitizadas de pontos/segmentos, distância final, transições GPS, ausência de coordenadas no Logcat, comandos e screenshots.

## Screenshots

Obrigatórias: ativa GPS, pausada, sinal perdido/recuperado e métricas; sem mapa/coordenadas.

## Definição de pronto

Walking/running persistem rota recuperável e métricas corretas; overview atualizado.

## Riscos e cuidados

Bateria, lotes grandes e callback tardio. Não persistir rejected. Não ajustar threshold sem registrar evidência física.

## Rollback

Desabilitar tipos GPS na seleção e parar capture; preservar dados v6 para descarte/forward fix.

Não criar, alterar ou executar testes automatizados.

## Registro de execução — integração de captura e métricas — 2026-08-04

### Causa do escalonamento confirmada

- `WorkoutRecordingService` era owner do foreground service, mas não possuía
  fonte GPS, `WorkoutRouteRepository` nem acumulador.
- A fonte existente era usada somente durante preparação; a tela ativa mostrava
  tipo, duração e controles, sem distância, ritmo, velocidade ou estado GPS.
- A base operacional de `013-08` permite esta integração sem aguardar seu aceite
  físico integrado.

### Implementação

- `WorkoutRecordingService` cria a rota somente depois de a sessão persistir em
  `ACTIVE` e somente para caminhada/corrida. Treino livre e esteira não criam
  rota, não iniciam fonte e não exibem métricas GPS.
- Fonte GPS recebe a geração `runtimeGeneration` da sessão. O consumidor valida
  identidade da fonte, geração e estado `ACTIVE`; callbacks tardios são
  descartados. Aquecimento da preparação continua sem persistência.
- `WorkoutRouteAccumulator` recebe candidatos, aplica filtro, contabiliza
  rejeições de forma categórica e mantém sequência, segmentos e distância
  somente de pontos aceitos. Lacuna prolongada, perda/recuperação GPS, pausa e
  recuperação iniciam segmento novo.
- Lotes usam um ponto por confirmação, reduzindo a janela de perda durante morte
  do processo. Cursor e revisão do acumulador só avançam após sucesso de
  `appendAcceptedPoints`; conflito ou falha ambígua reconcilia o snapshot antes
  de permitir retry. Checkpoints de sessão refletem distância GPS persistida sem
  alterar contratos existentes.
- Pausa encerra a fonte e faz flush antes da mutação da sessão; retomada e
  recuperação restauram snapshot e abrem geração/segmento compatíveis.
- Falha de criação, append ou checkpoint de rota interrompe a captura, mantém a
  sessão aberta e publica erro seguro. Retry reinicia fonte, reenvia lote
  pendente quando possível e não finaliza atividade.
- `WorkoutLiveMetricsStore`, `ExercisesViewModel` e `ActiveWorkoutShell` expõem
  distância, ritmo atual/médio, velocidade atual/média, contagens, segmentos e
  estado GPS categórico. Nenhuma coordenada é exibida ou registrada.
- Notificação recebeu somente estado GPS categórico. Finalização da rota em
  estado terminal permanece responsabilidade da transação de `013-12`; esta
  subtarefa não cria nem finaliza atividade, não toca Health Connect ou BLE live.

### Correções durante retomada

- Substituído uso de `Duration.toSeconds()` incompatível com API mínima por
  acesso compatível aos componentes da duração; lint voltou a passar.
- Append com lote pendente agora exige sessão `ACTIVE`; cada ponto é confirmado
  imediatamente, e retry reconcilia snapshot persistido quando a resposta do
  append pode ter sido perdida. Fonte stale e conflito de pausa reabrem captura
  somente se sessão continuar ativa.
- Cleanup terminal sempre encerra fonte GPS. Tipos sem GPS permanecem fora do
  caminho de rota.

### Arquivos alterados

- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/exercises/WorkoutRecordingService.kt`;
- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/exercises/WorkoutLiveMetrics.kt`;
- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/exercises/ExercisesViewModel.kt`;
- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/exercises/WorkoutShells.kt`;
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/notification/WorkoutNotificationBuilder.kt`;
- esta subtarefa e `013-00-overview.md`.

### Comandos, resultados e evidências

- `git diff --check`: passou.
- Primeira execução de `./gradlew lint assembleDebug assembleRelease` encontrou
  erro `NewApi` em `Duration.toSeconds()`; correção aplicada.
- Execução final de `./gradlew lint assembleDebug assembleRelease`: passou;
  103 tarefas, 28 executadas e 75 atualizadas. Permanecem somente avisos
  preexistentes e avisos de locale/compatibilidade sem erro de lint.
- `./gradlew installDebug`: passou; APK instalado no dispositivo disponível
  Galaxy S22 com Android 16.
- Nenhum teste automatizado foi criado, alterado ou executado. `Phase003PrivacyGuardTest`
  não foi executado.

### Estado e bloqueios residuais

- Status permanece `in_progress`; item do overview permanece `[ ]` e progresso
  `7/17`.
- Validação manual obrigatória de caminhada/corrida externa, pausa/retomada,
  perda/recuperação GPS, gaps, tela apagada, process death com lotes e falha de
  persistência não foi executada nesta retomada.
- Screenshots obrigatórias de ativa GPS, pausada, sinal perdido/recuperado e
  métricas não foram produzidas. Portanto, implementação não é declarada
  concluída.
- Risco residual: ainda existe uma janela mínima entre aceitação em memória e
  confirmação do append; se a morte ocorrer nessa janela antes de qualquer
  resposta persistida, o ponto individual pode ser perdido. A captura não expõe
  esse ponto como métrica confirmada, e lotes confirmados permanecem persistidos
  e recuperáveis.
- `013-08` segue `in_progress`; aceite integrado, gates físicos e evidências de
  sua subtarefa continuam bloqueados independentemente deste handoff. `013-12`
  continua responsável por finalizar rota e criar/finalizar atividade.

## Registro de revisão independente e correções — 2026-08-04

### Achados corrigidos

- A primeira revisão encontrou janela de lote grande e retry sem reconciliação
  após resposta ambígua de `appendAcceptedPoints`. O limite foi reduzido para um
  ponto por confirmação, e o retry agora compara revisão, cursor, último ponto,
  segmentos e métricas do snapshot antes de reenviar ou confirmar localmente.
- A revisão seguinte encontrou exposição de métricas pending durante retry e
  falha de persistência. `confirmedRouteMetrics` agora separa métricas já
  persistidas das métricas em memória; UI/notificação publicam somente o estado
  confirmado quando existe lote pendente.

### Resultado da revisão

- Revisão independente final: implementação estática aprovada sem novo
  bloqueador, mas conclusão bloqueada por gate físico e screenshots obrigatórios.
- Status permanece `in_progress`; item permanece `[ ]`; progresso permanece
  `7/17`.

### Validação final desta correção

- `git diff --check`: passou.
- `./gradlew lint assembleDebug assembleRelease`: `BUILD SUCCESSFUL`.
- `./gradlew installDebug`: `BUILD SUCCESSFUL`; APK instalado no Galaxy S22 com
  Android 16.
- Nenhum teste automatizado foi criado, alterado ou executado.

### Riscos residuais

- Ainda existe janela mínima entre acumulação em memória e commit do append; uma
  morte exatamente nessa janela pode perder o ponto individual, sem expô-lo como
  métrica confirmada.
- Caminhada/corrida real, pausa/retomada, perda/recuperação GPS, gaps, morte de
  processo com lotes, falha de persistência e screenshots ainda exigem execução
  manual sanitizada.

## Registro de validação manual parcial — 2026-08-04

### Dispositivo e atividade existente

- Galaxy S22 com Android 16 e APK debug instalado por `installDebug`.
- Atividade existente de `WALKING` foi preservada; não houve descarte, alteração
  direta de banco ou fabricação de localização.
- Snapshot sanitizado confirmou sessão `FINISHING`, rota `CAPTURING`, 41 pontos
  aceitos, 2 segmentos, distância persistida de 155,9 m e sequência contínua de
  `0` a `40`. Nenhuma coordenada foi documentada.
- Distribuição sanitizada confirmou 20 pontos no segmento `0` e 21 no segmento
  `1`; não houve sequência não monotônica nem coordenada fora dos limites.

### Recuperação e privacidade

- `adb shell am crash` encerrou o processo; após reabertura, a superfície
  `Treino interrompido`/`A finalização está pendente` e `Tipo: Caminhada` voltou
  a aparecer.
- Após o crash, serviço de gravação não permaneceu em foreground.
- Snapshot posterior manteve 41 pontos, 2 segmentos e 155,9 m.
- Dump UI não exibiu coordenadas. Inspeção do Logcat filtrado pelo processo não
  encontrou payload de localização do app; linhas observadas eram de UI/runtime
  e Bluetooth sanitizado.

### Limites

- A atividade existente estava em `FINISHING`; portanto, não foi alterada para
  reproduzir nova caminhada, pausa/retomada ou falha de persistência.
- Não foram validados nesta execução: tela ativa com métricas GPS, pausa física,
  perda/recuperação GPS, gap em movimento, morte durante sessão `ACTIVE`, falha
  real de escrita e screenshots obrigatórios.
- Status permanece `in_progress`; item permanece `[ ]`; progresso permanece
  `7/17`. Validação manual completa e screenshots continuam bloqueando conclusão.

### Comandos executados

```bash
./gradlew installDebug
adb devices -l
adb shell am start -n br.com.mwconsulting.championcompanion.dev/br.com.mwconsulting.championcompanion.MainActivity
adb shell am crash br.com.mwconsulting.championcompanion.dev
adb shell dumpsys activity services br.com.mwconsulting.championcompanion.dev
adb exec-out uiautomator dump /dev/stdout
adb logcat --pid <pid> -d -v brief
```

Nenhum teste automatizado foi criado, alterado ou executado.

## Registro de validação manual completa parcial — 2026-08-04

### Caminhada GPS

- Sessão nova `WALKING` iniciou após GPS atingir `GPS pronto · bom`, sem ponto
  persistido durante countdown.
- Tela ativa exibiu distância, ritmo médio, velocidade média, contagem de
  pontos e segmentos; nenhuma coordenada apareceu.
- Pausa confirmou `Pausado`, `GPS: pausado` e distância congelada em 26 m, com 7
  pontos e 1 segmento.
- Retomada confirmou captura posterior e novo segmento: 53 m/15 pontos/2
  segmentos; após novo fix, 60 m/18 pontos/3 segmentos.
- GPS desligado durante `ACTIVE` exibiu `GPS: localização desativada`, manteve
  sessão viva e não aumentou distância durante perda. Screenshot sanitizado de
  estado perdido foi capturado temporariamente.
- GPS restaurado exibiu `GPS: pronto`; distância avançou para 60 m e segmento
  novo apareceu. Screenshot sanitizado de recuperação foi capturado
  temporariamente.

### Background e process death

- Com app em background e tela apagada, `WorkoutRecordingService` permaneceu
  presente e foreground; ao reabrir, sessão ativa e métricas permaneceram
  disponíveis.
- `adb shell am crash` durante `ACTIVE` abriu superfície de recuperação.
  `Retomar treino` restaurou captura; UI mostrou 86 m, 27 pontos e 5
  segmentos, depois 90,7 m e 28 pontos no snapshot sanitizado.
- Snapshot pré-descarte confirmou sessão GPS com 28 pontos aceitos, 5 segmentos,
  90,7 m, sequência sem regressão e nenhuma coordenada fora dos limites.

### Tipos sem GPS

- `Treino livre` iniciou e exibiu somente duração/controles; não exibiu card GPS.
- Sessão livre foi descartada após validação. Localização do aparelho foi
  restaurada para modo ativo ao final.

### Screenshots e bloqueio residual

- Screenshots temporários capturados: métricas ativas, pausada, GPS perdido e
  GPS recuperado; não foram mantidos no repositório por conterem dados de tela
  do dispositivo.
- Falha real de persistência não foi reproduzida. Não foi usado hook, mock,
  alteração de banco ou manipulação destrutiva de armazenamento.
- Status permanece `in_progress`; item permanece `[ ]`; progresso permanece
  `7/17`. Critério de degradação em falha de escrita segue sem evidência física.

## Registro de tentativa de falha de persistência — 2026-08-04

- Sessão GPS de teste iniciou com 3 pontos aceitos antes da injeção de falha.
- Arquivos SQLite foram temporariamente alterados para modo somente leitura
  (`444`) por `run-as`, sem alteração de conteúdo. Conexões já abertas pelo Room
  continuaram escrevendo; pontos avançaram de 2 para 3 e nenhum erro seguro de
  append foi observado. O método não reproduziu falha de persistência real.
- Após `am force-stop`, iniciar app com arquivos somente leitura não apresentou
  a UI do app. Permissões foram restauradas para `600` imediatamente; reabertura
  recuperou sessão normalmente. Sessão de teste foi descartada ao final.
- Permissões finais verificadas: banco, WAL, SHM, journal e lock em `-rw-------`.
- Resultado: falha controlada não é evidência de degradação segura. Critério
  continua pendente; não foram usados mock, hook, alteração de conteúdo ou
  armazenamento cheio.

## Desvio de aceite e waiver

- Usuário autorizou conclusão em 2026-08-04, aceitando que falha de
  `appendAcceptedPoints` não é reproduzível com segurança no dispositivo
  não-root disponível. Alterar permissões enquanto Room está aberto não
  interrompe conexões existentes; iniciar com arquivos somente leitura impede
  abertura normal do app.
- Waiver cobre somente evidência física de degradação em write failure. Não
  afirma que falha foi testada nem remove risco residual. Não foram usados mock,
  hook, alteração de conteúdo ou armazenamento cheio.
- Status passa a `completed`; item do overview passa a `[x]`; progresso passa a
  `8/17`. Seam debug-only ou ambiente de storage fault permanecem opções para
  validação futura, fora desta subtarefa concluída.

### Comandos da tentativa

```bash
adb shell run-as br.com.mwconsulting.championcompanion.dev chmod 444 databases/champion-companion.db databases/champion-companion.db-wal databases/champion-companion.db-shm databases/champion-companion.db-journal databases/champion-companion.db.lck
adb shell run-as br.com.mwconsulting.championcompanion.dev chmod 600 databases/champion-companion.db databases/champion-companion.db-wal databases/champion-companion.db-shm databases/champion-companion.db-journal databases/champion-companion.db.lck
adb shell am force-stop br.com.mwconsulting.championcompanion.dev
```

### Comandos adicionais

```bash
adb shell settings put secure location_mode 0
adb shell settings put secure location_mode 3
adb shell input keyevent 3
adb shell input keyevent 224
adb shell am crash br.com.mwconsulting.championcompanion.dev
adb shell dumpsys activity services br.com.mwconsulting.championcompanion.dev
adb exec-out uiautomator dump /dev/stdout
adb exec-out screencap -p
```
