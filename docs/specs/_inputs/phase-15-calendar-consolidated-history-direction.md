# Direção do calendário e histórico consolidado da Fase 15

## Contexto

A Fase 15 implementa uma visão temporal unificada dos dados locais do Champion Companion.

As fases concluídas já entregaram:

- resumos diários de passos;
- distância diária estimada quando disponível;
- frequência cardíaca manual;
- histórico D1;
- agregações e gráfico de BPM;
- atividades importadas do relógio;
- revisão e calibração de atividades;
- estados de workflow;
- peso local quando disponível;
- fila persistida do Health Connect;
- estados de sincronização;
- Health Connect write-only;
- Room como fonte operacional e visual primária.

As Fases 13 e 14 foram adiadas por decisão de produto.

A Fase 15 deve funcionar integralmente sem:

- exercícios iniciados pelo celular;
- foreground service;
- WorkManager;
- GPS;
- BLE em background;
- leitura do Health Connect.

## Decisão de priorização

Não renumerar as fases.

Registrar no roadmap:

```text
Fase 13 — adiada
Fase 14 — adiada
Fase 15 — próxima fase em execução
```

Motivo:

- os exercícios do Champion C001 já atendem ao uso principal;
- D3/D4 importa os treinos;
- Fase 11 revisa e calibra;
- Fase 12 sincroniza com o Health Connect;
- calendário, histórico e navegação temporal possuem maior valor imediato.

A Fase 15 não depende funcionalmente das Fases 13 e 14.

## Objetivo

Criar o fluxo:

```text
Dados locais persistidos
        ↓
Projeção temporal por data
        ↓
Calendário mensal
        ↓
Detalhe diário
        ↓
Históricos e gráficos
```

O usuário deve conseguir:

- navegar entre meses;
- voltar ao mês atual;
- identificar dias com dados;
- abrir um dia;
- visualizar passos;
- visualizar progresso da meta atual;
- visualizar distância diária;
- visualizar BPM;
- visualizar atividades;
- visualizar peso quando houver;
- visualizar estados de sincronização;
- abrir detalhes relacionados;
- consultar histórico de passos;
- consultar gráfico de passos;
- consultar histórico de distância;
- consultar gráfico de distância;
- usar tudo offline.

## Regra absoluta sobre testes

Não criar, alterar, planejar, recomendar ou executar testes automatizados.

Não:

- criar arquivos em `app/src/test`;
- criar arquivos em `app/src/androidTest`;
- alterar testes existentes;
- criar task de testes;
- executar suites de testes;
- incluir testes nos critérios de aceite;
- incluir cobertura;
- incluir testes na Definition of Done.

A validação deve usar:

```bash
./gradlew lint assembleDebug assembleRelease
./gradlew installDebug
```

Além de validação manual e screenshots.

## Princípios

### Fonte local

O Calendário lê exclusivamente:

- Room;
- repositories locais;
- DataStore para preferências simples.

O Calendário não acessa:

- BLE;
- `HealthConnectClient`;
- provider do Health Connect;
- scanner;
- conexão GATT;
- coordinator de importação;
- `ForegroundBleOperationGate`.

### Read-only

A Fase 15 é predominantemente uma fase de leitura e navegação.

O Calendário não deve:

- alterar dados históricos;
- atualizar passos;
- importar D1;
- importar D3/D4;
- editar atividades;
- sincronizar com Health Connect;
- editar peso;
- editar perfil.

Ações existentes devem continuar em suas telas proprietárias.

### Sem tabela de calendário

Não criar tabela materializada de calendário.

O calendário deve ser uma projeção derivada dos dados já existentes.

Não duplicar em uma nova tabela:

- passos;
- BPM;
- atividades;
- peso;
- estados de sincronização.

### Offline-first

Abrir o calendário ou um dia:

- não inicia BLE;
- não exige relógio;
- não exige internet;
- não exige Health Connect disponível;
- não solicita permissões;
- não altera a fila de sincronização.

## Escopo

A Fase 15 inclui:

- registro documental do adiamento das Fases 13 e 14;
- arquitetura do read model temporal;
- contratos de calendário;
- queries por intervalo;
- calendário mensal;
- marcadores por dia;
- navegação entre meses;
- seleção de data;
- detalhe diário;
- resumo de passos;
- distância diária;
- resumo de BPM;
- atividades do dia;
- peso do dia;
- status de sincronização;
- meta atual de passos;
- gráfico e histórico de passos;
- gráfico e histórico de distância;
- períodos de histórico;
- navegação para detalhes existentes;
- estados vazios;
- estados de loading;
- erros seguros;
- acessibilidade;
- validação offline;
- atualização de baseline, roadmap e documentação.

## Fora do escopo

A Fase 15 não inclui:

- exercícios iniciados pelo aplicativo;
- foreground service;
- WorkManager;
- retry em background;
- leitura do Health Connect;
- dados de outros aplicativos;
- edição de atividades;
- revisão;
- calibração;
- edição de peso;
- perfil completo;
- alteração de meta de passos;
- GPS;
- rota;
- mapas;
- exportação;
- backup;
- estatísticas clínicas;
- correlações médicas;
- interpretação médica;
- testes automatizados.

## Arquitetura

Criar um contrato equivalente a:

```text
CalendarRepository
```

Fluxo:

```text
CalendarScreen / CalendarDayScreen
                 ↓
           CalendarViewModel
                 ↓
          CalendarRepository
             ├── DailySummary
             ├── HeartRateMeasurement
             ├── WorkoutActivity
             ├── WeightMeasurement
             ├── HealthSyncOperation
             └── AppPreferences
```

Fronteiras:

- Compose não acessa DAO;
- ViewModel não combina entities;
- ViewModel não resolve regras de deduplicação;
- repository não acessa BLE;
- repository não acessa Health Connect;
- calendário não altera workflow;
- calendário não duplica regras de persistência;
- Hilt fornece as dependências.

## ADR

Criar uma ADR com o próximo número disponível para registrar:

- calendário como read model local;
- ausência de tabela materializada;
- sem acesso BLE ou Health Connect;
- regras de associação entre registros e datas;
- atribuição da atividade à data inicial;
- não somar passos de atividade ao resumo diário;
- não somar distância de atividades à distância diária estimada;
- política para múltiplos dispositivos;
- status de sync derivado do aggregate local;
- ausência de valores sintéticos;
- Fases 13 e 14 adiadas sem bloquear a Fase 15.

## Contratos de domínio

Planejar modelos equivalentes a:

```text
CalendarMonthQuery
CalendarMonthSnapshot
CalendarDayPresence
CalendarDaySummary
CalendarHeartRateSummary
CalendarWorkoutSummary
CalendarWeightSummary
CalendarSyncSummary
```

### CalendarMonthQuery

Deve conter:

```text
yearMonth
zoneId
```

### CalendarDayPresence

Deve indicar:

```text
hasSteps
hasHeartRate
workoutCount
hasWeight
hasSyncPending
hasSyncFailure
```

A UI não deve depender de entities ou enums de armazenamento.

### CalendarDaySummary

Deve conter:

```text
localDate
dailySummary
stepsGoal
heartRateSummary
workouts
weightMeasurements
syncSummary
```

Usar modelos de apresentação ou domínio imutáveis.

## Semântica das datas

### Resumo diário

Usar diretamente:

```text
DailySummary.key.localDate
```

Não reconverter `collectedAt` para decidir o dia.

### Frequência cardíaca

Resolver o dia por:

```text
measurement.measuredAt
    → ZoneId selecionado
    → LocalDate
```

Consulta diária:

```text
[startOfDay, startOfNextDay)
```

### Atividade

Associar ao dia de:

```text
WorkoutActivity.metrics.startedAt
```

convertido pelo `ZoneId` selecionado.

Uma atividade que cruza a meia-noite aparece no dia em que começou.

Não duplicar a mesma atividade em dois dias.

### Peso

Associar pelo:

```text
WeightMeasurement.measuredAt
```

convertido pelo `ZoneId` selecionado.

### Sincronização

O dia da operação deve ser o dia do aggregate local:

- resumo diário → `localDate`;
- BPM → `measuredAt`;
- atividade → `startedAt`;
- peso → `measuredAt`.

Não usar `syncedAt` para mover o dado para outro dia.

`syncAt` pode ser exibido como informação secundária.

### Mudança de timezone

O mês e o detalhe usam o `ZoneId` atual da interface.

Regras:

- `DailySummary.localDate` permanece estável;
- registros baseados em `Instant` são projetados no timezone atual;
- não alterar os registros persistidos;
- não persistir uma segunda data apenas para o calendário;
- documentar que uma mudança extrema de timezone pode mover BPM, peso ou atividade para um dia adjacente.

## Intervalos

Usar intervalos semiabertos:

```text
[fromInclusive, untilExclusive)
```

Para mês:

```text
primeiro dia do mês
até primeiro dia do mês seguinte
```

Para dia:

```text
início do dia
até início do dia seguinte
```

Não usar `23:59:59.999`.

Isso deve funcionar corretamente em mudanças de horário civil.

## Múltiplos dispositivos

Não somar resumos diários de dispositivos diferentes.

Política:

1. usar o resumo `VALID` do dispositivo atualmente associado;
2. quando não houver resumo desse dispositivo, usar o resumo `VALID` mais recentemente atualizado para o dia;
3. nunca somar dois resumos completos do mesmo dia;
4. indicar a origem quando o resumo vier de dispositivo histórico.

Esta política se aplica a:

- passos;
- distância diária;
- calorias diárias, quando exibidas.

BPM e atividades continuam registros individuais e podem permanecer associados a dispositivos históricos.

## Passos

Fonte diária:

```text
DailySummary.steps
```

Regras:

- não somar passos das atividades;
- não gerar zero para um dia ausente;
- diferenciar `sem dados` de `0 passos`;
- usar apenas resumo válido;
- mostrar data da última atualização;
- mostrar origem;
- mostrar meta atual.

## Meta de passos

O banco atual não persiste uma meta histórica em cada resumo.

Nesta fase, usar:

```text
AppPreferences.dailyStepsGoal
```

Rotular como:

```text
Meta atual
```

Não afirmar que essa era a meta configurada no dia histórico.

Não criar migration apenas para armazenar meta histórica nesta fase.

## Distância diária

Fonte:

```text
DailySummary.estimatedDistanceMeters
```

Regras:

- não somar distâncias das atividades;
- não usar distância do Health Connect;
- não substituir ausência por zero;
- identificar claramente que é estimativa;
- mostrar indisponível quando faltarem dados pessoais.

As distâncias das atividades aparecem separadamente dentro da seção de atividades.

## Frequência cardíaca diária

Usar somente:

```text
MANUAL_MEASUREMENT
WATCH_D1_HISTORY
```

Não misturar automaticamente amostras internas de atividades ao histórico geral de BPM.

Para o dia, calcular:

```text
quantidade
média
mínimo
máximo
última medição
```

Somente registros:

- válidos;
- com timestamp;
- dentro do intervalo.

Amostras de atividades continuam visíveis nos detalhes das respectivas atividades.

## Atividades

Incluir atividades importadas que não estejam:

```text
DISCARDED
```

Mostrar:

- título ou fallback;
- tipo confirmado ou sugerido;
- início;
- duração;
- distância final;
- calorias;
- BPM médio;
- workflow;
- status de sync.

Atividades pendentes de revisão ou calibração continuam visíveis.

Não transformar sua presença no calendário em confirmação ou sincronização.

## Distância final da atividade

Usar:

```text
1. calibrada;
2. original;
3. estimada;
4. indisponível.
```

A distância da atividade é mostrada no card da atividade.

Ela não participa do total de distância diária.

## Peso

Mostrar medidas persistidas do dia.

Quando houver mais de uma:

- ordenar da mais recente para a mais antiga;
- usar a mais recente como resumo;
- manter a lista das demais no detalhe diário.

Não implementar:

- cadastro;
- edição;
- exclusão;
- meta de peso;
- gráfico completo de peso.

Esses itens permanecem para a Fase 16.

## Sincronização

Criar resumo diário equivalente a:

```text
pendingCount
syncingCount
failedCount
syncedCount
```

Status visual do dia:

```text
Sem operações
Pendente
Sincronizando
Sincronizado
Falha
Parcial
```

Política:

- falha possui maior prioridade visual;
- depois syncing;
- depois pending;
- somente todas concluídas produz `Sincronizado`;
- combinação de sucesso e pendência/falha produz `Parcial`.

Não consultar o provider.

Usar somente a fila local.

## Calendário mensal

A tela principal deve conter:

- título;
- mês e ano;
- ação mês anterior;
- ação próximo mês;
- ação Hoje;
- cabeçalho dos dias da semana;
- grade mensal;
- legenda;
- estado vazio seguro.

Usar o primeiro dia da semana definido pela localidade atual.

Não assumir permanentemente domingo ou segunda-feira.

## Célula do dia

Cada célula pode apresentar:

- número do dia;
- destaque de hoje;
- destaque de seleção;
- indicador de passos;
- indicador de BPM;
- quantidade ou indicador de atividades;
- indicador de pendência ou erro.

Evitar excesso visual.

Prioridade recomendada:

1. erro ou pendência;
2. atividade;
3. BPM;
4. passos.

A célula deve possuir descrição acessível completa, por exemplo:

```text
24 de julho, possui passos, frequência cardíaca e duas atividades.
Há uma sincronização pendente.
```

## Dias fora do mês

Podem ser:

- ocultados;
- ou mostrados com contraste reduzido.

Não devem abrir um dia de outro mês sem atualizar corretamente o mês selecionado.

## Datas futuras

Podem ser selecionadas, mas devem mostrar estado vazio.

Não criar registros ou valores simulados.

## Detalhe diário

Criar destino equivalente a:

```text
CalendarDay(localDateIso)
```

Validar com:

```text
LocalDate.parse(localDateIso)
```

A tela deve conter:

1. cabeçalho da data;
2. resumo de passos;
3. distância diária;
4. frequência cardíaca;
5. atividades;
6. peso;
7. sincronização;
8. estados vazios por seção.

Abrir o detalhe não inicia nenhuma operação externa.

## Navegação

Fluxo principal:

```text
Calendário
    ↓
Dia selecionado
    ↓
Detalhe diário
```

Ações opcionais do detalhe:

```text
BPM → detalhe de frequência cardíaca
Passos → detalhe de passos
Distância → detalhe de distância
Atividade → detalhe existente da atividade
```

Ao abrir uma atividade pelo calendário:

- reutilizar a tela existente;
- não duplicar a tela de atividade;
- preservar `CalendarDay` no back stack;
- voltar retorna ao mesmo mês e dia.

Continuar usando Navigation3 e back stack manual.

Não introduzir `NavHost` ou `NavController`.

## CalendarRepository

Planejar operações equivalentes a:

```text
observeMonth(query)
observeDay(localDate, zoneId)
```

O repository deve:

- combinar fontes de forma tipada;
- evitar casts indexados em listas de `combine`;
- resolver a política de múltiplos dispositivos;
- produzir modelos prontos para o ViewModel;
- não expor entities;
- não criar efeitos colaterais.

## Queries necessárias

Evoluir os contratos locais para suportar intervalos.

### DailySummaryRepository

Já possui intervalo por `LocalDate`.

Garantir política explícita de seleção por dia.

### HeartRateRepository

Reutilizar consulta por `Instant`.

Permitir intervalo arbitrário quando necessário.

### WorkoutActivityRepository

Adicionar operação equivalente a:

```text
observeActivitiesBetween(from, until)
```

Usar:

```text
startedAtEpochMillis >= from
startedAtEpochMillis < until
```

Excluir `DISCARDED` por padrão no calendário.

### PersonalMetricsRepository

Adicionar operação equivalente a:

```text
observeWeightsBetween(from, until)
```

Usar o perfil local atual quando necessário.

### HealthSyncRepository

Adicionar operação observável ou snapshot equivalente para consultar operações dos aggregates presentes no intervalo.

Evitar uma consulta por item da lista.

Preferir consulta em lote por:

```text
aggregateType
aggregateIds
```

## Schema e migration

Não criar tabela nova.

Não há migration planejada como requisito inicial da Fase 15.

Alterações esperadas:

- novos contratos;
- novas queries;
- novos projections;
- novos mappers;
- novo repository de calendário.

Não adicionar índices ou subir a versão do Room sem evidência concreta de problema de desempenho.

Caso a auditoria demonstre necessidade obrigatória de índice:

- documentar;
- tratar em task específica;
- exportar novo schema;
- não misturar com implementação visual.

## CalendarViewModel

Estado equivalente a:

```text
selectedMonth
selectedDate
monthSnapshot
dayDetail
isLoadingMonth
isLoadingDay
error
```

Intents equivalentes:

```text
Load
PreviousMonth
NextMonth
GoToToday
SelectDate
OpenSelectedDate
Retry
DismissError
```

Regras:

- mês inicial é o mês atual;
- data inicial é hoje;
- mudança de mês não consulta BLE;
- seleção não altera persistência;
- estado deve sobreviver a recomposição;
- restaurar mês e data quando possível.

## Histórico de passos

Transformar `StepsDetailScreen` em tela funcional de histórico.

Períodos:

```text
7 dias
30 dias
90 dias
```

Para cada período:

- pontos diários reais;
- total;
- média por dia com registro;
- máximo;
- quantidade de dias com registro;
- quantidade de dias que alcançaram a meta atual.

Não tratar dia ausente como zero.

## Gráfico de passos

Preferir gráfico de barras.

Cada barra representa um resumo diário real.

Regras:

- dias ausentes aparecem como lacunas;
- exibir resumo textual;
- não adicionar biblioteca externa sem necessidade;
- reutilizar Canvas ou componentes existentes;
- escala deve lidar com valores altos;
- gráfico não substitui a lista.

## Histórico de distância

Transformar `DistanceDetailScreen` em tela funcional.

Períodos:

```text
7 dias
30 dias
90 dias
```

Para cada período:

- distância diária estimada;
- total;
- média por dia com valor;
- maior distância;
- quantidade de dias com estimativa;
- quantidade de dias sem estimativa, quando houver resumo.

Não misturar distância de atividade.

## Gráfico de distância

Usar pontos diários reais.

Pode usar:

- barras;
- ou linha com pontos.

Regras:

- ausência não vira zero;
- indicar estimativa;
- usar quilômetros na apresentação;
- manter metros no domínio;
- fornecer resumo textual acessível.

## Histórico visual

Passos e distância devem possuir listas ordenadas do mais recente para o mais antigo.

Cada item deve mostrar:

- data;
- valor;
- origem;
- atualização;
- indicação de estimativa para distância.

Tocar no item pode abrir:

```text
CalendarDay(data)
```

## Peso

A tela específica de peso permanece limitada ao resumo atual.

A Fase 15 pode mostrar peso dentro do detalhe diário, mas não deve antecipar o histórico completo e gerenciamento da Fase 16.

## Estados vazios

Distinguir:

```text
Mês sem dados
Dia sem dados
Sem passos
Sem BPM
Sem atividades
Sem peso
Sem operações de sincronização
Distância indisponível
```

Não usar uma única mensagem genérica para todas as seções.

## Erros

Considerar:

- falha ao ler mês;
- falha ao ler dia;
- registro inconsistente;
- data inválida na rota;
- erro de mapper;
- aggregate ausente;
- operação de sync órfã;
- falha de armazenamento.

Não apresentar stack trace.

Uma fonte inconsistente não deve impedir todas as outras seções do dia quando houver degradação segura possível.

## Loading

Calendário deve preservar o mês anterior enquanto o próximo carrega, quando possível.

O detalhe diário deve representar:

```text
Loading
Ready
Empty
InvalidDate
Failed
```

## Logging

Pode registrar:

- mês carregado;
- data selecionada;
- quantidade de dias com dados;
- quantidade de registros por categoria;
- categoria de erro.

Não registrar:

- BPM;
- passos;
- peso;
- distância;
- calorias;
- título;
- notas;
- IDs persistentes;
- `clientRecordId`;
- payloads.

## Privacidade

Não alterar:

- `allowBackup=false`;
- regras de armazenamento privado;
- política sem servidor;
- política sem rota.

O calendário não deve exportar dados.

## Acessibilidade

Garantir:

- ordem de foco previsível;
- nomes completos dos meses;
- célula do dia com descrição;
- seleção anunciada;
- hoje anunciado;
- marcadores descritos por texto;
- gráficos com resumo textual;
- listas como alternativa aos gráficos;
- alvos de toque adequados;
- contraste para estados pendente, erro e selecionado.

## Validação manual

Validar no Galaxy S22 usando dados locais reais.

### Calendário

1. abrir Calendário;
2. confirmar mês atual;
3. navegar mês anterior;
4. navegar mês seguinte;
5. usar Hoje;
6. confirmar dias com indicadores;
7. selecionar um dia;
8. abrir o detalhe;
9. voltar e preservar o mês.

### Dados

10. confirmar passos;
11. confirmar meta atual;
12. confirmar distância estimada;
13. confirmar BPM;
14. confirmar atividades;
15. confirmar peso quando disponível;
16. confirmar estados de sincronização;
17. confirmar atividade descartada ausente.

### Navegação

18. abrir detalhes de passos;
19. abrir detalhes de distância;
20. abrir frequência cardíaca;
21. abrir atividade;
22. voltar ao mesmo dia.

### Históricos

23. alternar 7, 30 e 90 dias em passos;
24. validar gráfico;
25. validar lista;
26. alternar 7, 30 e 90 dias em distância;
27. validar gráfico;
28. validar lista;
29. confirmar lacunas sem zero sintético.

### Offline

30. desligar Bluetooth;
31. indisponibilizar Health Connect quando possível;
32. abrir calendário;
33. abrir detalhe;
34. navegar entre meses;
35. confirmar que nenhuma permissão é solicitada;
36. confirmar que nenhum BLE é iniciado.

### Privacidade e visual

37. confirmar ausência de valores sensíveis no Logcat;
38. confirmar acessibilidade;
39. registrar screenshots das telas alteradas.

## Critérios de aceite

A Fase 15 será concluída quando:

- Fases 13 e 14 estiverem registradas como adiadas;
- Fase 15 estiver registrada como próxima execução;
- ADR do read model estiver aceita;
- Calendário deixar de ser placeholder;
- mês atual abrir corretamente;
- navegação entre meses funcionar;
- ação Hoje funcionar;
- dias com passos forem identificados;
- dias com BPM forem identificados;
- dias com atividades forem identificados;
- pendências e falhas forem indicadas;
- detalhe diário funcionar;
- passos forem exibidos sem soma duplicada;
- distância diária não somar atividades;
- BPM consolidar manual e D1;
- atividades descartadas forem omitidas;
- peso existente for exibido;
- sincronização usar somente estado local;
- gráfico de passos funcionar;
- histórico de passos funcionar;
- gráfico de distância funcionar;
- histórico de distância funcionar;
- dias ausentes não forem convertidos em zero;
- navegação para detalhes preservar retorno;
- tudo funcionar offline;
- nenhuma operação BLE ocorrer;
- nenhum acesso ao Health Connect ocorrer;
- lint, debug e release concluírem;
- instalação no Galaxy S22 funcionar;
- screenshots forem registradas;
- documentação refletir o estado final.
