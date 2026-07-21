# Task 004-04 — Criar Catálogo UUID Champion e Filtro De Dispositivo

## Status

Concluida.

Resumo: criados `ChampionUuidCatalog` e `ChampionDeviceFilter` para centralizar UUIDs confirmados do Champion C001, explicitar canais obrigatorios/opcionais e filtrar dispositivos por prefixo `champion` em `name` ou `localName` com normalizacao segura. FEA2, FEC8 e F002 write seguem nao obrigatorios.

## Objetivo

Centralizar UUIDs do Champion C001 e implementar filtro de scan seguro por nome/localName.

## Contexto

UUIDs e regras de filtro não podem ficar duplicados entre scanner, client, manager e testes. FEA2/FEC8/F002 write não são obrigatórios nesta fase.

## Documentos Obrigatórios

- `docs/specs/004-native-ble-core.md`
- `docs/protocol/champion-c001.md`
- `docs/protocol/ble-command-mapping.md`
- `docs/protocol/ble-transport.md`

## Dependências

- `004-02` concluída.

## Entradas

- catálogo confirmado na documentação de protocolo.

## Arquivos Exatos Permitidos

- `app/src/main/java/br/com/mwconsulting/championcompanion/core/bluetooth/ChampionUuidCatalog.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/bluetooth/ChampionDeviceFilter.kt`
- este arquivo

## Arquivos Que Não Podem Ser Modificados

- `feature/**`
- `data/**`
- `core/model/**`

## Escopo

- catálogo central de services, characteristics e CCCD;
- aliases tipados de canal;
- filtro por `name` e `localName` com prefixo `champion`.

## Fora Do Escopo

- scan concreto;
- discovery concreto;
- validação física de services no relógio.

## Regras De Segurança

- não tornar FEA2, FEC8 ou F002 write obrigatórios;
- filtro não pode aceitar substring arbitrária;
- tratar nomes ausentes sem exceção.

## Passos De Implementação

1. Criar catálogo UUID único com aliases legíveis e separação obrigatório/opcional.
2. Criar filtro Champion que avalie `name` e `localName` com normalização segura.
3. Preparar API de deduplicação de resultados para uso do scanner.
4. Atualizar `Status` deste arquivo.

## Critérios De Aceite

- UUIDs não ficam duplicados em outros componentes;
- filtro aceita `Champion`, `champion`, `Champion C001` e rejeita nomes incompatíveis;
- regras opcionais vs obrigatórias estão explícitas.

## Testes

- JVM para catálogo, aliases e filtro do dispositivo.

## Comandos De Validação

```bash
./gradlew app:testDebugUnitTest --tests "*ChampionDeviceFilter*"
```

## Validação Final

- confirmar que nenhum UUID ficou hardcoded fora do catálogo criado.

## Atualização Documental Esperada

- `Status` deste arquivo concluído.

## Implementacao Registrada

Arquivos criados:

- `app/src/main/java/br/com/mwconsulting/championcompanion/core/bluetooth/ChampionUuidCatalog.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/bluetooth/ChampionDeviceFilter.kt`

Conclusoes:

- catalogo unico agora concentra F008, F002, FEE7, FEA1, FEA2, FEC8 e CCCD;
- `requiredTransportChannels` ficou restrito a `F008_NOTIFY` e `F008_WRITE`;
- `defaultSubscribedChannels` inclui F008 notify, F002 notify e FEA1 notify;
- F002 write, FEA2 e FEC8 seguem opcionais e fora do caminho critico de `TransportReady`;
- filtro aceita `Champion`, `champion` e `Champion C001` por prefixo e rejeita substring arbitraria;
- nomes ausentes sao tratados sem excecao;
- API de deduplicacao por `transportKey` foi preparada para uso futuro do scanner.

Validacoes executadas:

- busca por UUIDs BLE hardcoded em `app/src/main/java/**/*.kt`: ocorrencias somente em `ChampionUuidCatalog.kt`;
- `./gradlew app:compileDebugKotlin`: sucesso;
- `./gradlew app:testDebugUnitTest --tests "*ChampionDeviceFilter*"`: build bem-sucedido, sem testes BLE especificos ainda.

Spec: nenhum ajuste factual necessario.
