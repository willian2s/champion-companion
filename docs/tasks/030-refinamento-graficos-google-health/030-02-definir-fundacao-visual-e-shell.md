# 030-02 — Definir fundação visual e shell

- **Ticker:** `030`
- **Número:** `02`
- **Status:** `pending`
- **Depends on:** `030-01`

## Objetivo e resultado esperado

Transformar benchmark integral aprovado em linguagem visual compartilhada para
shell, cabeçalhos, cards, listas, estados, controles, diálogos e gráficos,
preservando destinos, callbacks e limites dos dados reais. A referência
Google Health/Fitbit orienta apresentação; não cria funcionalidades.

## Requisitos e critérios cobertos

- R1–R3 e R6; critérios 2, 3, 4, 6 e 8 da Spec 030.

## Escopo incluído

- componentes stateless em `core/designsystem`;
- navegação inferior, menu Mais, cabeçalhos e transições visuais;
- cards, listas, botões, chips, banners, feedback e diálogos;
- estados de loading, vazio, erro, indisponível, pendência e sucesso;
- composição de gráficos, períodos `D`/`S`/`M`/`3M`/`A`, unidade, marker,
  tooltip, eixos, gaps, scroll, temas e fonte ampliada;
- política opt-in de adoção pelas features.

## Escopo excluído

- novos destinos, callbacks, queries ou estados de domínio;
- Room, DataStore, BLE, Health Connect, ViewModels e read models;
- migração visual específica de Home, Saúde, Calendário ou outras features;
- troca de engine, novo Canvas de série ou paridade funcional com Google Health.

## Dependências

- `030-01` deve concluir inventário e benchmark integral.

## Arquivos e símbolos prováveis

- `core/designsystem/*`;
- `core/designsystem/chart/*`;
- `core/navigation/ChampionNavigation.kt` somente para apresentação;
- ADR-023 e ADR-027 como decisões de referência.

## Passos de implementação

1. Modelar tokens, slots e estados neutros necessários à apresentação.
2. Padronizar hierarquia, espaçamento, formas, cores e descrições acessíveis.
3. Encapsular renderer e interação de gráficos sem expor Vico às features.
4. Aplicar mudanças de shell de modo opt-in, preservando back stack e callbacks.
5. Validar fonte ampliada, orientações, temas, rolagem e estados sem dados.

## Testes e comandos de validação

```bash
git diff --check
./gradlew lint assembleDebug assembleRelease
./gradlew installDebug
adb devices -l
adb shell am start -n br.com.mwconsulting.championcompanion.dev/br.com.mwconsulting.championcompanion.MainActivity
```

Não criar, alterar ou executar testes automatizados. Registrar evidência visual
sanitizada e declarar `N/A` para cenários indisponíveis.

## Definição de pronto

- API visual é neutra, stateless e sem Vico nas features;
- shell e primitives compartilham linguagem coerente;
- períodos sem fonte não são expostos com ação;
- acessibilidade, temas, orientação e scroll foram validados;
- lint/build, smoke e decisões estão registrados.

## Riscos e cuidados

- evitar abstração prematura e mudança global involuntária;
- não transformar estado visual em novo contrato funcional;
- preservar `HealthChart` enquanto previews/testes exigirem sua API;
- não usar cor como único sinal de estado.
