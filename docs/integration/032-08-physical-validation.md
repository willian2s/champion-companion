# 032-08 — Validação física e fechamento

- **Data:** 2026-09-11
- **Dispositivo:** Champion C001 real
- **Fonte:** validação manual confirmada pelo usuário e dumps sanitizados do banco
- **Status:** concluída

## Evidência

A validação manual do fluxo D3/D4 v4 foi concluída com o Champion C001. Dumps do
banco foram usados para conferir o resultado persistido, a derivação e os
invariantes sem versionar payload BLE integral, identificadores pessoais ou dump
completo.

### Valores sanitizados observados

| Verificação | Resultado observado |
| --- | --- |
| Blocos D4 completos | `34`; `recordCount == samples` em `34/34` |
| Fechamento de passos, distância, calorias raw e `sportValue` | `34/34` para cada soma header × série |
| CRC do bloco | fechamento em `34/34` |
| Atividade de referência | `07:27:14` → `08:09:36`; duração `42:22`; `N = 44`; pausa `0` |
| Granularidade efetiva da referência | `T/N`, aproximadamente `57,77 s` por sample; não `N × 60 s` |
| Fechamento da distância na atividade v4 | header `5.760 m` = soma da série `5.760 m`; provenance `WATCH_NATIVE` antes da calibração |
| Referência manual | `5.850 m`; fator observado `5.850 / 5.760 = 1,015625` |
| Distância após calibração | `5.850 m`; provenance contextual calibrada |

Os números acima são agregados sanitizados; bytes, datas completas,
identificadores e linhas individuais do banco foram omitidos.

Para os demais cenários, o dump sanitizado confirmou os estados persistidos:

| Cenário | Evidência persistida |
| --- | --- |
| Variação forte de intensidade | série nativa não uniforme; distância foi mantida por intervalo |
| Sem calibração contextual | `WATCH_NATIVE`, fator efetivo `1,0` |
| Perfil aprendido | `RUNNING` e `WALKING` separados, sem compartilhamento de evidência |
| Distância nativa inelegível | `STEP_LENGTH_FALLBACK` |
| Review e Health Connect | review obrigatório; operações remotas somente após ação explícita |
| Daily Summary, cadência e `CYCLING` | políticas e valores permaneceram independentes |

Todos os cenários mínimos da task `032-08` foram exercitados e considerados
válidos:

- D3 preservou slot físico mesmo com lacuna/slot zero;
- atividade com variação de intensidade preservou a série nativa;
- `startedAt`, `endedAt`, `recordCount` e quantidade de samples permaneceram
  coerentes sem impor `N × 60 s`;
- passos da série foram comparados ao total do header como diagnóstico;
- distância nativa da série foi comparada ao total do header;
- referência externa manual foi aceita como calibração com tolerância de erro;
- atividades sem calibração contextual mantiveram distância nativa;
- fatores aprendidos de `RUNNING` e `WALKING` permaneceram contextuais e
  independentes;
- distância nativa inelegível usou fallback por comprimento de passo;
- review-before-sync e sincronização Health Connect permaneceram explícitos;
- Daily Summary, cadência e `CYCLING` não apresentaram regressão.

## Limitações observadas

Semântica operacional de `pauseStateRaw` não zero, unidade funcional de
`sportValue`, campos reservados/desconhecidos do header e dos samples, política
de reprocessamento de históricos, tolerâncias finais para divergências futuras,
cobertura de samples acima de 255 passos, retenção de payload diagnóstico e
variações de firmware fora do dispositivo validado continuam pendentes.
Referência externa manual continua sujeita a erro e permanece apenas como
referência de calibração; a validação não a tratou como medição absoluta.

## Conclusão

Critérios físicos da Spec 032 foram atendidos no dispositivo validado. Não há
bloqueio residual para fechar a spec; limitações de firmware, semântica raw,
reprocessamento, tolerâncias, cobertura e retenção acima permanecem registradas
como riscos técnicos, não como falhas do fluxo entregue.
