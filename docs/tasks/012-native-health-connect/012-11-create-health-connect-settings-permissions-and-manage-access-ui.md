# Task 012-11 - Criar Settings de Health Connect, permissões e manage access

## Status

Planejada.

## Objetivo

Substituir o placeholder de Configurações por uma superfície funcional de Health Connect, incluindo status, toggle, permissões, `Sincronizar agora`, provider e `Gerenciar acesso`.

## Contexto

`SettingsScreen` hoje é placeholder. A Fase 12 exige um ponto explícito de controle para disponibilidade, preferência, permissões por capacidade e gerenciamento de acesso do Health Connect.

## Documentos obrigatórios

- `AGENTS.md`
- `docs/specs/012-native-health-connect.md`
- `docs/tasks/012-native-health-connect/012-00-overview.md`
- `docs/tasks/012-native-health-connect/012-08-implement-health-connect-sync-coordinator-partial-results-and-retry.md`
- documentação oficial Android para permissões e manage access do Health Connect

## Dependências

- `012-08`.

## Entradas

- infraestrutura Android de disponibilidade e intents;
- coordinator pronto;
- preferência `healthSyncEnabled`.

## Arquivos permitidos

- `docs/specs/012-native-health-connect.md`
- `docs/tasks/012-native-health-connect/012-00-overview.md`
- `docs/tasks/012-native-health-connect/012-11-create-health-connect-settings-permissions-and-manage-access-ui.md`
- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/settings/SettingsScreen.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/settings/SettingsFeatureContract.kt`
- novos arquivos Kotlin estritamente necessários para estado e intents de Settings
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/navigation/ChampionNavigation.kt`, se necessário

## Arquivos proibidos

- `app/src/test/**`
- `app/src/androidTest/**`
- `ChampionDatabase.kt`
- `ChampionDaos.kt`
- `RoomRepositories.kt`
- `docs/decisions/**`

## Escopo

- mostrar disponibilidade;
- mostrar integração ativada ou pausada;
- mostrar permissões concedidas e faltantes por grupo amigável;
- mostrar provider;
- ação `Conectar`;
- ação `Pausar sincronização`;
- ação `Sincronizar agora`;
- ação `Gerenciar acesso`;
- mostrar pendentes, falhas e última sincronização.

## Fora do escopo

- mostrar valores individuais de saúde;
- leitura do Health Connect;
- alterar fluxo de Home/Saúde/Exercícios além do necessário para navegação.

## Passos

1. Criar estado de UI e intents de Configurações.
2. Substituir placeholder por seção funcional de Health Connect.
3. Integrar toggle, connect, sync now e manage access.
4. Exibir permissões por grupo amigável.
5. Atualizar spec e overview.

## Critérios de aceite

- `Settings` deixa de ser placeholder;
- permissões aparecem por capacidade amigável;
- toggle respeita `healthSyncEnabled`;
- `Sincronizar agora` e `Gerenciar acesso` ficam acessíveis;
- peso e altura não disparam permissão sem dado local elegível.

## Validação manual

- inspeção visual da tela;
- revisão das ações e estados apresentados.

## Comandos de build

```bash
./gradlew lint assembleDebug assembleRelease
./gradlew installDebug
```

## Evidências

- diff da tela e contrato de Settings;
- lista das ações visíveis;
- screenshot da tela final.

## Screenshot quando visual

- screenshot obrigatório.

## Atualização do overview

- marcar `012-11` como concluída;
- registrar que `Settings` deixou de ser placeholder.

## Validação final

- Não criar, alterar ou executar testes automatizados.
- Task executável em sessão nova, sem depender de contexto anterior.
