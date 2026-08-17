# 024-02 — Evoluir fundação visual

- **Ticker:** `024`
- **Número:** `02`
- **Status:** pending

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
