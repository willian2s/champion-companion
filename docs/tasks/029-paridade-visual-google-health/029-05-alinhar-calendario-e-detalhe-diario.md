# 029-05 — Alinhar calendário e detalhe diário

- **Ticker:** `029`
- **Número:** `05`
- **Status:** `pending`
- **Depende de:** `029-03` e `029-01`

## Objetivo e resultado esperado

Reproduzir a composição observada para calendário, navegação mensal, marcadores,
legenda e detalhe diário, preservando leitura local, links contextuais e
semântica dos read models existentes.

## Requisitos e critérios de aceite cobertos

- R2, R4, R7 e R8 da Spec 029.
- Critérios 4, 9, 10 e 11.
- Cobrir meses com e sem atividades, dias selecionados e retorno.

## Escopo incluído

- `CalendarScreen` e `CalendarDayScreen`;
- header, navegação de mês, grade, marcadores, legenda, cards e estados vazio,
  loading e erro;
- equivalência de densidade e ordem observada no app instalado;
- abertura de detalhe diário e retorno sem iniciar sync ou medição;
- evidências sanitizadas de calendário e detalhe.

## Escopo excluído

- nova fonte de dados, GPS, rota, BLE ou treino phone;
- alterar o conjunto canônico de fontes do calendário;
- incluir BPM de atividade no calendário sem decisão funcional explícita;
- abas Semana/Mês específicas da superfície de exercícios, tratadas em 029-06.

## Dependências

- baseline 029-01;
- fundação 029-03;
- `CalendarHistory.kt`, `CalendarViewModel`, `CalendarScreen.kt` e
  `CalendarDayScreen.kt`.

## Arquivos e símbolos prováveis

- `feature/calendar/CalendarScreen.kt`;
- `feature/calendar/CalendarDayScreen.kt`;
- `feature/calendar/CalendarViewModel.kt`;
- `feature/calendar/CalendarFeatureContract.kt`;
- `core/model/CalendarHistory.kt`;
- `WorkoutActivityRepository.observeActivitiesBetween` e
  `CalendarDaySummary`.

## Passos de implementação

1. Mapear diferenças entre baseline e calendário atual.
2. Ajustar shell, grade, marcadores, legenda, cards e estados com componentes
   compartilhados.
3. Preservar navegação mensal, seleção, detalhe e retorno.
4. Garantir que abertura seja somente leitura e não dispare sincronização.
5. Validar mês com atividade, mês vazio e detalhe diário.
6. Registrar screenshots sanitizados e limitações.

## Testes e comandos de validação

```bash
git diff --check
./gradlew lint assembleDebug assembleRelease
```

Smoke físico em portrait/landscape, tema claro/escuro, fonte ampliada,
rolagem, mês anterior/próximo, dia com/sem atividade, detalhe e retorno. Não
criar testes automatizados.

## Definição de pronto

- calendário e detalhe correspondem à matriz estrutural;
- atividades, marcadores e ausência continuam semanticamente corretos;
- nenhuma operação remota ou destrutiva é iniciada;
- acessibilidade, retorno, build, lint e smoke passam;
- evidências estão sanitizadas.

## Riscos e cuidados

- Não confundir calendário existente com calendário da aba Mês de exercícios.
- Mudança de timezone pode alterar dia exibido; preservar range canônico.
- Não fabricar marcador para melhorar semelhança visual.
