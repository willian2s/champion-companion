# 014-02 — Definir contratos temporais e de métricas derivadas

- **Ticker:** `014`
- **Número:** `02`
- **Status:** pending

## Objetivo e resultado esperado

Criar contratos Android-free que separem fatos raw/medidos, snapshot derivado,
elegibilidade temporal, métricas por intervalo, splits e projeções filhas.
Resultado: domínio compilável e sem dependência de Room, Compose, BLE Android ou
Health Connect SDK.

## Contexto

`WorkoutMetrics` e `WorkoutInterval` misturam origem e cálculo. ADR-018 exige
camadas explícitas e snapshot reproduzível antes de desenhar migration.

## Requisitos cobertos

- R2 a R9;
- ADR-018, especialmente snapshot, tempo, identidade e ausência tipada.

## Dependências

- `014-01` concluída.

## Arquivos permitidos

- `app/src/main/java/br/com/mwconsulting/championcompanion/core/model/WatchWorkoutActivity.kt`;
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/model/DeviceProfileDailySummary.kt`;
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/model/HeartRateAndSync.kt`;
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/model/RepositoryContracts.kt`;
- novos arquivos de contrato em `core/model`;
- spec, ADR-018 e overview da Fase 014.

Símbolos prováveis: `StepLengthSource`, `WorkoutDerivationMethod`,
`WorkoutDerivationSnapshot`, `MeasuredWorkoutInterval`,
`DerivedWorkoutInterval`, `TemporalSeriesEligibility`, `WorkoutSplit` e
`HealthSyncRecordType.SPEED`.

## Arquivos proibidos

- `core/bluetooth`, `core/database`, `data`, `feature` e `core/healthconnect`;
- Gradle, manifesto, migrations, schemas e Fase 013.

## Escopo

- invariantes e nomes do domínio;
- quatro casos de duração;
- fórmulas V1 e zero passos;
- ausência de distância/série/split;
- chaves estáveis conceituais para filhos Health Connect.

## Fora do escopo

- persistência Room;
- parser D4;
- UI e reprocessamento operacional;
- mapper Health Connect;
- testes automatizados.

## Passos de implementação

1. Definir raw/measured/derived sem reutilizar coluna legada com nova semântica.
2. Modelar `stepLengthMeters` positivo e origem enumerada.
3. Modelar snapshot imutável com método, versão e `derivedAt`.
4. Modelar resultado temporal válido/inelegível com motivo explícito.
5. Modelar métricas por intervalo, acumulado e split.
6. Adicionar `SPEED` ao tipo de sync sem habilitar writer ainda.
7. Manter adapters temporários necessários para não quebrar consumidores fora da
   subtarefa.
8. Atualizar overview após validação.

## Critérios de aceite

- Contratos representam todos os requisitos sem tipos Android.
- Distância original não pode ser confundida com canônica pelo nome/API.
- Snapshot guarda valor e origem usados, sem referência mutável ao perfil.
- Casos temporais inválidos permanecem representáveis sem exceção silenciosa.
- `SPEED` não torna atividade elegível antes de `014-12`.

## Validação manual

- Revisar invariantes com exemplos `T=N×60`, parcial, curto e longo.
- Revisar API contra ADR-018 e consumidores inventariados.
- Confirmar ausência de dependências Android no domínio.

## Comandos permitidos

```bash
./gradlew lint assembleDebug assembleRelease
git diff --check
git status --short
```

Não criar nem executar testes automatizados.

## Evidências

- Diff dos contratos;
- tabela requisito → tipo/invariante;
- resultado dos builds e revisão de arquitetura.

## Definição de pronto

Contratos compilam, não ativam comportamento incompleto e item `014-02` está
marcado no overview.

## Riscos e cuidados

- Mudança ampla em `WorkoutInterval` pode quebrar mappers; usar evolução
  compatível até migration.
- Não expor passo pessoal em `toString`, logs ou IDs.
- Não renomear infraestrutura histórica phone.

## Rollback

Remover novos contratos/adapters e restaurar enum sem `SPEED`; sem efeito em
dados persistidos.

## Atualização obrigatória do overview

Atualizar `014-00-overview.md`, marcar somente `014-02` e registrar decisões de
contrato que afetem `014-03`.
