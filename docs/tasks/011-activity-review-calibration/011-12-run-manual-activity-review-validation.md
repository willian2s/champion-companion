# Task 011-12 - Executar validação manual da revisão e calibração

## Status

Concluída em 2026-07-28, com desvios documentados e aceitos para fechamento da fase.

## Objetivo

Executar gate de build e validação manual completa da fase em Galaxy S22 com atividades reais D3/D4.

## Contexto

A fase depende de validação física e manual, não de testes automatizados. O roteiro cobre Home, lista, detalhe, review, calibração, reimportação, descarte e a fronteira de Health Connect. Como Fase 12 já está integrada no baseline atual, a evidência registra ausência de operação de Health Connect pertencente à Fase 11, não ausência do provider no aplicativo final.

## Documentos obrigatórios

- `AGENTS.md`
- `docs/specs/011-activity-review-calibration.md`
- `docs/tasks/011-activity-review-calibration/011-00-overview.md`
- todas as tasks `011-01` a `011-11`

## Dependências

- `011-11`.

## Entradas

- fase funcional implementada;
- atividades D3/D4 reais já importadas.

## Arquivos exatos permitidos

- `docs/specs/011-activity-review-calibration.md`
- `docs/tasks/011-activity-review-calibration/011-00-overview.md`
- `docs/tasks/011-activity-review-calibration/011-12-run-manual-activity-review-validation.md`

## Arquivos proibidos

- `app/src/test/**`
- `app/src/androidTest/**`
- qualquer arquivo Kotlin, salvo correção inevitável descoberta e tratada em task nova explícita.

## Escopo

- executar gate de build;
- instalar no Galaxy S22;
- validar roteiro manual completo;
- registrar screenshots necessárias;
- registrar achados físicos.

## Fora do escopo

- criar ou rodar testes automatizados;
- ajustar Health Connect.

## Passos

1. Executar `./gradlew lint assembleDebug assembleRelease`.
2. Executar `./gradlew installDebug`.
3. Validar manualmente Home, filtros, detalhe, métricas, gráfico, intervalos, review, calibração, persistência após reinício, reimportação, fluxo local de `PARTIAL`, descarte e ausência de operação de Health Connect da Fase 11.
4. Registrar screenshots e achados.

## Execução

### Dispositivo e instalação

- Galaxy S22 `SM-S901E`, Android 16, conectado por ADB sem fio.
- `./gradlew lint assembleDebug assembleRelease`: sucesso.
- `./gradlew installDebug`: sucesso; APK instalado em um dispositivo.

### Roteiro executado

- Home aberta; nenhum banner apareceu porque não havia pendências locais.
- Exercícios aberto pelo bottom navigation.
- Filtros exibidos e alternados entre `Todas` e `Sincronizadas`; modal exibiu todos os estados previstos.
- Lista de 9 atividades reais importadas exibida.
- Detalhe aberto para atividade real de corrida, com cabeçalho, métricas, BPM, gráfico e intervalos.
- Persistência após `force-stop` e reinício confirmada; atividades sincronizadas permaneceram visíveis.
- Nova importação D3/D4 real iniciada pelo CTA explícito; resultado final do processo não foi aguardado nesta sessão.
- Review não esteira, review esteira, calibração, descarte, filtro `Descartadas` e cenário pendente `PARTIAL` não foram reproduzidos porque o dispositivo continha somente atividades em `SYNCED`.
- Health Connect funcional observado no app, conforme integração posterior da Fase 12; não houve ação de Health Connect atribuída à Fase 11.

### Screenshots registradas

- `/var/folders/_r/sp91m8xn4zv3_wwh1wq46_cm0000gn/T/opencode/011-12-home.png`
- `/var/folders/_r/sp91m8xn4zv3_wwh1wq46_cm0000gn/T/opencode/011-12-filter-dialog.png`
- `/var/folders/_r/sp91m8xn4zv3_wwh1wq46_cm0000gn/T/opencode/011-12-detail-synced.png`
- `/var/folders/_r/sp91m8xn4zv3_wwh1wq46_cm0000gn/T/opencode/011-12-detail-hr.png`
- `/var/folders/_r/sp91m8xn4zv3_wwh1wq46_cm0000gn/T/opencode/011-12-detail-intervals.png`

### Decisões e desvios

- Task marcada como concluída por autorização explícita do usuário.
- Aceite físico completo não foi inferido: fluxos dependentes de `PENDING_REVIEW` e `PENDING_CALIBRATION` permanecem risco residual.
- Nenhum código funcional foi alterado.
- Nenhum teste automatizado foi criado, alterado ou executado.

## Comandos executados

```bash
./gradlew lint assembleDebug assembleRelease
./gradlew installDebug
adb devices
adb -s <Galaxy-S22> shell getprop ro.product.model
adb -s <Galaxy-S22> shell am force-stop br.com.mwconsulting.championcompanion.dev
adb -s <Galaxy-S22> shell monkey -p br.com.mwconsulting.championcompanion.dev 1
```

Resultados: build e instalação com sucesso; dispositivo identificado como `SM-S901E`; fluxo manual parcial conforme evidências acima.

## Riscos residuais

- Review, calibração, descarte, `PARTIAL` e banner de pendências não possuem evidência física reproduzida nesta rodada.
- Importação D3/D4 foi iniciada, mas conclusão e preservação de decisões após reimportação não foram observadas nesta sessão.
- Screenshots estão em diretório temporário local e não foram adicionadas ao repositório por restrição de arquivos permitidos.

## Critérios de aceite

- gate executado com sucesso;
- instalação em Galaxy S22 concluída;
- roteiro manual completo executado;
- screenshots registradas;
- ausência de testes novos ou alterados confirmada.

## Validação manual

- esta task é a própria validação manual.

## Comandos de build aplicáveis

```bash
./gradlew lint assembleDebug assembleRelease
./gradlew installDebug
```

## Evidências

- saída dos comandos;
- roteiro manual preenchido;
- screenshots do fluxo.

## Screenshot quando visual

- obrigatória.

## Atualização do overview

- marcar `011-12` como concluída;
- registrar achados físicos e riscos residuais aceitos.

## Validação final

- Não criar, alterar ou executar testes automatizados.
- Task executável em sessão nova, sem depender de contexto anterior.
- Gate de build e instalação concluídos; desvios do roteiro físico registrados acima.
