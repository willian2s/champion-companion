# 024-03 — Refatorar shell de navegação

- **Ticker:** `024`
- **Número:** `03`
- **Status:** pending

## Objetivo e resultado esperado

Aplicar nova apresentação ao shell, insets e barra inferior sem alterar a
máquina de navegação. Resultado: base visual coerente para telas migradas e
antigas, com comportamento atual preservado.

## Requisitos e critérios cobertos

- R2, R3, R5, R6 e R7.
- Critérios 4, 8, 9, 10 e 11 da spec.

## Escopo incluído

- `Scaffold`, fundo, insets, barra inferior, indicadores e rótulos;
- menu `Mais` e apresentação das transições;
- semântica e alvos de toque do shell;
- smoke de todas as rotas e retornos.

## Escopo excluído

- `NavController`, nova IA, novas rotas ou remoção de destinos;
- mudança de guards, saver, callbacks, ViewModels ou política phone;
- redesign do conteúdo interno das telas.

## Dependências

- `024-02` concluída.

## Arquivos e símbolos prováveis

- `core/navigation/ChampionNavigation.kt`;
- componentes de shell adicionados em `core/designsystem`;
- `AppDestination.kt` apenas como referência, sem edição prevista.

## Passos de implementação

1. Isolar apresentação do shell sem mover ownership do back stack.
2. Aplicar primitives de fundo, insets e navegação.
3. Preservar cinco entradas visíveis, menu `Mais` e seleção das subárvores.
4. Preservar animação, back contextual, restauração e confirmações de saída.
5. Validar destinos top-level e detalhes acessíveis.
6. Revisar diff para excluir mudança funcional.
7. Registrar evidências e atualizar overview.

## Testes e comandos de validação

Não executar testes automatizados.

```bash
git diff --check
./gradlew lint assembleDebug assembleRelease
./gradlew installDebug
adb devices -l
adb shell am start -n br.com.mwconsulting.championcompanion.dev/br.com.mwconsulting.championcompanion.MainActivity
```

Smoke ADB: Início, Saúde, Calendário, Exercícios, menu `Mais`, Dispositivo,
Perfil, Configurações, detalhe de Saúde, voltar e restauração após recriação.

## Definição de pronto

- shell visual atualizado;
- destinos, seleção, menu, back stack, saver e guards equivalentes;
- barra funciona com fonte `1,3x` e landscape;
- capturas sanitizadas portrait/landscape registradas;
- comandos aprovados e overview atualizado.

## Riscos e cuidados

- `ChampionNavigation.kt` é hotspot funcional; limitar diff à apresentação;
- não esconder barra inferior em detalhes nesta entrega;
- português longo pode comprimir rótulos;
- não tocar caminhos bloqueados de treino phone.
