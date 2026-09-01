# 029-03 — Evoluir fundação visual e shell

- **Ticker:** `029`
- **Número:** `03`
- **Status:** `pending`
- **Depende de:** `029-02`

## Objetivo e resultado esperado

Adaptar tokens e componentes visuais compartilhados para suportar a composição
observada no baseline, aplicando shell consistente sem alterar destinos,
back stack ou contratos de navegação.

## Requisitos e critérios de aceite cobertos

- R4 e R7 da Spec 029.
- Critérios 4, 9 e 11.
- Superfícies, espaçamentos, formas, hierarquia tipográfica, estados e ações
  comuns ficam reutilizáveis e opt-in.

## Escopo incluído

- tokens de espaçamento, formas, superfícies, elevação, estados e componentes
  editoriais necessários à comparação;
- shell e navegação visual comum;
- acessibilidade semântica, contraste, fonte ampliada e tema claro/escuro;
- migração mínima das telas para demonstrar fundação semântica.

## Escopo excluído

- nova rota, novo destino, NavController ou substituição de Navigation 3;
- mudança de domínio, persistência, BLE, Health Connect ou backup;
- gráficos específicos, abas de exercícios ou filtros de workflow;
- cópia de fonte, asset, marca ou texto da referência.

## Dependências

- decisão e matriz de 029-02;
- `core/designsystem` atual;
- `ChampionNavigation.kt`, `AppDestination.kt` e save tokens.

## Arquivos e símbolos prováveis

- `app/src/main/java/br/com/mwconsulting/championcompanion/core/designsystem/`;
- `core/navigation/ChampionNavigation.kt`;
- `core/navigation/AppDestination.kt`;
- componentes `HealthCard`, estados de loading/empty/error e tema Champion.

## Passos de implementação

1. Mapear divergências comuns no baseline e evitar tokens por tela sem motivo.
2. Criar ou ajustar componentes stateless e opt-in.
3. Aplicar shell visual preservando destinations, saver, back stack e callbacks.
4. Verificar semântica, foco, touch target, contraste e fonte ampliada.
5. Validar uma tela piloto e registrar screenshot Champion sanitizado.

## Testes e comandos de validação

```bash
git diff --check
./gradlew lint assembleDebug assembleRelease
```

No aparelho, executar smoke sanitizado do shell em portrait, landscape, tema
claro/escuro e fonte ampliada. Não criar testes automatizados.

## Definição de pronto

- fundação suporta padrões observados sem duplicação desnecessária;
- navegação funcional e contratos permanecem intactos;
- piloto passa acessibilidade e estados básicos;
- build, lint e smoke físico passam nos estados alcançados;
- divergências impossíveis de reproduzir estão documentadas.

## Riscos e cuidados

- Mudança global de token pode alterar telas não migradas; usar componentes
  opt-in e rollout gradual.
- Não transformar shell Champion em cópia de navegação Google.
- Não remover legado sem consumidor comprovadamente ausente.
