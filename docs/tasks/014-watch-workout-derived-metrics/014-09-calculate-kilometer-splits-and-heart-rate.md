# 014-09 — Calcular splits e BPM temporal

- **Ticker:** `014`
- **Número:** `09`
- **Status:** completed
- **Concluída em:** 2026-08-06

## Objetivo e resultado esperado

Gerar splits cumulativos de 1 km com tempo interpolado linearmente e BPM
ponderado por sobreposição temporal das amostras reais.

Resultado: splits determinísticos, com fronteiras estáveis e ausentes quando a
série não for elegível.

## Dependências

- `014-08` concluída.

## Arquivos permitidos

- novos contratos/políticas de split em `core/model`;
- read models/mappers de atividade;
- persistência de snapshot, se prevista em `014-02/03`;
- overview.

## Arquivos proibidos

- parser, perfil, Compose e Health Connect;
- Gradle, manifesto, migrations e schemas;
- BPM sintético ou média global importada.

## Escopo

### Splits

- limites em `1.000 m`, `2.000 m`, `3.000 m` e seguintes;
- cruzamento interno usa interpolação linear pela fração da distância;
- janelas contíguas e semiabertas `[start, end)`;
- split parcial final, quando apresentado, é identificado separadamente;
- série inelegível ou não monotônica não produz split.

### BPM temporal

Cada amostra real `i` representa sample-and-hold em:

```text
[sampledAt_i, sampledAt_i+1)
```

A última amostra é limitada pelo fim da sessão. Para cada split:

1. recortar cada intervalo de validade pela janela do split;
2. multiplicar BPM pela duração da sobreposição;
3. dividir pela soma das durações cobertas;
4. manter BPM ausente quando a cobertura for zero.

Amostra exatamente no início pertence ao split; amostra exatamente no fim
pertence ao próximo, evitando dupla contagem.

## Fora do escopo

- suavização, zonas de BPM ou estimativa de amostra ausente;
- UI/gráficos;
- record Health Connect por split;
- testes automatizados.

## Passos de implementação

1. Percorrer o acumulado canônico em ordem temporal.
2. Emitir cada cruzamento de múltiplo de 1.000 m.
3. Interpolar instante pela fração linear no intervalo cruzado.
4. Formar janelas semiabertas de split.
5. Construir intervalos sample-and-hold para BPM real.
6. Agregar por duração de sobreposição.
7. Representar ausência quando não houver cobertura.
8. Falhar fechado para série inelegível ou não monotônica.
9. Atualizar overview.

## Critérios de aceite

- Limites ocorrem exatamente nos múltiplos de 1 km.
- Cruzamento interno usa interpolação reproduzível.
- Janelas não se sobrepõem nem deixam dupla contagem na fronteira.
- BPM não usa média simples sem tempo, média global ou amostra fora da janela.
- Sem cobertura, BPM permanece ausente.
- Zero distância não cria split.
- Série implausível bloqueada pela política não produz split funcional.

## Validação

```bash
./gradlew lint assembleDebug assembleRelease
./gradlew installDebug
git diff --check
git status --short
```

Não criar nem executar testes automatizados.

## Evidências

- tabela de cruzamentos/frações/instantes;
- janelas semiabertas;
- intervalos BPM considerados e durações de sobreposição;
- casos de fronteira e ausência;
- builds e smoke do read model.

## Definição de pronto

Splits e BPM temporal determinísticos estão disponíveis para UI, com item
`014-09` marcado.

## Implementação

- `RoomRepositories` calcula splits a partir da série canônica persistida e das
  amostras BPM reais somente quando a série é elegível e não possui indicadores
  de implausibilidade, antes do preflight e da inserção transacional do snapshot.
  A política que bloqueia projeção remota do Health Connect não oculta read
  models locais válidos.
- `DatabaseMappers` persiste splits com IDs próprios e exige round-trip exato de
  janelas, métricas, interpolação e BPM ponderado quando a projeção existe.
- Snapshots legados de `014-07` sem linhas de split continuam legíveis; o read
  path não sintetiza projeção. Rederivação explícita é necessária para
  materializar splits elegíveis.
- Reimportação compara amostras reais por instante e BPM antes de preservar
  snapshot ativo. Falha estrutural de fronteira aborta cálculo completo, sem
  omitir split por `mapNotNull`.

## Arquivos alterados

- `app/src/main/java/br/com/mwconsulting/championcompanion/data/DatabaseMappers.kt`;
- `app/src/main/java/br/com/mwconsulting/championcompanion/data/RoomRepositories.kt`;
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/model/WatchWorkoutDerivedMetrics.kt`;
- este arquivo e `014-00-overview.md`.

## Decisões e desvios

- Splits locais usam série canônica elegível sem indicadores de
  implausibilidade. A política `NO_GLOBAL_THRESHOLD_BLOCKS_PROJECTION` continua
  bloqueando somente projeções remotas, sem ocultar read models locais válidos.
- Snapshots de `014-07` sem splits permanecem legíveis; não há síntese no read
  path. Materialização exige rederivação explícita.
- `calculateOrNull` substitui omissão parcial por falha fechada. A função
  compatível `calculate` permanece por estabilidade de contrato, mas caminhos
  de persistência usam exclusivamente a variante estrita.

## Comandos executados

- `./gradlew app:compileDebugKotlin` — PASS; somente avisos de depreciação já
  existentes em `RoomRepositories.kt`;
- `./gradlew app:lintDebug` — PASS;
- `./gradlew lint assembleDebug assembleRelease` — PASS; mesmos avisos de
  depreciação preexistentes;
- `./gradlew installDebug` — PASS; APK final instalado no Galaxy S22
  `SM-S901E`;
- `git diff --check` — PASS;
- testes automatizados — não criados nem executados, conforme escopo.

## Evidências e riscos residuais

- O preflight calcula e compara splits persistidos com
  `WorkoutSplitCalculator.calculateOrNull`, incluindo limites exatos,
  interpolação temporal, janelas semiabertas, sample-and-hold e cobertura BPM
  ausente.
- Política remota de plausibilidade permanece independente dos splits locais:
  indicadores de implausibilidade bloqueiam splits funcionais; ausência de
  limiar global bloqueia somente projeções remotas, conforme `014-02` e ADR-018.
- Ausência de linhas é aceita para compatibilidade de snapshot legado; linhas
  presentes precisam reproduzir cálculo canônico e ausência de indicadores.
- Reimportação com BPM real alterado desativa snapshot ativo, evitando round-trip
  com amostras divergentes.
- Round-trip Room físico e validação D4 em dispositivo permanecem para `014-15`.
- UI, Health Connect, parser e reprocessamento de legado continuam bloqueados
  para subtarefas posteriores.

## Rollback

Ocultar splits e retornar lista vazia/ausência, preservando a série canônica.
