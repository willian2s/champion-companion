# 026 — Overview: PoC MapLibre + OpenFreeMap com GPS e rota

- **Status geral:** completed
- **Spec:** [026-maplibre-openfreemap-gps-route-poc.md](../../specs/026-maplibre-openfreemap-gps-route-poc.md)
- **Progresso:** 8/8 subtarefas concluídas

## Objetivo

Validar fisicamente, em fluxo debug-only e sem reativar treinos iniciados pelo celular, se MapLibre Compose + OpenFreeMap conseguem apresentar posição GPS e uma rota que cresce em tempo real, com câmera utilizável, estado totalmente efêmero e acesso manual por botão técnico na página de Exercícios.

## Decisão experimental

```text
MapLibre Compose 0.14.0
        +
OpenFreeMap public instance
        +
GPS nativo do aparelho (objetivo)
        via
FusedLocationProviderClient (API de coleta debug-only)
```

Premissas atuais:

- MapLibre Compose vem do Maven Central;
- OpenFreeMap não exige cadastro, API key, token ou pagamento;
- OpenFreeMap não oferece SLA;
- atribuição cartográfica deve permanecer;
- GPS nativo continua sendo objetivo de validação, enquanto fused combina GPS,
  rede móvel, Wi-Fi, cache e sensores disponíveis para coletar fixes;
- `FusedLocationProviderClient` usa alta precisão, intervalo de 1 s, intervalo
  mínimo de 1 s e distância mínima de 0 m;
- `lastLocation` recente pode aquecer readiness e posição corrente, mas não
  entra automaticamente na rota antes de `Iniciar`;
- `com.google.android.gms:play-services-location:21.3.0` fica somente em
  `debugImplementation`; release não contém fused;
- `PhoneWorkoutPolicy` continua bloqueando a antiga feature phone;
- nenhuma rota será persistida nesta PoC;
- o CTA de acesso fica somente em Exercícios no build debug, dispara `Intent`
  explícito para a Activity e não é CTA de treino;
- ADB é fallback diagnóstico, não a única abertura prevista;
- release não expõe botão, `Intent` executável ou entrypoint da PoC.

## Checklist

- [x] [026-01-alinhar-documentacao-e-adr.md](026-01-alinhar-documentacao-e-adr.md)
- [x] [026-02-configurar-maplibre-e-openfreemap.md](026-02-configurar-maplibre-e-openfreemap.md)
- [x] [026-03-criar-entrypoint-debug-e-estado.md](026-03-criar-entrypoint-debug-e-estado.md)
- [x] [026-04-integrar-gps-readiness-e-filtro.md](026-04-integrar-gps-readiness-e-filtro.md)
- [x] [026-05-renderizar-mapa-posicao-e-rota.md](026-05-renderizar-mapa-posicao-e-rota.md)
- [x] [026-06-implementar-camera-stop-e-reset.md](026-06-implementar-camera-stop-e-reset.md)
- [x] [026-07-validar-fisicamente-e-casos-negativos.md](026-07-validar-fisicamente-e-casos-negativos.md)
- [x] [026-08-fechar-poc-e-registrar-decisao.md](026-08-fechar-poc-e-registrar-decisao.md)

## Observações

026-01 concluída. ADR-024 registra MapLibre Compose + OpenFreeMap como stack
experimental `debug-only`; guards da Fase 013 permanecem vigentes fora da
exceção. Não há bloqueios para 026-02. Termos, disponibilidade e ausência de
SLA do OpenFreeMap devem ser revalidados antes da configuração.

026-02 concluída com MapLibre Compose `0.14.0` em `debugImplementation` via
Maven Central. `settings.gradle.kts` não precisou de alteração. Release não
resolve MapLibre; style Positron e atribuição permanecem definidos para a tela
debug das subtarefas seguintes. Renderização física ainda não foi validada.

026-03 concluída com Activity, shell Compose, state machine explícita, estados
separados de mapa/GPS, ações neutras e CTA técnico variant-safe em Exercícios.
No debug, o CTA dispara `Intent` explícito para a Activity; no release, o
provider retorna `null`, sem botão, Intent executável ou referência à PoC. ADB
permanece fallback diagnóstico. Builds, lint, instalação debug, abertura da
Activity e screenshot sanitizado em estado neutro foram confirmados.

026-04 concluída com fonte GPS debug independente do binding produtivo, permissão
precisa por ação explícita, readiness baseada em `GpsReadinessEvaluator`, filtro
`RoutePointFilter`, identidade efêmera por geração, rota em memória, distância e
contadores de fixes aceitos/rejeitados. Builds, lint, instalação e abertura da
Activity foram confirmados; validação física de GPS, rota e Logcat sanitizado
permanece na 026-07. Não há bloqueios atuais.

026-05 concluída com `FeatureCollection` GeoJSON efêmera, `LineString` somente a
partir de dois pontos aceitos, marcador derivado de `currentPoint`, `LineLayer` e
`CircleLayer` usando a mesma fonte. A ordem `[longitude, latitude]`, atribuição e
style Positron foram preservados. `git diff --check`, compilação debug, lint,
builds debug/release e instalação debug passaram. Screenshot neutro já existente
confirma mapa-base e atribuição; validação física de crescimento da rota,
responsividade e Logcat sanitizado permanece na 026-07. Não há bloqueios atuais.

026-06 concluída com `CameraState`, follow moderado preservando zoom manual,
suspensão por gesto, recentralização, stop idempotente com invalidação de geração,
fit bounds final e reset efêmero incluindo identidade, métricas e nonce de comando.
Lint, builds, instalação debug e abertura direta da Activity foram confirmados no
dispositivo `SM-S901E`. O smoke físico completo de pan/zoom, recentralização,
percurso e fit bounds permanece na 026-07; smoke fused posterior confirmou
stop/reset e segundo ciclo, sem substituir o gate. Não há bloqueios atuais.

026-07 está em andamento após smoke físico reportar estado stale de permissão,
readiness lento, perda de GPS ao iniciar, câmera sem centralização e jank na
navegação. Correções foram aplicadas para preservar runtime quente, replayar
snapshot GPS, usar último fix recente como seed, separar posição corrente da rota,
serializar câmera, atualizar GeoJSON de forma assíncrona e separar mapa da rolagem.
O plano inicial baseado em `LocationManager.GPS_PROVIDER` e a tentativa
intermediária com registros paralelos de `GPS_PROVIDER`/`NETWORK_PROVIDER` foram
substituídos por `FusedLocationProviderClient`, com alta precisão, callback de
1 s, `lastLocation` recente como seed e cleanup explícito. Fused pode combinar
GPS, rede móvel, Wi-Fi, cache e sensores; `LocationManager` permanece somente
para observar o estado global de localização. A dependência
`play-services-location:21.3.0` está somente em `debugImplementation`, sem
dependência fused no release. `git diff --check`, `app:compileDebugKotlin`,
`app:lintDebug`, `app:assembleDebug`, builds completos e `dependencyInsight` do
runtime release passaram.

Smoke físico fused no `SM-S901E`: após tocar `Preparar GPS`, a UI mostrou
`GPS/PoC Pronto`, precisão de 7 m e 18 fixes; após iniciar, mostrou
`PoC Capturando`, precisão de 6 m, 29 fixes e 1 ponto aceito. Stop/reset
funcionaram, e o segundo ciclo voltou a `Capturando` sem contaminação. Essa
evidência cobre entrega básica fused e ciclo de vida, mas não encerra o gate:
não houve percurso externo de aproximadamente 500 m, casos negativos ou
inspeção sanitizada do Logcat. O checklist permanece desmarcado.

Atualização da `026-07` em 2026-08-20: foi criado o objeto debug-only
`GpsMapPocMockGeometryDebugMode` para aceitar localização mock/fake somente na
inspeção de geometria, câmera, stop e reset. Permissão precisa e localização
habilitada continuam necessárias; validações de coordenada, precisão, idade,
velocidade e ordem permanecem. A adaptação para readiness e para a fronteira do
filtro é local à PoC, sem alteração dos contratos produtivos. A UI identifica
`Modo geometria debug: mock aceito` e informa que o modo não comprova GPS real.
O gate de percurso real continua bloqueado e o progresso permanece `6/8`, com
checklists inalterados.

Validação física debug com localização mock no `SM-S901E` confirmou rota visual:
linha azul, início verde e fim vermelho após parada; posição corrente laranja
durante tracking. Primeiro ciclo registrou 8 fixes, 8 pontos aceitos e 54 m;
segundo ciclo, após limpar, registrou 6 fixes, 6 pontos aceitos e 39 m. Reset
 final zerou pontos aceitos e distância. Evidência mock não substitui percurso
real; `026-07` permanece desmarcada.

Comparação de duas atividades reais no mesmo estacionamento mostrou variância
visual entre execuções: primeira com 529 m, 414 fixes, 144 aceitos e 270
rejeitados; segunda com 544 m, 386 fixes, 145 aceitos e 241 rejeitados. Ambas
reportaram precisão de 5 m. A segunda apresenta zigue-zagues mais acentuados,
mas métricas agregadas não indicam degradação clara; a diferença é compatível
com multipercurso e variação do fused em ambiente de estacionamento.

Atualização visual da `026-07`: a rota usa linha azul com contraste sobre
`Positron`; o início fica verde, o fim fica vermelho após `Parar` e a posição
corrente fica laranja durante tracking. Fontes GeoJSON e camadas foram
separadas para evitar sobreposição semântica dos marcadores. Checklist permanece
desmarcado.

Diagnóstico debug para próximas tentativas foi adicionado em `026-07`: a UI
exibe todos os motivos de rejeição e buckets de precisão com zeros visíveis,
enquanto o coordinator conta precisão somente durante `captureActive` e zera as
contagens em `start()`/`clear()`. `git diff --check` e
`./gradlew app:compileDebugKotlin app:lintDebug app:assembleDebug` passaram. O
gate físico, casos negativos, Logcat sanitizado e `026-08` continuam bloqueados;
progresso permanece `6/8`.

Correção de câmera em `026-07`: primeiro posicionamento real observado com
`mock_location_app` nulo centralizou em zoom urbano `16.0`, igual à regra de
Recentralizar para zero/um ponto aceito. Com dois ou mais pontos, zoom manual
continua preservado. Builds completos, instalação debug e `git diff --check`
passaram; checklist permanece desmarcado.

Novas atividades reais com diagnóstico debug: atividade 3 registrou 538 m, 183
fixes, 122 aceitos e 61 rejeitados; atividade 4 registrou 557 m, 188 fixes, 122
aceitos e 66 rejeitados. Atividade 4 teve rejeições exclusivamente em
`Abaixo do piso de ruído`; atividade 3 teve 60 rejeições nessa categoria e uma
rejeição não classificada no card. Atividade 3 teve 2 fixes em `<=3 m`, 138 em `>3–5 m`
e 43 em `>5–10 m`; atividade 4 teve 0, 48 e 140 respectivamente. A segunda
execução confirma distribuição de precisão pior, sem evidência de velocidade
impossível, stale, duplicação ou erro de ordem. Evidências visuais foram
removidas após análise; somente métricas agregadas permanecem.

Consolidação: pan/zoom, follow e Recentralizar foram confirmados; follow só
retorna após Recentralizar, conforme esperado. Evidências visuais removidas
mostravam fit bounds após parada. Smoke de permissão negada mostrou `Permissão
necessária`; smoke de localização desligada exibiu aviso do Android e restaurou
modo `3`. Logcat mostrou solicitações Fused de alta precisão atribuídas à PoC,
sem telemetria da aplicação, coordenadas ou motivos de rejeição. Smoke de
localização aproximada mostrou `Precisão insuficiente` e bloqueou `Iniciar`;
networking isolado do pacote não derrubou mapa já cacheado; perda temporária e
stop precoce continuam sem classificação. Progresso permanece `6/8`.

Tentativa adicional sem atividade externa não conseguiu executar stop precoce ou
perda temporária: GPS real não atingiu readiness e fake GPS externo exigiu
configuração obrigatória, que não foi realizada. Nenhum resultado foi inventado;
aparelho foi restaurado. Progresso permanece `6/8`.

Nova tentativa encontrou fake GPS ativo com providers mock confirmados, mas o
joystick não produziu fix mock novo via input ADB; `Iniciar` permaneceu
desabilitado. Stop precoce e perda temporária continuam sem classificação.

Após movimento manual do joystick, tracking mock ficou ativo; localização foi
desligada por aproximadamente 5 s e restaurada sem crash, com contadores
retomando após recuperação. Esse smoke confirma resiliência básica, mas não
classifica explicitamente o estado `GPS perdido` nem cobre stop precoce ou
segundo ciclo. Evidência permanece mock/debug; progresso `6/8`.

Segundo ciclo mock foi executado sem reiniciar Activity: `Limpar` → `Preparar`
→ `Iniciar` → `Parar`; diagnóstico registrou 29 fixes `<=3 m` e 2 em `>3–5 m`,
sem contaminação. Novo `Limpar` zerou buckets. Stop precoce imediato terminou
com zero buckets e sem crash. Evidência cobre isolamento/robustez mock, não fecha
gate GPS real; progresso permanece `6/8`.

Tentativa de rede após limpeza de cache excedeu timeout do shell sem estado UI
classificável; networking, localização e permissões foram restaurados. Rede
indisponível permanece inconclusiva; `026-07` não deve ser fechada ainda.

Pausa do joystick durante tracking mock manteve distância em 60 m e pontos
aceitos em 9, mas fixes subiram de 29 para 42 e rejeitados de 20 para 33 em 10 s;
UI não mostrou `GPS perdido`/`fraco`. Rota ficou contida, porém caso negativo não
foi aprovado porque Fused continuou emitindo callbacks mock/stale.

Após limpeza manual de cache, rede do pacote foi bloqueada e Activity reiniciada;
UI permaneceu `Mapa-base: Pronto`, sem crash. Networking foi restaurado. Resultado
prova estabilidade offline, mas não prova falha visual do mapa por possível cache
remanescente; requisito de erro de mapa continua inconclusivo.

Confirmação do operador: atividades 3 e 4 usaram GPS real, sem fake GPS; fit
bounds foi considerado aprovado nas capturas pós-parada. A divergência de uma
rejeição na atividade 3 foi aceita como resíduo de observabilidade, sem alterar
filtro ou suavização. Prints foram removidos após análise.

Decisão: não repetir atividade 3. O percurso e diagnóstico permanecem evidência
útil; atividade 4 no mesmo contexto confirma a hipótese de ruído. Próximo passo:
smoke de casos negativos restantes; progresso permanece `6/8`.

Correção de câmera da `026-07` centralizou em helper explícito a regra de zoom:
zero ou um ponto aceito usa exatamente `URBAN_ACTIVITY_ZOOM` no primeiro
posicionamento, follow, Recentralizar e ponto único após parada; com dois ou mais
pontos, o zoom manual atual recebe somente o piso de `forLocation()`. `git diff
--check` e `./gradlew app:compileDebugKotlin app:lintDebug` passaram. O gate
físico, casos negativos, Logcat sanitizado e `026-08` continuam bloqueados;
progresso permanece `6/8`.

Repetição de localização desligada com ADB reconectado confirmou
`ProviderRequest[OFF]` sem crash; dump sanitizado mostrou `GPS: Localização
desativada` e `PoC: GPS perdido`, com 37 fixes e 457 m. Localização aguarda
restauração operacional; progresso `6/8`.

Smoke debug controlado de mapa executado no aparelho: `Simular erro do mapa`
mostrou `Mapa-base: Indisponível` sem converter GPS/PoC em erro; `Restaurar mapa`
restaurou sinteticamente somente `mapStatus`; GPS/PoC permaneceram
independentes, sem alteração de rota ou métricas. Dump posterior observou
GPS/PoC `Pronto`, sem atribuir essa transição ao restore sintético.
Isolamento mapa/GPS aprovado;
falha real de rede continua limitada por cache. Progresso permanece `6/8`.

Regressão `Simular erro do mapa` → `Limpar` passou no `SM-S901E`: botão voltou a
`Simular erro do mapa`, sem estado preso ou restauração sintética disponível para
erro real. Progresso permanece `6/8`.

`026-07` concluída com aceitação explícita da limitação de rede: mapa permaneceu
`Pronto` offline por cache, enquanto smoke controlado confirmou isolamento entre
erro de mapa e GPS. GPS real, câmera, rota, reset, ciclos e casos negativos
principais foram validados; rejeição não classificada foi aceita como resíduo de
observabilidade. Progresso atualizado para `7/8`. `026-08` é única subtarefa
pendente e não foi iniciada automaticamente.

### Fechamento final — 2026-08-21

`026-08` concluída. A Spec 026 está `completed` e classificada como
`go_with_adjustments`: o conceito foi validado no `SM-S901E`, sem bloqueio
estrutural, mas provider/cache e observabilidade exigem ajustes e revalidação.
As evidências sanitizadas registram GPS real em duas atividades de
aproximadamente 538 m e 557 m, 122 pontos aceitos em cada, rota visual, câmera,
stop, fit bounds, reset, segundo ciclo, casos negativos principais e isolamento
entre mapa e GPS. Crescimento visual foi observado, mas não houve medição final
explícita de jank após as correções de responsividade.

A falha real do mapa em cold-cache permaneceu inconclusiva porque o mapa ficou
`Pronto` offline, provavelmente por cache. Uma rejeição sem categoria na
atividade 3 foi aceita como resíduo de observabilidade. Nenhuma coordenada ou
dado sensível foi preservado em Logcat/evidências.

O resultado não reabre a Fase 013, não altera `PhoneWorkoutPolicy` e não
autoriza produção. Qualquer efetivação exige nova spec e decisão explícita.

## Ordem

```text
026-01 → 026-02 → 026-03 → 026-04 → 026-05 → 026-06 → 026-07 → 026-08
```

## Gate

O gate exige validação física de um percurso externo de aproximadamente 500 m ou mais, observando:

- mapa-base carregado;
- atribuição preservada;
- GPS pronto;
- rota crescendo somente com pontos aceitos;
- pan/zoom manual;
- recentralização;
- stop com rota preservada em memória;
- enquadramento da rota final;
- reset;
- segundo ciclo sem contaminação do primeiro.

Resultado final obrigatório:

```text
go
ou
go_with_adjustments
ou
no_go
```

## Guard rails

- não reabrir Fase 013;
- não liberar `PhoneWorkoutPolicy`;
- não usar `WorkoutRecordingService`;
- não adicionar Room/migration;
- não usar Health Connect;
- não usar BLE;
- permitir CTA somente técnico em Exercícios no debug, sem tratá-lo como treino;
- não adicionar CTA em Home, Saúde, Dispositivo ou Configurações;
- não expor botão, `Intent` executável ou entrypoint da PoC em release;
- não registrar coordenadas;
- não criar credenciais de mapa;
- não remover atribuição do mapa;
- não criar ou alterar testes automatizados sem pedido explícito do usuário.
