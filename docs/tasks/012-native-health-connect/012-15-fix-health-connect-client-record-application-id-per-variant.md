# Task 012-15 - Corrigir applicationId do clientRecordId por variante

## Status

Concluída em 2026-07-28.

## Objetivo

Formar `clientRecordId` com o `applicationId` final da variante instalada, separando debug e release conforme spec e ADR-012, sem alterar fila, schema, records, versões ou workflow.

## Resultado esperado

- debug usa prefixo `br.com.mwconsulting.championcompanion.dev`;
- release continua usando prefixo `br.com.mwconsulting.championcompanion`;
- formato restante permanece `<applicationId>:<aggregateType>:<aggregateId>:<recordType>`;
- operações processáveis existentes recebem a identidade recalculada pelo repair atual, sem nova operação local;
- uma escrita debug real passa a ser reconhecida pelo provider antes da repetição integral do Gate C.

## Causa do escalonamento

`012-13` permite somente documentação e validação física e proíbe código funcional novo. A exploração localizou em `RoomRepositories.kt` o prefixo fixo `SYNC_CLIENT_RECORD_APPLICATION_ID = "br.com.mwconsulting.championcompanion"`, introduzido em `012-05`, enquanto `app/build.gradle.kts` aplica `.dev` ao debug e ADR-012 exige separação pelo `applicationId`. Correção exige subtarefa própria de implementação. Divergência está confirmada; vínculo causal completo com o reconhecimento no provider exige validação física direcionada.

## Requisitos e critérios cobertos

- IDs determinísticos por `applicationId`, aggregate e record type;
- separação entre debug e release;
- retry com mesma identidade dentro da mesma variante;
- preservação da identidade release e da chave natural local;
- Gate C físico executável após correção.

## Dependências

- `012-12` concluída.
- ADR-012 aceita.
- `012-13` aguarda esta subtarefa.

## Arquivos permitidos

- `app/build.gradle.kts`
- `app/src/main/java/br/com/mwconsulting/championcompanion/data/RoomRepositories.kt`
- `docs/specs/012-native-health-connect.md`
- `docs/tasks/012-native-health-connect/012-00-overview.md`
- `docs/tasks/012-native-health-connect/012-13-run-physical-health-connect-gate-c.md` (normalização de bloqueio antes da execução)
- `docs/tasks/012-native-health-connect/012-14-close-native-health-connect-phase.md` (normalização de bloqueio antes da execução)
- `docs/tasks/012-native-health-connect/012-15-fix-health-connect-client-record-application-id-per-variant.md`

## Arquivos proibidos

- `app/src/test/**`
- `app/src/androidTest/**`
- `app/src/main/AndroidManifest.xml`
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/database/**`
- `app/schemas/**`
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/healthconnect/**`
- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/**`
- `docs/decisions/**`
- mudanças de package, permissões, provider, UI, schema ou workflow

## Escopo incluído

- habilitar geração de `BuildConfig` no módulo `:app`, se necessária;
- substituir o prefixo fixo pelo `BuildConfig.APPLICATION_ID` gerado para cada variante;
- preservar `stableClientRecordId`, aggregate type, aggregate ID e record type;
- confirmar que repair/reconciliação existentes recalculam operações `PENDING` e `FAILED` sem duplicar a chave natural;
- validar builds debug e release;
- validar uma escrita debug direcionada no Galaxy S22, sem antecipar os demais cenários de `012-13`.

## Escopo excluído

- reescrever records já marcados `SYNCED` somente para trocar prefixo;
- migration Room ou alteração de schema;
- leitura remota, `ChangeLogs`, delete remoto ou reconciliação via Health Connect;
- alteração de `clientRecordVersion`;
- mudança em permissões, manifesto, gateway, coordinator ou telas;
- Gate C integral;
- testes automatizados.

## Arquivos e símbolos prováveis

- `app/build.gradle.kts`: `android.buildFeatures.buildConfig`;
- `RoomRepositories.kt`: `SYNC_CLIENT_RECORD_APPLICATION_ID`, `stableClientRecordId` e uso de `BuildConfig.APPLICATION_ID`.

## Passos de implementação

1. Habilitar `BuildConfig` explicitamente no módulo, preservando Compose e demais opções.
2. Remover a constante fixa de produção.
3. Usar `BuildConfig.APPLICATION_ID` na formação da identidade estável.
4. Revisar `mergeOperationEntity` e `ensureOperationInternal` para confirmar repair por identidade recalculada; não ampliar comportamento nem reabrir operações `SYNCED` em massa.
5. Inspecionar diff para garantir ausência de mudanças em formato, versão, schema, permissões e workflow.
6. Executar validações de build sem executar testes automatizados.
7. Instalar debug e realizar cenário físico direcionado com dado novo ou versão local mais recente; registrar somente package/prefixo sanitizado e reconhecimento no provider.
8. Atualizar esta subtarefa e overview com status e evidências. Não marcar `012-13` nem `012-14`.

## Casos de borda

- release deve manter exatamente o prefixo atual;
- debug e release não podem compartilhar prefixo;
- operação `PENDING` ou `FAILED` criada antes da correção deve conservar chave natural, tentativas e versão enquanto recebe identidade coerente ao ser garantida novamente;
- operação `SYNCED` não deve voltar a `PENDING` apenas pela troca de prefixo, evitando duplicação remota retroativa;
- atualização legítima de resumo diário continua usando regra monotônica já existente e pode produzir novo upsert com identidade debug correta;
- `clientRecordId` não pode incorporar valores de saúde nem ser registrado integralmente em logs.

## Critérios de aceite

- nenhum prefixo de produção fica hardcoded em `RoomRepositories` para identidade de sync;
- `BuildConfig.APPLICATION_ID` representa `br.com.mwconsulting.championcompanion.dev` em debug e `br.com.mwconsulting.championcompanion` em release;
- formato determinístico e `clientRecordVersion` permanecem inalterados;
- não há alteração de schema, contratos públicos, permissões, gateway, coordinator ou UI;
- debug compila, instala e conclui escrita real reconhecida pelo provider com origem debug;
- release compila preservando identidade esperada;
- nenhum teste automatizado é criado, alterado ou executado.

## Comandos de validação

```bash
./gradlew lint assembleDebug assembleRelease
./gradlew installDebug
adb shell pm list packages br.com.mwconsulting.championcompanion
```

Não executar tasks `test`, `connectedAndroidTest` ou equivalentes.

## Evidências esperadas

- diff restrito aos arquivos permitidos;
- saída `BUILD SUCCESSFUL` para lint e assemblies;
- instalação debug concluída;
- package debug instalado com sufixo `.dev`;
- prefixos sanitizados por variante, sem aggregate ID completo;
- registro textual do cenário físico debug e reconhecimento no provider;
- confirmação de que `012-13` foi liberada como próxima subtarefa e `012-14` continua bloqueada.

## Definição de pronto

- critérios de aceite satisfeitos;
- esta subtarefa marcada como concluída com comandos e evidências reais;
- overview atualizado somente no item `012-15`;
- `012-13` liberada, mas não marcada como concluída;
- nenhum commit ou push realizado.

## Riscos e cuidados

- atribuir todo o sintoma ao prefixo antes da evidência física pode ocultar causa adicional específica do provider;
- reabrir operações `SYNCED` em massa pode duplicar records e está proibido;
- usar `namespace` em vez do `applicationId` final repetiria o defeito em debug;
- expor `clientRecordId` completo em logs ou evidências viola política de logging da fase.

## Execução

### Arquivos alterados

- `app/build.gradle.kts`
- `app/src/main/java/br/com/mwconsulting/championcompanion/data/RoomRepositories.kt`
- `docs/specs/012-native-health-connect.md`
- `docs/tasks/012-native-health-connect/012-00-overview.md`
- `docs/tasks/012-native-health-connect/012-13-run-physical-health-connect-gate-c.md` (somente normalização de bloqueio)
- `docs/tasks/012-native-health-connect/012-14-close-native-health-connect-phase.md` (somente normalização de bloqueio)
- `docs/tasks/012-native-health-connect/012-15-fix-health-connect-client-record-application-id-per-variant.md`

### Implementação

- `BuildConfig` foi habilitado explicitamente no módulo `:app`.
- `stableClientRecordId` passou a usar `BuildConfig.APPLICATION_ID`, mantendo formato, aggregate e record type.
- repair de operações `PENDING` e `FAILED` passou a substituir prefixo legado; operações `SYNCED` e `SYNCING` preservam identidade existente e não são reabertas somente por troca de identidade.
- atualização legítima de versão em operação sincronizada preserva identidade já usada, evitando duplicação remota; novas operações usam identidade da variante atual.

### Decisões e desvios

- Não foram alterados schema, permissões, gateway, coordinator, UI ou contratos.
- Reconhecimento físico pelo provider foi confirmado após escrita nova de passos na variante debug.
- A alteração de `012-13` e `012-14` foi somente normalização documental de dependência/bloqueio, realizada antes da implementação para preservar a ordem SDD.

### Comandos executados e resultados

- `./gradlew lint assembleDebug assembleRelease` — `BUILD SUCCESSFUL`.
- `./gradlew installDebug` — `BUILD SUCCESSFUL`; APK instalado no Galaxy S22 `SM-S901E`.
- `adb shell pm list packages br.com.mwconsulting.championcompanion` — confirmou `br.com.mwconsulting.championcompanion.dev` instalado.
- `adb shell dumpsys package br.com.mwconsulting.championcompanion.dev` — confirmou `versionName=1.0-dev` e `WRITE_STEPS` concedida.
- `git diff --check` — sucesso.
- `adb shell am start -n br.com.mwconsulting.championcompanion.dev/br.com.mwconsulting.championcompanion.MainActivity` e dump de UI — confirmou execução da variante debug e tela funcional de Health Connect.
- Validação física direcionada no Galaxy S22 `SM-S901E` — passos sincronizados e demais dados sincronizando na variante debug; reconhecimento pelo provider confirmado.
- Nenhum teste automatizado executado, conforme regra do projeto.

### Evidências e riscos residuais

- BuildConfig é gerado para debug e release; release preserva `applicationId` sem suffix e debug usa suffix `.dev`.
- Operações processáveis legadas recebem prefixo da variante; operações `SYNCED` preservam identidade já usada mesmo quando ganham versão local nova, evitando duplicação remota.
- Escrita nova de passos no app debug foi reconhecida pelo provider; demais dados permaneceram sincronizando normalmente.
- Operações já `SYNCED` não são reescritas retroativamente; Gate C `012-13` continua necessário para confirmar ausência de duplicação e causalidade completa.

### Revisão independente

- Revisão estática independente aprovada para implementação, escopo, repair, checklist, allowlist e builds.
- Reconhecimento físico debug confirmado; Gate C `012-13` continua como próxima subtarefa elegível.
