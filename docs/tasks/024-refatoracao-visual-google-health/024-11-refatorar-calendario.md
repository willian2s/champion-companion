# 024-11 — Refatorar Calendário

- **Ticker:** `024`
- **Número:** `11`
- **Status:** completed

## Objetivo e resultado esperado

Refatorar calendário mensal para melhorar navegação temporal, seleção e leitura
dos indicadores. Resultado: grade estável e hierarquia mais leve sem alterar
read model, seleção ou abertura do dia.

## Requisitos e critérios cobertos

- R4–R9; critérios 5, 7–12 da spec.
- Regra cromática comum: quando presentes, header azul-petróleo nominal do
  Champion com conteúdo branco; cards, indicadores e progresso com acentos
  nominais `primary`/`secondary`/`tertiary` da paleta clara/escura conforme o
  tema, seguindo o piloto `024-05`, sem alterar o tema raiz.

## Escopo incluído

- header, navegação mês anterior/próximo/hoje;
- grade de 42 células, seleção, indicadores e legenda;
- estados parcial, vazio, loading e erro;
- CTA de abrir dia.

## Escopo excluído

- read model, ViewModel, cálculo de indicadores ou detalhe diário;
- novo tipo de evento ou fonte de dados.

## Dependências

- `024-10` concluída.

## Arquivos e símbolos prováveis

- `feature/calendar/CalendarScreen.kt`;
- `CalendarScreen`, `CalendarHeader`, grade, célula, legenda e estados locais.

## Passos de implementação

1. Preservar estrutura de sete colunas e semântica de cada dia.
2. Reorganizar header e controles mensais para leitura rápida.
3. Tornar seleção e indicadores distinguíveis sem depender só de cor.
4. Compactar legenda e CTA sem ocultar estados parciais.
5. Validar meses com cinco/seis semanas e labels localizados.
6. Exercitar abrir dia, voltar e mudar mês.
7. Registrar capturas sanitizadas e overview.

## Testes e comandos de validação

Sem testes automatizados.

```bash
git diff --check
./gradlew lint assembleDebug assembleRelease
./gradlew installDebug
adb devices -l
adb shell am start -n br.com.mwconsulting.championcompanion.dev/br.com.mwconsulting.championcompanion.MainActivity
```

Smoke: meses anterior/próximo, hoje, seleção, abrir dia, portrait, fonte `1,3x`,
landscape e temas.

## Definição de pronto

- grade mantém sete colunas e 42 células sem corte;
- seleção, indicadores e legenda são acessíveis;
- navegação temporal e abertura do dia preservadas;
- estados locais equivalentes;
- comandos, capturas e overview registrados.

## Riscos e cuidados

- largura reduzida pode comprimir números e marcadores;
- fonte ampliada não deve quebrar grade;
- cores locais só migram para tokens quando semântica existir;
- não alterar candidate temporal ou read model.

## Registro de execução

- **Status:** completed após exceção R5 registrada na spec e decisão explícita do
  usuário por preservar dimensões originais.
- **Arquivos alterados:**
  `app/src/main/java/br/com/mwconsulting/championcompanion/feature/calendar/CalendarScreen.kt` e
  `024-11-captura-real-sanitizada-calendar-portrait.png`.
- **Decisões:** Calendário recebeu header azul-petróleo nominal com conteúdo
  branco, controles mensais com ícones Material e ação `Hoje` destacada por
  `primary`. Grade mantém sete colunas e 42 células, com largura aproximada de
  dimensões originais e altura mínima de 56 dp no Galaxy S22. Marcadores usam
  três slots fixos e formas distintas para passos, frequência cardíaca e
  atividade. Seleção usa borda nominal; estados de sincronização permanecem
  semânticos e na legenda. CTA do dia foi priorizado antes da legenda
  compactada.
- **Desvios:** nenhum requisito funcional alterado. ViewModel, intents, read
  model, seleção, abertura do dia, navegação e estados locais foram preservados.
  Loading, erro, parcial e vazio ficaram `N/A` sem fabricação.
- **Comandos executados:** `git diff --check`; `./gradlew app:lintDebug`;
  `./gradlew lint assembleDebug assembleRelease`; `./gradlew installDebug`;
  `adb devices -l`; abertura via `adb shell am start -n
  br.com.mwconsulting.championcompanion.dev/br.com.mwconsulting.championcompanion.MainActivity`;
  dumps de hierarquia sanitizados via `uiautomator`; smoke de mês anterior,
  próximo, `Hoje`, seleção, `Abrir dia`, retorno, rolagem, fonte `1,3x`, tema
  claro, landscape e restauração do aparelho.
- **Resultados e evidências:** lint, builds debug/release e instalação passaram
  no Galaxy S22 `SM-S901E`. Smoke confirmou controles mensais, espaçamento entre
  células, legenda, CTA, seleção, abertura do detalhe e retorno. Fonte `1,3x`,
  tema claro e landscape abriram sem corte de controles; estado final restaurado
  para fonte `1,0x`, tema escuro e portrait. Captura estrutural sanitizada, sem
  cronologia real nem associação entre datas e indicadores:
  `024-11-captura-real-sanitizada-calendar-portrait.png`. Captura foi refeita em
  primeiro plano após descartar captura inválida de painel de notificações.
- **Riscos residuais:** estados loading, erro, parcial e vazio não foram
  observados fisicamente; permanecem `N/A`. Exceção registrada na spec mantém
  células visuais abaixo de 48 dp no viewport portrait de 360 dp para preservar
  dimensões e sete colunas originais.
- **Revisão independente:** revisão inicial bloqueou alvo de toque, distinção
  visual dos marcadores, combinação hoje/seleção e privacidade da captura.
  Correções foram aplicadas; revisão posterior confirmou captura válida e o
  bloqueio foi resolvido pela exceção R5 registrada na spec.
- **Correção posterior solicitada pelo usuário:** margens horizontais foram
  alinhadas ao padrão das demais superfícies e todas as superfícies de cartão
  receberam `ChampionShapeLarge`; células passaram a usar `ChampionShapeSmall`.
  Lint, builds, instalação e inspeção física foram repetidos após a correção.
- **Correção posterior adicional solicitada pelo usuário:** dimensões originais
  do calendário foram restauradas: margem externa `extraLarge`, conteúdo interno
  `small` e espaçamento `extraSmall` entre colunas. Células voltaram a ser
  superfícies únicas, sem slot expandido nem sobreposição visual. Artefatos
  auxiliares de status e marcador superior de hoje foram removidos; indicadores
  mantêm três slots consistentes e formas nominais por métrica.
- **Correção posterior de contraste solicitada pelo usuário:** pontos passaram a
  escolher variante clara ou escura de `primary`/`secondary`/`tertiary` conforme
  luminância real do fundo da célula e da legenda. Assim, containers claros no
  tema escuro não recebem acentos claros de baixo contraste. Lint, builds,
  instalação, abertura, controles, rolagem e captura foram repetidos após o
  ajuste.
