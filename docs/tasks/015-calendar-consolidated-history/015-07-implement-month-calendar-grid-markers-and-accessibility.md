# 015-07 — Implementar grade mensal, marcadores e acessibilidade

- **Status:** concluída em 28/07/2026; refinamento visual concluído em 28/07/2026.
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
- Indicadores usam pontos coloridos somente para passos (azul), frequência
  cardíaca (verde) e atividade física (amarelo), na ordem azul, verde e amarelo.
- Números das datas usam mesma cor de conteúdo dos indicadores da presença, para
  manter contraste explícito nos dias com atividade.
- A borda de Hoje usa essa cor de conteúdo e tem prioridade sobre a borda de
  seleção, evitando baixo contraste quando Hoje também está selecionado.
- Espaçamento horizontal da grade usa `ChampionSpacing.extraSmall`, igual ao
  espaçamento vertical; células usam somente largura disponível da viewport,
  sem largura forçada ou deslocamento horizontal.
- AndroidX Material 3 `DatePicker` foi avaliado, mas não oferece marcadores de
  presença por célula; `CalendarView` também não atende à grade Compose com
  indicadores. A grade própria foi mantida, corrigindo o overflow na origem.
- Altura mínima das células foi reduzida de 64 dp para 56 dp. O texto visual
  `Hoje` foi removido da célula; destaque por borda/negrito e descrição acessível
  permanecem, evitando empilhamento com os marcadores.
- A grade recebe `ChampionSpacing.small` de respiro horizontal interno, evitando
  contato visual direto entre células e bordas do card.
- Falha e pendência mantêm fundo de estado para preservar prioridade visual da
  spec, mas não geram pontos ou símbolos; peso permanece somente na descrição
  acessível e no detalhe.
- Legenda identifica fundos de falha e pendência separadamente dos três pontos
  métricos.
- Issues do snapshot permanecem visíveis como estado parcial após seleção; retry
  mensal reinicia `Load` do mês, sem confundir erro diário com erro mensal.
- Nenhum desvio aprovado; landscape foi usado somente para smoke visual, sem
  alterar o contrato portrait-first da tela.

## Screenshot

- `015-07-calendar-grid.png`: Julho de 2026, dados locais, seleção, Hoje e
  pontos coloridos; captura atualizada após ajuste de contraste, espaçamento,
  proporção compacta e cores.
- `015-07-calendar-next-month.png`: Agosto de 2026, navegação para mês sem
  indicadores.
- `015-07-calendar-font-scale.png`: escala de fonte 1.3, grade sem corte ou
  sobreposição, com pontos coloridos.
- `015-07-calendar-landscape.png`: rotação forçada após refinamento; primeiros
  rows da grade permaneceram dentro da largura disponível.

## Riscos residuais

- TalkBack não foi ativado para validação falada; UIAutomator confirmou 41
  descrições acessíveis, incluindo mês, dias completos, Hoje, seleção, passos,
  BPM e atividades.
- Loading e erro de fonte não foram reproduzidos com fonte local real durante
  smoke manual; estados permanecem cobertos por fluxo de `CalendarViewModel` e
  ações recuperáveis na UI.
- Landscape foi capturado após rotação forçada; navegação inferior permanece
  sobreposta à área inferior da tela durante o smoke manual.

## Comandos executados

- `./gradlew :app:compileDebugKotlin` — sucesso após correções intermediárias.
- `./gradlew lint assembleDebug assembleRelease` — sucesso.
- `./gradlew :app:compileDebugKotlin installDebug` — sucesso após o refinamento
  visual; APK instalado no `SM-S901E - 16`.
- `./gradlew :app:compileDebugKotlin installDebug` — sucesso após compactação da
  grade; APK instalado no `SM-S901E - 16`.
- `./gradlew :app:compileDebugKotlin installDebug` — sucesso após troca dos
  indicadores por pontos coloridos; APK instalado no `SM-S901E - 16`.
- `./gradlew lint assembleDebug assembleRelease` — sucesso no estado final, após
  pontos coloridos e refinamentos de layout.
- `./gradlew installDebug` — sucesso no `SM-S901E - 16`.
- `adb shell am force-stop ... && adb shell am start ...` — sucesso.
- `adb shell input tap ...` — navegação para Calendário, mês anterior/próximo,
  seleção e Hoje.
- `adb exec-out screencap -p > /var/folders/_r/sp91m8xn4zv3_wwh1wq46_cm0000gn/T/opencode/calendar-contrast.png` — screenshot do refinamento visual; cópia registrada em `015-07-calendar-grid.png`.
- `adb exec-out screencap -p > /var/folders/_r/sp91m8xn4zv3_wwh1wq46_cm0000gn/T/opencode/calendar-colors.png` — screenshot final dos pontos coloridos; cópia registrada em `015-07-calendar-grid.png`.
- `adb shell settings put system font_scale 1.3` — validação de escala; valor
  restaurado para `1.0`.
- `adb shell settings put system user_rotation 1` — smoke visual em landscape;
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
- Screenshot atualizado confirmou números de datas com contraste nos dias de
  atividade, borda escura e legível em Hoje selecionado e gaps horizontais de
  4 dp, iguais aos verticais; grade permaneceu integralmente dentro da viewport
  no Galaxy S22.
- Correção posterior removeu `requiredWidth`/`offset` e confirmou ausência de
  corte lateral no calendário.
- Compactação posterior confirmou células de 56 dp e Hoje sem empilhamento
  visual com pontos coloridos.
- Respiro interno posterior confirmou distância visível de 8 dp entre grade e
  bordas laterais do card.
- Pontos finais confirmaram azul para passos, verde para frequência cardíaca e
  amarelo para atividade física; sincronização preserva somente fundo de estado
  e peso não renderiza ponto.
- Legenda final diferencia pontos métricos de fundos de falha e pendência.
- Screenshots auxiliares de próximo mês, escala 1.3 e landscape
  foram recapturados após troca dos indicadores; Agosto, escala 1.3 e a grade
  em landscape ficaram registrados nos arquivos correspondentes.
- Não houve operações BLE, Health Connect, migration, DAO, repository ou
  biblioteca externa.

## Atualização do overview

Marcar somente o item 015-07 após aceite de 015-06.

## Regra de testes

Não criar, alterar ou executar testes automatizados.

## Execução independente

Executar em sessão nova usando somente documentos e dependências listados. Não
depender de conversa anterior nem avançar para a próxima subtarefa.
