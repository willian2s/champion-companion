# 026-03 — Criar entrypoint debug e estado da PoC

- **Ticker:** `026`
- **Número:** `03`
- **Status:** pending

## Objetivo

Criar uma superfície técnica debug-only, isolada da navegação de produto, com estado observável e ações explícitas para executar a PoC.

## Resultado esperado

Uma Activity técnica pode ser aberta por ADB em build debug, renderiza UI Compose neutra e possui state machine suficiente para receber GPS e mapa nas tasks seguintes.

## Dependências

- `026-02` concluída;
- Mapbox disponível no grafo debug.

## Escopo incluído

- `GpsMapPocActivity`;
- `GpsMapPocScreen`;
- `GpsMapPocUiState`;
- `GpsMapPocCoordinator` ou equivalente;
- `app/src/debug/AndroidManifest.xml`;
- estados e ações da PoC;
- UI técnica sem coordenadas.

## Escopo excluído

- implementação real de GPS;
- linha da rota;
- câmera Mapbox definitiva;
- persistência;
- navegação principal.

## Estrutura preferida

```text
app/src/debug/java/br/com/mwconsulting/championcompanion/feature/gpspoc/
├── GpsMapPocActivity.kt
├── GpsMapPocScreen.kt
├── GpsMapPocUiState.kt
└── GpsMapPocCoordinator.kt
```

## State machine mínima

Estados conceituais:

- `Initializing`;
- `PermissionRequired`;
- `SearchingGps`;
- `Ready`;
- `Tracking`;
- `GpsLost`;
- `Stopped`;
- `Error`.

Evitar combinação de booleans sem invariantes claras.

## UI mínima

- título `GPS + Mapbox — PoC`;
- área reservada para mapa;
- estado GPS;
- precisão agregada quando disponível;
- distância;
- pontos aceitos/rejeitados;
- `Iniciar` / `Parar`;
- `Recentralizar`;
- `Limpar`;
- erro sanitizado.

Não exibir latitude, longitude, altitude, token ou IDs internos.

## Regras de isolamento

- Activity somente no manifest debug;
- sem `AppDestination` permanente;
- sem CTA em telas existentes;
- sem modificação do `PhoneWorkoutPolicy`;
- sem `WorkoutRecordingService`.

## Passos de implementação

1. Criar source set e package debug.
2. Registrar Activity não exportada ou com exposição mínima necessária para ADB local.
3. Criar estado e coordinator sem GPS concreto.
4. Renderizar shell Compose seguindo design system vigente.
5. Implementar ações com comportamento seguro ainda que dependências seguintes não estejam prontas.
6. Confirmar ausência total do entrypoint no release.

## Validação

```bash
git diff --check
./gradlew lint assembleDebug assembleRelease
./gradlew installDebug
```

Smoke:

- abrir Activity por ADB;
- fechar e reabrir;
- confirmar que telas de produto não mudaram;
- capturar screenshot neutro sanitizado se necessário.

## Definição de pronto

- Activity debug abre no aparelho;
- state machine existe sem estados impossíveis evidentes;
- release não registra Activity;
- nenhuma navegação produtiva foi alterada;
- UI não revela dados sensíveis;
- task deixa pontos de integração claros para `026-04` e `026-05`.
