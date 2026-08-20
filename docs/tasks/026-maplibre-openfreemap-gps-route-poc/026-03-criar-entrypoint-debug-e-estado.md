# 026-03 — Criar entrypoint debug e estado da PoC

- **Ticker:** `026`
- **Número:** `03`
- **Status:** pending

## Objetivo

Criar uma superfície técnica isolada em `src/debug`, acessível por ADB, com state machine explícita e sem alterar a navegação de produto.

## Resultado esperado

Existe uma Activity debug-only capaz de apresentar mapa, readiness, métricas técnicas e ações válidas da PoC, sem qualquer CTA nas superfícies normais do Champion Companion.

## Requisitos e critérios cobertos

- Spec 026: critérios 1 a 5;
- isolamento debug-only;
- estado explícito para mapa e GPS;
- nenhum acoplamento à antiga sessão phone.

## Escopo incluído

- `GpsMapPocActivity`;
- `GpsMapPocScreen`;
- `GpsMapPocUiState`;
- coordinator/store técnico;
- `app/src/debug/AndroidManifest.xml`;
- abertura por ADB;
- estados neutros de mapa e GPS;
- ações `Iniciar`, `Parar`, `Recentralizar` e `Limpar` conforme estado.

## Escopo excluído

- adicionar `AppDestination` permanente;
- CTA em Home/Exercícios/Configurações;
- `WorkoutSessionActivity`;
- `WorkoutRecordingService`;
- Room;
- Health Connect;
- BLE.

## Dependências

- `026-02` concluída;
- MapLibre Compose disponível no build;
- style OpenFreeMap resolvível.

## Estrutura preferida

```text
app/src/debug/java/
└── br/com/mwconsulting/championcompanion/feature/gpspoc/
    ├── GpsMapPocActivity.kt
    ├── GpsMapPocScreen.kt
    ├── GpsMapPocUiState.kt
    └── GpsMapPocCoordinator.kt
```

## Estado conceitual

```text
Initializing
PermissionRequired
SearchingGps
Ready
Tracking
GpsLost
Stopped
Error
```

O estado do mapa-base deve ser distinguível do estado GPS quando útil, evitando reportar indisponibilidade de rede como falha de localização.

## UI mínima

- título técnico `GPS + MapLibre — PoC`;
- área principal de mapa;
- status do mapa-base;
- status GPS;
- precisão horizontal;
- distância;
- pontos aceitos;
- pontos rejeitados;
- `Recentralizar`;
- `Iniciar/Parar`;
- `Limpar`.

Não exibir coordenadas, endereço ou identificadores internos.

## Passos de implementação

1. Criar Activity apenas no source set debug.
2. Registrar Activity apenas no manifest debug.
3. Criar state machine sem booleans incompatíveis.
4. Renderizar shell da tela e mapa-base.
5. Garantir que ausência de permissão não derrube a tela.
6. Garantir que falha de mapa-base possua feedback sanitizado.
7. Manter ações desabilitadas quando pré-condições não estiverem satisfeitas.
8. Confirmar abertura por ADB.
9. Confirmar inexistência do entrypoint em release.

## Validação

```bash
git diff --check
./gradlew lint assembleDebug assembleRelease
./gradlew installDebug
```

Smoke por ADB deve abrir a Activity e registrar screenshot sanitizado sem localização pessoal.

## Definition of Done

- Activity existe somente em debug;
- mapa-base pode ser apresentado no shell técnico;
- estados iniciais são seguros;
- release não expõe entrypoint;
- nenhuma navegação de produto foi alterada;
- screenshot sanitizado foi registrado quando aplicável;
- builds e lint passam.

## Riscos e cuidados

- não usar nome/manifest merger que exponha Activity na variante release;
- não criar dependência do core para Compose;
- não iniciar GPS automaticamente apenas por abrir a tela;
- não versionar screenshot de localização real.
