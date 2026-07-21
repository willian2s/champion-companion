# Champion Companion

## Ler primeiro

- Leia `docs/core/current-project-baseline.md` antes de mudar código, dependências ou arquitetura.
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

- Existem suites em `app/src/test` e `app/src/androidTest`; não trate criação ou execução de novos testes como etapa padrão.
- `Phase003PrivacyGuardTest` bloqueia termos de localização/rota em `core/model`, `core/database`, `data`, `core/logging` e testes existentes. Não introduza latitude/longitude/GPX/rotas nesses escopos.
- `DebugStructuredLoggerTest` valida logs diagnósticos só em debug e apenas campos aprovados. Não exponha payload BLE, dados de saúde, tokens, credenciais ou identificadores pessoais.

## Privacidade e backup

- `AndroidManifest.xml` mantém `android:allowBackup="false"`.
- `app/src/main/res/xml/backup_rules.xml` e `data_extraction_rules.xml` negam backup e device transfer para root/files/database/sharedpref/external. Preserve isso ao tocar manifest ou regras XML.

## Convenções de mudança

- Use Conventional Commits.
- `AGENTS.md` é fonte de verdade para regras operacionais de execução das tasks.
- Sempre que executar uma task com spec associada, atualize também o `overview` da spec correspondente no mesmo trabalho.
- Se algum arquivo de task, spec ou overview repetir essa regra com texto divergente, siga `AGENTS.md` e alinhe a documentação no mesmo trabalho.
- Se mudança introduzir decisão arquitetural, de persistência, privacidade, navegação, build ou workflow com impacto duradouro, crie ou atualize ADR correspondente em `docs/decisions/` no mesmo trabalho.
- Se mudança for visual, inclua screenshot no PR.
- Não misture refatorações não relacionadas.
