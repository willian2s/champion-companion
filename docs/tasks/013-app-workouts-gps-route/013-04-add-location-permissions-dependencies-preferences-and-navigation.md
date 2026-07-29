# 013-04 — Adicionar permissões, dependências, preferência e navegação

- **Ticker:** `013`
- **Número:** `04`
- **Status:** concluída

## Objetivo e resultado esperado

Preparar configuração Android e rotas de produto sem iniciar coleta. Resultado: permissões mínimas, countdown em DataStore e destinos Navigation3 disponíveis, sem FGS ou GPS disparado ao abrir telas.

## Contexto

Manifesto limita `ACCESS_FINE_LOCATION` ao SDK 30 por legado BLE; não há FGS/notificação/route permission. DataStore não possui countdown. Navegação não possui subfluxo phone workout.

## Documentos obrigatórios

Spec 013; `013-02/03`; ADR-014; AGENTS atualizado; documentação Android/Health Connect referenciada na task.

## Dependências

`013-03` concluída.

## Arquivos permitidos

`AndroidManifest.xml`; `app/build.gradle.kts`; `gradle/libs.versions.toml`; `core/preferences/**`; `core/navigation/AppDestination.kt`; `ChampionNavigation.kt`; contratos/shells mínimos de `feature/exercises`; resources de strings; task/overview.

## Arquivos e símbolos prováveis

`AppPreferences`, `AppPreferencesCodec`, `AppPreferencesRepository`, `AppDestination`, saver do `ChampionNavigation` e shells `WorkoutTypeSelection`, `WorkoutPreparation`, `ActiveWorkout` e `WorkoutRecovery`.

## Arquivos proibidos

Room/schema/repositories de rota; runtime GPS; FGS funcional; BLE; testes.

## Requisitos cobertos

Permissões; countdown 0/3/5/10 padrão 5; destinos seleção/preparação/ativa/recuperação; WRITE_EXERCISE_ROUTE; sem background location/WorkManager.

## Escopo

- `ACCESS_FINE_LOCATION` aplicável aos SDKs atuais;
- FGS base/location/connectedDevice e notificação conforme API alvo;
- `WRITE_EXERCISE_ROUTE`;
- service declaration reservada somente se classe/shell seguro existir nesta task, senão em 013-08;
- preference `workoutCountdownSeconds` validada;
- destinos serializáveis com IDs, nunca objetos completos;
- shells sem side effects.

## Fora do escopo

Solicitar permissão em runtime, contar, iniciar serviço, obter localização ou sincronizar rota.

## Passos de implementação

1. Confirmar requisitos de permissões por min/target 28/36.
2. Declarar somente permissões necessárias; não declarar `ACCESS_BACKGROUND_LOCATION`.
3. Confirmar suporte do SDK 1.1.0; não atualizar dependência sem necessidade.
4. Adicionar countdown ao codec/default/update repository com allowlist 0/3/5/10.
5. Adicionar destinos equivalentes a WorkoutTypeSelection, WorkoutPreparation(type), ActiveWorkout(sessionId), WorkoutRecovery(sessionId opcional).
6. Integrar saver/back stack manual e seleção correta da aba Exercícios.
7. Criar shells passivos e garantir voltar da preparação sem persistência.
8. Atualizar overview.

## Critérios de aceite

- manifesto contém permissões mínimas e nenhuma background location;
- countdown inválido normaliza para 5 e escolha válida persiste;
- destinos sobrevivem ao saver e transportam só IDs/tipo;
- abrir shells não cria sessão, rota, BLE ou serviço;
- release continua compilável sem keystore.

## Validação manual

Navegar pelos shells, voltar sem atividade, reiniciar app e confirmar countdown. Inspecionar manifesto mesclado e ausência de prompts ao abrir preparação sem ação.

## Comandos

```bash
./gradlew lint assembleDebug assembleRelease
./gradlew installDebug
```

## Evidências

Manifesto mesclado relevante, valor DataStore observado via UI futura/shell, navegação/back e saídas Gradle.

## Screenshots

Capturar shells somente se mudança visual for exposta no PR; evitar coordenadas/dados pessoais.

## Definição de pronto

Configuração e destinos estão prontos sem side effects; overview atualizado.

## Riscos e cuidados

Permissão FGS varia por API. Não solicitar localização para treino livre/esteira. Não adicionar biblioteca externa sem justificativa.

## Rollback

Remover permissões/dependências/destinos/preference em conjunto; preservar chaves DataStore antigas.

Não criar, alterar ou executar testes automatizados.

## Registro de execução

### Estado e escopo

- Dependência `013-03` confirmada como concluída; ticker `013` consistente entre
  spec, pasta, overview e subtarefas.
- Escopo executado somente nesta subtarefa: permissões Android, preferência de
  countdown, destinos Navigation3/back stack manual e shells passivos.

### Decisões e desvios

- `ACCESS_FINE_LOCATION` deixou de ser limitado ao SDK 30 e foi acompanhado por
  `ACCESS_COARSE_LOCATION`, necessário para solicitação Android de localização
  precisa. Nenhuma permissão `ACCESS_BACKGROUND_LOCATION` foi declarada.
- Declaradas permissões base/location/connectedDevice de foreground service,
  notificações e `WRITE_EXERCISE_ROUTE`. Nenhum service foi registrado porque
  ainda não existe classe/shell operacional seguro; declaração fica para
  `013-08`.
- Navigation3 `1.1.4`, Health Connect `1.1.0` e demais dependências existentes
  foram mantidas; não houve atualização de biblioteca nem inclusão de
  localização externa.
- Countdown foi incluído em `AppPreferences`, codec e repository com allowlist
  `0/3/5/10`, default `5` e normalização fail-safe para `5`. Métodos default no
  contrato preservam implementações legadas.
- Destinos carregam somente tipo/string ou IDs string validados. Saver/restorer,
  classificação da aba Exercícios e shells sem criação de sessão, rota, BLE,
  GPS ou serviço foram integrados.

### Arquivos alterados

- `app/src/main/AndroidManifest.xml`
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/model/AppPreferences.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/model/RepositoryContracts.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/preferences/AppPreferencesStore.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/navigation/AppDestination.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/navigation/ChampionNavigation.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/exercises/WorkoutShells.kt`
- este arquivo e `013-00-overview.md`

### Comandos executados, resultados e evidências

- `git diff --check`: passou.
- `./gradlew lint assembleDebug assembleRelease`: `BUILD SUCCESSFUL`; lint,
  compilação Kotlin, assemble debug e release concluídos sem keystore local.
- `./gradlew installDebug`: `BUILD SUCCESSFUL`; APK instalado no dispositivo
  `SM-S901E - 16`.
- Manifestos mesclados debug/release em
  `app/build/intermediates/merged_manifests/*/process*Manifest/AndroidManifest.xml`
  contêm FINE/COARSE, foreground service, notificação e
  `WRITE_EXERCISE_ROUTE`; não contêm `ACCESS_BACKGROUND_LOCATION`.
- Revisão independente: aprovada com ressalvas, sem bloqueadores. Atualização
  específica do countdown foi corrigida para editar somente sua chave DataStore.
- Nenhum teste automatizado foi criado, alterado ou executado, conforme regra
  explícita da Fase 13.

### Riscos residuais

- Solicitação runtime, readiness, countdown funcional e fluxo de permissão
  permanecem em `013-06/07`.
- Foreground service, declaração de classe, notificação funcional e captura
  permanecem em `013-08`.
- Shells estão passivos e navegação está preparada; CTA de entrada e ações de
  sessão pertencem às subtarefas posteriores.
- Implementações futuras que usem apenas método default de atualização do
  contrato devem preferir edição atômica da chave; implementação de produção já
  sobrescreve o método sem snapshot completo.

### Estado de conclusão

- Critérios desta subtarefa atendidos; nenhuma subtarefa seguinte iniciada.
- Próxima subtarefa elegível: `013-05`.
