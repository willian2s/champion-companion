# Champion Companion

## Ler primeiro

- Leia `docs/baseline/current-baseline.md` antes de mudar código, dependências ou arquitetura.
- Se tocar escopo funcional, confira spec correspondente em `docs/specs/` e ADRs em `docs/decisions/`.

## Estrutura real

- Repo tem só módulo Android `:app`; `settings.gradle.kts` não inclui outros módulos.
- Entrypoint: `app/src/main/java/br/com/mwconsulting/championcompanion/MainActivity.kt` -> `ChampionCompanionTheme` -> `core/navigation/ChampionNavigation.kt`.
- Navegação usa `androidx.navigation3` com back stack manual (`mutableStateListOf<AppDestination>`). Não procure `NavHost`/`NavController`.
- Infra fica em `core/common`, `core/error`, `core/logging`, `core/navigation`, `core/preferences`, `core/database`.
- Domínio e contratos ficam em `core/model`; implementação persistente fica em `data/` via `RoomRepositories` + `RepositoryModule`.
- Features atuais: `feature/home`, `health`, `calendar`, `exercises`, `device`, `profile`, `settings`.

## Toolchain e comandos

- Use só wrapper: `./gradlew`.
- Toolchain de compilação está fixada em Java/Kotlin 21 (`app/build.gradle.kts`). SDK alvo/compile: 36.
- Verificação completa esperada para mudanças em código ou Gradle:

```bash
./gradlew lint assembleDebug assembleRelease
```

- Comandos focados úteis:

```bash
./gradlew app:lintDebug
```

## Quirks de build

- `gradle.properties` mantém `org.gradle.configuration-cache=true`; não introduza mudanças que quebrem cache sem necessidade.
- Flags `android.disallowKotlinSourceSets=false` e `android.sync.suppressAgpWarnings=UNSUPPORTED_PROJECT_OPTION_USE` existem por compatibilidade KSP 2.2.10/AGP 9.3. Não remova sem upgrade coordenado.
- `assembleRelease` deve continuar funcionando sem credenciais locais: `app/build.gradle.kts` só aplica assinatura release quando `keystore.properties` existe.

## Room, DataStore, DI

- Banco usa Room 3 com `AndroidSQLiteDriver()` em produção; suites instrumentadas existentes também usam esse driver.
- Schemas exportados ficam em `app/schemas/br.com.mwconsulting.championcompanion.core.database.ChampionDatabase/`. Se mudar entidades/DAOs/versão, atualize schema exportado correspondente.
- DataStore de preferências usa nome fixo `champion_preferences` em `core/preferences/PreferencesModule.kt`.
- Hilt está no app inteiro: `ChampionCompanionApplication` tem `@HiltAndroidApp`; módulos principais ficam em `core/database`, `core/logging`, `core/preferences`, `data`.

## Guard rails

- Existem suites em `app/src/test` e `app/src/androidTest`; não trate criação ou execução de testes como etapa padrão e não crie novos testes, salvo pedido explícito do usuário.
- `Phase003PrivacyGuardTest` ainda bloqueia termos de localização/rota em
  `core/model`, `core/database`, `data`, `core/logging` e testes existentes.
  Essa regra ficou incompatível com a política autorizada da Fase 13 e é dívida
  conhecida: não alterar, contornar, renomear termos ou executar esse guard como
  validação desta fase. Implementação de rota só é permitida para sessão
  explícita de caminhada/corrida da Fase 13, nas fronteiras de domínio,
  persistência e dados; nunca em logs, diagnósticos, exportações ou testes.
- Fase 13 não autoriza rastreamento passivo, servidor, exportação, GPX, TCX,
  FIT, backup ou device transfer de rota. Treino livre e esteira não usam
  localização. Fase 12 continua sem rota; Fase 14 mantém WorkManager para
  tarefas adiáveis, sem controlar sessão ativa.
- `DebugStructuredLoggerTest` valida logs diagnósticos só em debug e apenas campos aprovados. Não exponha payload BLE, dados de saúde, tokens, credenciais ou identificadores pessoais.

## Diretriz autoritativa após encerramento da Fase 013

- Não continuar implementando ou corrigindo treinos iniciados pelo celular.
- Não expandir GPS, rota, `ExerciseRoute` ou BLE live em sessão phone.
- Preservar migrations, schemas, dados e evidências existentes até task explícita de remoção.
- Priorizar importação, derivação, revisão e sincronização com Health Connect de treinos iniciados pelo relógio.

## Planejamento documental da Fase 014

- Fase 014 usa ticker `014` e permanece `pending` até implementação e validação
  explícitas.
- Fluxo planejado: Champion C001 inicia e encerra; importação D3/D4 explícita;
  derivação; revisão local; sincronização Health Connect.
- Distância canônica de `WATCH_D3_D4` usa somente passos válidos ×
  `stepLengthMeters`. Distância original do relógio permanece diagnóstica e não
  é fallback.
- `stepLengthMeters` representa um passo individual. Contratos novos usam essa
  terminologia e congelam valor, origem e versão da derivação por atividade.
- Fase 014 não autoriza GPS, rota, mapa, `ExerciseRoute`, treino phone, BLE live,
  importação silenciosa, TCX, FIT, GPX ou remoção de legado da Fase 013.
- Health Connect continua write-only, local-first e explícito; projeções filhas,
  IDs estáveis, versões monotônicas, retry e reconciliação seguem ADR-018.

## Privacidade e backup

- `AndroidManifest.xml` mantém `android:allowBackup="false"`.
- `app/src/main/res/xml/backup_rules.xml` e `data_extraction_rules.xml` negam backup e device transfer para root/files/database/sharedpref/external. Preserve isso ao tocar manifest ou regras XML.

## Convenções de mudança

- Use Conventional Commits.
- `AGENTS.md` é fonte de verdade para regras operacionais de execução das tasks.
- Em documentação, ADRs, specs, tasks, roadmap, comentários e textos de UI escritos durante as tasks, siga sempre as regras gramaticais do português brasileiro, incluindo acentuação correta.
- Sempre que executar uma task com spec associada, atualize também o `overview` da spec correspondente no mesmo trabalho.
- Se algum arquivo de task, spec ou overview repetir essa regra com texto divergente, siga `AGENTS.md` e alinhe a documentação no mesmo trabalho.
- Se mudança introduzir decisão arquitetural, de persistência, privacidade, navegação, build ou workflow com impacto duradouro, crie ou atualize ADR correspondente em `docs/decisions/` no mesmo trabalho.
- Se mudança for visual, inclua screenshot no PR.
- Não misture refatorações não relacionadas.
- Testes não devem ser considerados para este projeto e não devem ser criados sem pedido explícito do usuário.
