# 024-02 — Evoluir fundação visual

- **Ticker:** `024`
- **Número:** `02`
- **Status:** completed

## Objetivo e resultado esperado

Adicionar primitives e componentes opt-in necessários à nova linguagem visual,
preservando tema e consumidores atuais. Resultado: fundação reutilizável sem
redesign implícito das telas ainda não migradas.

## Requisitos e critérios cobertos

- R1, R2, R4 e R5.
- Critérios 3, 8, 10, 11 e 12 da spec.

## Escopo incluído

- papéis visuais de página, header, seção, métrica e ação comprovados no
  baseline;
- componentes stateless com slots, textos e callbacks;
- previews existentes ou novos quando úteis, sem testes;
- iconografia Material genérica em lugar de glyphs textuais quando tocados;
- acessibilidade, mínimo de toque e adaptação.

## Escopo excluído

- mudança global especulativa de paleta, tipografia ou shapes;
- alteração de telas de feature;
- dependência externa, fonte Google ou ativo Fitbit;
- novos testes automatizados.

## Dependências

- `024-01` concluída;
- ADR visual criada e Spec 002.

## Arquivos e símbolos prováveis

- `core/designsystem/ChampionTokens.kt`;
- `ChampionDisplayComponents.kt`;
- `ChampionFeedbackComponents.kt`;
- `ChampionActionAndStatusComponents.kt`;
- `ChampionStatusTokens.kt`;
- possível novo arquivo Compose no mesmo pacote;
- previews de `core/designsystem`.

## Passos de implementação

1. Traduzir somente padrões aprovados na matriz para APIs Compose pequenas.
2. Manter APIs existentes ou adaptar de forma compatível.
3. Criar container de página/header/seção apenas onde a invariância estiver
   comprovada.
4. Preservar formatação e regras de domínio fora do design system.
5. Conferir contraste, semântica, toque mínimo e fonte ampliada em preview/app.
6. Revisar consumidores para garantir ausência de alteração implícita.
7. Registrar decisões, capturas e resultado dos comandos.

## Testes e comandos de validação

Não criar, alterar ou executar testes automatizados.

```bash
git diff --check
./gradlew lint assembleDebug assembleRelease
./gradlew installDebug
adb devices -l
adb shell am start -n br.com.mwconsulting.championcompanion.dev/br.com.mwconsulting.championcompanion.MainActivity
```

Smoke: componentes em claro/escuro, dynamic/fallback representativo, fonte
`1,3x` e landscape; confirmar telas antigas sem regressão visual ampla.

## Definição de pronto

- primitives aditivas implementadas e documentadas;
- nenhum componente depende de estado ou infraestrutura funcional;
- consumidores antigos permanecem equivalentes;
- sem ativo, fonte ou dependência Google/Fitbit;
- lint, builds, instalação e smoke ADB registrados;
- overview atualizado.

## Riscos e cuidados

- mudança em token existente pode causar big-bang oculto;
- componente universal com muitas flags deve ser rejeitado;
- dynamic color e tokens semânticos fixos exigem contraste explícito;
- não executar suites existentes.

## Implementação realizada

- tokens opt-in `ChampionLayout` adicionados para padding de página, ritmo de
  seção/conteúdo e alvo acionável mínimo de `48 dp`, reutilizando a escala
  existente sem alterar seus valores;
- componentes stateless e baseados em slots adicionados para página, cabeçalho,
  seção, resumo de métrica e grupo de ações;
- títulos de página, seção e métrica marcados semanticamente como heading;
- metadados permanecem slots do chamador e valores/unidades continuam
  pré-formatados fora do design system;
- previews adicionados para temas claro/escuro, fonte `1,3x`, landscape e
  conteúdo longo, usando somente dados genéricos;
- consumidores, tema global, APIs existentes e glyphs de status permaneceram
  inalterados.

## Evidências desta execução

- `git diff --check`: passou, sem saída;
- `./gradlew :app:compileDebugKotlin`: passou (`BUILD SUCCESSFUL`);
- `./gradlew lint assembleDebug assembleRelease`: passou (`BUILD SUCCESSFUL`);
- `./gradlew installDebug`: passou; APK instalado no Galaxy S22 `SM-S901E`;
- testes automatizados: não criados, alterados ou executados;
- smoke ADB sanitizado: passou em portrait escuro, rolagem, retorno, landscape,
  fonte `1,3x` e landscape claro; configuração original restaurada;
- configuração final confirmada: `1080x2340`, density `480`, fonte `1.0`,
  portrait e tema escuro;
- status atualizado para `completed` após validação física e revisão documental.

## Execução final — arquivos, decisões e evidências

### Arquivos alterados

- `app/src/main/java/br/com/mwconsulting/championcompanion/core/designsystem/ChampionTokens.kt`;
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/designsystem/ChampionFoundationComponents.kt`;
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/designsystem/ChampionFoundationPreviews.kt`;
- `docs/tasks/024-refatoracao-visual-google-health/024-02-captura-sanitizada-fundacao.svg`;
- `docs/tasks/024-refatoracao-visual-google-health/024-02-captura-fundacao-dark-portrait.png`;
- `docs/tasks/024-refatoracao-visual-google-health/024-02-captura-fundacao-light-landscape-1-3.png`;
- `docs/tasks/024-refatoracao-visual-google-health/024-02-evoluir-fundacao-visual.md`;
- `docs/tasks/024-refatoracao-visual-google-health/024-00-overview.md`.

Nenhuma feature, tema global, consumidor existente, dependência, contrato
funcional, persistência, BLE ou Health Connect foi alterado.

### Decisões e desvios

- `ChampionLayout` adiciona tokens opt-in para ritmo de página, seção, conteúdo e
  alvo acionável mínimo de `48 dp`, sem alterar valores existentes.
- `ChampionPage`, `ChampionPageHeader`, `ChampionSection`,
  `ChampionMetricSummary` e `ChampionActionGroup` são stateless e baseados em
  conteúdo, metadata e callbacks fornecidos pelo chamador.
- Títulos de página, seção e resumo de métrica recebem semântica `heading`; os
  valores e unidades continuam formatados fora do design system.
- Header aceita conteúdo inicial e ações; grupo de ações mantém ação primária
  separada de slots secundários.
- Previews cobrem claro, escuro, fallback sem dynamic color, fonte `1,3x`,
  landscape e conteúdo longo com dados genéricos.
- Glyphs de status existentes não foram alterados para evitar redesign implícito
  em consumidores ainda não migrados; iconografia Material fica para adoção
  opt-in nas superfícies migradas.
- A captura SVG é reconstrução estrutural sanitizada; os dois PNGs são capturas
  reais da surface temporária de validação, sem dados pessoais, recortadas para
  remover barras do sistema e depois preservadas como evidência.

### Comandos executados e resultados

| Comando | Resultado |
|---|---|
| `git diff --check` | Passou; sem erro |
| `./gradlew :app:compileDebugKotlin` | Passou; `BUILD SUCCESSFUL` |
| Primeira `./gradlew lint assembleDebug assembleRelease` | Interrompida pelo limite de 120 s, sem erro reportado; repetida com limite de 300 s |
| `./gradlew lint assembleDebug assembleRelease` | Passou; `BUILD SUCCESSFUL` |
| `./gradlew installDebug` | Passou; instalado em 1 dispositivo |
| `./gradlew :app:assembleDebug :app:installDebug` (surface temporária) | Passou; captura física dos componentes executada; surface e wiring removidos depois |
| `adb devices -l` | Passou; Galaxy S22 `SM-S901E` autorizado |
| `adb shell am start -n br.com.mwconsulting.championcompanion.dev/br.com.mwconsulting.championcompanion.MainActivity` | Passou; MainActivity aberta |
| Smoke portrait escuro | Passou; abertura, rolagem e retorno; hierarquia sanitizada 77 nós/8 clicáveis/1 rolável |
| Smoke landscape com fonte `1,3x` | Passou; hierarquia sanitizada 69 nós/8 clicáveis/1 rolável |
| Smoke landscape claro | Passou; hierarquia sanitizada 69 nós/8 clicáveis/1 rolável |
| `adb exec-out screencap -p` | Passou; PNGs reais sanitizados para portrait escuro e landscape claro com fonte `1,3x` |
| Restauração de configuração | Passou; fonte `1.0`, rotação portrait, auto-rotação `0`, tema escuro |

Não foram executados testes automatizados, conforme R8 e a definição explícita
da subtarefa.

### Evidências

- `024-02-captura-sanitizada-fundacao.svg` registra composição genérica sem
  métricas, identificadores ou ativos proprietários.
- `024-02-captura-fundacao-dark-portrait.png` registra renderização real em tema
  escuro/fallback, portrait e fonte `1.0`.
- `024-02-captura-fundacao-light-landscape-1-3.png` registra renderização real
  em tema claro/fallback, landscape e fonte `1,3x`; conteúdo inferior permanece
  acessível por rolagem.
- Previews registram cenários claro/escuro, fallback, fonte ampliada, landscape
  e conteúdo longo.
- Dumps `uiautomator` foram temporários, usados somente para contagens genéricas
  e removidos do aparelho. Surface e wiring temporários de captura não ficaram
  no código final.

### Riscos residuais

- Componentes permanecem opt-in e sem consumidor de feature até `024-03`/`024-04`;
  aceite visual integrado da linguagem ainda pertence ao piloto Saúde.
- A captura física usa fallback para manter contraste determinístico; dynamic
  color continua preservado no tema e requer validação contextual durante adoção
  pelas superfícies migradas.
- Ícones de status legados permanecem até migração explícita, evitando mudança
  implícita nas superfícies não migradas.
