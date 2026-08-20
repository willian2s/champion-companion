# 026-07 — Validar fisicamente e executar casos negativos

- **Ticker:** `026`
- **Número:** `07`
- **Status:** in_progress

## Objetivo

Executar o gate físico da PoC em aparelho Android real, incluindo percurso externo, interação com mapa e cenários negativos mínimos, com evidência sanitizada.

## Resultado esperado

Existe evidência suficiente para classificar tecnicamente a PoC sem depender de emulador, dados simulados ou screenshots contendo localização pessoal.

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
