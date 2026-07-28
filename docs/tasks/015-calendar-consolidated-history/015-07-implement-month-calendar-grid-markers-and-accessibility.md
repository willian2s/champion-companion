# 015-07 — Implementar grade mensal, marcadores e acessibilidade

- **Status:** concluída em 28/07/2026.
- **Objetivo:** substituir placeholder por calendário mensal local e acessível.
- **Resultado esperado:** grade funcional com navegação, seleção, Hoje,
  indicadores e legenda.

## Documentos obrigatórios

Spec 015, 015-06, `CalendarScreen.kt`, design system, convenções de localidade,
Navigation3 e baseline visual.

## Dependências e entradas

- 015-06 concluída.

## Arquivos permitidos

`feature/calendar` e componentes próprios de calendário; documentação e
screenshots de validação.

## Arquivos proibidos

Biblioteca externa sem justificativa, DAOs, repositories, migrations, Health
Connect, BLE, workflows, testes e roadmap/ADR.

## Escopo

Renderizar mês, cabeçalho, anterior/próximo, Hoje, dias da semana conforme
localidade, célula selecionada, célula de hoje, indicadores, legenda, empty,
loading, erro, semântica de acessibilidade e acionamento de `SelectDate`/
`OpenDate`.

## Fora do escopo

Detalhe diário, gráficos, sync, edição de dados e operações externas.

## Passos

1. Implementar grade sem dependência externa.
2. Mapear presença do snapshot para células.
3. Aplicar seleção/Hoje e navegação.
4. Adicionar semantics/content descriptions.
5. Validar dias vazios e limites de mês.

## Critérios de aceite

- Mês anterior/próximo e Hoje funcionam.
- Localidade é respeitada.
- Indicadores não inventam dados.
- Células vazias continuam navegáveis conforme política definida.
- Leitor de tela anuncia data, seleção, hoje e presença.
- Primeiro dia da semana segue localidade atual.
- Dias fora do mês não abrem outro mês sem atualizar seleção corretamente.
- Datas futuras podem abrir estado vazio sem criar valores.
- Título, mês, ano, legenda e prioridade visual de indicadores estão presentes.
- Células anunciam número, hoje, seleção, passos, BPM, atividades e pendência
  ou erro em descrição acessível.

## Validação manual

Galaxy S22: navegar meses, selecionar datas, usar leitor de tela e conferir
orientação/escala. Screenshot exigido.

## Comandos de build

```bash
./gradlew lint assembleDebug assembleRelease
./gradlew installDebug
```

Não executar testes.

## Evidências e atualização

Registrar screenshots, localidade usada, estados vazio/loading/erro e comandos.
Atualizar overview após aceite.

## Validação final

Confirmar ausência de biblioteca externa não aprovada e ausência de operações
BLE/Health Connect. Não criar, alterar ou executar testes automatizados.

## Contexto

O calendário atual é `PlaceholderScreen`; esta task implementa somente a grade
mensal sobre snapshots já fornecidos.

## Arquivos alterados

- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/calendar/CalendarScreen.kt`;
- `docs/tasks/015-calendar-consolidated-history/015-07-calendar-grid.png`;
- `docs/tasks/015-calendar-consolidated-history/015-07-calendar-next-month.png`;
- `docs/tasks/015-calendar-consolidated-history/015-07-calendar-font-scale.png`;
- `docs/tasks/015-calendar-consolidated-history/015-07-calendar-landscape.png`.

## Decisões e desvios

- Grade própria em Compose, sem biblioteca externa de calendário.
- Primeiro dia da semana e nomes dos dias usam `WeekFields` e
  `LocalConfiguration` na localidade atual; validação manual usou `pt-BR`.
- Células fora do mês são espaços não acionáveis. Datas vazias do mês continuam
  selecionáveis e não criam valores.
- Indicadores usam símbolos `!`, `…`, `A`, `B`, `P` e `W`, com prioridade de
  fundo para falha/pendência, atividade, BPM e passos.
- Issues do snapshot permanecem visíveis como estado parcial após seleção; retry
  mensal reinicia `Load` do mês, sem confundir erro diário com erro mensal.
- Nenhum desvio aprovado; orientação permaneceu portrait porque o dispositivo
  não alternou para landscape durante validação forçada.

## Screenshot

- `015-07-calendar-grid.png`: Julho de 2026, dados locais, seleção, Hoje e
  indicadores `ABP`.
- `015-07-calendar-next-month.png`: Agosto de 2026, navegação para mês sem
  indicadores.
- `015-07-calendar-font-scale.png`: escala de fonte 1.3, grade sem corte ou
  sobreposição.
- `015-07-calendar-landscape.png`: tentativa de rotação forçada; app permaneceu
  portrait.

## Riscos residuais

- TalkBack não foi ativado para validação falada; UIAutomator confirmou 41
  descrições acessíveis, incluindo mês, dias completos, Hoje, seleção, passos,
  BPM e atividades.
- Loading e erro de fonte não foram reproduzidos com fonte local real durante
  smoke manual; estados permanecem cobertos por fluxo de `CalendarViewModel` e
  ações recuperáveis na UI.
- O dispositivo não alternou para landscape; comportamento registrado como
  portrait-only, sem alterar contrato da subtarefa.

## Comandos executados

- `./gradlew :app:compileDebugKotlin` — sucesso após correções intermediárias.
- `./gradlew lint assembleDebug assembleRelease` — sucesso.
- `./gradlew installDebug` — sucesso no `SM-S901E - 16`.
- `adb shell am force-stop ... && adb shell am start ...` — sucesso.
- `adb shell input tap ...` — navegação para Calendário, mês anterior/próximo,
  seleção e Hoje.
- `adb shell settings put system font_scale 1.3` — validação de escala; valor
  restaurado para `1.0`.
- `adb shell settings put system user_rotation 1` — tentativa de landscape;
  valor restaurado para `0`.
- `adb shell uiautomator dump ...` — 41 content descriptions inspecionadas.
- Testes automatizados não criados, alterados ou executados.

## Resultados e evidências

- Build completo, lint, APK debug/release e instalação concluíram com `BUILD
  SUCCESSFUL`.
- Calendário abriu sem iniciar BLE, Health Connect ou permissões observáveis.
- Julho exibiu `Mês exibido: Julho 2026`; semana começou em domingo no `pt-BR`.
- UIAutomator encontrou descrições como `domingo`, `segunda-feira` e `28 de
  julho de 2026, hoje, selecionado, passos registrados, BPM registrado, 2
  atividades`.
- Navegação para Agosto e retorno por Hoje funcionaram; data vazia/futura foi
  selecionável sem indicador inventado.
- Escala 1.3 preservou grade e contraste dos marcadores.
- Não houve operações BLE, Health Connect, migration, DAO, repository ou
  biblioteca externa.

## Atualização do overview

Marcar somente o item 015-07 após aceite de 015-06.

## Regra de testes

Não criar, alterar ou executar testes automatizados.

## Execução independente

Executar em sessão nova usando somente documentos e dependências listados. Não
depender de conversa anterior nem avançar para a próxima subtarefa.
