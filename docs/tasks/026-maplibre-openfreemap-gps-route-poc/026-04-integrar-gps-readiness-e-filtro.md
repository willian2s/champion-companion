# 026-04 — Integrar GPS, readiness e filtro

- **Ticker:** `026`
- **Número:** `04`
- **Status:** completed

## Objetivo

Adicionar à PoC uma fonte GPS debug-only baseada em `LocationManager.GPS_PROVIDER`, reutilizando readiness e filtragem existentes sem liberar a antiga infraestrutura produtiva da Fase 013.

## Resultado esperado

A tela técnica recebe fixes reais, distingue prontidão GPS, aceita somente pontos válidos durante tracking e acumula distância/contadores em memória.

## Requisitos e critérios cobertos

- Spec 026: critérios 11 a 17 e 27;
- fonte única de localização do Champion Companion;
- readiness preciso;
- filtro existente como fronteira para rota;
- nenhuma coordenada em logs.

## Escopo incluído

- `GpsMapPocLocationSource` debug-only;
- solicitação runtime de `ACCESS_FINE_LOCATION`/`ACCESS_COARSE_LOCATION`;
- `LocationManager.GPS_PROVIDER`;
- intervalo inicial de 1 s e deslocamento mínimo solicitado 0 m;
- conversão para `WorkoutRoutePointCandidate`;
- readiness existente;
- `RoutePointFilter`;
- IDs efêmeros necessários ao filtro;
- distância e contadores em memória;
- distinção entre fixes recebidos, pontos aceitos e rejeitados.

## Escopo excluído

- `LocationModule` produtivo;
- alteração de `PhoneWorkoutPolicy`;
- `WorkoutRecordingService`;
- foreground service;
- background location;
- persistência;
- segmentos complexos de pausa/process recovery.

## Dependências

- `026-03` concluída;
- permissões de localização já presentes no manifesto principal confirmadas;
- contratos históricos de GPS/rota revisados antes da implementação.

## Readiness inicial

```text
STRONG    <= 10 m
GOOD      <= 20 m
WEAK      <= 50 m
UNUSABLE  > 50 m
fix recente <= 10 s
```

`Iniciar` somente em `READY` com qualidade `GOOD` ou `STRONG`.

## Filtro inicial

```text
idade máxima                  10 s
precisão horizontal máxima    50 m
velocidade humana máxima      12 m/s
deslocamento mínimo           3 m
mock                          rejeitado
duplicata                     rejeitada
ordem inválida                rejeitada
```

## Regras de ciclo

Antes de `Iniciar`:

- fixes podem aquecer readiness;
- nenhum ponto entra na rota.

Ao iniciar:

- novo routeId efêmero;
- nova geração de runtime;
- sequência zero;
- distância e contadores zerados;
- rota anterior removida.

Durante tracking:

```text
Location
   ↓
Candidate
   ↓
RoutePointFilter
   ├── Rejected → contador por categoria
   └── Accepted → lista + distância
```

Ao parar:

- fixes podem continuar alimentando readiness se necessário;
- nenhum novo ponto entra na rota encerrada.

## Privacidade

Nunca logar:

- latitude;
- longitude;
- altitude;
- bearing combinado com posição;
- GeoJSON;
- rota.

Logs técnicos podem conter somente categorias, contagens e estados aprovados.

## Passos de implementação

1. Criar fonte debug independente do binding produtivo descontinuado.
2. Solicitar permissão somente por ação explícita.
3. Emitir estados de permission/service/readiness/fix necessários à PoC.
4. Converter fixes para candidate sem logar dados sensíveis.
5. Alimentar readiness antes e durante tracking.
6. Criar identidade efêmera por ciclo.
7. Aplicar `RoutePointFilter` apenas durante tracking.
8. Atualizar contadores e distância.
9. Garantir que callbacks de geração antiga não contaminem ciclo novo.
10. Confirmar cleanup ao sair/resetar.

## Validação

```bash
git diff --check
./gradlew lint assembleDebug assembleRelease
./gradlew installDebug
```

Smoke físico deve cobrir permissão, GPS buscando e estado pronto sem versionar coordenadas.

## Definition of Done

- permissão precisa funciona;
- readiness é observável;
- tracking só inicia com GPS elegível;
- pontos pré-start não entram na rota;
- filtro existente controla aceitação;
- contadores/distância são efêmeros;
- callbacks tardios não contaminam ciclo novo;
- nenhum dado de localização aparece em Logcat;
- builds/lint passam.

## Riscos e cuidados

- `LocationManager` exige cleanup correto de listener/callback;
- não duplicar regras puras já existentes;
- não confundir precisão horizontal com força de sinal;
- GPS indoor pode impedir READY e isso é comportamento esperado, não motivo para afrouxar policy sem evidência física.

## Execução

- **Status final:** completed.
- **Arquivos alterados:** `app/src/debug/java/br/com/mwconsulting/championcompanion/feature/gpspoc/GpsMapPocLocationSource.kt`, `GpsMapPocCoordinator.kt`, `GpsMapPocUiState.kt`, `GpsMapPocScreen.kt` e `GpsMapPocActivity.kt`.
- **Decisões e desvios:** a fonte debug implementa `WorkoutLocationSource` separadamente da implementação produtiva para não tocar `PhoneWorkoutPolicy` nem `LocationModule`; permissão é solicitada somente pelo botão `Preparar GPS`; rota, IDs, filtro, readiness, contadores e distância permanecem em memória; nenhuma coordenada é registrada. Captura ativa permanece separada de readiness para recuperar `GpsLost` sem resetar rota; localização desativada possui estado próprio e polling sanitizado para reativação.
- **Comandos executados:** `git diff --check`; `./gradlew app:compileDebugKotlin`; `./gradlew lint assembleDebug assembleRelease`; `./gradlew installDebug`; abertura física por `adb shell am start` e inspeção de `dumpsys activity activities`.
- **Resultados e evidências:** compilação debug, lint, `assembleDebug`, `assembleRelease` e instalação passaram; Activity da PoC abriu em estado `resumed`; fonte usa `GPS_PROVIDER`, intervalo de 1 s, deslocamento mínimo de 0 m, readiness monotônica e filtro com política padrão; callbacks antigos são isolados por geração e cleanup invalida runtime encerrado; `receivedFixCount` é zerado no início; fixes durante `GpsLost` continuam sendo filtrados; variante release continua sem entrypoint debug. Revisão independente aprovou a implementação sem bloqueador funcional.
- **Riscos residuais:** percurso físico, qualidade real do GPS, aceitação/rejeição em deslocamento e ausência de coordenadas no Logcat ainda exigem validação sanitizada da `026-07`; câmera, renderização da rota e reset visual permanecem nas subtarefas seguintes.
