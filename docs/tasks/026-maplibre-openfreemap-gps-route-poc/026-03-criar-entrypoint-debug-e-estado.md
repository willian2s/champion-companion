# 026-03 — Criar entrypoint debug e estado da PoC

- **Ticker:** `026`
- **Número:** `03`
- **Status:** completed

## Objetivo

Criar uma superfície técnica isolada em `src/debug`, acessível manualmente por
CTA técnico na página de Exercícios do build debug, com state machine explícita e
sem alterar a navegação de produto. ADB permanece fallback diagnóstico.

## Resultado esperado

Existe uma Activity debug-only capaz de apresentar mapa, readiness, métricas
técnicas e ações válidas da PoC. No build debug, a página de Exercícios oferece
somente uma affordance técnica que abre essa Activity; não há CTA de treino nem
CTA equivalente nas demais superfícies.

## Requisitos e critérios cobertos

- Spec 026: critérios 1 a 5;
- isolamento debug-only;
- estado explícito para mapa e GPS;
- nenhum acoplamento à antiga sessão phone;
- acesso manual externo sem depender exclusivamente de ADB.

## Escopo incluído

- `GpsMapPocActivity`;
- `GpsMapPocScreen`;
- `GpsMapPocUiState`;
- coordinator/store técnico;
- `app/src/debug/AndroidManifest.xml`;
- CTA técnico em Exercícios somente no build debug;
- `Intent` explícito para a Activity debug;
- abertura por ADB como fallback diagnóstico;
- estados neutros de mapa e GPS;
- ações `Iniciar`, `Parar`, `Recentralizar` e `Limpar` conforme estado.

## Escopo excluído

- adicionar `AppDestination` permanente;
- CTA de treino ou affordance de produto;
- CTA em Home, Saúde, Dispositivo ou Configurações;
- botão, `Intent` executável ou entrypoint da PoC na variante release;
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
3. Adicionar CTA técnico somente à página de Exercícios no build debug.
4. Fazer o CTA disparar `Intent` explícito para a Activity, sem `AppDestination`.
5. Garantir que botão, `Intent` executável e entrypoint não apareçam em release.
6. Criar state machine sem booleans incompatíveis.
7. Renderizar shell da tela e mapa-base.
8. Garantir que ausência de permissão não derrube a tela.
9. Garantir que falha de mapa-base possua feedback sanitizado.
10. Manter ações desabilitadas quando pré-condições não estiverem satisfeitas.
11. Confirmar abertura manual pelo CTA; ADB pode complementar o smoke diagnóstico.

## Validação

```bash
git diff --check
./gradlew lint assembleDebug assembleRelease
./gradlew installDebug
```

Smoke primário deve instalar o build debug, abrir Exercícios e tocar o CTA
técnico para abrir a Activity, registrando screenshot sanitizado sem localização
pessoal. ADB pode ser usado como fallback diagnóstico, mas não substitui esse
fluxo externo.

## Definition of Done

- Activity existe somente em debug;
- CTA técnico em Exercícios existe somente em debug e não inicia treino;
- CTA dispara `Intent` explícito sem criar destino permanente;
- mapa-base pode ser apresentado no shell técnico;
- estados iniciais são seguros;
- release não expõe botão, `Intent` executável nem entrypoint;
- nenhuma navegação de produto foi alterada;
- screenshot sanitizado foi registrado quando aplicável;
- builds e lint passam.

## Revisão de escopo e decisão

Em 2026-08-20, exigência explícita de teste externo substituiu o plano de
abertura exclusivamente por ADB. O acesso manual por CTA técnico em Exercícios
fica dentro de `026-03`, pois pertence ao contrato do entrypoint debug; não há
motivo para criar nova subtarefa ou alterar a ordem de dependências.

O CTA deve existir apenas no build debug, abrir a Activity por `Intent` explícito
e ser identificado como PoC técnica. Não pode iniciar treino phone, usar
`WorkoutRecordingService`, alterar `PhoneWorkoutPolicy`, tocar Room, Health
Connect ou BLE, nem aparecer em Home, Saúde, Dispositivo, Configurações ou
release.

A revisão documental alterou o plano. Implementação e validação do CTA
foram concluídas pelo executor principal após a revisão de escopo. O CTA técnico
é variant-safe: o provider debug cria o `Intent` explícito e o provider release
retorna `null`, sem referência à Activity ou ao MapLibre.

## Execução

- **Arquivos alterados:** `app/src/debug/AndroidManifest.xml`, quatro arquivos
  `feature/gpspoc`, os launchers debug/release, `MainActivity.kt`,
  `ChampionNavigation.kt`, `ExercisesScreen.kt`, esta subtarefa e o overview;
  spec, ADR-024, baseline e `AGENTS.md` foram alinhados ao novo acesso manual.
- **Decisões e desvios:** abertura por ADB deixou de ser caminho único. CTA
  técnico foi autorizado somente em Exercícios/debug, sem `AppDestination`; o
  provider release retorna `null` para impedir exposição de botão ou Intent.
- **Comandos executados:** `git diff --check`; `./gradlew lint assembleDebug
  assembleRelease`; `./gradlew installDebug`; smoke ADB com `am start` e
  inspeção de `dumpsys activity`, Logcat e APKs.
- **Resultados e evidências:** lint, builds e instalação passaram; Activity
  abriu em estado `resumed`; CTA debug abriu a Activity por Intent explícito;
  release não contém Activity, pacote `gpspoc` ou recursos MapLibre.
- **Riscos residuais:** screenshot sanitizado foi capturado em estado neutro,
  sem rota ou localização pessoal, mas não foi versionado; mapa-base, GPS, rota,
  câmera e validação física externa continuam nas subtarefas posteriores. Não
  há coordenadas em logs ou evidências registradas.

## Evidências da execução

- Activity e manifest foram criados exclusivamente em `app/src/debug`.
- `GpsMapPocUiState` separa estado da PoC, mapa-base, readiness GPS e erro
  sanitizado, sem coordenadas, identificadores ou persistência.
- `GpsMapPocCoordinator` expõe `StateFlow` e valida pré-condições de `start`,
  `stop`, `recenter` e `clear`; GPS não é iniciado ao abrir a Activity.
- A abertura ADB foi confirmada com `br.com.mwconsulting.championcompanion.dev`;
  Activity permaneceu em estado resumed sem exceção fatal observada.
- APK debug contém Activity e recursos MapLibre; APK release não contém
  `GpsMapPocActivity` nem recursos MapLibre.
- Exercícios exibe o card técnico somente quando o callback debug existe; o
  detalhe de atividade não recebe o CTA. O callback debug abriu a Activity por
  `Intent` explícito no aparelho conectado; o provider release não cria botão,
  `Intent` ou referência à Activity.
- `git diff --check`, `./gradlew lint assembleDebug assembleRelease` e
  `./gradlew installDebug` passaram. A abertura direta por ADB também passou.
- Screenshot sanitizado do fluxo Exercícios → CTA → Activity foi capturado em
  estado neutro e não foi versionado; não contém rota ou localização pessoal.
  Validação física de GPS/rota pertence às subtarefas posteriores.

026-04 continua bloqueada até integração GPS explícita, readiness real e filtro.

## Riscos e cuidados

- não usar nome/manifest merger que exponha Activity na variante release;
- não deixar botão ou `Intent` de Exercícios escapar para release;
- não apresentar affordance técnica como início de treino phone;
- não criar dependência do core para Compose;
- não iniciar GPS automaticamente apenas por abrir a tela;
- não versionar screenshot de localização real.
