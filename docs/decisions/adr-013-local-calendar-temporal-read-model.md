# ADR-013 — Adotar calendário como read model temporal local derivado

## Status

Aceita

## Data

27/07/2026

## Contexto

As Fases 07 a 12 persistiram dados locais suficientes para uma visão temporal:
resumos diários, frequência cardíaca manual e D1, amostras de frequência
cardíaca de atividades importadas,
revisadas e calibradas, peso e operações locais de sincronização. Room permanece
a fonte operacional e visual primária. Health Connect é write-only e não deve
ser consultado para formar telas.

O calendário ainda é placeholder em `feature/calendar/CalendarScreen.kt`. Não
existem `CalendarRepository`, `CalendarViewModel`, destino de detalhe diário ou
read model temporal. Passos e distância possuem somente superfícies atuais e
shells de histórico. A composição precisa reunir fontes existentes sem expor
entities, duplicar persistência, iniciar operações externas ou transformar
ausência em valor sintético.

As decisões desta ADR devem permanecer compatíveis com:

- ADR-003: Room como fonte operacional, DataStore somente para preferências e
  separação entre domínio, entities, DAOs e formatos externos;
- ADR-005: Navigation 3 e back stack manual;
- ADR-009: Saúde como hub, detalhes de métricas como owners visuais e
  repositories como fonte da UI;
- ADR-010: histórico D1 local-first e Health Connect fora da fonte visual;
- ADR-011: atividades permanecem em workflow local até `READY_TO_SYNC`;
- ADR-012: Health Connect write-only, fila granular e sync foreground controlado.

Fases 13 e 14 foram adiadas por decisão de produto. A Fase 15 não depende
funcionalmente delas e prioriza calendário, detalhe diário e históricos locais.

## Decisão

### 1. Read model local e fluxo de camadas

O calendário será uma projeção derivada, imutável e read-only, formada no fluxo:

```text
CalendarScreen / CalendarDayScreen
            ↓
      CalendarViewModel
            ↓
     CalendarRepository
            ↓
Repositories locais + AppPreferences
            ↓
       Room / DataStore
```

`CalendarRepository` produzirá modelos de domínio ou apresentação, nunca
entities Room. A UI não acessará DAO, Room, DataStore ou `HealthConnectClient`
diretamente. O ViewModel não combinará entities nem resolverá seleção de
dispositivo, deduplicação ou semântica temporal.

A implementação será preferencialmente um `LocalCalendarRepository` separado
de `RoomRepositories`, composto sobre contratos locais existentes e
`AppPreferencesRepository`. A composição não terá efeitos colaterais e não
iniciará refresh, coleta, importação ou sincronização.

### 2. Persistência e schema

O calendário será derivado em memória sobre tabelas existentes. Não serão
persistidos células, marcadores, resumos consolidados, datas reprojetadas ou
status de calendário.

Esta fase não cria:

- tabela ou coluna nova;
- migration;
- índice preventivo;
- schema materializado de calendário;
- biblioteca externa de calendário sem decisão arquitetural própria.

Room permanece na versão atual. Necessidade futura de schema ou índice deverá
ser demonstrada por evidência de desempenho, registrada em decisão própria e
executada fora desta ADR.

### 3. Semântica de datas e timezone

Cada fonte será associada à data civil conforme seu próprio agregado:

| Fonte | Data do agregado |
|---|---|
| `DailySummary` | `key.localDate` |
| `HeartRateMeasurement` | `measuredAt` convertido no `ZoneId` selecionado |
| amostra de `WorkoutActivity` | `sampledAt` convertido no `ZoneId` selecionado |
| `WorkoutActivity` | `startedAt` convertido no `ZoneId` selecionado |
| `WeightMeasurement` | `measuredAt` convertido no `ZoneId` selecionado |
| `HealthSyncOperation` | data do agregado local relacionado |

Uma atividade que cruza meia-noite pertence somente ao dia definido por
`startedAt`. `syncedAt`, `syncAt`, `updatedAt` ou horário da tentativa de sync
nunca movem o dado clínico ou esportivo para outro dia.

Todas as consultas temporais novas usam intervalo semiaberto:

```text
[fromInclusive, untilExclusive)
```

Para mês, a janela começa no primeiro dia do mês e termina no primeiro dia do
mês seguinte. Para dia, começa no início do dia e termina no início do dia
seguinte. Não usar `23:59:59.999` nem fim de dia artificial.

`ZoneId` será explícito no read model e congelado durante a formação de cada
consulta ou snapshot. `DailySummary.key.localDate` permanece estável. Registros
baseados em `Instant` podem ser reprojetados para dia civil adjacente quando a
zona mudar, sem alteração dos registros persistidos e sem persistir uma segunda
data específica do calendário.

### 4. Seleção de múltiplos dispositivos

Para cada data, a projeção diária selecionará no máximo um resumo válido:

1. considerar somente resumos `VALID`;
2. preferir resumo do dispositivo atualmente associado;
3. se não houver candidato do dispositivo associado, usar o resumo válido mais
   recentemente atualizado;
4. resolver empates por `updatedAt` decrescente, `collectedAt` decrescente e
   identificador estável em ordem determinística;
5. nunca somar resumos completos de dispositivos diferentes;
6. preservar e exibir origem quando o resumo vier de dispositivo histórico.

Esta política vale para passos, distância diária e calorias diárias quando
exibidas. BPM, atividades e pesos permanecem registros individuais e podem
pertencer a dispositivos históricos.

Ausência de resumo é diferente de resumo válido com zero passos. A seleção do
dispositivo associado e do `ZoneId` deve permanecer consistente durante a
formação de um snapshot.

### 5. Fontes e regras de métricas

- Passos diários vêm exclusivamente de `DailySummary.steps`.
- Distância diária vem exclusivamente de
  `DailySummary.estimatedDistanceMeters`.
- Passos e distância de atividades não participam dos totais diários.
- A meta vem de `AppPreferences.dailyStepsGoal` e deve ser rotulada **Meta
  atual**; não existe meta histórica nesta fase.
- O histórico dedicado de frequência cardíaca da tela Saúde consolida
  `MANUAL_MEASUREMENT`, `WATCH_D1_HISTORY` e `WATCH_WORKOUT_ACTIVITY`.
  Amostras de atividade são lidas da tabela própria, somente para atividades não
  `DISCARDED`, preservam vínculo com a atividade e nunca são copiadas ou
  representadas como `HeartRateMeasurement`.
- O resumo de BPM do detalhe diário do calendário permanece limitado a
  `MANUAL_MEASUREMENT` e `WATCH_D1_HISTORY`; a extensão de
  `WATCH_WORKOUT_ACTIVITY` pertence somente ao histórico dedicado de Saúde.
- Atividades não `DISCARDED` permanecem visíveis, inclusive pendentes de revisão
  ou calibração, associadas por `startedAt`.
- O detalhe de atividade existente será reutilizado; o calendário não duplica
  tela nem altera workflow.
- Peso mostra medidas persistidas do dia por `measuredAt`, sem edição,
  gerenciamento ou histórico completo.

Ausência permanece ausência. Dia sem registro não vira zero; distância nula não
vira zero; zero persistido e válido continua zero. Erro, indisponibilidade,
ausência de dado e operação inexistente são estados distintos.

### 6. Fila local de sincronização

O calendário consultará somente `HealthSyncOperation` local, preferencialmente
em lote por `aggregateType` e `aggregateIds`, com chunking caso o limite de
parâmetros SQLite exija. Não consultará provider externo, não criará operações,
não reconciliará backlog e não iniciará sync.

A presença de uma operação não prova validade do aggregate. Falha de sync não
remove nem oculta dado local válido. Operação órfã ou aggregate ausente será
representado por degradação segura; nunca será associado a um dia usando
`syncedAt`.

O estado visual será derivado das operações locais e seguirá esta precedência:

1. nenhuma operação: `Sem operações`;
2. sucesso combinado com pendência, sincronização ou falha: `Parcial`;
3. sem sucesso combinado e alguma falha: `Falha`;
4. sem falha e alguma sincronização: `Sincronizando`;
5. sem falha ou sincronização e alguma pendência: `Pendente`;
6. operações existentes todas concluídas: `Sincronizado`.

### 7. Navegação

O fluxo temporal usará Navigation 3 e back stack manual:

```text
Calendar → CalendarDay(localDateIso) → detalhes relacionados
```

`CalendarDay(localDateIso)` será validado com `LocalDate.parse` e falha segura.
Ao abrir uma atividade, será reutilizado `ExerciseDetail`, preservando o dia e
o mês no back stack. Voltar removerá o destino atual, sem navegar novamente para
um pai.

Esta rota parametrizada é uma especialização concreta exigida pelo calendário,
não uma substituição da ADR-005. A decisão geral de usar Navigation 3, destinos
serializáveis e back stack manual permanece vigente. Não introduzir
`NavHost`/`NavController` nesta fase.

### 8. Fronteiras proibidas

O calendário não fará:

- acesso a BLE, scanner, GATT, refresh, D1, D3, D4 ou
  `ForegroundBleOperationGate`;
- acesso, leitura ou escrita no Health Connect ou solicitação de permissões;
- criação, reconciliação ou execução de operação de sincronização;
- `WorkManager`, foreground service, retry em background ou background BLE;
- exercícios iniciados pelo aplicativo;
- GPS, localização, latitude, longitude, rota, mapa, GPX, TCX ou FIT;
- edição de atividade, peso, perfil ou dados históricos;
- tabela materializada, migration ou índice preventivo.

### 9. Ordem autoritativa das fases

Sem renumerar fases:

- Fase 12 — concluída;
- Fase 13 — adiada: exercícios iniciados pelo aplicativo;
- Fase 14 — adiada: background, WorkManager e foreground service;
- Fase 15 — próxima fase em execução: calendário e histórico consolidado local.

A Fase 15 não depende funcionalmente das Fases 13 e 14. O roadmap deve
preservar o conteúdo futuro dessas fases, acrescentando somente o status de
priorização vigente.

## Alternativas consideradas e rejeitadas

- **Tabela materializada de calendário:** duplicaria dados e exigiria consistência
  e migration.
- **Composição na UI ou no ViewModel:** duplicaria política temporal e exporia
  entities.
- **CalendarRepository dentro de `RoomRepositories`:** misturaria composição
  temporal com implementação de persistência e preferências.
- **Health Connect como fonte visual:** violaria local-first, write-only e uso
  offline.
- **Somar resumos de dispositivos:** produziria dupla contagem.
- **Somar métricas de atividades aos totais:** misturaria agregados com fontes
  sobrepostas.
- **Fim artificial do dia:** falharia em mudanças de horário civil.
- **Consulta de sync por aggregate individual:** produziria N+1.
- **Carregar detalhes completos na grade:** aumentaria custo e N+1.
- **Persistir data reprojetada por timezone:** duplicaria semântica e poderia
  divergir do registro original.
- **Migrar para `NavHost`/`NavController`:** não é necessário para o fluxo
  parametrizado concreto e violaria a navegação vigente sem benefício demonstrado.
- **Duplicar detalhe de atividade:** divergiria do workflow existente.
- **Adicionar índice preventivo:** não há evidência de gargalo que justifique
  schema nesta fase.

## Consequências

### Positivas

- calendário funciona offline e sem operações externas;
- Room e repositories continuam fonte local única da experiência visual;
- política temporal e seleção de dispositivo ficam centralizadas;
- ausência, zero, erro e status de sync permanecem distinguíveis;
- timezone pode reprojetar eventos instantâneos sem mutar histórico;
- Fases 13 e 14 não bloqueiam valor imediato da Fase 15;
- ADRs 003, 005, 009, 010, 011 e 012 permanecem compatíveis.

### Negativas

- composição de várias fontes pode ter consistência eventual entre snapshots;
- timezone extremo pode mover BPM, peso e atividade para data adjacente;
- associação de dispositivo alterada pode mudar a origem selecionada no read model;
- consultas temporais grandes exigirão projeções leves e lotes;
- estados de ausência e degradação exigirão UI mais rica;
- back stack parametrizado continuará exigindo manutenção manual;
- desempenho deverá ser observado antes de qualquer alteração de schema.

## Impactos para subtarefas seguintes

- 015-03 define modelos, contratos, estados e intents sem expor entities.
- 015-04 adiciona queries semiabertas, projeções leves e consulta agrupada da
  fila, preservando APIs inclusivas antigas por compatibilidade aditiva.
- 015-05 compõe seleção determinística e snapshots imutáveis no repository local.
- 015-06 em diante implementa ViewModels, calendário, detalhe e históricos sem
  alterar as fronteiras desta decisão.

Necessidade de migration, índice, alteração de workflow ou acesso externo deve
abrir decisão específica; não pode ser introduzida implicitamente nas tasks
seguintes.
