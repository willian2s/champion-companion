# Evidência física 014-15

- **Data:** 2026-08-07
- **Aplicativo:** `1.0-dev` (`versionCode=1`), working tree pós-`014-17`, sem
  commit criado; build instalado via `./gradlew installDebug`
- **Dispositivo:** Galaxy S22 `SM-S901E`, Android 16, API 36
- **Escopo:** Champion C001, importação D3/D4 explícita, revisão local e
  sincronização foreground com Health Connect real
- **Privacidade:** evidência sem identificadores do relógio, payload BLE,
  identificadores pessoais ou valores de BPM; título usado no exercício foi
  sintético.

## Evidências visuais

- `014-15-physical-validation-evidence/import-summary.png`: importação
  explícita concluída parcialmente, com revisão pendente e contagens sanitizadas.
- `014-15-physical-validation-evidence/health-connect-summary.png`: estado local
  da fila exibido como sincronizado; não é confirmação de records no provider.
- `014-15-physical-validation-evidence/reprocess-confirmation.png`: confirmação
  específica exigida para reprocessamento de atividade já sincronizada.
- `014-15-physical-validation-evidence/reprocess-blocked.png`: reprocessamento
  não altera snapshot nem operação Health Connect quando projeção futura é
  necessária.

## Resultado dos gates

| Gate | Resultado | Evidência e limite |
|---|---|---|
| A — Importação e parser | `PASS` | Ação de busca foi explícita; leitura terminou com 3/3 blocos, 2 inseridos, 1 parcial, 0 inválidos e 1 pulado. A tela não expõe `d4-v2`, `rawMotionValue` nem soma de `3.737`; esses pontos permanecem cobertos por evidência estática das tasks anteriores. |
| B — Índices, duração e plausibilidade | `N/A` | Não há fixture física controlada para lacuna, duplicata, `1.637 s` ou limiar de plausibilidade. Nenhum dado foi inventado ou redistribuído. |
| C — Perfil e derivação | `N/A` | Comprimento manual já persistido foi usado e derivação canônica apareceu no detalhe. Sugestão por altura não foi exercitada: dispositivo não tinha altura persistida. |
| D — Métricas, UI e esteira | `N/A` | Detalhe mostrou distância canônica, série acumulada com 29 intervalos, métricas derivadas, gráfico, intervalos, BPM temporal e splits. `TREADMILL` não foi exercitado nesta sessão. |
| E — Reprocessamento | `N/A` | Ação explícita e confirmação específica apareceram para legado sincronizado. Resultado confirmou que nenhum snapshot/operação foi alterado e exigiu suporte futuro de projeção; não havia fixture legado não sincronizado e recuperável para concluir reprocessamento. |
| F — Health Connect | `PASS` nos cenários exercitados | Ambas as permissões foram concedidas no package debug correto e o provider exibiu `DistanceRecord` e `SpeedRecord`. Revogações isoladas de `WRITE_SPEED` e `WRITE_DISTANCE` produziram falhas parciais nomeadas; retry após reautorização terminou `Sincronizada`, sem duplicação observada. O app permaneceu write-only. |
| G — Regressão | `N/A` | Importação, revisão, métricas, BPM, dispositivo associado e sync explícito permaneceram funcionais. Calendário e matriz completa de regressão não foram repetidos nesta sessão; nenhum fluxo phone, GPS, rota, mapa ou BLE live foi acionado. |

## Observações

- A importação foi iniciada somente pelo botão `Buscar treinos no relógio`.
- A atividade foi revisada antes do botão `Sincronizar com Health Connect`.
- Após reinício do aplicativo, dados locais permaneceram disponíveis.
- A atividade phone pronta para sincronização foi preservada sem acionamento,
  confirmando que a validação não reabriu fluxo phone.
- A tentativa inicial não exibiu distância/velocidade após o sucesso local; a
  permissão de velocidade estava ausente. Esse fato histórico motivou o
  follow-up funcional em `docs/integration/014-15-health-connect-provider-follow-up.md`.
- A revalidação posterior resolveu o bloqueio de autorização para os cenários
  exercitados. Gates B, C, D, E e G permanecem `N/A` pelas limitações físicas
  registradas, sem inventar fixtures.
- `014-15` está concluída. A observação manual do provider não representa leitura
  ampla pelo aplicativo.

## Revalidação após 014-17

- Uma atividade elegível, após aceite explícito, foi sincronizada pelo package
  debug correto com ambas as permissões concedidas.
- Health Connect exibiu manualmente um `DistanceRecord` granular de `3,463 km` e
  um `SpeedRecord` de `7,245 km/h` para `Champion Companion Dev`.
- A revogação isolada de `WRITE_SPEED` foi refletida como `Velocidade — Pendente`
  em Settings; reautorização foi concluída pelo fluxo `Conectar`.
- Resultado: autorização e escrita de distância/velocidade passam para este
  cenário. A matriz posterior confirmou retry, falha parcial e ausência de
  duplicação observada.

### Resultado do retry com treinos novos

- Treino de 15 minutos foi aceito como `Corrida`; treino de 20 minutos foi
  aceito como `Treino livre`.
- Revogação de `WRITE_SPEED` gerou falha parcial somente no filho de velocidade.
- Reautorização e retry concluíram a corrida como `Sincronizada`.
- Health Connect mostrou distância acumulada `3,517 km` e uma única entrada de
  velocidade nova `0,202 km/h` para `12:33–12:49`; ausência de duplicação
  observada manualmente.
- Resultado: Gate F passa para este cenário. Os dois treinos antigos continuam
  pendentes de revisão e não integram a matriz física concluída.

### Revogação isolada de distância

- Com `WRITE_DISTANCE` revogada, o treino livre de 20 minutos exibiu falha
  parcial por capacidade no filho `Distância`, com três operações pendentes e
  retry recuperável.
- Após reautorização, retry terminou `Sincronizada`. O provider mostrou
  distância acumulada `3,570 km`; o acréscimo do treino foi `0,053 km` e não
  houve duplicação observada.
- Resultado: Gate F passa também para o cenário de revogação de distância.
