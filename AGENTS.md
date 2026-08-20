# Champion Companion

## Ler primeiro

- Leia `docs/baseline/current-baseline.md` antes de mudar código, dependências ou arquitetura.
- Para direção macro após a Fase 15, leia `docs/roadmap/post-phase-15-direction.md`.
- Se tocar escopo funcional, confira spec correspondente em `docs/specs/` e ADRs em `docs/decisions/`.
- Ao executar a Fase 16, leia `docs/specs/023-personal-metrics-calibration.md`, `docs/specs/_inputs/phase-16-personal-metrics-direction.md` e ADR-022.

## Fases e tickers

- Número de fase do roadmap e ticker de spec/task são conceitos independentes.
- Coincidência numérica não implica relação semântica. Exemplo: a Spec/Task `021` não representa a Fase 21.
- A Spec `023` executa a Fase 16 por escolha desta entrega; isso não cria regra `ticker == fase`.
- Fases representam evolução macro do produto; tickers identificam unidades de trabalho executáveis.
- Fases removidas não são renumeradas nem têm seus números reciclados.
- Correção posterior de funcionalidade concluída não reabre uma fase por padrão.
- Antes de planejar qualquer fase posterior à Fase 15, siga `docs/roadmap/post-phase-15-direction.md` e audite o estado real do código para não reimplementar trabalho já entregue por specs independentes.

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
- Schema atual antes da Fase 16: Room v12. A Spec 023 planeja migration aditiva v12 -> v13; não antecipe outra versão sem necessidade.
- Schemas exportados ficam em `app/schemas/br.com.mwconsulting.championcompanion.core.database.ChampionDatabase/`. Se mudar entidades/DAOs/versão, atualize schema exportado correspondente.
- DataStore de preferências usa nome fixo `champion_preferences` em `core/preferences/PreferencesModule.kt`.
- `dailyStepsGoal` continua em DataStore na Fase 16; não mova para Room.
- Hilt está no app inteiro: `ChampionCompanionApplication` tem `@HiltAndroidApp`; módulos principais ficam em `core/database`, `core/logging`, `core/preferences`, `data`.

## Guard rails

- Existem suites em `app/src/test` e `app/src/androidTest`; não trate criação ou execução de testes como etapa padrão e não crie novos testes, salvo pedido explícito do usuário.
- Treinos iniciados pelo celular, GPS, localização de sessão, rota, mapa e `ExerciseRoute` não fazem parte da direção futura do produto.
- Infraestrutura, migrations, schemas, dados e evidências históricas da Fase 13 permanecem preservados até trabalho explícito de limpeza. Preservação não autoriza expansão funcional.
- Não introduza localização em novos fluxos, nem use código legado da Fase 13 como justificativa para reativar treino phone.
- Na Fase 16, distância de referência é entrada manual externa; não coletar GPS nem criar integração Adidas/Strava.
- Perfis aprendidos de `RUNNING` e `WALKING` são independentes. Não aplicar perfil contextual a `CYCLING`, `TREADMILL` ou resumo diário além do que a Spec 023 autoriza.
- Mudança de perfil/calibração não pode reescrever snapshots históricos nem atividades sincronizadas silenciosamente.
- `Phase003PrivacyGuardTest` preserva o bloqueio de termos de localização/rota em áreas sensíveis; não contorne ou renomeie termos para burlar o guard.
- `DebugStructuredLoggerTest` valida logs diagnósticos só em debug e apenas campos aprovados. Não exponha payload BLE, dados de saúde, tokens, credenciais ou identificadores pessoais.

## Diretriz autoritativa após encerramento da Fase 013

- Não continuar implementando ou corrigindo treinos iniciados pelo celular como direção de produto.
- Não expandir GPS, rota, `ExerciseRoute` ou BLE live em sessão phone.
- Preservar migrations, schemas, dados e evidências existentes até task explícita de remoção.
- Priorizar importação, derivação, revisão e sincronização com Health Connect de treinos iniciados pelo relógio.

## Estado documental da Fase 014 e correções posteriores

- A Fase 014 está `completed`, com progresso `17/17`.
- Fluxo vigente: Champion C001 inicia e encerra; importação D3/D4 explícita; derivação; revisão local; sincronização Health Connect.
- Distância canônica de `WATCH_D3_D4` usa somente passos válidos × `stepLengthMeters`. Distância original do relógio permanece diagnóstica e não é fallback.
- `stepLengthMeters` representa um passo individual e fica congelado por snapshot de atividade.
- Parser efetivo de novas importações é `d4-v3`; `d4-v1` e `d4-v2` permanecem legados identificáveis.
- Derivação temporal efetiva antes da Fase 16 usa `watch-d3-d4-v2`; a Spec 023 planeja nova derivation version contextual sem alterar o parser.
- A Spec/Task `021` corrigiu duração D4, normalização temporal e candidate semântico do calendário. Esse ticker não representa a Fase 21.
- A Spec `022` está concluída: `WorkoutType.CYCLING` usa distância manual opcional e nunca distância/velocidade por passos. O ticker não representa a Fase 22.
- Health Connect continua write-only, local-first e explícito; projeções filhas, IDs estáveis, versões monotônicas, retry e reconciliação seguem ADR-018/ADR-019/ADR-021.

## Direção pós-Fase 15

A fonte autoritativa é `docs/roadmap/post-phase-15-direction.md`.

Resumo final:

- Fase 16: `completed`, executada pela Spec `023-personal-metrics-calibration`;
- Fases 17–25: `closed` por `product_maturity_no_further_phases`, sem backlog
  macro futuro.

O Champion Companion é um aplicativo pessoal. Não criar fases de beta, lançamento público, matriz multi-device, paridade obrigatória com MotionLog ou cobertura ampla de testes sem nova decisão explícita.

## Privacidade e backup

- `AndroidManifest.xml` mantém `android:allowBackup="false"`.
- `app/src/main/res/xml/backup_rules.xml` e `data_extraction_rules.xml` negam backup e device transfer para root/files/database/sharedpref/external. Preserve isso; a Fase 17 foi encerrada sem implementação.
- Backup manual/local não é backlog macro. Qualquer mudança exige decisão explícita e task independente.

## Convenções de mudança

- Use Conventional Commits.
- `AGENTS.md` é fonte de verdade para regras operacionais de execução das tasks.
- Em documentação, ADRs, specs, tasks, roadmap, comentários e textos de UI escritos durante as tasks, siga sempre as regras gramaticais do português brasileiro, incluindo acentuação correta.
- Sempre que executar uma task com spec associada, atualize também o `overview` da spec correspondente no mesmo trabalho.
- Se algum arquivo de task, spec ou overview repetir essa regra com texto divergente, siga `AGENTS.md` e alinhe a documentação no mesmo trabalho.
- Se mudança introduzir decisão arquitetural, de persistência, privacidade, navegação, build ou workflow com impacto duradouro, crie ou atualize ADR correspondente no mesmo trabalho.
- Se mudança for visual, inclua screenshot no PR.
- Não misture refatorações não relacionadas.
- Testes não devem ser considerados para este projeto e não devem ser criados sem pedido explícito do usuário.
