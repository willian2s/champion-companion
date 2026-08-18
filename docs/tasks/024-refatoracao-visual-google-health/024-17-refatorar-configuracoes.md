# 024-17 — Refatorar Configurações

- **Ticker:** `024`
- **Número:** `17`
- **Status:** pending

## Objetivo e resultado esperado

Refatorar Configurações para apresentar integração Health Connect, permissões e
fila local com hierarquia e controles claros. Resultado: status compreensível
sem sugerir leitura remota ou alterar sync write-only.

## Requisitos e critérios cobertos

- R4–R9; critérios 5, 7–12 da spec.

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
