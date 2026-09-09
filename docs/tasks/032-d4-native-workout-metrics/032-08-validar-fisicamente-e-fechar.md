# 032-08 — Validar fisicamente e fechar

- **Status:** pending
- **Depende de:** 032-07

## Objetivo

Validar em Champion C001 real o parser v4, distância nativa, calibração e
integração final antes de encerrar a spec.

## Cenários mínimos

- importação D3 com slot zero entre slots válidos;
- atividade com variação forte de intensidade;
- `startedAt`, `endedAt`, `recordCount` e quantidade de samples coerentes com o
  header sem impor `N × 60 s`;
- soma de passos da série comparada ao header apenas como diagnóstico;
- soma de distância nativa comparada ao header;
- referência externa manual para observar fator de correção, aceitando erro da
  referência;
- atividade sem calibração contextual;
- atividade com fator RUNNING ou WALKING aprendido;
- fallback por comprimento de passo quando distância nativa for inelegível;
- review-before-sync e Health Connect explícito;
- nenhuma regressão em Daily Summary, cadência e `CYCLING`.

## Evidência

Registrar somente dados sanitizados e necessários. Não versionar payload BLE
integral, identificadores pessoais ou dump de banco completo.

## Fechamento

- atualizar Spec 032, ADR-029, overview, baseline e protocolo;
- registrar limitações de firmware/unidades não fechadas;
- executar `./gradlew lint assembleDebug assembleRelease`;
- executar `git diff --check`;
- revisão independente antes do merge em `main`.
