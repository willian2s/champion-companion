# 024-02 — Evoluir fundação visual

- **Ticker:** `024`
- **Número:** `02`
- **Status:** completed

## Objetivo e resultado esperado

Confirmar primitives e invariantes suficientes para iniciar o rollout visual,
preservando tema e consumidores atuais. Resultado: fundação mínima sem promover
componentes compartilhados antes de dois usos reais, conforme R2 e ADR-023.

## Requisitos e critérios cobertos

- R1, R2, R4 e R5.
- Critérios 3, 10, 11 e 12 da spec.
- Critério 8 fica `N/A` nesta etapa sem composição visual nova; será validado na
  superfície que renderizar cada composição real.

## Escopo incluído

- auditoria dos tokens, tema e componentes vigentes contra a matriz visual;
- preservação da escala de espaçamento, shapes, tipografia do sistema, dynamic
  color e paleta fallback existentes;
- mínimo acionável de `48 dp` como invariante de acessibilidade já atendido pela
  escala e pelos controles existentes;
- registro do gate de promoção: componente compartilhado exige uso real em duas
  superfícies; elemento estritamente pertencente ao shell pode usar a exceção
  explícita de R2.

## Escopo excluído

- mudança global especulativa de paleta, tipografia ou shapes;
- alteração de telas de feature;
- promoção antecipada de containers de página, cabeçalho, seção, resumo de
  métrica ou grupo de ações;
- dependência externa, fonte Google ou ativo Fitbit;
- novos testes automatizados.

## Dependências

- `024-01` concluída;
- ADR visual criada e Spec 024.

## Arquivos e símbolos prováveis

- `core/designsystem/ChampionTokens.kt`;
- `ChampionDisplayComponents.kt`;
- `ChampionFeedbackComponents.kt`;
- `ChampionActionAndStatusComponents.kt`;
- `ChampionStatusTokens.kt`;
- possível novo arquivo Compose no mesmo pacote;
- previews de `core/designsystem`.

## Passos de implementação

1. Auditar primitives e contratos existentes contra os padrões aprovados.
2. Manter APIs existentes e rejeitar tokens ou componentes sem consumidor real.
3. Reservar a exceção de R2 para elementos invariantes do shell implementados em
   `024-03`.
4. Manter candidatos visuais locais à primeira superfície; promover somente
   após segundo uso real equivalente.
5. Preservar formatação e regras de domínio fora do design system.
6. Revisar consumidores para garantir ausência de alteração implícita.
7. Registrar decisão, desvios e resultado dos comandos.

## Testes e comandos de validação

Não criar, alterar ou executar testes automatizados.

```bash
git diff --check
./gradlew lint assembleDebug assembleRelease
./gradlew installDebug
adb devices -l
adb shell am start -n br.com.mwconsulting.championcompanion.dev/br.com.mwconsulting.championcompanion.MainActivity
```

Smoke: confirmar telas antigas sem regressão após a remoção das APIs prematuras.
Validação visual de componentes ocorrerá nas superfícies que realmente os
renderizarem.

## Definição de pronto

- primitives e invariantes existentes auditadas e documentadas como suficientes;
- nenhuma API compartilhada nova permanece sem dois consumidores reais;
- consumidores antigos permanecem equivalentes;
- sem ativo, fonte ou dependência Google/Fitbit;
- lint, builds, instalação e smoke ADB registrados;
- overview atualizado.

## Riscos e cuidados

- mudança em token existente pode causar big-bang oculto;
- componente universal com muitas flags deve ser rejeitado;
- preview não conta como consumidor real;
- dynamic color e tokens semânticos fixos exigem contraste explícito;
- não executar suites existentes.

## Resolução do escalonamento — 2026-08-17

### Causa confirmada

- O commit `97c9757` promoveu `ChampionLayout`, `ChampionPage`,
  `ChampionPageHeader`, `ChampionSection`, `ChampionMetricSummary` e
  `ChampionActionGroup` em `core/designsystem`.
- Auditoria de referências encontrou somente definições e previews, sem uso real
  em feature ou shell. Preview e surface temporária não satisfazem o gate de duas
  superfícies de R2 e ADR-023.
- Slots arbitrários de cabeçalho e ações transferiam ao chamador a garantia de
  alvo mínimo de `48 dp`; portanto, a API não assegurava integralmente R5.
- O smoke ADB anterior abriu telas vigentes depois da remoção da surface
  temporária. Ele comprovou ausência de regressão ampla, mas não renderização da
  fundação que permaneceu no código.

### Decisão

Escolhida a opção 2: reduzir a fundação às primitives e invariantes já existentes
e adiar componentes. Não foi criada exceção na spec nem na ADR, pois isso
enfraqueceria deliberadamente o guard contra abstração prematura e contrariaria
a alternativa rejeitada de catálogo antes do piloto.

- removidos `ChampionLayout`, `ChampionPage`, `ChampionPageHeader`,
  `ChampionSection`, `ChampionMetricSummary`, `ChampionActionGroup` e previews
  exclusivos;
- removidas capturas que representavam exclusivamente o protótipo descartado,
  evitando tratá-las como evidência do estado final;
- preservados `ChampionSpacing`, `ChampionShapes`, `ChampionTypography`, tema,
  dynamic color, paleta fallback e componentes vigentes com consumidores reais;
- `ChampionSpacing.massive` continua representando `48 dp`, e
  `PrimaryActionButton` continua impondo altura mínima de `48 dp`;
- nenhum arquivo de feature, shell, contrato funcional, dependência ou estado foi
  alterado.

`024-02` permanece `completed`: resultado válido desta fundação é auditoria e
limite de promoção, não catálogo novo. `024-03` pode introduzir somente elemento
invariante do shell; primeira composição de superfície deve permanecer local e
só pode migrar para `core/designsystem` após segundo uso real equivalente.

### Arquivos da resolução

- `app/src/main/java/br/com/mwconsulting/championcompanion/core/designsystem/ChampionTokens.kt`;
- removidos `ChampionFoundationComponents.kt` e
  `ChampionFoundationPreviews.kt`;
- removidas três capturas `024-02-captura-*` vinculadas ao protótipo;
- `024-02-evoluir-fundacao-visual.md`;
- `024-00-overview.md`.

### Validações da resolução

| Comando | Resultado |
|---|---|
| Auditoria de referências das seis APIs removidas | Passou; nenhum consumidor real existia |
| `git diff --check` | Passou; sem erro |
| `./gradlew lint assembleDebug assembleRelease` | Passou; `BUILD SUCCESSFUL` |
| `./gradlew installDebug` | Passou; instalado no Galaxy S22 `SM-S901E` |
| `adb devices -l` e abertura da `MainActivity` | Passou; dispositivo autorizado e activity aberta |
| Smoke ADB das telas vigentes | Passou; Saúde, Início, rolagem, menu `Mais` e retorno responderam sem disparar ação funcional |
| Configuração física após smoke | `1080x2340`, density `480`, fonte `1.0`, portrait e tema escuro; nenhuma configuração foi alterada |

Dump `uiautomator` foi temporário e removido do aparelho. Smoke confirma ausência
de regressão nas telas vigentes; não constitui renderização de componente novo.
Nenhum teste automatizado foi criado, alterado ou executado, conforme R8.

### Riscos e bloqueios restantes

- linguagem visual compartilhada ainda não possui dois usos reais; promoção de
  componentes continua bloqueada até essa evidência existir;
- aceite visual integrado continua pertencendo ao hub Saúde em `024-04`;
- `024-03` não pode usar a exceção de shell para componentes internos de página;
- dynamic color, fonte `1,3x`, landscape e temas devem ser validados na superfície
  que renderizar cada composição real.
