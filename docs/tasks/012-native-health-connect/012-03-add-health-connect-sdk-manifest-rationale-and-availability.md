# Task 012-03 - Adicionar SDK, manifesto, rationale e disponibilidade do Health Connect

## Status

Planejada.

## Objetivo

Integrar a base Android do Health Connect na aplicação, incluindo SDK estável, disponibilidade, permissões de escrita, provider, queries e rationale, sem tocar persistência funcional de sync.

## Contexto

A fase precisa preparar a fronteira Android do Health Connect antes de mapeadores, coordinator e UI funcional de permissões.

## Documentos obrigatórios

- `AGENTS.md`
- `docs/specs/012-native-health-connect.md`
- `docs/tasks/012-native-health-connect/012-00-overview.md`
- `docs/tasks/012-native-health-connect/012-02-formalize-health-connect-sync-policy-adr-and-roadmap.md`
- documentação oficial atual do Android para disponibilidade, permissões, rationale e `HealthConnectClient`

## Dependências

- `012-02`.

## Entradas

- ADR da fase;
- roadmap atualizado;
- estado real de `AndroidManifest.xml`, `app/build.gradle.kts` e `gradle/libs.versions.toml`.

## Arquivos permitidos

- `docs/specs/012-native-health-connect.md`
- `docs/tasks/012-native-health-connect/012-00-overview.md`
- `docs/tasks/012-native-health-connect/012-03-add-health-connect-sdk-manifest-rationale-and-availability.md`
- `app/build.gradle.kts`
- `gradle/libs.versions.toml`
- `app/src/main/AndroidManifest.xml`
- arquivos Kotlin novos ou existentes estritamente de infraestrutura Android/Hilt/rationale/disponibilidade

## Arquivos proibidos

- `app/src/test/**`
- `app/src/androidTest/**`
- `core/database/**`
- `data/RoomRepositories.kt`
- `docs/decisions/**`
- `docs/roadmap/**`

## Escopo

- adicionar `androidx.health.connect:connect-client:1.1.0` ao version catalog;
- adicionar dependência ao módulo `:app`;
- modelar disponibilidade `Available`, `ProviderUpdateRequired`, `Unavailable`;
- adicionar permissões write-only necessárias;
- adicionar queries do provider para Android 13 ou inferior;
- adicionar Activity de rationale;
- adicionar alias ou configuração compatível quando exigido pela plataforma;
- fornecer bindings Hilt necessários para disponibilidade e intents do provider.

## Fora do escopo

- ler dados do Health Connect;
- pedir permissões de leitura;
- integrar fila persistida;
- sincronizar dados locais;
- alterar regras de backup.

## Passos

1. Integrar SDK estável ao catálogo e ao módulo.
2. Atualizar manifesto para provider, queries e permissões de escrita.
3. Criar infraestrutura Android de disponibilidade e intents de provider.
4. Criar rationale Activity e bindings Hilt.
5. Atualizar spec e overview com os detalhes reais introduzidos.

## Critérios de aceite

- SDK `1.1.0` integrado;
- manifesto pronto para Health Connect sem alterar política de backup;
- disponibilidade modelada;
- rationale pronta para uso posterior;
- nenhuma escrita real no provider é executada nesta task.

## Validação manual

- revisão de manifesto, dependências e wiring Hilt;
- inspeção manual da rationale e intents.

## Comandos de build

```bash
./gradlew lint assembleDebug assembleRelease
./gradlew installDebug
```

## Evidências

- diff de `libs.versions.toml`;
- diff de `app/build.gradle.kts`;
- diff de `AndroidManifest.xml`;
- lista de novos arquivos Android/Hilt/rationale.

## Screenshot quando visual

- screenshot da rationale, se a tela já ficar acessível nesta task.

## Atualização do overview

- marcar `012-03` como concluída;
- registrar readiness Android do Health Connect.

## Validação final

- Não criar, alterar ou executar testes automatizados.
- Task executável em sessão nova, sem depender de contexto anterior.
