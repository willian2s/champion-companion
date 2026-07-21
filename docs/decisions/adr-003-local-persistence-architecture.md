# ADR-003 — Adotar Room e DataStore com contratos de domínio separados

## Status

Aceita

## Contexto

ADR-001 definiu uso de Room. Implementação atual também já usa DataStore para
preferências simples e contratos de repositório em `core/model`.

Código atual materializa esta divisão:

- `core/model` contém modelos e interfaces de repositório;
- `data/RoomRepositories.kt` implementa contratos com mapeadores explícitos;
- `core/database/DatabaseModule.kt` cria `ChampionDatabase` com Room 3 e
  `AndroidSQLiteDriver()`;
- `core/preferences/PreferencesModule.kt` cria DataStore `champion_preferences`.

Aplicativo novo não herda base local do MotionLog e não precisa ler schema legado.

## Decisão

Persistência local seguirá estes limites:

- Room será fonte operacional para agregados de domínio, filas de sync e payloads diagnósticos;
- DataStore será usado apenas para `AppPreferences` e preferências simples;
- modelos de domínio, entidades Room, DAOs e formatos externos permanecerão separados;
- conversão entre domínio e persistência ocorrerá por mapeadores explícitos;
- app não lerá `motionlog.db`, não importará dados locais legados e não manterá compatibilidade de schema com aplicativo anterior.

Schemas Room exportados permanecem versionados em
`app/schemas/br.com.mwconsulting.championcompanion.core.database.ChampionDatabase/`.

## Consequências

### Positivas

- domínio fica desacoplado de Android, Room e detalhes de schema;
- preferências simples não competem com banco relacional;
- evolução futura de sync, BLE e Health Connect pode reutilizar contratos estáveis;
- banco novo evita custo e risco de migração de app legado.

### Negativas

- mais código de mapeamento e testes entre camadas;
- mudanças de schema exigem atualizar export de schema e testes relacionados;
- inexistência de importação local exige reconstrução de dados ao migrar de app legado.

## Alternativas consideradas

- usar apenas Room para tudo, incluindo preferências simples;
- usar apenas DataStore para estados persistentes;
- expor entidades/DAOs diretamente para camadas superiores;
- reutilizar banco local do MotionLog.
