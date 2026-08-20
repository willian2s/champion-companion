# 024-17 — Refatorar Configurações

- **Ticker:** `024`
- **Número:** `17`
- **Status:** completed

## Objetivo e resultado esperado

Refatorar Configurações para apresentar integração Health Connect, permissões e
fila local com hierarquia e controles claros. Resultado: status compreensível
sem sugerir leitura remota ou alterar sync write-only.

## Requisitos e critérios cobertos

- R4–R9; critérios 5, 7–12 da spec.
- Regra cromática comum: quando presentes, header azul-petróleo nominal do
  Champion com conteúdo branco; cards, indicadores e progresso com acentos
  nominais `primary`/`secondary`/`tertiary` da paleta clara/escura conforme o
  tema, seguindo o piloto `024-05`, sem alterar o tema raiz.

## Escopo incluído

- header, disponibilidade, toggle, conexão/permissões;
- sincronizar agora, gerenciar acesso e provider update;
- grupos de permissão, fila, status e feedback;
- retorno de intents externas e estados indisponíveis.

## Escopo excluído

- gateway, coordinator, permissões solicitadas ou fila persistida;
- leitura Health Connect, sync automático ou nova preferência.

## Dependências

- `024-16` concluída;
- provider real disponível ou estados `N/A` documentados.

## Arquivos e símbolos prováveis

- `feature/settings/SettingsScreen.kt`;
- `SettingsScreen`, `SettingsHeaderCard`, `HealthConnectStatusCard`,
  `PermissionGroupsCard` e `SyncQueueCard`.

## Passos de implementação

1. Priorizar estado da integração e controle principal.
2. Reorganizar permissões e fila como detalhes operacionais.
3. Evitar linha horizontal comprimida para três ações em fonte ampliada.
4. Preservar toggle, permission launcher, provider update e manage access.
5. Manter texto claro de write-only e significado de sincronizado.
6. Validar retorno das intents externas sem mudar estado indevidamente.
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

Smoke: abrir Configurações, rolar, gerenciar acesso/voltar quando seguro; não
sincronizar nem mudar permissões apenas para evidência; fonte/landscape/temas.

## Definição de pronto

- integração, permissões e fila têm hierarquia clara;
- ações cabem em fonte ampliada e landscape;
- contratos write-only e foreground permanecem explícitos;
- intents externas retornam com segurança;
- comandos, capturas sanitizadas e overview registrados.

## Riscos e cuidados

- `Sincronizar agora` tem efeito remoto;
- não revogar/conceder permissões para screenshot;
- provider package e fila podem expor contexto técnico;
- não afirmar confirmação remota inexistente.

## Registro de execução

### Status

`completed`; Configurações reorganizada com header editorial, estado da
integração em primeiro plano, ações empilhadas, permissões operacionais e fila
local em detalhes posteriores. Contratos Health Connect write-only, foreground e
local-first permaneceram inalterados.

### Arquivos alterados

- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/settings/SettingsScreen.kt`;
- `docs/tasks/024-refatoracao-visual-google-health/024-17-captura-real-sanitizada-configuracoes.png`;
- `docs/tasks/024-refatoracao-visual-google-health/024-17-refatorar-configuracoes.md`;
- `docs/tasks/024-refatoracao-visual-google-health/024-00-overview.md`.

### Decisões e desvios

- Header passou a usar azul-petróleo nominal do Champion com conteúdo branco e
  título `Configurações` semanticamente marcado.
- Integração, disponibilidade, provider, toggle e sincronização foreground foram
  agrupados antes de permissões e fila.
- `Conectar`, `Sincronizar agora` e `Gerenciar acesso` deixaram a linha horizontal
  comprimida e passaram a ocupar largura própria, preservando callbacks e estados.
- Permissões passaram a usar status textual com sinal icônico, separação vertical
  e acento secundário/terciário; nenhum grupo foi removido do contrato visual.
- Fila passou a destacar contadores por intenção cromática e mantém explícito que
  `Sincronizado` significa escrita local aceita, sem leitura do provider.
- Feedback usa container terciário e composição vertical para acomodar mensagens
  longas em fonte ampliada.
- Nenhuma alteração em `SettingsViewModel`, gateway, coordinator, permissões,
  fila persistida, DataStore, Room, navegação ou tema raiz.

### Comandos executados e resultados

| Comando/verificação | Resultado |
|---|---|
| `git diff --check` | Passou antes e após ajuste final |
| `./gradlew lint assembleDebug assembleRelease` | Passou; `BUILD SUCCESSFUL` |
| `./gradlew installDebug` | Passou; instalado no Galaxy S22 `SM-S901E` |
| `adb devices -l` | Passou; dispositivo autorizado |
| `adb shell am start -n br.com.mwconsulting.championcompanion.dev/br.com.mwconsulting.championcompanion.MainActivity` | Passou |
| Smoke ADB Configurações | Passou; abertura por `Mais`, header, integração, toggle, ações, permissões e fila observados |
| Smoke ADB rolagem | Passou; grupos de permissão e fila local acessíveis após rolagem |
| Smoke ADB `Gerenciar acesso` e retorno seguro | Passou sem mutação; provider externo não abriu nesta configuração, retorno real ficou `N/A` |
| Smoke ADB fonte `1,3x` | Passou; ações e textos essenciais permaneceram acessíveis |
| Smoke ADB landscape | Passou; superfície rolável e header/integração sem sobreposição |
| Smoke ADB temas claro/escuro | Passou; aparelho restaurado para tema escuro |
| Restauração do aparelho | Passou; `font_scale=1.0`, portrait, `user_rotation=0`, tema escuro |
| Captura sanitizada | Passou; header e descrição genérica registrados em `024-17-captura-real-sanitizada-configuracoes.png` |

Não foram criados, alterados ou executados testes automatizados.

### Evidências

- Header nominal, título único e descrição write-only aparecem antes dos
  controles operacionais.
- Ações têm largura completa e alvos observados acima de `48 dp`, sem a linha
  horizontal anterior de três ações.
- UI Automator expôs headings, descrição do switch, status de permissões,
  contadores da fila e texto sem confirmação remota.
- Captura sanitizada contém somente composição editorial genérica, sem métricas
  pessoais ou timestamps.

### Riscos residuais

- Provider update, indisponibilidade, loading, erro e feedback transitório não
  foram fabricados; permanecem `N/A` nesta validação física.
- `Gerenciar acesso` não abriu tela externa no aparelho disponível; callback foi
  exercitado sem alteração de estado e `ActivityNotFoundException` permaneceu
  tratado com segurança.
- `Sincronizar agora` não foi acionado por ter efeito remoto; estados de sucesso,
  falha parcial e retry permanecem `N/A`.
- Revisão independente inicial aprovou o código e apontou somente documentação
  pendente; filtro de grupos foi removido antes da validação final para preservar
  todos os grupos fornecidos pelo contrato.
