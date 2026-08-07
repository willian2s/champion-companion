# Evidência física 014-15

- **Data:** 2026-08-07
- **Aplicativo:** `1.0-dev` (`versionCode=1`), commit `0286c79`
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
| F — Health Connect | `FAIL` bloqueador | A sincronização explícita foi marcada como sucesso local, mas o Health Connect real não exibiu distância nem velocidade; velocidade apareceu como não permitida. A UI não confirmou records remotos. Fan-out, midpoint, retry idempotente e matriz de falha parcial também não foram observados. |
| G — Regressão | `N/A` | Importação, revisão, métricas, BPM, dispositivo associado e sync explícito permaneceram funcionais. Calendário e matriz completa de regressão não foram repetidos nesta sessão; nenhum fluxo phone, GPS, rota, mapa ou BLE live foi acionado. |

## Observações

- A importação foi iniciada somente pelo botão `Buscar treinos no relógio`.
- A atividade foi revisada antes do botão `Sincronizar com Health Connect`.
- Após reinício do aplicativo, dados locais permaneceram disponíveis.
- A atividade phone pronta para sincronização foi preservada sem acionamento,
  confirmando que a validação não reabriu fluxo phone.
- O Health Connect real não exibiu distância/velocidade após o sucesso local; a
  permissão de velocidade estava ausente. Follow-up funcional:
  `docs/integration/014-15-health-connect-provider-follow-up.md`.
- `014-15` permanece bloqueada: Gate F tem `FAIL` bloqueador; gates B, E e partes
  de A/C/D/G também precisam de fixture ou evidência independente adicional.

## Revalidação parcial após 014-17

- Uma atividade elegível, após aceite explícito, foi sincronizada pelo package
  debug correto com ambas as permissões concedidas.
- Health Connect exibiu manualmente distância granular `3,463 km` e velocidade
  `7,245 km/h` para `Champion Companion Dev`.
- A revogação isolada de `WRITE_SPEED` foi refletida como `Velocidade — Pendente`
  em Settings; reautorização foi concluída pelo fluxo `Conectar`.
- Resultado: autorização e escrita de distância/velocidade passam para este
  cenário. Retry, falha parcial e ausência de duplicação continuam sem evidência
  física; `014-15` permanece `blocked`.
