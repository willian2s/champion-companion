# 014-15 — Executar validação física completa

- **Ticker:** `014`
- **Número:** `15`
- **Status:** pending

## Objetivo e resultado esperado

Executar gate físico completo com Champion C001, Galaxy S22 e Health Connect
real, sem corrigir produção dentro da própria task.

Resultado: evidência sanitizada da cadeia importação → derivação → revisão → UI →
reprocessamento → projeção Health Connect, com gates `PASS`, `FAIL` ou `N/A`
justificado.

## Dependências

- `014-01` a `014-14` concluídas.
- O ramo `014-11` de reprocessamento precisa estar validado.

## Arquivos permitidos

- `docs/integration/014-*` para evidências sanitizadas;
- screenshots no local adotado pelo repositório;
- correções documentais estritamente factuais;
- overview para registrar o resultado da task.

## Arquivos proibidos

- código de produção, Gradle, manifesto, migrations e schemas;
- correções funcionais escondidas no gate;
- reabertura da Fase 013;
- payload BLE, IDs pessoais ou valores de saúde identificáveis em evidência
  pública.

## Gates

### A — Importação e parser

- importação D3/D4 permanece explícita;
- novas importações usam `d4-v2`;
- sequência de referência soma `3.737` passos;
- raw permanece preservado;
- distância/calorias temporais raw não viram métricas.

### B — Índices, duração e plausibilidade

- índices contínuos elegíveis funcionam;
- lacuna/duplicata falha fechada;
- `1.637 s` produz último intervalo de 17 s;
- plausibilidade é registrada sem redistribuir passos.

### C — Perfil e derivação

- valor manual e sugestão aceita funcionam;
- sugestão não aceita não persiste;
- distância total é exatamente a soma dos intervalos;
- edição do perfil não muda snapshot antigo;
- ausência de perfil mantém canônico ausente.

### D — Métricas, UI e esteira

- acumulado termina na distância canônica;
- cadência, velocidade e ritmo usam a mesma série;
- splits e BPM temporal correspondem à linha do tempo;
- original/calibrada não aparecem como fallback;
- `TREADMILL` de relógio não exige calibração histórica incompatível.

### E — Reprocessamento

- legado `d4-v1` recuperável reprocessa explicitamente sem BLE;
- revisão, `recordedAt`, título, notas e calibração histórica permanecem;
- repetição idempotente;
- nenhuma sincronização automática.

### F — Health Connect

- uma operação existe por filho;
- `DistanceRecord` usa intervalos positivos;
- `SpeedRecord` usa midpoint;
- retry não duplica;
- agregado legado e filhos não coexistem após reconciliação;
- falha parcial e delete-before-insert retomam pelos mesmos IDs/versões/payloads.

### G — Regressão

- importação D3/D4, revisão, calendário, BPM, dispositivo e sync explícito
  continuam funcionais;
- nenhum fluxo phone, GPS, rota, mapa ou BLE live é reativado.

## Passos de implementação

1. Registrar versões sem identificadores sensíveis.
2. Executar cada gate e guardar evidência mínima.
3. Usar somente dados necessários e sanitizados.
4. Registrar divergências e riscos residuais.
5. Abrir follow-up para qualquer correção funcional.
6. Manter a fase `in_progress` se houver gate bloqueador.
7. Atualizar overview após revisão independente.

## Comandos permitidos

```bash
./gradlew lint assembleDebug assembleRelease
./gradlew installDebug
git diff --check
git status --short
```

Não criar nem executar testes automatizados.

## Critérios de aceite

- Todos os gates obrigatórios possuem evidência suficiente.
- Gates indisponíveis recebem `N/A` apenas com justificativa factual aceita.
- Qualquer `FAIL` bloqueador impede conclusão da task.
- Nenhuma correção de produção está misturada à validação.
- Evidências não expõem dados sensíveis.

## Definição de pronto

Gate físico revisado e item `014-15` marcado. A fase ainda não é encerrada nesta
task; o fechamento documental pertence a `014-16`.

## Rollback

Reverter somente afirmações documentais incorretas. Evidências válidas permanecem
históricas.
