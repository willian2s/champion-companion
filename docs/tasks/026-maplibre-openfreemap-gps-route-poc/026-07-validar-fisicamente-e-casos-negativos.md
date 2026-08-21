# 026-07 — Validar fisicamente e executar casos negativos

- **Ticker:** `026`
- **Número:** `07`
- **Status:** completed

## Objetivo

Executar o gate físico da PoC em aparelho Android real, incluindo percurso externo, interação com mapa e cenários negativos mínimos, com evidência sanitizada.

## Resultado esperado

Existe evidência suficiente para classificar tecnicamente a PoC sem usar
emulador ou dados simulados como prova do gate, e sem screenshots contendo
localização pessoal.

O modo de geometria debug-only pode aceitar localização mock/fake exclusivamente
para inspeção visual de geometria, câmera, stop e reset. Esse modo não encerra o
gate: validação de percurso real, critério de GPS real e classificação final
continuam pendentes.

## Requisitos e critérios cobertos

- Spec 026: critérios 18 a 35;
- percurso real;
- responsividade do mapa;
- qualidade visual da rota;
- isolamento entre ciclos;
- cenários negativos de rede e GPS;
- privacidade da evidência.

## Pré-condições

```text
aparelho Android real disponível para teste externo
build debug instalado
rede disponível para cenário principal
localização habilitada
permissão precisa disponível
ambiente externo adequado para GPS
```

O cenário de rede indisponível deve ser exercitado separadamente.

## Cenário principal

1. Instalar build debug.
2. Abrir a página de Exercícios no build debug e tocar o CTA técnico da PoC.
3. Confirmar mapa OpenFreeMap carregado.
4. Confirmar atribuição visível.
5. Conceder localização precisa quando solicitada.
6. Aguardar GPS `READY` com qualidade elegível.
7. Iniciar tracking.
8. Percorrer trajeto externo de aproximadamente 500 m ou mais.
9. Observar posição progredindo.
10. Confirmar rota crescendo somente com pontos aceitos.
11. Fazer pan/zoom manual.
12. Confirmar que follow não briga com gesto.
13. Acionar `Recentralizar`.
14. Continuar tracking.
15. Parar.
16. Confirmar que novos fixes não alteram a rota final.
17. Confirmar fit bounds da rota.
18. Limpar.
19. Iniciar segundo ciclo sem reiniciar o app.
20. Confirmar ausência de estado do primeiro ciclo.

## Observações técnicas mínimas

Registrar de forma sanitizada:

- aparelho/modelo geral e versão Android;
- versão MapLibre Compose usada;
- style OpenFreeMap usado;
- resultado de carregamento do mapa-base;
- categoria de qualidade GPS observada;
- duração aproximada do teste;
- distância agregada aproximada;
- contagem de fixes/aceitos/rejeitados, se útil;
- percepção de fluidez/jank;
- comportamento de pan/zoom/follow;
- resultado do stop/reset;
- categorias de erro observadas.

Nunca registrar coordenadas, endereço, screenshot de rota pessoal ou GeoJSON.

## Casos negativos mínimos

### Rede indisponível

- abrir PoC sem conectividade;
- verificar que falha do mapa-base não vira erro de GPS;
- app não deve crashar;
- comportamento ao restaurar rede deve ser observado sem inventar requisito de retry automático se a biblioteca não o fornecer.

### Permissão negada

- negar localização;
- `Iniciar` permanece indisponível;
- retry explícito continua possível.

### Localização aproximada

- conceder apenas coarse quando o Android permitir;
- tracking não inicia;
- UI informa necessidade de localização precisa.

### GPS desligado

- desabilitar localização;
- estado próprio é apresentado;
- nenhum tracking inicia.

### GPS procurando

- abrir em condição sem fix elegível;
- UI permanece estável em searching/weak.

### Perda temporária de qualidade

- durante tracking, observar comportamento quando readiness deixa de ser ideal;
- não fabricar segmento ou ponto inválido;
- qualquer decisão efetiva deve ser registrada para futura spec.

### Stop precoce

- parar com zero ou um ponto aceito;
- não crashar;
- câmera permanece em estado válido.

### Dois ciclos

- executar start/stop/reset/start sem reiniciar app;
- não reutilizar rota, contadores ou geração anteriores.

### Localização mock fora do modo de geometria

- o modo `GpsMapPocMockGeometryDebugMode` é a única exceção debug-only;
- fora dessa fronteira, o mock continua rejeitado por readiness/filtro
  compartilhados;
- nenhuma aceitação mock autoriza percurso real, produto, release ou fechamento
  do gate.

## Evidência visual

Screenshot versionado deve usar estado neutro ou mapa sem localização pessoal identificável.

Não versionar:

- rota real;
- região residencial;
- coordenadas;
- barra de status com informação sensível;
- logs contendo localização.

## Validação de logs

Inspecionar Logcat procurando exposição indevida de:

- latitude/longitude;
- altitude;
- GeoJSON;
- route/bounds serializados;
- URLs contendo localização.

Falha de privacidade bloqueia o gate.

## Comandos

```bash
git diff --check
./gradlew lint assembleDebug assembleRelease
./gradlew installDebug
```

Comandos ADB podem ser usados opcionalmente para instalar, diagnosticar ou abrir
a Activity como fallback, preservando evidência sanitizada. O gate deve exercitar
o CTA de Exercícios, pois ADB sozinho não comprova o requisito de acesso externo.

## Definition of Done

- cenário principal executado em aparelho real;
- percurso real >= aproximadamente 500 m concluído ou limitação física explicitamente registrada sem declarar sucesso;
- modo mock, quando usado, identificado explicitamente como geometria debug e
  nunca apresentado como GPS real;
- mapa, rota e câmera avaliados;
- casos negativos mínimos exercitados;
- dois ciclos consecutivos exercitados;
- Logcat não expõe localização;
- screenshot/evidência não expõe local pessoal;
- achados estão registrados para `026-08`;
- nenhuma classificação final é inventada antes da análise dos resultados.

## Riscos e cuidados

- indisponibilidade do OpenFreeMap durante teste deve ser distinguida de defeito do MapLibre ou GPS;
- trajeto pessoal é dado sensível; evidência textual deve permanecer agregada;
- qualidade GPS varia com ambiente; registrar contexto sem afrouxar filtro durante o gate apenas para obter aprovação.

## Diagnóstico debug para próximas tentativas — 2026-08-21

- **Status:** `in_progress`; o gate físico continua bloqueado e o checklist não
  foi marcado.
- **Implementação:** `GpsMapPocUiState` agora expõe buckets debug de precisão;
  `GpsMapPocCoordinator` zera os buckets em `start()`/`clear()` e conta somente
  fixes recebidos durante `captureActive`; `GpsMapPocScreen` mostra todos os
  motivos de rejeição e todos os buckets, incluindo zeros, em ordem determinística.
- **Privacidade:** diagnóstico não exibe latitude, longitude nem precisão exata
  por fix; somente categorias e contagens agregadas.
- **Validação executada:** `git diff --check` e
  `./gradlew app:compileDebugKotlin app:lintDebug app:assembleDebug` passaram.
  Nenhum teste automatizado foi criado ou executado.
- **Riscos residuais:** ainda falta repetir percurso real e casos negativos com a
  nova evidência, inspecionar Logcat sanitizado e concluir avaliação de câmera;
  `026-08` permanece bloqueada.
- **Smoke de instalação após instrumentação:** `./gradlew lint assembleDebug
  assembleRelease` e `./gradlew installDebug` passaram. `mock_location_app`
  retornou `null` no `SM-S901E`; Activity abriu em ambiente de localização real.
  Dump UI sanitizado confirmou as onze categorias de rejeição e seis buckets de
  precisão, todos zerados antes de captura, sem coordenadas.

## Execução parcial

- **Status atual:** in_progress; smoke físico fused parcial realizado, mas gate completo ainda bloqueado pelos cenários não exercitados.
- **Arquivos alterados:** `app/src/debug/java/br/com/mwconsulting/championcompanion/feature/gpspoc/GpsMapPocLocationSource.kt`, `GpsMapPocCoordinator.kt`, `GpsMapPocUiState.kt` e `GpsMapPocScreen.kt`.
- **Achados físicos reportados:** permissão concedida permanecia apresentada como necessária; readiness demorava apesar de precisão e fixes; `Iniciar` perdia o sinal; câmera não centralizava e `Recentralizar` podia não produzir efeito; navegação apresentava travamentos e disputa com rolagem.
- **Decisões e desvios:** o runtime de preparação agora permanece ativo no início da captura, isolando ciclos por `routeId`, sequência e fronteira temporal de captura; candidatos anteriores ao toque são descartados da rota. Snapshot de eventos GPS tornou-se replayável; último fix recente pode aquecer readiness sem entrar automaticamente na rota. O plano inicial usava `LocationManager.GPS_PROVIDER`; uma tentativa intermediária registrou `GPS_PROVIDER` e `NETWORK_PROVIDER` em paralelo, mas a fonte atual usa `FusedLocationProviderClient` para combinar GPS, rede móvel, Wi-Fi, cache e sensores disponíveis. A solicitação fused usa alta precisão, intervalo de 1.000 ms, intervalo mínimo coerente e distância mínima de 0 m; `lastLocation` recente e válido aquece readiness/posição sem entrar automaticamente na rota. Posição corrente efêmera foi separada de `routePoints`, com validação de coordenadas, idade, precisão, velocidade, bearing e ordem temporal; posição expirada é removida. Recentralização exige posição disponível, atualização GeoJSON tornou-se assíncrona e mapa foi separado da rolagem vertical dos cards.
- **Comandos executados:** `git diff --check`; `./gradlew app:compileDebugKotlin`; `./gradlew app:lintDebug`; `./gradlew app:assembleDebug`; comandos anteriores `./gradlew lint assembleDebug assembleRelease`, `./gradlew installDebug`, `adb shell am start -n br.com.mwconsulting.championcompanion.dev/br.com.mwconsulting.championcompanion.feature.gpspoc.GpsMapPocActivity` e `adb shell dumpsys activity activities`.
- **Resultados e evidências:** `git diff --check`, compilação Kotlin debug, lint debug e assemble debug concluíram com sucesso. Evidência física anterior permanece no `SM-S901E` com Android API 36 e Activity em estado `resumed`. Nenhum teste automatizado foi criado. A revisão independente aprovou a correção estrutural para continuar validação, sem classificar o gate como aprovado.
- **Riscos residuais e bloqueio:** o smoke fused exercitou stop/reset e segundo ciclo, mas ainda não substitui percurso externo de aproximadamente 500 m, avaliação completa de pan/zoom, Recentralizar e fit bounds, casos negativos e inspeção sanitizada do Logcat. `026-07` não pode ser marcada como concluída nem permitir fechamento `026-08` antes dessa evidência.
- **Ajuste operacional posterior:** a fonte debug foi migrada de registros paralelos em `GPS_PROVIDER`/`NETWORK_PROVIDER` para `FusedLocationProviderClient`, com `LocationRequest` de alta precisão a 1.000 ms, intervalo mínimo coerente, distância mínima de 0 m e callbacks entregues no handler principal. `lastLocation` recente e válido aquece readiness e posição corrente, sem entrar automaticamente na rota. Falhas de `Task`, `SecurityException` e API não encerram o processo; serviço/readiness são atualizados e o tick tenta novamente. Callback fused é removido em stop, clear e close; GNSS ficou `null` sem callback obrigatório.
- **Build após migração:** `git diff --check`; `./gradlew app:compileDebugKotlin app:lintDebug app:assembleDebug`; `./gradlew lint assembleDebug assembleRelease`; `./gradlew app:dependencyInsight --configuration releaseRuntimeClasspath --dependency play-services-location` concluíram com sucesso. `play-services-location:21.3.0` ficou somente em `debugImplementation`; dependency insight não encontrou a biblioteca no runtime release.
- **Smoke físico após migração fused:** no `SM-S901E`, após tocar `Preparar GPS`, a UI mostrou `GPS/PoC Pronto`, precisão de 7 m e 18 fixes. Após iniciar, mostrou `PoC Capturando`, precisão de 6 m, 29 fixes e 1 ponto aceito. Stop/reset funcionaram; o segundo ciclo voltou a `Capturando` sem contaminação. Não houve percurso externo de aproximadamente 500 m, casos negativos ou inspeção sanitizada do Logcat.
- **Status preservado:** `in_progress`; o gate completo do fused continua bloqueado por percurso, casos negativos e Logcat sanitizado não exercitados. O smoke parcial de dois ciclos não foi classificado como aprovação.

## Execução da mudança de geometria debug — 2026-08-20

- **Status:** `in_progress`; a mudança foi implementada sem marcar a subtarefa ou
  o gate como concluídos.
- **Arquivos alterados:** `GpsMapPocMockGeometryDebugMode.kt`,
  `GpsMapPocLocationSource.kt`, `GpsMapPocCoordinator.kt`,
  `GpsMapPocScreen.kt`, esta subtarefa, a Spec 026, ADR-024 e o overview 026.
- **Decisão:** o objeto `GpsMapPocMockGeometryDebugMode`, restrito a
  `app/src/debug`, aceita mock somente para geometria/câmera/stop/reset. A fonte
  continua exigindo permissão precisa, localização habilitada e validações
  existentes. Mock válido pode alimentar readiness, posição e seed recente;
  `lastLocation` continua fora da rota antes de `Iniciar`.
- **Desvio controlado:** readiness recebe mock como admissível somente nessa
  fronteira debug; o coordinator libera posição mock com as mesmas validações e
  adapta apenas o candidato mock para o `RoutePointFilter` compartilhado. Nenhum
  contrato `main`, filtro produtivo, `PhoneWorkoutPolicy`, persistência ou log foi
  alterado. A origem mock não é mascarada em UI/documentação.
- **UI/evidência:** a tela exibe `Modo geometria debug: mock aceito`, informa que
  o resultado não comprova GPS real e mantém explícita a exigência de permissão
  precisa/localização habilitada. Não foi adicionado app fake GPS, integração ou
  credencial.
- **Comandos e resultados:** `git diff --check` passou. A primeira execução de
  `./gradlew app:compileDebugKotlin app:lintDebug` excedeu o timeout operacional
  de 120 s durante o lint; a repetição com timeout de 300 s passou. Depois,
  `./gradlew lint assembleDebug assembleRelease` passou. O
  `./gradlew app:dependencyInsight --configuration releaseRuntimeClasspath
  --dependency play-services-location` passou sem encontrar a dependência no
  runtime release. `./gradlew installDebug` passou; no `SM-S901E` com Android
  API 36, a Activity debug abriu e a inspeção UI mostrou mapa-base `Pronto`, o
  banner de mock e as exigências explícitas. A verificação do APK release não
  encontrou Activity, modo ou marcador de mock; `app/src/main` e
  `app/src/release` não contêm o objeto. Nenhum teste automatizado foi criado ou
  executado, conforme política do projeto.
- **Riscos residuais:** o modo permite inspeção visual, mas não substitui
  percurso externo real nem os casos negativos de permissão, rede, GPS e
  Logcat. O gate de GPS real e a decisão para `026-08` continuam bloqueados.
- **Verificação final após alinhamento documental:** `git diff --check` passou;
  `./gradlew lint assembleDebug assembleRelease` passou;
  `./gradlew app:dependencyInsight --configuration releaseRuntimeClasspath
  --dependency play-services-location` passou sem dependência fused no runtime
  release; `./gradlew installDebug` instalou no `SM-S901E`. Nenhum teste
  automatizado foi criado ou executado.

## Ajuste visual da rota — 2026-08-20

- **Status:** `in_progress`; checklist permanece desmarcado.
- **Arquivos alterados:** `GpsMapPocScreen.kt`, esta subtarefa, Spec 026 e
  overview 026.
- **Decisão:** separar fontes GeoJSON e camadas para linha, início, fim e
  posição corrente. Linha azul (`#1565C0`), início verde (`#2E7D32`), fim
  vermelho (`#C62828`) após `Parar` e posição corrente laranja (`#FF8F00`)
  durante tracking, todos com contorno branco.
- **Motivo:** a cor derivada de `MaterialTheme.colorScheme.primary` aparecia
  branca no mapa e não diferenciava marcadores. Cores explícitas garantem
  contraste visual sobre `Positron` e semântica visual distinta.
- **Validação:** a primeira compilação falhou por ausência de overload de
  GeoJSON para `WorkoutRoutePoint`; overload foi adicionado. Em seguida,
  `./gradlew app:compileDebugKotlin app:lintDebug app:assembleDebug` e
  `git diff --check` passaram. Nenhum teste automatizado foi criado ou
  executado.
- **Risco residual:** validação visual em aparelho com rota mock ainda precisa
  confirmar contraste e marcadores no mapa; gate de percurso real, casos
  negativos e Logcat sanitizado continuam pendentes.
- **Verificação final:** `./gradlew lint assembleDebug assembleRelease`,
  `git diff --check` e `./gradlew installDebug` passaram; APK debug atualizado
  foi instalado no `SM-S901E`.

## Validação física da geometria com mock — 2026-08-20

- **Dispositivo:** `SM-S901E`, Android API 36, APK debug atualizado.
- **Fonte:** app externo de localização mock ativo; localização permaneceu
  somente como entrada efêmera da PoC. Nenhuma coordenada foi registrada.
- **Preparação:** mapa-base carregou como `Pronto`; UI mostrou modo geometria
  debug; precisão observada foi 1 m e fixes mock chegaram ao fused.
- **Primeiro ciclo:** `Preparar GPS` → `Iniciar` → `Parar`; 8 fixes, 8 pontos
  aceitos, 54 m, 0 rejeitados. Screenshot temporário confirmou linha azul,
  marcador inicial verde e marcador final vermelho com contorno branco.
- **Segundo ciclo:** `Limpar` zerou a rota antes do novo `Iniciar`; ciclo
  registrou 6 fixes, 6 pontos aceitos, 39 m e 0 rejeitados. Screenshot
  temporário confirmou mesmas cores e ausência de contaminação visual.
- **Reset final:** `Limpar` zerou pontos aceitos e distância; estado permaneceu
  utilizável para novo ciclo.
- **Evidência:** dumps UI sanitizados confirmaram métricas agregadas e ciclo de
  vida. Screenshots ficaram fora do repositório porque exibem controles do app
  fake GPS; não contêm coordenadas legíveis, mas não são evidência do gate real.
- **Resultado:** inspeção visual mock aprovada para geometria/câmera/stop/reset;
  não classifica GPS real nem encerra `026-07`.
- **Riscos residuais:** percurso externo real de aproximadamente 500 m, pan/zoom
  completo, Recentralizar, fit bounds, casos negativos e Logcat sanitizado ainda
  pendentes. Checklist permanece `[ ]` e overview permanece `6/8`.

## Comparação de duas atividades reais no mesmo estacionamento — 2026-08-21

- **Evidências recebidas:** `026-estacionamento-1-1-rota.jpeg`,
  `026-estacionamento-1-2-metrica.jpeg`, `026-estacionamento-1-3-limpo.jpeg`,
  `026-estacionamento-2-1-rota.jpeg`, `026-estacionamento-2-2-metrica.jpeg` e
  `026-estacionamento-2-3-limpo.jpeg`.
- **Atividade 1:** precisão exibida de 5 m; 414 fixes; 529 m; 144 pontos
  aceitos; 270 rejeitados.
- **Atividade 2:** precisão exibida de 5 m; 386 fixes; 544 m; 145 pontos
  aceitos; 241 rejeitados.
- **Achado visual:** a segunda rota mostra zigue-zagues e excursões mais
  abruptas, sobretudo no trecho final. A primeira também possui duas linhas
  paralelas e laços, porém com variação visual mais suave.
- **Interpretação:** métricas não mostram piora clara na segunda execução:
  pontos aceitos são praticamente iguais, distância varia 15 m (aprox. 2,8%)
  e a proporção de rejeição cai de aproximadamente 65,2% para 62,4%. Com
  precisão arredondada de 5 m e `minimumDisplacementMeters` de 3 m, ruído de
  posição pode superar o piso e entrar como ponto aceito. Fused também pode
  alternar contribuições de GPS, Wi-Fi e rede móvel; estacionamento favorece
  multipercurso. Hipótese ainda não é prova causal.
- **Limitação da evidência:** card agregado não informa motivo de cada rejeição,
  fonte do fix ou precisão não arredondada. Não há base suficiente para
  concluir bug de ciclo, duplicação ou regressão do filtro.
- **Decisão:** não alterar filtro ou suavização nesta etapa. Próxima medição
  deve separar motivos de rejeição e registrar distribuição de precisão por
  fix, sem coordenadas, antes de escolher piso maior, suavização ou política
  específica da PoC.
- **Status:** `in_progress`; checklist permanece `[ ]`, overview permanece
  `6/8`. Evidência real melhora diagnóstico, mas não fecha casos negativos,
  Logcat sanitizado nem validação completa de câmera.

## Auditoria do Logcat das tentativas — 2026-08-21

- **Comando:** `adb logcat -d -v threadtime | rg -i
  'GpsMapPoc|RoutePoint|gpspoc|fused|location'`.
- **Resultado:** buffer disponível não contém telemetria da PoC com motivo de
  rejeição, precisão por fix ou identificação das atividades. Há mensagens de
  componentes de localização do sistema/Samsung/Fused, mas não existe vínculo
  confiável com pontos aceitos ou rejeitados da PoC.
- **Conclusão:** dados das tentativas anteriores não podem ser reconstruídos a
  partir do Logcat atual. O buffer pode ter sido rotacionado e a PoC não grava
  essas métricas detalhadas. Não foi extraído nem registrado Logcat bruto para
  preservar privacidade.

## Correção de zoom do primeiro posicionamento — 2026-08-21

- **Status:** `in_progress`; correção de câmera validada em código, sem fechar o
  gate físico.
- **Implementação:** `GpsMapPocScreen` centraliza a regra em
  `cameraZoomForAcceptedPointCount`. Com zero ou um ponto aceito, follow,
  Recentralizar e o caso de ponto único após `Stopped` usam exatamente
  `URBAN_ACTIVITY_ZOOM`; com dois ou mais pontos, o zoom manual atual passa por
  `forLocation()` somente para aplicar o piso mínimo.
- **Escopo:** alteração restrita ao source set debug e ao arquivo da tela; não
  altera pan/zoom manual, suspensão de follow, fit bounds final ou constantes.
- **Validação:** `git diff --check` e
  `./gradlew app:compileDebugKotlin app:lintDebug` passaram. Nenhum teste
  automatizado foi criado ou executado.
- **Bloqueios:** ainda faltam repetir percurso externo, casos negativos,
  inspeção sanitizada do Logcat e confirmação física da centralização inicial;
  `026-08` permanece bloqueada.
- **Smoke físico inicial:** após `./gradlew lint assembleDebug assembleRelease`,
  `./gradlew installDebug` e abertura da Activity, `mock_location_app` retornou
  `null`. A primeira posição observada no mapa real apareceu com zoom urbano
  próximo de `16.0`, coerente com Recentralizar para zero/um ponto aceito. A
  captura visual temporária não foi versionada por conter localização pessoal e
  overlay externo; não substitui validação completa de follow/recentralização.

## Atividades reais com diagnóstico debug — 2026-08-21

- **Evidências recebidas:** quatro prints das atividades 3 e 4. Prints foram
  analisados e removidos do diretório por conterem mapa/local identificável;
  somente métricas agregadas permanecem registradas aqui.
- **Atividade 3:** `PoC Parado`; precisão exibida de 5 m; 183 fixes; 538 m; 122
  pontos aceitos; 61 rejeitados. Diagnóstico: 60 `Abaixo do piso de ruído` e
  uma rejeição não classificada no card; demais motivos zero. Buckets: 2 em
  `<=3 m`, 138 em `>3–5 m`, 43 em `>5–10 m`,
  zero acima de 10 m e inválidos.
- **Atividade 4:** `PoC Parado`; precisão exibida de 5 m; 188 fixes; 557 m; 122
  pontos aceitos; 66 rejeitados. Diagnóstico: 66 `Abaixo do piso de ruído`,
  demais motivos zero. Buckets: 0 em `<=3 m`, 48 em `>3–5 m`, 140 em `>5–10 m`,
  zero acima de 10 m e inválidos.
- **Achado:** atividade 4 teve concentração muito maior de fixes entre 5 e 10 m
  (140 contra 43), enquanto atividade 3 concentrou fixes entre 3 e 5 m (138
  contra 48). Isso sustenta maior incerteza/variância espacial na atividade 4.
- **Filtro:** atividade 4 teve rejeições exclusivamente abaixo do piso de ruído;
  atividade 3 deixou uma rejeição sem categoria visível. Não houve evidência de
  velocidade impossível, fix stale, duplicação, ordem temporal, coordenada
  inválida ou localização mock nas categorias exibidas.
- **Decisão:** não alterar filtro ou suavização. Ambas as rotas tiveram 122
  pontos aceitos; a diferença de uma rejeição sem categoria na atividade 3 é
  aprovada como resíduo de observabilidade e não exige repetição da atividade.
- **Reset:** prints removidos mostravam `Distância 0 m`, `Pontos aceitos 0` e
  contadores de diagnóstico zerados após limpeza; posição corrente permanecia
  disponível para novo ciclo.
- **Status:** `in_progress`; checklist permanece `[ ]`, overview permanece
  `6/8`. Gate real, casos negativos e Logcat sanitizado continuam pendentes.

## Consolidação de evidências e smoke ADB — 2026-08-21

- **Câmera:** usuário confirmou pan/zoom manual, follow e `Recentralizar`.
  Follow não retorna automaticamente após gesto; retorna somente por
  `Recentralizar`, comportamento esperado. Os prints 3 e 4, removidos após
  análise, mostravam rota completa enquadrada com margem após `Parar`, evidência
  de fit bounds.
- **Logcat:** auditoria do buffer atual encontrou registros de sistema/Fused com
  solicitações `HIGH_ACCURACY` de 1 s atribuídas ao pacote da PoC, mas nenhum
  log da aplicação com motivos de rejeição, precisão individual, coordenadas,
  GeoJSON ou rota serializada. Logs brutos não foram preservados.
- **Permissão negada:** `pm revoke` de localização precisa e aproximada seguido
  de abertura da Activity mostrou `Permissão necessária` e instrução de
  localização precisa; `Iniciar` não foi habilitado.
- **Localização desligada:** smoke via `settings put secure location_mode 0`
  exibiu aviso do Android de localização desativada; localização foi restaurada
  para modo `3` ao final. A camada específica da Activity não foi classificada
  por esse comando devido ao aviso do sistema.
- **Localização aproximada:** `appops` deixou somente coarse ativo; após
  `Preparar GPS`, UI mostrou `Precisão insuficiente` e `Iniciar` permaneceu
  desabilitado. Fine/coarse foram restaurados ao final.
- **Rede indisponível:** networking foi desabilitado somente para o pacote da
  PoC e restaurado depois. Mapa permaneceu `Pronto`, provavelmente por cache;
  resultado inconclusivo, sem classificar falha de mapa. Tentativa de broadcast
  de modo avião foi bloqueada por `SecurityException` do shell.
- **Perda temporária e stop precoce:** não classificados nesta rodada; o smoke
  encerrou sem iniciar captura quando readiness não estava elegível.
- **Fit bounds:** significa câmera ajustar centro e zoom para conter todos os
  pontos aceitos após `Parar`, sem cortar início/fim; comportamento aparece nas
  duas capturas completas, mas ainda não foi testado com pan/zoom imediatamente
  anterior à parada.
- **Status:** evidência existente cobre cenário real, diagnóstico, reset,
  câmera principal e permissão negada. Checklist permanece `[ ]`; overview
  permanece `6/8` até rede, perda temporária, stop precoce, segundo ciclo e
  Logcat sanitizado serem resolvidos.
- **Confirmação do operador:** atividades 3 e 4 foram executadas com GPS real,
  sem fake GPS; fit bounds foi considerado aprovado nas capturas completas após
  `Parar`. Essa confirmação operacional complementa a evidência disponível; a
  divergência de uma rejeição foi aceita como resíduo de observabilidade.

## Decisão sobre repetição da atividade 3 — 2026-08-21

- **Decisão:** não repetir atividade real 3. A execução já fornece evidência
  válida de percurso, rota, métricas, distribuição de precisão e reset; atividade
  4 percorreu mesmo contexto, apresentou 122 pontos aceitos e confirmou a mesma
  hipótese de ruído.
- **Resíduo aceito:** a diferença `61 rejeitados` contra `60` razões na atividade
  3 é tratada como falha de observabilidade do diagnóstico, não como falha da
  rota ou motivo para descartar atividade. Não ajustar filtro/suavização por esse
  único desvio.
- **Próximo gate:** seguir com smoke de casos negativos e registrar divergência
  como risco residual. `026-07` só fecha após critérios restantes; overview segue
  `6/8`.

## Tentativa de smoke adicional sem atividade externa — 2026-08-21

- **Preparação:** permissões fine/coarse, localização mode `3`, networking do
  pacote habilitado e `mock_location_app` limpo ao final.
- **Stop precoce/perda temporária:** não executáveis com GPS real parado no
  ambiente atual; após `Preparar GPS`, readiness não ficou elegível e `Iniciar`
  permaneceu desabilitado. Não simular resultado.
- **Mock auxiliar:** app fake GPS externo abriu somente instrução de configuração
  obrigatória; não foi configurado nem usado para classificar casos de GPS real.
- **Conclusão:** tentativa confirma limitação operacional, não comportamento
  aprovado. Stop precoce e perda temporária permanecem pendentes; aparelho foi
  restaurado para estado normal.

## Tentativa com fake GPS e joystick — 2026-08-21

- **Estado:** app fake GPS ativo, mock provider selecionado e serviço em execução;
  `dumpsys location` confirmou providers mock `gps`, `network` e `fused`.
- **Ação:** PoC foi trazida para frente; `Preparar GPS` foi acionado; joystick
  recebeu tentativa de deslocamento via input ADB.
- **Resultado:** provider manteve último fix mock antigo, sem novo tempo de
  atualização; `Iniciar` permaneceu desabilitado. Stop precoce e perda
  temporária não foram executados.
- **Conclusão:** não classificar como aprovação nem falha do fluxo. A posição
  mock precisa ser movida pelo controle efetivo do app fake antes de a PoC
  receber fix elegível. Estado final preservado com localização habilitada,
  rede habilitada e `airplane_mode_on=0`.

## Smoke com joystick ativo — 2026-08-21

- **Tracking mock:** após movimento manual do joystick, PoC ficou com `Parar`
  habilitado e `Iniciar` desabilitado; diagnóstico registrou 27 fixes no bucket
  `<=3 m`, depois 31 antes da interrupção.
- **Perda temporária:** localização foi desligada por aproximadamente 5 s durante
  tracking e restaurada para mode `3`. Não houve crash nem perda de controle do
  ciclo; após restauração, bucket chegou a 34 e `Parar` continuou disponível.
  A UI visível não expôs classificação explícita de `GPS perdido`, portanto o
  resultado é smoke de resiliência, não aprovação completa do estado transitório.
- **Parada:** tracking mock foi encerrado após recuperação. Stop precoce sem
  percurso e segundo ciclo limpo não foram confirmados nesta rodada.
- **Origem:** evidência é exclusivamente mock/debug; não conta para gate GPS real.

## Segundo ciclo e stop precoce com joystick — 2026-08-21

- **Segundo ciclo:** após `Limpar`, `Preparar GPS` e `Iniciar` sem reiniciar a
  Activity, tracking mock voltou a ficar ativo. Diagnóstico registrou 29 fixes
  em `<=3 m` e 2 em `>3–5 m`; demais buckets ficaram zerados. `Parar` encerrou
  ciclo sem reusar contadores do ciclo anterior.
- **Reset:** `Limpar` posterior zerou buckets e deixou `Iniciar` disponível para
  novo ciclo; nenhuma rota anterior foi reutilizada.
- **Stop precoce:** novo `Iniciar` seguido imediatamente de `Parar` terminou
  com todos os buckets zerados e sem crash, representando zero pontos aceitos.
- **Origem:** evidências são mock/debug; validam isolamento e robustez de ciclo,
  mas não substituem critérios equivalentes do percurso GPS real.
- **Rede após limpeza de cache:** tentativa de limpar cache do pacote, bloquear
  networking e reabrir PoC excedeu timeout do shell sem produzir estado UI
  classificável. Networking, localização e permissões foram restaurados; caso
  de rede permanece inconclusivo.

## Pausa do joystick durante tracking — 2026-08-21

- **Antes da pausa:** 29 fixes, 9 pontos aceitos, 20 rejeitados, 60 m de
  distância e precisão exibida de 1 m.
- **Após 10 s pausado:** 42 fixes, 9 pontos aceitos, 33 rejeitados, 60 m de
  distância e precisão exibida de 2 m.
- **Resultado:** rota não cresceu e nenhum ponto adicional foi aceito, mas Fused
  continuou entregando callbacks mock/stale; contagem de rejeições aumentou. UI
  não exibiu estado explícito `GPS perdido`/`fraco`. Não classificar como caso
  negativo aprovado; comportamento comprova apenas contenção da rota durante
  pausa do joystick.
- **Restauração:** joystick foi retomado via app fake e localização permaneceu
  habilitada; Activity PoC voltou ao primeiro plano.

## Tentativa de desligamento manual da localização — 2026-08-21

- **Ação:** operador desligou Localização pelos atalhos do Android durante
  tracking mock, para separar perda do provider de pausa do joystick.
- **Evidência:** ADB perdeu o dispositivo imediatamente após a ação; nenhum
  dump UI pós-desligamento foi possível. Não classificar estado `GPS perdido`
  nem recuperação.
- **Restauração pendente:** operador deve religar Localização e reconectar ADB
  antes de novo diagnóstico. Caso permanece inconclusivo.

## Repetição com ADB reconectado — 2026-08-21

- **Ação:** com tracking/mock ativo, Localização foi desligada e ADB permaneceu
  conectado na segunda tentativa.
- **UI:** dump sanitizado com viewport correto mostrou `Estado técnico`,
  `Mapa-base: Pronto`, `GPS: Localização desativada` e `PoC: GPS perdido`.
  Métricas visíveis: precisão não disponível, 37 fixes e 457 m; nenhum crash.
- **Sistema:** `dumpsys location` registrou `ProviderRequest[OFF]` após
  desligamento, confirmando interrupção do provider; nenhum crash foi observado.
- **Conclusão:** critério de infraestrutura e estado negativo da UI aprovados
  com evidência ADB sanitizada. O dump inicial anterior estava fora do viewport
  de status por rolagem insuficiente. Localização permanece desligada até
  restauração operacional após captura.

## Smoke controlado de erro do mapa — 2026-08-21

- **Implementação:** botão debug-only `Simular erro do mapa` altera somente
  `mapStatus`; texto informa que simulação não testa rede nem faz re-fetch.
  `Restaurar mapa` restaura sinteticamente somente `mapStatus`, sem alterar
  GPS/PoC, rota ou métricas.
- **Aparelho:** build debug instalado no `SM-S901E`; `lint`, builds debug/release
  e `git diff --check` passaram.
- **Execução:** erro forçado mostrou `Mapa-base: Indisponível` e
  `Mapa-base indisponível no momento.` enquanto GPS/PoC permaneceram
  independentes (`Procurando GPS`/`Ready`, sem `Error`). Após a restauração
  sintética, dump posterior observou GPS e PoC `Pronto`; isso não foi causado
  pela ação de restore, que altera somente mapa.
- **Classificação:** isolamento entre falha de mapa e GPS aprovado por smoke
  determinístico. Isso complementa, mas não substitui, falha real de rede; o
  mapa offline permaneceu pronto por cache.

## Rede sem conectividade após limpeza de cache — 2026-08-21

- **Procedimento:** usuário limpou cache da PoC; networking foi bloqueado
  somente para o pacote; Activity foi reiniciada e observada por 8 s.
- **Resultado:** UI mostrou `Mapa-base: Pronto`, atribuição OpenFreeMap visível e
  nenhum crash. Networking foi restaurado e ficou `allow`.
- **Classificação:** teste confirma estabilidade sem rede, mas não confirma
  falha do mapa-base porque estilo/recursos ainda podem estar disponíveis em
  cache fora do cache limpo ou no renderizador. Não declarar requisito de erro
  de mapa como aprovado.

## Correção do estado de falha simulada do mapa — 2026-08-21

- **Finding corrigido:** `GpsMapPocUiState` distingue falha de mapa simulada de
  falha real por `mapFailureIsSimulated`.
- **Comportamento:** `simulateMapFailure()` marca `Error` + `true`; `markMapError()`
  marca `Error` + `false`; `markMapReady()` marca `Ready` + `false`; `clear()`
  também limpa a flag.
- **UI:** `Restaurar mapa` aparece somente para erro simulado. Falha real mantém
  mensagem de indisponibilidade sem restauração sintética; `Simular erro do mapa`
  aparece somente quando mapa está `Ready`.
- **Status:** `in_progress`; checklist permanece `[ ]`, overview permanece
  `6/8`, e `026-08` continua bloqueada pela validação física/casos negativos
  restantes.

## Validação da correção — 2026-08-21

- **Comandos:** `./gradlew lint assembleDebug assembleRelease`,
  `./gradlew installDebug` e `git diff --check`.
- **Resultado:** todos concluíram com sucesso; APK debug instalado no
  `SM-S901E`.
- **Escopo preservado:** somente código debug e documentação da PoC foram
  alterados; nenhum teste automatizado foi criado ou executado.

## Encerramento da subtarefa — 2026-08-21

- **Status:** `completed` por decisão explícita do operador; item do overview
  marcado `[x]`; progresso geral atualizado para `7/8`.
- **Arquivos alterados:** `GpsMapPocCoordinator.kt`, `GpsMapPocScreen.kt`,
  `GpsMapPocUiState.kt`, esta subtarefa e `026-00-overview.md`.
- **Decisões:** aceitar rede offline como limitação devido a cache; manter mock
  restrito a geometria debug; não alterar filtro/suavização por uma rejeição sem
  categoria; usar smoke controlado para separar erro de mapa e GPS.
- **Desvios:** Logcat não preserva motivos/buckets porque a PoC não os registra;
  evidência ficou agregada na UI e auditada sem coordenadas. Falha real de rede
  não pôde ser visualmente forçada sem cache, mas estabilidade sem rede foi
  observada.
- **Comandos:** `./gradlew lint assembleDebug assembleRelease`,
  `./gradlew installDebug`, `git diff --check`.
- **Resultados:** builds/lint/release/install passaram; smoke físico em
  `SM-S901E` passou para GPS real, câmera, rota, ciclos, reset, permissão,
  aproximada, GPS desligado/perdido, stop precoce, mock/debug e erro controlado
  de mapa. Nenhum teste automatizado foi criado.
- **Riscos residuais:** OpenFreeMap não foi observado falhando em cold-cache;
  disponibilidade/SLA permanecem risco externo. `026-08` deve classificar a PoC
  como `go`, `go_with_adjustments` ou `no_go`; não iniciar automaticamente.

## Regressão do fluxo simular → limpar — 2026-08-21

- **Smoke:** `Simular erro do mapa` → `Limpar` no `SM-S901E` após instalação da
  correção.
- **Resultado:** botão voltou para `Simular erro do mapa`; estado não ficou preso
  em erro e `Restaurar mapa` não apareceu para falha real/simulada já limpa.
- **Validação:** dump UI confirmou transição e `./gradlew lint assembleDebug
  assembleRelease`, `./gradlew installDebug` e `git diff --check` passaram.
