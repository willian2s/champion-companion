# 015-12 — Executar validação manual offline do calendário

- **Status:** concluída em 29/07/2026, com aceite explícito de riscos residuais.
- **Objetivo:** validar fluxo integrado no Galaxy S22 sem fontes externas.
- **Resultado esperado:** evidências manuais, screenshots e builds obrigatórios.

## Documentos obrigatórios

Spec 015, overview, tasks 015-01 a 015-11, baseline atualizada durante a
implementação e instruções de privacidade.

## Dependências e entradas

- 015-11 concluída.
- Galaxy S22 disponível.
- Dados locais reais já persistidos.

## Arquivos permitidos

Este arquivo, overview, evidências e screenshots em local documental aprovado.

## Arquivos proibidos

Código, ADR, roadmap, schema, testes, alterações de dados remotos, BLE,
Health Connect e permissões.

## Escopo

Executar somente validação manual com Bluetooth desligado, relógio desconectado
e Health Connect indisponível ou sem permissão quando reproduzível. Confirmar
abertura, mês anterior/próximo, Hoje, seleção, detalhe, atividade, BPM, passos,
distância, estados vazios/erro e retorno.

## Fora do escopo

Qualquer sincronização, conexão, provider, coleta, teste automatizado ou
correção não documentada.

## Passos

1. Executar build completo.
2. Instalar debug.
3. Desligar Bluetooth e desconectar relógio.
4. Tornar Health Connect indisponível quando reproduzível.
5. Abrir calendário e navegar meses.
6. Abrir dia com e sem dados.
7. Abrir históricos e atividade relacionada.
8. Conferir peso mais recente, medidas adicionais e atividade descartada ausente.
9. Conferir meta atual, origem, última atualização, estimativa e unidade.
10. Conferir períodos 7/30/90, gráficos, listas, lacunas, zero real e dias
    futuros.
11. Conferir marcadores, legenda, seleção, Hoje, primeiro dia da semana,
    acessibilidade e dias fora do mês.
12. Conferir estados `Sem operações`, `Pendente`, `Sincronizando`,
    `Sincronizado`, `Falha` e `Parcial`, incluindo aggregate ausente, operação
    órfã e degradação parcial segura.
13. Conferir ausência de permissões/operações externas e ausência de dados
    sensíveis no Logcat.
14. Capturar screenshots visuais.
15. Registrar resultado por cenário e riscos residuais.

Os 39 cenários devem ser percorridos explicitamente: 1 abrir Calendário; 2
confirmar mês atual; 3 mês anterior; 4 mês seguinte; 5 Hoje; 6 indicadores;
7 selecionar dia; 8 abrir detalhe; 9 voltar preservando mês; 10 passos; 11 meta
atual; 12 distância estimada; 13 BPM; 14 atividades; 15 peso; 16 estados de
sync; 17 atividade descartada ausente; 18 detalhe de passos; 19 detalhe de
distância; 20 frequência cardíaca; 21 atividade; 22 voltar ao mesmo dia; 23
períodos de passos 7/30/90; 24 gráfico de passos; 25 lista de passos; 26
períodos de distância 7/30/90; 27 gráfico de distância; 28 lista de distância;
29 lacunas sem zero; 30 Bluetooth desligado; 31 Health Connect indisponível
quando possível; 32 calendário offline; 33 detalhe offline; 34 meses offline;
35 nenhuma permissão; 36 nenhum BLE; 37 Logcat sem dados sensíveis; 38
acessibilidade; 39 screenshots.

## Critérios de aceite

- Builds obrigatórios passam.
- Instalação passa.
- Calendário, detalhe e históricos abrem offline.
- Nenhum prompt de permissão aparece.
- Nenhuma operação BLE começa.
- Nenhuma operação Health Connect começa.
- Screenshots cobrem mudanças visuais.
- Atividade `DISCARDED` não aparece; atividades pendentes permanecem visíveis.
- Resumo diário mostra origem e última atualização; distância mostra estimativa e
  usa quilômetros somente na apresentação.
- Peso mais recente e medidas adicionais respeitam ordenação.
- Estados de sync e degradação parcial não são confundidos com vazio.

## Validação manual

Esta task é a validação manual no Galaxy S22 descrita acima. Screenshot exigido.

## Comandos de build

```bash
./gradlew lint assembleDebug assembleRelease
./gradlew installDebug
```

Não executar testes.

## Evidências e atualização

Registrar saída dos comandos, versão instalada, configuração offline,
screenshots, cenários aprovados, falhas e riscos residuais. Atualizar overview.

## Validação final

Confirmar que não houve acesso BLE ou Health Connect e que nenhum teste foi
executado. Não criar, alterar ou executar testes automatizados.

## Contexto

Fase 15 deve permanecer útil com fontes externas desligadas. Esta task comprova
o comportamento local em dispositivo físico.

## Arquivos alterados

- este arquivo;
- `docs/tasks/015-calendar-consolidated-history/015-00-overview.md`;
- `docs/tasks/015-calendar-consolidated-history/015-12-calendar.png`;
- `docs/tasks/015-calendar-consolidated-history/015-12-calendar-day-detail.png`;
- `docs/tasks/015-calendar-consolidated-history/015-12-calendar-empty.png`;
- `docs/tasks/015-calendar-consolidated-history/015-12-steps-history.png`;
- `docs/tasks/015-calendar-consolidated-history/015-12-distance-history.png`;
- `docs/tasks/015-calendar-consolidated-history/015-12-heart-rate-history.png`;
- `docs/tasks/015-calendar-consolidated-history/015-12-calendar-activity.png`;
- `docs/tasks/015-calendar-consolidated-history/015-12-exercise-detail.png`;
- `docs/tasks/015-calendar-consolidated-history/015-12-steps-chart-list.png`;
- `docs/tasks/015-calendar-consolidated-history/015-12-steps-list-detail.png`;
- `docs/tasks/015-calendar-consolidated-history/015-12-distance-chart-empty.png`;
- `docs/tasks/015-calendar-consolidated-history/015-12-distance-list-empty.png`;
- `docs/tasks/015-calendar-consolidated-history/015-12-heart-rate-chart.png`;
- `docs/tasks/015-calendar-consolidated-history/015-12-heart-rate-list.png`.

## Decisões e desvios

- A tentativa inicial de `installDebug` foi bloqueada por ausência temporária de
  dispositivo; execução foi retomada após conexão do Galaxy S22.
- Validação usou Galaxy S22 `SM-S901E`, Android 16, variante debug
  `br.com.mwconsulting.championcompanion.dev`, em `pt-BR`.
- Bluetooth foi desligado por `adb shell svc bluetooth disable` e confirmado com
  `settings get global bluetooth_on = 0`; relógio permaneceu desconectado.
- Health Connect não foi tornado indisponível por alteração externa nem aberto;
  nenhum prompt, provider ou operação foi acionado durante o fluxo. Cenário de
  indisponibilidade específica permanece não reproduzido.
- Distância positiva não estava disponível por ausência de dados pessoais
  suficientes; estado sem estimativa foi validado sem inventar zero.
- Estados locais de sync observados foram `Sincronizado` e ausência de
  pendências. Estados `Pendente`, `Sincronizando`, `Falha`, `Parcial` e fila
  indisponível não foram fabricados nem reproduzidos.
- Usuário aceitou como cobertura suficiente, neste app pessoal, o diagnóstico
  pré-existente de contagem de atividades no Logcat; nenhum payload ou valor
  clínico foi exposto e nenhuma alteração de código foi autorizada.
- Usuário confirmou que ausência de dados de peso é condição atual do produto;
  estado vazio foi aceito, sem fabricar ordenação ou valor real.
- Usuário aceitou cobertura de atividades com registros locais disponíveis;
  detalhes, status e ausência de `DISCARDED` foram observados.
- Usuário aceitou como riscos residuais a ausência de distância positiva, zero
  real e estados de sync diferentes de `Sincronizado`; não houve fabricação de
  dados nem alteração de persistência para reproduzi-los.

## Comandos executados

- `git status --short`
- `./gradlew lint assembleDebug assembleRelease`
- `./gradlew installDebug` — bloqueado por `DeviceException: No connected devices!`
- `adb devices -l` — primeira consulta sem dispositivo; segunda consulta com
  `SM-S901E` conectado
- `./gradlew installDebug` — `BUILD SUCCESSFUL` após conexão do dispositivo
- `adb shell svc bluetooth disable`
- `adb shell settings get global bluetooth_on`
- `adb shell am force-stop ... && adb shell am start ...`
- `adb exec-out screencap -p > ...` — capturas dos cenários documentados
- `adb shell uiautomator dump ...` — árvore e descrições semânticas
- `adb logcat -c`, inicialização do app e `adb logcat --pid ...`
- `adb shell dumpsys activity activities`
- `git diff --check`

Não foram executados testes automatizados. O build completo executou somente
lint e assemblies solicitados; a primeira instalação falhou antes da conexão
do dispositivo e a segunda passou.

## Resultados e evidências

- `./gradlew lint assembleDebug assembleRelease`: `BUILD SUCCESSFUL`; lint e
  APKs debug/release foram gerados.
- `./gradlew installDebug`: `BUILD SUCCESSFUL`; APK instalado no Galaxy S22
  `SM-S901E - 16`.
- `adb devices -l`: dispositivo conectado por ADB sem fio.
- Calendário abriu offline; meses, Hoje, seleção, indicadores, legenda, dia com
  dados, dia vazio e retorno preservando julho foram observados.
- Detalhe diário exibiu passos, **Meta atual**, origem e atualização, distância
  indisponível, BPM, atividades e status local da fila. Atividade abriu o
  `ExerciseDetail` compartilhado e voltou ao mesmo dia.
- Históricos de passos exibiram `7D`, `30D`, `90D`, totais, média, máximo, dias
  registrados, meta atual, gráfico, lacunas e lista paginada.
- Histórico de distância exibiu `7D`, `30D`, `90D`, estado sem estimativas,
  gráfico sem valores sintéticos e lista em quilômetros com origem/atualização.
- Histórico dedicado de BPM exibiu períodos diário/semanal/mensal, gráfico e
  lista com leituras manuais e amostras vinculadas a atividade.
- `adb shell settings get global bluetooth_on`: `0`.
- `dumpsys activity activities`: `launchFailed=false`.
- Logcat filtrado pelo PID do app não mostrou payload BLE, chamada de conexão,
  Health Connect, permissões, tokens, IDs ou valores de BPM/passos/distância/
  peso; porém mostrou diagnóstico pré-existente `ExercisesDbStatus` com
  contagens de atividades. Critério de privacidade não atendido integralmente;
  desvio foi explicitamente aceito para fechamento documental no contexto de
  app pessoal. Registro não foi alterado, pois código é proibido no escopo.
- UI Automator encontrou descrições para mês, navegação, sete dias da semana,
  datas, seleção, Hoje, presença e datas sem dados.

## Cenários validados

1. Abrir Calendário — aprovado.
2. Confirmar mês atual — aprovado: julho de 2026.
3. Mês anterior — aprovado: junho de 2026.
4. Mês seguinte — aprovado: julho de 2026.
5. Hoje — aprovado: 29/07/2026.
6. Indicadores — aprovado: passos, BPM e atividade.
7. Selecionar dia — aprovado: 28/07/2026 e 30/07/2026.
8. Abrir detalhe — aprovado.
9. Voltar preservando mês — aprovado.
10. Passos — aprovado.
11. Meta atual — aprovado: rótulo exibido.
12. Distância estimada — aprovado como indisponível, sem valor sintético; falta
    de distância positiva aceita como risco residual.
13. BPM — aprovado.
14. Atividades — aprovado: duas atividades no dia com dados.
15. Peso — estado sem dados exibido; ausência de medidas reais aceita pelo
    usuário como condição atual do produto.
16. Estados de sync — `Sincronizado` e ausência de pendências aprovados; demais
    estados não reproduzidos e aceitos como risco residual pelo usuário.
17. Atividade descartada ausente — aprovado com cobertura disponível; atividades
    e detalhes locais foram validados, `DISCARDED` não apareceu e atividade
    pendente não estava disponível.
18. Detalhe de passos — aprovado.
19. Detalhe de distância — aprovado.
20. Frequência cardíaca — aprovado.
21. Atividade — aprovado.
22. Voltar ao mesmo dia — aprovado.
23. Períodos de passos 7/30/90 — aprovado.
24. Gráfico de passos — aprovado.
25. Lista de passos — aprovado, com paginação e lacunas.
26. Períodos de distância 7/30/90 — aprovado.
27. Gráfico de distância — aprovado, sem estimativas no período.
28. Lista de distância — aprovado, com `Sem estimativa` e unidade km.
29. Lacunas sem zero — aprovado em passos e distância; zero real não disponível
    e aceito como risco residual.
30. Bluetooth desligado — aprovado, valor global `0`.
31. Health Connect indisponível — não reproduzido; provider não foi acessado.
32. Calendário offline — aprovado.
33. Detalhe offline — aprovado.
34. Meses offline — aprovado.
35. Nenhuma permissão — aprovado; nenhum prompt apareceu.
36. Nenhum BLE — aprovado; nenhuma operação de scan/conexão começou.
37. Logcat sem dados sensíveis — aprovado com desvio aceito pelo usuário no
    contexto de app pessoal: contagem pré-existente apareceu, sem payload ou
    valor clínico.
38. Acessibilidade — aprovado por UI Automator; TalkBack falado não foi ativado.
39. Screenshots — aprovado; quatorze capturas documentais registradas.

## Screenshot

- `015-12-calendar.png`: mês, seleção, Hoje e indicadores.
- `015-12-calendar-day-detail.png`: detalhe diário com passos e distância.
- `015-12-calendar-empty.png`: seleção de dia sem dados.
- `015-12-steps-history.png`: histórico de passos e períodos.
- `015-12-distance-history.png`: histórico de distância sem estimativas.
- `015-12-heart-rate-history.png`: histórico dedicado de BPM.
- `015-12-calendar-activity.png`: atividades e status local no detalhe diário.
- `015-12-exercise-detail.png`: detalhe compartilhado da atividade.
- `015-12-steps-chart-list.png` e `015-12-steps-list-detail.png`: gráfico e
  lista de passos.
- `015-12-distance-chart-empty.png` e `015-12-distance-list-empty.png`:
  gráfico e lista sem estimativas.
- `015-12-heart-rate-chart.png` e `015-12-heart-rate-list.png`: gráfico e
  lista dedicada de BPM.

## Riscos residuais

- Health Connect indisponível e ausência de permissões específicas não foram
  forçados; somente ausência de prompts e ausência de chamadas foram observadas.
- Distância positiva, zero real,
  estados de sync diferentes de `Sincronizado`, degradação parcial, erro/retry,
  aggregate ausente e operação órfã não foram reproduzidos com dados locais
  existentes; usuário aceitou explicitamente esses riscos residuais.
- Peso com medidas reais e atividade pendente permanecem sem dados locais nesta
  sessão; ausência foi aceita pelo usuário para este app pessoal.
- TalkBack falado não foi ativado; acessibilidade foi verificada pela árvore UI
  Automator e descrições semânticas.
- Diagnóstico pré-existente `ExercisesDbStatus` registra contagens de atividades
  no Logcat; usuário aceitou o desvio no contexto pessoal, sem alteração de
  código nesta subtarefa.

## Atualização do overview

Item `015-12` marcado `[x]`; progresso recalculado para `13/14`. Riscos
residuais e aceites registrados em `Observações`; `015-13` não foi iniciada.

## Regra de testes

Não criar, alterar ou executar testes automatizados.

## Execução independente

Executar em sessão nova usando somente documentos e dependências listados. Não
depender de conversa anterior nem avançar para a próxima subtarefa.
