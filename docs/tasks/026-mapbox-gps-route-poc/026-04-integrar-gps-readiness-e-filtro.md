# 026-04 — Integrar GPS, readiness e filtro

- **Ticker:** `026`
- **Número:** `04`
- **Status:** pending

## Objetivo

Conectar a PoC a uma fonte GPS debug-only baseada em `LocationManager.GPS_PROVIDER`, reutilizando os contratos puros existentes de readiness e filtragem sem liberar a infraestrutura produtiva descontinuada.

## Resultado esperado

A tela técnica observa permissão, serviço, qualidade do fix e pontos aceitos/rejeitados; somente após GPS `READY` o usuário pode iniciar um ciclo de captura.

## Dependências

- `026-03` concluída;
- permissões de localização existentes confirmadas no manifesto principal.

## Escopo incluído

- `GpsMapPocLocationSource` debug-only;
- `LocationManager.GPS_PROVIDER`;
- `ACCESS_FINE_LOCATION` e `ACCESS_COARSE_LOCATION` runtime;
- Activity Result API para permissão;
- `WorkoutRoutePointCandidate`;
- `GpsReadinessEvaluator` e política existente;
- `RoutePointFilter` e política existente;
- geração efêmera de route/point IDs e sequência;
- distância acumulada em memória;
- contadores de fixes, aceitos e rejeitados.

## Escopo excluído

- `LocationModule` produtivo;
- `PhoneWorkoutPolicy`;
- `WorkoutRecordingService`;
- foreground service;
- background;
- Room.

## Política inicial

Readiness:

```text
STRONG    <= 10 m
GOOD      <= 20 m
WEAK      <= 50 m
UNUSABLE  > 50 m
fix recente <= 10 s
```

Filtro:

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

- GPS pode aquecer antes do início.
- Fix anterior ao toque em `Iniciar` nunca entra na rota.
- `Iniciar` só é válido com GPS `READY` e qualidade `GOOD` ou `STRONG`.
- Cada ciclo cria identidade efêmera nova.
- Callback de geração antiga não atualiza ciclo novo.
- `Parar` bloqueia novos pontos na rota, ainda que readiness continue sendo observado.
- `Limpar` remove todo estado do ciclo.

## Privacidade

Proibido em Logcat e erros:

- latitude;
- longitude;
- altitude;
- bearing;
- coordenadas serializadas.

Logs técnicos podem conter apenas categorias e contagens sanitizadas.

## Passos de implementação

1. Criar fonte debug isolada do binding Hilt produtivo.
2. Implementar permission/service state.
3. Emitir fix recente e readiness.
4. Converter `Location` em candidate sem logar conteúdo.
5. Implementar início de ciclo e filtro.
6. Atualizar métricas somente para pontos aceitos.
7. Garantir cleanup e geração contra callback tardio.
8. Conectar estado ao coordinator da PoC.

## Validação

```bash
git diff --check
./gradlew lint assembleDebug assembleRelease
./gradlew installDebug
```

Smoke físico mínimo:

- permissão negada;
- localização aproximada;
- GPS desligado;
- aguardando fix;
- GPS pronto;
- iniciar/parar/limpar sem mapa definitivo;
- confirmar ausência de coordenadas no Logcat.

## Definição de pronto

- GPS real alimenta readiness;
- `Iniciar` respeita qualidade;
- pontos pré-início são ignorados;
- pontos aceitos/rejeitados são distinguidos;
- distância usa apenas pontos aceitos;
- ciclo novo não recebe estado antigo;
- nenhuma infraestrutura produtiva descontinuada foi liberada.
