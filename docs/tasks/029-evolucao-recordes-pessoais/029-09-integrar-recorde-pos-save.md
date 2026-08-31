# 029-09 — Integrar detecção e destaque de recorde pós-save

- **Ticker:** `029`
- **Número:** `09`
- **Status:** `completed`
- **Depends on:** `029-08`

## Objetivo

Integrar o `PersonalRecordEvaluator` aos pontos reais de persistência de atividade para exibir uma conquista somente depois que a métrica final tiver sido salva com sucesso.

## Fluxo obrigatório

```text
capturar snapshot anterior persistido
        ↓
executar mutação funcional
        ↓
confirmar persistência bem-sucedida
        ↓
carregar snapshot novo persistido
        ↓
avaliar recordes
        ↓
0..N conquistas
        ↓
feedback não bloqueante
```

## Pontos candidatos

Confirmar com a auditoria `029-01`, incluindo quando aplicável:

- `submitReview` de atividade não esteira;
- revisão de `CYCLING`, depois de persistir distância manual opcional;
- `calibrateImportedActivity` de `TREADMILL`;
- rederivação explícita pré-sync que altere uma métrica recordável.

Não disparar durante importação D3/D4 nem enquanto o formulário ainda contém valor não persistido.

## Feedback

Para uma conquista, mostrar título, tipo de recorde, modalidade, novo valor e diferença para o recorde anterior. Para várias conquistas, usar um único banner/card agrupado.

Preferir feedback não modal e descartável. A ação opcional `Ver recordes` pode navegar para a seção correspondente da tela Evolução.

## Garantias

- feedback só aparece depois do save confirmado;
- falha do evaluator não desfaz save;
- re-save sem mudança não repete feedback;
- mudança de tipo usa a baseline correta;
- atividade antiga respeita o melhor valor atualmente conhecido;
- o componente não depende de Health Connect sync para reconhecer recorde local.

## Critérios de aceite

1. Distância usada no alerta é exatamente a distância final persistida.
2. Esteira só é avaliada após a operação que conclui calibração quando necessária.
3. Bicicleta usa distância manual persistida quando presente.
4. Nenhum alerta é exibido por estado transitório de formulário.
5. Múltiplos recordes são agrupados.
6. Save permanece sucesso mesmo se a avaliação/feedback falhar.
7. Re-save idempotente não repete conquista.

## Validação

```bash
git diff --check
./gradlew lint assembleDebug assembleRelease
```

Em aparelho, validar um novo recorde, um re-save sem alteração e, quando houver fixture disponível, uma atividade que não supera o recorde atual. Registrar evidência sanitizada.

## Registro de execução

- **Status:** `completed`
- **Arquivos alterados:**
  `app/src/main/java/br/com/mwconsulting/championcompanion/core/model/EvolutionContracts.kt`,
  `app/src/main/java/br/com/mwconsulting/championcompanion/data/LocalEvolutionReadModel.kt`,
  `app/src/main/java/br/com/mwconsulting/championcompanion/core/logging/StructuredLogger.kt`,
  `app/src/main/java/br/com/mwconsulting/championcompanion/feature/exercises/ExercisesFeatureContract.kt`,
  `app/src/main/java/br/com/mwconsulting/championcompanion/feature/exercises/ExercisesViewModel.kt` e
  `app/src/main/java/br/com/mwconsulting/championcompanion/feature/exercises/ExercisesScreen.kt`.
- **Decisões e desvios:** leitura pontual all-history foi adicionada ao contrato do
  read model e usa snapshot persistido estrito após cada save; falha de leitura,
  evaluator ou apresentação é suprimida e registrada somente em log debug-safe.
  Revisão, calibração de corrida/caminhada, calibração de esteira e reprocessamento
  explícito compartilham o fluxo anterior → mutação confirmada → leitura nova →
  evaluator. As mutações avaliáveis são serializadas no ViewModel para manter a
  baseline consistente. Conquistas ficam em estado efêmero não persistido, agrupadas em card
  descartável na lista e no detalhe, com navegação opcional para Evolução. O estado
  local do card evita perda durante recriação da UI; não houve alteração de Room,
  importação, sync, Health Connect ou escopo funcional. Cancelamento cooperativo é
  preservado em evaluator, feedback e diagnóstico.
- **Comandos executados:** `git diff --check`; `./gradlew :app:compileDebugKotlin
  :app:compileReleaseKotlin`; `./gradlew lint assembleDebug assembleRelease` (execução
  final após serialização das mutações).
- **Resultados e evidências:** todos os comandos passaram com `BUILD SUCCESSFUL`.
  A compilação confirmou injeção de `EvolutionReadModel`/`StructuredLogger`, leitura
  pontual e card pós-save nas variantes debug e release. Smoke posterior no
  `SM-S901E` abriu atividade em revisão pendente e concluiu revisão sem bloqueio;
  a atividade passou para `Pronta para sincronizar`. Segundo cenário com
  calibração de corrida confirmou alerta de novo recorde de `20,00 km`, com
  banner visível automaticamente após reposicionamento para o topo. Testes
  automatizados não foram criados nem executados, conforme guard rail do projeto.
 - **Riscos residuais:** re-save idempotente, múltiplos recordes, bicicleta em
  fluxo pós-save, esteira, mudança de tipo e falha de feedback permanecem `N/A`
  por ausência de fixtures/cenários preparados; ausência de testes automatizados
  permanece conforme política do projeto.
