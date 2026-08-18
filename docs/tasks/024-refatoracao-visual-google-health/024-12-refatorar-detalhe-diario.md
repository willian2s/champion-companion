# 024-12 — Refatorar detalhe diário

- **Ticker:** `024`
- **Número:** `12`
- **Status:** completed

## Objetivo e resultado esperado

Refatorar detalhe diário para apresentar panorama local do dia antes de fontes,
problemas e fila. Resultado: passos, distância, BPM, atividades e peso agrupados
com navegação contextual preservada.

## Requisitos e critérios cobertos

- R4–R9; critérios 5, 7–12 da spec.
- Regra cromática comum: quando presentes, header azul-petróleo nominal do
  Champion com conteúdo branco; cards, indicadores e progresso com acentos
  nominais `primary`/`secondary`/`tertiary` da paleta clara/escura conforme o
  tema, seguindo o piloto `024-05`, sem alterar o tema raiz.

## Escopo incluído

- header, data, retorno, resumo e seções métricas;
- atividades, peso, problemas por fonte e fila local informativa;
- atalhos para detalhes de Saúde e Exercícios;
- loading, erro, data inválida e ausência.

## Escopo excluído

- read model, sync, repositories ou regra temporal;
- ação nova sobre dados; detalhe permanece somente leitura.

## Dependências

- `024-11` concluída.

## Arquivos e símbolos prováveis

- `feature/calendar/CalendarDayScreen.kt`;
- `CalendarDayScreen`, `DayHeader` e cards/seções locais.

## Passos de implementação

1. Priorizar data e panorama do dia.
2. Agrupar métricas relacionadas sem ocultar ausência ou origem.
3. Manter atividades e atalhos acionáveis com labels contextuais.
4. Rebaixar fila e problemas técnicos para seção secundária legível.
5. Preservar retorno ao calendário e tratamento de data inválida.
6. Validar dias completos, vazios e estados disponíveis.
7. Registrar evidências e overview.

## Testes e comandos de validação

Sem testes automatizados.

```bash
git diff --check
./gradlew lint assembleDebug assembleRelease
./gradlew installDebug
adb devices -l
adb shell am start -n br.com.mwconsulting.championcompanion.dev/br.com.mwconsulting.championcompanion.MainActivity
```

Smoke: Calendário → dia → detalhes relacionados → voltar; rolagem, fonte
`1,3x`, landscape e temas representativos.

## Definição de pronto

- panorama diário antecede detalhes técnicos;
- atalhos e retorno preservados;
- nenhuma sincronização iniciada;
- estados vazio/erro/data inválida seguros;
- comandos, capturas e overview registrados.

## Riscos e cuidados

- conteúdo longo pode pressionar landscape;
- fila informativa não pode parecer CTA;
- captura pode reunir várias métricas pessoais;
- não alterar candidate temporal nem navegação cruzada.

## Registro de execução

- **Status:** completed.
- **Arquivos alterados:**
  `app/src/main/java/br/com/mwconsulting/championcompanion/feature/calendar/CalendarDayScreen.kt` e
  `024-12-captura-real-sanitizada-detalhe-diario.png`.
- **Decisões:** o detalhe passou a abrir com header nominal azul-petróleo e
  conteúdo branco, seguido por panorama local com passos, distância, BPM,
  atividades, peso e estado das fontes. Métricas detalhadas foram agrupadas em
  cards com acentos nominais `primary`, `secondary` e `tertiary`; problemas por
  fonte e fila ficaram em seções secundárias informativas. Status de fila por
  atividade permanece observável na seção de fila, sem CTA novo. Retorno,
  atalhos, labels de origem, estados de ausência e tratamento de data inválida
  foram preservados.
- **Desvios:** nenhum contrato funcional foi alterado. ViewModel, read model,
  intents, navegação, persistência, sincronização, BLE e regra temporal não
  foram tocados. Não foram criados, alterados ou executados testes
  automatizados. Loading, erro total, data inválida, ausência e problemas por
  fonte não foram fabricados para evidência e permanecem `N/A`.
- **Comandos executados:** `git diff --check`; `./gradlew app:compileDebugKotlin`;
  `./gradlew lint assembleDebug assembleRelease`; `./gradlew installDebug`;
  `adb devices -l`; abertura via `adb shell am start -n
  br.com.mwconsulting.championcompanion.dev/br.com.mwconsulting.championcompanion.MainActivity`;
  dumps de hierarquia sanitizados via `uiautomator`; smoke Calendário → dia →
  Passos → retorno, rolagem, fila informativa, status por atividade, fonte
  `1,3x`, landscape, temas claro/escuro e restauração do aparelho.
- **Resultados e evidências:** `git diff --check`, compilação Kotlin, lint,
  builds debug/release e instalação passaram. Galaxy S22 `SM-S901E` confirmou
  header, panorama, rolagem, abertura do detalhe de Passos, retorno contextual
  e status individual da fila. Fonte `1,3x`, landscape e tema claro abriram
  sem cortar header ou ação essencial; estado final restaurado para fonte
  `1,0x`, portrait, rotação automática desativada e tema escuro. Captura
  sanitizada, com data, métricas e timestamps ocultos:
  `024-12-captura-real-sanitizada-detalhe-diario.png`.
- **Riscos residuais:** loading, erro, data inválida, ausência e problemas
  parciais não foram observados fisicamente; permanecem `N/A`. A captura não
  representa conteúdo pessoal real, apenas estrutura da superfície.
