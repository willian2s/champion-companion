# 024-06 — Refatorar frequência cardíaca

- **Ticker:** `024`
- **Número:** `06`
- **Status:** pending

## Objetivo e resultado esperado

Refatorar detalhe de frequência cardíaca, organizando resumo, medição manual,
importação D1, gráfico e histórico. Resultado: ações e estados operacionais
claros sem alterar protocolos, persistência ou confirmação de saída.

## Requisitos e critérios cobertos

- R4–R9; critérios 5, 7–12 da spec.

## Escopo incluído

- header e retorno;
- resumo atual e origem;
- medição manual, progresso, cancelamento e retry;
- pull-to-refresh D1, metadata e feedback;
- períodos, gráfico, resumo acessível e histórico.

## Escopo excluído

- `ManualHeartRateCoordinator`, protocolo D0/F002, coordinator D1;
- cálculo, repository, persistência, sync ou novo teste;
- nova coleta ou leitura Health Connect.

## Dependências

- `024-05` concluída;
- relógio/estado real disponível somente quando necessário e seguro.

## Arquivos e símbolos prováveis

- `feature/health/HealthMetricDetailScreens.kt`;
- `feature/health/HeartRateHistoryChart.kt`;
- `HeartRateDetailScreen` e Composables locais de resumo, operação e histórico.

## Passos de implementação

1. Mapear todos os ramos de `HealthUiState` antes do diff.
2. Separar visualmente resumo, ações, tendência e histórico.
3. Tornar medição e importação distinguíveis, mantendo gatilhos explícitos.
4. Preservar confirmação de saída e resultado parcial não persistido.
5. Manter gráfico com períodos, lacunas, unidade e descrição acessível.
6. Validar estados seguros disponíveis; marcar operações não exercidas `N/A`.
7. Revisar ausência de mudança em intents/callbacks e registrar evidências.

## Testes e comandos de validação

Sem testes automatizados.

```bash
git diff --check
./gradlew lint assembleDebug assembleRelease
./gradlew installDebug
adb devices -l
adb shell am start -n br.com.mwconsulting.championcompanion.dev/br.com.mwconsulting.championcompanion.MainActivity
```

Smoke: Saúde → Frequência cardíaca, períodos, rolagem, back; medição/D1 somente
se seguro, sem gerar dado para evidência; fonte `1,3x`, landscape e temas.

## Definição de pronto

- resumo, ações, gráfico e histórico possuem hierarquia clara;
- medição/D1 e confirmações preservadas;
- nenhum valor sintético ou resultado parcial;
- evidência sanitizada e estados `N/A` registrados;
- comandos aprovados e overview atualizado.

## Riscos e cuidados

- tela é hotspot com operações bloqueantes;
- não cancelar operação real sem consentimento;
- histórico pode conter métricas pessoais: sanitizar captura;
- mudanças de composição não podem reiniciar intents ou collectors.
