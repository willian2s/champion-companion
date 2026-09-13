# 033-02 — Implementar parser e collector D1 v2

- **Status:** completed
- **Depende de:** 033-01
- **Pode rodar em paralelo com:** 033-03

## Objetivo

Implementar o contrato D1 v2 sem persistência nem UI.

## Escopo

- parser dos campos completos e endianness congelada;
- dayOffset/hour packed;
- BPM nullable com faixa 30..200;
- pacote estrutural válido independente de BPM;
- collector por posição física e total;
- conclusão mesmo quando o último pacote não possui BPM válido;
- progresso, gaps, duplicatas, fora de ordem e timeout parcial;
- modelos internos semanticamente nomeados, sem inventar unidades raw desconhecidas.

## Fora do escopo

- Room;
- Health Connect;
- composição D1/D4;
- UI.

## Validação

```bash
./gradlew lint assembleDebug assembleRelease
git diff --check
```

Não criar nem executar testes automatizados sem autorização explícita.

## Evidências

- `ChampionD1HistoryProtocol` agora interpreta layout fixo D1 v2 completo, com
  endianness congelada, data resolvida por `dayOffset`, BPM nullable em `30..200`
  e conversões somente de calorias e distância conhecidas.
- `MultipartCollector` conserva fatos estruturais sem depender de
  `HeartRateMeasurement`, substitui duplicatas pela última posição recebida,
  acompanha gaps/fora de ordem e conclui pela posição/total mesmo sem BPM.
- O coordinator usa `d1-v2` e não rebaixa coleta estrutural válida quando nenhum
  BPM elegível existe; persistência legacy continua recebendo somente medições de
  BPM válidas.
- `./gradlew lint assembleDebug assembleRelease` — sucesso.
- `git diff --check` — sucesso.
- Testes automatizados não foram criados nem executados.

## Arquivos alterados

- `app/src/main/java/br/com/mwconsulting/championcompanion/core/bluetooth/ChampionD1HistoryProtocol.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/health/ChampionD1HeartRateSyncCoordinator.kt`
- `docs/tasks/033-unified-watch-daily-sync/033-00-overview.md`
- `docs/tasks/033-unified-watch-daily-sync/033-02-implementar-parser-collector-d1-v2.md`

## Decisões e desvios

- O resultado do collector expõe `D1Packet` completo em `packets`/`facts`; a
  projeção legacy para `HeartRateMeasurement` continua limitada a BPM válido.
- Callbacks de pacote, timeout e cancelamento foram serializados, e o resultado
  terminal é idempotente, para impedir corrida entre conclusão pelo último pacote
  e timeout.
- Contratos Room, Health Connect, composição diária e UI não foram alterados.
- Testes legados que descrevem `d1-v1` não foram alterados nem executados, pois
  a task proíbe testes automatizados sem autorização explícita.

## Comandos executados

```text
./gradlew lint assembleDebug assembleRelease
git diff --check
```

## Resultados e evidências

- `./gradlew lint assembleDebug assembleRelease`: sucesso; lint, compilação e
  empacotamento debug/release concluídos.
- `git diff --check`: sucesso, sem erro de whitespace.
- Não foram criados nem executados testes automatizados.

## Riscos residuais

- Não há fixtures D1 físicas no repositório; endianness, packing, escala e raw
  seguem contrato documental congelado até validação no Champion C001.
- Persistência dos fatos completos ainda pertence à `033-03`; o coordinator
  mantém somente a projeção legacy de BPM válido nesta etapa.
- Suíte automatizada histórica ainda codifica expectativas de `d1-v1` e deverá
  ser tratada somente mediante autorização explícita para testes.
