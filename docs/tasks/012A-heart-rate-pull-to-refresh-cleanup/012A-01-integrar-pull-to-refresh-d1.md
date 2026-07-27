# Task 012A-01 - Integrar pull-to-refresh D1

## Status

completed em 2026-07-27

## Objetivo

Adicionar pull-to-refresh à tela de frequência cardíaca como novo gatilho explícito da importação D1, preservando regras de concorrência, acessibilidade e ausência de auto-BLE passivo.

## Resultado esperado

- `HeartRateDetailScreen` usa `PullToRefreshBox`;
- o indicador de refresh acompanha o estado D1 em andamento;
- o gesto não inicia D1 durante medição manual nem durante importação D1 ativa;
- a tela expõe ação acessível equivalente ao gesto.

## Requisitos cobertos

- substituir o botão permanente de busca por gesto explícito de refresh;
- preservar D1 como ação manual;
- bloquear concorrência com medição manual e com D1 já ativo;
- manter comportamento passivo de `Load`, `ON_START` e `ON_RESUME`.

## Escopo incluído

- `feature/health/HealthMetricDetailScreens.kt`
- `feature/health/HealthViewModel.kt`, se necessário para centralizar semântica do refresh
- `feature/health/HealthFeatureContract.kt`, somente se a solução exigir intent dedicada

## Escopo excluído

- Room, repositories, BLE core, coordinator D1 e navegação;
- roadmap, ADR e baseline;
- criação de testes novos.

## Dependências

- nenhuma.

## Arquivos e símbolos prováveis

- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/health/HealthMetricDetailScreens.kt`
- `HeartRateDetailScreen`
- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/health/HealthViewModel.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/health/HealthFeatureContract.kt`

## Passos de implementação

1. Envolver `HeartRateDetailScreen` em `PullToRefreshBox`, preservando `verticalScroll`, back handling e ciclo de vida atuais.
2. Derivar `isRefreshing` do estado D1 existente.
3. Rotear `onRefresh` para o fluxo D1 explícito já existente, sem ligá-lo a `Load`.
4. Garantir bloqueio do gesto quando houver medição manual ou importação D1 ativas.
5. Adicionar ação acessível equivalente ao gesto, alinhada ao padrão da Home.
6. Se a UI ficar com regra demais, promover a decisão para intent dedicada no contrato da feature.

## Testes e comandos de validação

Não criar testes novos.

Validação manual esperada nesta subtarefa:

- puxar para atualizar no topo da tela;
- confirmar início de apenas uma importação D1;
- confirmar que o gesto não inicia D1 durante medição manual;
- confirmar que retorno à tela e `Load` continuam passivos.

## Definição de pronto

- refresh explícito funcional na tela;
- concorrência preservada;
- acessibilidade do refresh explícita;
- subtarefa e overview atualizados com status e evidências.

## Riscos e cuidados

- não quebrar a convivência entre `PullToRefreshBox` e `verticalScroll`;
- não iniciar BLE por eventos passivos de lifecycle;
- não permitir múltiplas importações D1 concorrentes.

## Implementação concluída

- `HeartRateDetailScreen` passou a usar `PullToRefreshBox`, preservando a rolagem vertical, o tratamento de retorno e os eventos de lifecycle existentes.
- `isRefreshing` deriva dos estados ativos `Preparing`, `Connecting`, `Reading` e `Persisting` do D1.
- O callback compartilhado de refresh só envia `HealthIntent.StartD1Import` quando não há medição manual nem importação D1 em andamento.
- A raiz semântica da tela expõe a ação acessível `Atualizar histórico do relógio`, reutilizando o mesmo callback guardado.
- Os CTAs, o painel D1 e os textos atuais foram preservados para a subtarefa `012A-02`.

## Comandos executados

```bash
git diff --check
./gradlew :app:compileDebugKotlin --console=plain
./gradlew app:lintDebug --console=plain
./gradlew lint assembleDebug assembleRelease --console=plain
./gradlew installDebug --console=plain
adb devices
adb shell am start -n br.com.mwconsulting.championcompanion.dev/br.com.mwconsulting.championcompanion.MainActivity
adb shell input swipe 540 500 540 1500 700
adb shell am force-stop br.com.mwconsulting.championcompanion.dev
```

## Testes e resultados

- `git diff --check`: concluído sem erros.
- `:app:compileDebugKotlin`: `BUILD SUCCESSFUL`.
- `app:lintDebug`: `BUILD SUCCESSFUL`.
- `lint assembleDebug assembleRelease`: primeira execução falhou por estado incremental transitório, com referências internas existentes temporariamente não resolvidas; após o daemon ficar ocioso e a compilação focada restaurar os outputs, a repetição concluiu com `BUILD SUCCESSFUL`.
- `installDebug`: `BUILD SUCCESSFUL`; APK instalado no dispositivo `SM-S901E`.
- Validação física: a tela abriu sem iniciar D1 automaticamente; o gesto no topo iniciou a importação e apresentou a ação contextual `Cancelar importação`.
- Bloqueio durante medição manual e ausência de múltiplos disparos foram conferidos pela guarda compartilhada no callback; não houve sessão física manual completa nesta subtarefa.
- A ação acessível foi conferida na árvore semântica declarada no código; não houve sessão manual com TalkBack.
- Nenhum teste automatizado foi criado ou executado, conforme regra do projeto.

## Evidências

- Arquivo de produção alterado: `app/src/main/java/br/com/mwconsulting/championcompanion/feature/health/HealthMetricDetailScreens.kt`.
- O diff ficou restrito à integração do pull-to-refresh e à ação semântica da `012A-01`.
- `HealthIntent.Load`, `ON_START` e `ON_RESUME` continuam apenas recarregando estado local.
- Nenhum contrato, `ViewModel`, coordinator BLE, repository, banco, navegação, baseline, roadmap ou ADR foi alterado.
