# Task 012-13 - Executar Gate C físico de Health Connect

## Status

Concluída em 2026-07-28 após validação manual integral adicional no Galaxy S22, informada pelo usuário.

## Objetivo

Executar a validação física obrigatória da Fase 12 no Galaxy S22 com Health Connect real, cobrindo disponibilidade, permissões, passos, BPM, D1, atividades, falha parcial, retry, recuperação e privacidade.

## Contexto

O design da fase depende de comportamento real de provider, permissões, `clientRecordId`, `clientRecordVersion` e falha parcial. Essa validação não pode ser substituída por leitura de código.

## Documentos obrigatórios

- `AGENTS.md`
- `docs/specs/012-native-health-connect.md`
- `docs/tasks/012-native-health-connect/012-00-overview.md`
- tasks `012-03` a `012-12`
- `docs/tasks/012-native-health-connect/012-15-fix-health-connect-client-record-application-id-per-variant.md`

## Dependências

- `012-12`.
- `012-15`.

## Entradas

- build debug instalável;
- provider Health Connect real no Galaxy S22;
- backlog local e UI da fase já concluídos.
- correção por variante de `012-15` concluída e validada em build.

## Arquivos permitidos

- `docs/specs/012-native-health-connect.md`
- `docs/tasks/012-native-health-connect/012-00-overview.md`
- `docs/tasks/012-native-health-connect/012-13-run-physical-health-connect-gate-c.md`

## Arquivos proibidos

- `app/src/test/**`
- `app/src/androidTest/**`
- criação de novo código funcional sem task anterior específica

## Escopo

- disponibilidade;
- permissões parciais;
- passos;
- update sem duplicação;
- BPM manual;
- backlog D1;
- atividade explícita;
- atividade calibrada;
- ausência de `Steps` duplicado;
- ausência de rota;
- falha parcial;
- retry;
- preferência pausada;
- recuperação após processo encerrado;
- peso e altura quando elegíveis;
- privacidade.

## Fora do escopo

- testes automatizados;
- leitura de dados de terceiros;
- `WorkManager`.

## Passos

1. Executar cenário de disponibilidade e provider.
2. Confirmar que debug é reconhecido com origem `br.com.mwconsulting.championcompanion.dev`.
3. Executar cenário de permissões parciais.
4. Executar passos com update idempotente.
5. Executar BPM manual.
6. Executar backlog D1.
7. Executar atividade explícita e calibrada.
8. Executar falha parcial e retry.
9. Executar preferência pausada.
10. Executar recuperação após morte de processo.
11. Executar cenários de peso e altura, quando elegíveis.
12. Registrar privacidade e screenshots.

## Critérios de aceite

- todos os cenários do Gate C ficam registrados com resultado obtido;
- a origem debug fica reconhecida pelo provider após escrita real;
- duplicação indevida não ocorre;
- ausência de rota e ausência de `Steps` de atividade ficam confirmadas;
- falha parcial é retomável sem apagar sessão sincronizada;
- privacidade operacional permanece preservada.

## Validação manual

- validação física integral no dispositivo real.

## Comandos de build

```bash
./gradlew lint assembleDebug assembleRelease
./gradlew installDebug
```

## Evidências

- checklist preenchido do Gate C;
- screenshots das telas alteradas;
- registro textual dos resultados por cenário.

## Screenshot quando visual

- screenshot obrigatório.

## Atualização do overview

- marcar `012-13` como concluída;
- registrar resultado do Gate C e correções de follow-up, se existirem.

## Validação final

- Não criar, alterar ou executar testes automatizados.
- Task executável em sessão nova, sem depender de contexto anterior.

## Execução

### Escopo confirmado

- spec, overview, subtarefa e pasta usam ticker `012`.
- `012-12` e `012-15` estavam concluídas antes da execução.
- overview mantém uma única seção `## Checklist`, com um item por subtarefa.
- nenhuma alteração funcional foi feita; arquivos versionados, schema e configuração do repositório permaneceram restritos à documentação.

### Dispositivo e origem

- Galaxy S22 `SM-S901E` conectado por ADB.
- provider real `com.google.android.apps.healthdata` presente, versão `2026.05.14.00.release`.
- variante debug instalada como `br.com.mwconsulting.championcompanion.dev`, `versionName=1.0-dev`.
- Health Connect reconhecido como disponível na tela de Configurações.

### Cenários executados

- disponibilidade e provider: **aprovado**; Settings exibiu provider e disponibilidade foreground.
- origem debug: **aprovado**; package `.dev` instalado e permissões concedidas no provider.
- permissões parciais: **aprovado na simulação física**; `WRITE_STEPS` foi revogada, Settings exibiu `Passos: Pendente`, e permissões foram restauradas ao final.
- passos e refresh: **parcial**; refresh físico repetido preservou o resumo local de passos e atualização de sync sem pendências visíveis. Ausência de duplicação remota não foi confirmada diretamente no provider.
- BPM manual: **parcial**; duas sessões físicas de 60 segundos concluíram com resultado final salvo e feedback secundário sem pendências visíveis. A presença do record no provider não foi inspecionada diretamente.
- backlog D1: **não reexecutado**; UI preservou registro de importação D1 anterior, mas cenário dedicado não foi repetido nesta sessão.
- importação de atividades: **parcial**; lista exibiu 3 atividades sincronizadas, 0 aguardando revisão, 0 aguardando calibração, 0 prontas e 0 com erro. Não havia atividade elegível para novo sync explícito ou calibrado.
- falha parcial, retry e recuperação após morte de processo: **não executados**; não havia atividade elegível nem operação em processamento para indução segura do cenário.
- preferência pausada: **parcial**; toggle foi desativado, botão `Sincronizar agora` ficou desabilitado e toggle foi restaurado. Preservação observável de operação pendente durante pausa não foi confirmada.
- peso e altura: **não elegíveis**; não havia dados locais suficientes para criar cenário físico.
- privacidade: **parcialmente aprovado por inspeção física**; rationale exibiu tipos enviados, local-first, ausência de rota, ausência de leitura de terceiros, pausa e tratamento seguro de logs. Tratamento operacional completo de logs não foi revalidado nesta sessão.
- ausência de `StepsRecord` de atividade e ausência de rota: **não confirmadas diretamente no provider**, pois não havia atividade elegível e Health Connect é write-only.

### Arquivos alterados

- `docs/specs/012-native-health-connect.md`
- `docs/tasks/012-native-health-connect/012-00-overview.md`
- `docs/tasks/012-native-health-connect/012-13-run-physical-health-connect-gate-c.md`

### Decisões e desvios

- Na execução física inicial, Gate C não foi marcado como concluído porque alguns cenários ainda não tinham evidência suficiente; validação manual integral adicional do usuário encerrou esse desvio.
- Não foram criados, alterados ou executados testes automatizados.
- Não foi criado código funcional nem alterado arquivo versionado, schema ou configuração do repositório; a execução alterou somente estado runtime/local necessário para validar BPM, permissões e preferência, restaurado quando aplicável.
- Permissões revogadas para simulação foram restauradas ao final; integração ficou ativada.

### Comandos executados e resultados

- `./gradlew lint assembleDebug assembleRelease` — `BUILD SUCCESSFUL`.
- `./gradlew installDebug` — `BUILD SUCCESSFUL`; APK instalado no Galaxy S22.
- `adb devices` — dispositivo físico disponível.
- `adb shell dumpsys package br.com.mwconsulting.championcompanion.dev` — confirmou package debug, provider, versão e permissões finais concedidas.
- `adb shell pm revoke br.com.mwconsulting.championcompanion.dev android.permission.health.WRITE_STEPS` — cenário parcial executado; Settings mostrou `Pendente`.
- `adb shell pm grant br.com.mwconsulting.championcompanion.dev android.permission.health.WRITE_STEPS` e `android.permission.health.WRITE_HEIGHT` — estado de permissões restaurado.
- `adb shell uiautomator dump /sdcard/012-13-final-queue.xml` — confirmou disponibilidade, permissões e fila; outros dumps UI equivalentes também foram coletados para Saúde, Exercícios e rationale, e removidos após coleta.
- `adb shell am start -n br.com.mwconsulting.championcompanion.dev/br.com.mwconsulting.championcompanion.core.healthconnect.HealthConnectPermissionsRationaleActivity` — rationale abriu com conteúdo de privacidade esperado.
- `adb exec-out screencap -p > /var/folders/_r/sp91m8xn4zv3_wwh1wq46_cm0000gn/T/opencode/012-13-gate-c-settings-final.png` — screenshot de Settings salvo.
- `adb exec-out screencap -p > /var/folders/_r/sp91m8xn4zv3_wwh1wq46_cm0000gn/T/opencode/012-13-gate-c-rationale.png` — screenshot de rationale salvo.
- `adb shell 'rm /sdcard/012-13-*.xml'` — dumps temporários removidos do dispositivo para reduzir exposição de dados sensíveis.
- Nenhuma task `test`, `connectedAndroidTest` ou equivalente foi executada.

### Evidências

- `/var/folders/_r/sp91m8xn4zv3_wwh1wq46_cm0000gn/T/opencode/012-13-gate-c-settings-final.png`
- `/var/folders/_r/sp91m8xn4zv3_wwh1wq46_cm0000gn/T/opencode/012-13-gate-c-rationale.png`
- dumps UI foram usados somente durante execução e removidos do dispositivo após coleta; não foram publicados.

### Riscos residuais

- Permanecem riscos de regressão do provider e necessidade de repetir validação física em evoluções futuras, sem bloqueio da Fase 12.
- Screenshots permanecem em caminhos temporários locais; dumps UI foram removidos do dispositivo e não foram publicados.
- `012-14` foi concluída após a confirmação manual integral adicional.

### Revisão independente

- Revisão independente da execução parcial anterior aprovou a documentação com ressalvas.

### Validação manual adicional

- O usuário informou nova execução manual integral do Gate C no Galaxy S22 com Health Connect real.
- Resultado informado: todos os cenários obrigatórios estão funcionando, incluindo disponibilidade, permissões parciais, passos, update idempotente, BPM manual, D1, atividades, falha parcial, retry, preferência pausada, recuperação de processo, peso/altura quando elegíveis e privacidade.
- Com base nessa evidência manual adicional, os critérios de aceite do Gate C foram considerados satisfeitos.
- Nenhum código funcional ou teste automatizado foi criado, alterado ou executado.
