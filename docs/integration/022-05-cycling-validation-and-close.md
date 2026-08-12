# Validação de ciclismo — 022-05

- **Spec:** `022-cycling-workouts`
- **Subtarefa:** `022-05`
- **Data:** 2026-08-12
- **Status:** concluída
- **Escopo:** validação física parcial, sem publicação de identificadores ou dados de saúde desnecessários

## Ambiente

- APK debug instalado sobre instalação existente do pacote de desenvolvimento.
- Dispositivo físico Android 16 conectado por ADB durante parte da validação.
- Health Connect disponível para a escrita exercitada.
- Nenhum `pm clear`, uninstall, wipe ou corrupção artificial foi executado.

## Gate A — Upgrade do banco

Evidência observada após `installDebug` e abertura do app:

- `PRAGMA user_version`: `12`;
- `PRAGMA integrity_check`: `ok`;
- `workout_activities`: 7 linhas;
- `watch_workout_imports`: 7 linhas;
- `workout_derivation_snapshots`: 8 linhas;
- `workout_derived_intervals`: 168 linhas;
- `workout_heart_rate_samples`: 152 linhas.

O banco abriu sem wipe e a migration v11→v12 foi aplicada. A navegação e a
revisão subsequentes não criaram novas operações Health Connect além das já
existentes.

## Gate B — Bicicleta sem distância

Executado com atividade D3/D4 de 11/08, originalmente sugerida como treino
livre, sem informar distância. O upload foi deliberadamente omitido para não
duplicar atividade já sincronizada no app de produção.

Resultado local observado:

- `confirmedType = CYCLING`;
- `workflowState = READY_TO_SYNC`;
- `manualDistanceMeters = NULL`;
- duração oficial, BPM, calorias e passos/raw preservados;
- distância e velocidade média ficaram ausentes;
- nenhum `PENDING_CALIBRATION` foi criado;
- contagem de operações Health Connect permaneceu `88`;
- nenhum filho `DISTANCE` ou `SPEED` foi criado;
- nenhum upload Health Connect foi executado neste cenário.

O upload não foi repetido neste cenário para evitar duplicação de atividade já
sincronizada no app de produção; a escrita Health Connect foi validada no Gate C.

## Gate C — Bicicleta com distância

Executado usando atividade D3/D4 pendente de revisão, originalmente sugerida
como corrida. A atividade foi explicitamente revisada como `Bicicleta` com
distância manual válida.

Resultado local observado:

- `confirmedType = CYCLING`;
- `workflowState = READY_TO_SYNC` antes da escrita remota;
- distância manual persistida em metros;
- duração oficial usada no cálculo;
- UI exibiu distância e velocidade média;
- cálculo independente confirmou distância / duração oficial;
- UI manteve calorias e BPM disponíveis;
- UI não exibiu pace, cadência de passos ou passos como métrica de bicicleta.

Após sincronização explícita, atividade ficou `SYNCED`. Operações locais
observadas para atividade foram exatamente:

```text
DISTANCE / manual-distance / SYNCED / INSERT
EXERCISE_SESSION / exercise-session / SYNCED / INSERT
HEART_RATE / heart-rate-series / SYNCED / INSERT
TOTAL_CALORIES_BURNED / total-calories-burned / SYNCED / INSERT
```

Não foram observadas operações `DistanceInterval` ou `SpeedSeries`. Frozen
payload da distância continha chave `manual-distance` e valor válido; payload da
sessão continha o tipo genérico de biking. Não foi feito readback remoto;
Health Connect permanece write-only.

## Gate D — Regressão dos tipos existentes

**Passou.** Atividade `RUNNING` existente permaneceu `SYNCED`, com label,
distância, snapshots e métricas legíveis. Atividades `FREE_WORKOUT` e os dois
treinos de 11/08 também permaneceram disponíveis. Calendário exibiu 2
atividades em 11/08 e detalhe diário não perdeu atividade nem inventou resumo ou
distância agregada. Nenhuma atividade `SYNCED` foi alterada.

## Gate E — Efeitos colaterais

**Passou para efeitos observados.** Código e fluxo não introduziram GPS, rota ou
treino phone para bicicleta. Abrir detalhe, calendário e detalhe diário não
alterou as 88 operações locais. Upload foi omitido deliberadamente.

## Comandos e resultados

```text
git diff --check                         PASS
./gradlew lint assembleDebug assembleRelease  PASS
./gradlew installDebug                   PASS
adb devices                              PASS; dispositivo reconectado
adb shell am start ...MainActivity      PASS
adb shell uiautomator dump ...           PASS; exercícios, revisão e calendário
consultas SQLite locais                  PASS; v12, integridade, contagens e operações
```

Testes automatizados não foram criados nem executados, conforme regras do
repositório e escopo da spec.

## Estado de aceite

Passaram: upgrade sem wipe com integridade v12; bicicleta com e sem distância;
velocidade média local somente com distância; distância/velocidade ausentes sem
distância; ausência de métricas de passos na UI de bicicleta; projeção genérica
de biking e um único `ManualDistance` no cenário sincronizado; preservação local
de BPM/calorias/passos; calendário sem efeito remoto; build e instalação.

Não houve pendência funcional identificada nesta rodada. O upload do caso sem
distância não foi repetido para evitar duplicação; o cenário sincronizado com
distância confirmou funcionamento Health Connect.

## Riscos residuais

- Health Connect foi validado por operações locais de escrita e payload congelado,
  sem leitura remota.
