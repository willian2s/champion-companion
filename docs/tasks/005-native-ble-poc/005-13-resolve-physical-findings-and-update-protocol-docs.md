# Task 005-13 — Resolver Achados Físicos E Atualizar Protocolos

## Status

Em andamento em 2026-07-21. O principal achado fisico da `005-12` foi resolvido fisicamente: a segunda revalidacao do dia atingiu `transport_ready` com `steps_capability=available` e `heart_rate_capability=available`. O bloqueio restante ficou concentrado no handshake A1. Primeiro, o relatorio sanitizado marcou `error_category=internal_failure`, e o core foi ajustado para preservar timeout/cancelamento reais do await de pacote em vez de mascarar esse caminho como falha interna. Na terceira revalidacao do mesmo dia, o ciclo `a0971467-e876-4192-96c1-c0c306ed0b3e` confirmou a falha real como `error_category=timeout` durante `handshaking`. A PoC Android passou entao a alinhar o A1 ao timing observado no MotionLog background: atraso inicial curto e retry limitado em caso de timeout. Na quarta revalidacao do mesmo dia, o ciclo `bd57e034-aa94-43e1-987a-f0745ceea48b` manteve `error_category=timeout`, o que expôs nova divergencia concreta com o `motionlog-app`: a PoC nativa ainda nao ativava todos os `defaultSubscribedChannels` antes do A1. O connect foi corrigido para subir `F008_NOTIFY`, `F002_NOTIFY` e `FEA1_NOTIFY` sempre que esses canais estiverem disponiveis. Documentacao de protocolo foi atualizada para refletir as quatro evidencias fisicas do dia e distinguir o que segue inferido.

## Objetivo

Corrigir achados da primeira validação física, criar fixtures sanitizadas derivadas e atualizar documentação de protocolo distinguindo inferido de confirmado.

## Contexto

Depois da primeira execução real, divergências de UUID, property, timing, A1, FEA1, D0, F002, stop e disconnect podem exigir ajustes pequenos no core, coordenador ou testes.

## Documentos Obrigatórios

- `docs/tasks/005-native-ble-poc/005-12-run-first-physical-protocol-validation.md`
- `docs/specs/005-native-ble-poc.md`
- `docs/protocol/champion-c001.md`
- `docs/protocol/ble-command-mapping.md`
- `docs/protocol/handshake-a1.md`
- `docs/protocol/heart-rate-d0-f002.md`

## Dependências

- `005-12` concluída com evidências ou bloqueio claro.

## Pré-Condições

- existir lista de achados reais para resolver.

## Entradas

- relatório sanitizado;
- notas da validação física;
- código atual da PoC.

## Arquivos Exatos Permitidos

- `app/src/main/java/br/com/mwconsulting/championcompanion/core/bluetooth/**`
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/bluetooth/android/**`
- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/device/**`
- `app/src/test/java/br/com/mwconsulting/championcompanion/**`
- `docs/protocol/champion-c001.md`
- `docs/protocol/ble-command-mapping.md`
- `docs/protocol/handshake-a1.md`
- `docs/protocol/heart-rate-d0-f002.md`
- `docs/tasks/005-native-ble-poc/005-13-resolve-physical-findings-and-update-protocol-docs.md`
- `docs/tasks/005-native-ble-poc/005-00-overview.md`, somente para checklist

## Arquivos Proibidos

- `docs/roadmap/champion-companion-roadmap.md`
- `docs/decisions/*.md`, salvo se task explicitamente concluir que ADR é necessária em trabalho futuro

## Escopo

- corrigir achados físicos relacionados à PoC;
- ajustar testes e fixtures;
- atualizar docs de protocolo com evidência confirmada.

## Fora Do Escopo

- gate final de dez ciclos;
- reabrir escopo para D1/D3/D4.

## Passos

1. Priorizar achados que bloqueiam ciclo completo.
2. Corrigir código minimamente.
3. Ajustar ou adicionar testes e fixtures sanitizadas.
4. Atualizar docs de protocolo distinguindo claramente inferido de confirmado físico.
5. Revalidar fluxo exploratório necessário.
6. Atualizar `Status` e overview.

## Regras De Segurança

- não commitar dados reais de saúde;
- não reescrever silenciosamente evidência anterior;
- não ampliar escopo fora da PoC.

## Critérios De Aceite

- bloqueios principais da primeira validação física foram resolvidos ou isolados;
- docs de protocolo refletem evidência física nova;
- fixtures derivadas permanecem sanitizadas.

## Testes

- testes automatizados afetados pela correção;
- reexecução física dirigida quando necessário.

## Comandos De Validação

```bash
./gradlew lint testDebugUnitTest assembleDebug assembleRelease
./gradlew app:connectedDebugAndroidTest
git status --short
```

## Evidências Esperadas

- lista de achados resolvidos;
- docs atualizados;
- fixtures sanitizadas.

## Execução Desta Sessão

1. Achado de entrada priorizado:
   - ciclo fisico `a86ae7d2-6b02-4890-a330-2babd26ae2c0`;
   - `state_sequence`: `idle -> ready_to_scan -> scanning -> device_found -> connecting -> failed`;
   - `error_category`: `incompatible_capability`;
   - erro sanitizado no app: `Canal Bluetooth incompatível com operação`.
2. Correção aplicada no core:
   - `ChampionCapabilityRegistry` agora aceita canal de escrita compativel quando Android anunciar `WRITE` ou apenas `WRITE_WITHOUT_RESPONSE`;
   - `ChampionTransportSession` deixou de rejeitar comandos de escrita quando a characteristic e write-capable por qualquer uma dessas duas properties;
   - `AndroidGattFacade` passou a selecionar `WRITE_TYPE_DEFAULT` ou `WRITE_TYPE_NO_RESPONSE` conforme properties reais da characteristic no Android.
3. Cobertura automatizada adicionada:
   - teste de capability resolver para `F008_WRITE` com `WRITE_WITHOUT_RESPONSE`;
   - teste de `ChampionTransportSession` para comando `WITH_RESPONSE` sobre canal anunciado somente como `WRITE_WITHOUT_RESPONSE`.
4. Documentacao atualizada:
   - `docs/protocol/champion-c001.md`;
   - `docs/protocol/ble-command-mapping.md`;
   - `docs/protocol/handshake-a1.md`;
   - `docs/protocol/heart-rate-d0-f002.md`.
5. Revalidacao fisica adicional registrada:
   - ciclo `859c7105-57d5-4667-8b77-461ba69084f8`;
   - `state_sequence`: `idle -> permissions_required -> ready_to_scan -> scanning -> device_found -> connecting -> transport_ready -> handshaking -> failed`;
   - `transport_ready=true`;
   - `handshake_confirmed=false`;
   - `steps_capability=available`;
   - `heart_rate_capability=available`;
   - `error_category=internal_failure`, depois isolado como mascaramento de erro no await do pacote.
6. Terceira revalidacao fisica registrada:
   - ciclo `a0971467-e876-4192-96c1-c0c306ed0b3e`;
   - `state_sequence`: `idle -> ready_to_scan -> scanning -> device_found -> connecting -> transport_ready -> handshaking -> failed`;
   - `transport_ready=true`;
   - `handshake_confirmed=false`;
   - `steps_capability=available`;
   - `heart_rate_capability=available`;
   - `error_category=timeout`.
7. Ajuste adicional aplicado na PoC:
   - `DevicePocCoordinator` agora executa A1 com atraso inicial de `1_000 ms`;
   - timeout em A1 pode disparar retry limitado a 3 tentativas com `1_500 ms` entre elas;
   - criterio de sucesso continua igual: somente notify `F008` com `A1` e byte 19 igual a `01`.
8. Quarta revalidacao fisica registrada:
   - ciclo `bd57e034-aa94-43e1-987a-f0745ceea48b`;
   - `state_sequence`: `idle -> ready_to_scan -> scanning -> device_found -> connecting -> transport_ready -> handshaking -> failed`;
   - `transport_ready=true`;
   - `handshake_confirmed=false`;
   - `steps_capability=available`;
   - `heart_rate_capability=available`;
   - `error_category=timeout`.
9. Novo achado resolvido no bootstrap do connect:
   - `ChampionUuidCatalog.defaultSubscribedChannels` ja listava `F008_NOTIFY`, `F002_NOTIFY` e `FEA1_NOTIFY`;
   - `ChampionConnectionManager` ativava apenas `F008_NOTIFY` antes de `TransportReady`;
   - o connect agora ativa todos os `defaultSubscribedChannels` disponiveis, alinhando a PoC nativa ao `motionlog-app`.

## Achados Resolvidos Ou Isolados

- bloqueio principal de `TransportReady` foi isolado para compatibilidade de property GATT do canal de escrita principal;
- PoC Android agora suporta variacao de property sem reescrever o protocolo de alto nivel;
- confirmacao fisica do valor exato de property ainda ficou pendente, porque o relatorio sanitizado nao exporta esse detalhe;
- falha de handshake agora ficou isolada do transporte e do resolver de capabilities;
- categorizacao sanitizada do await A1 deixou de mascarar timeout/cancelamento como `internal_failure`.
- terceira evidência fisica confirmou que o await do A1 realmente expira no hardware atual, nao mais `internal_failure`;
- a PoC Android agora reproduz timing mais proximo do MotionLog background antes de concluir que o A1 falhou.
- quarta evidência fisica mostrou que timing sozinho nao bastava;
- a divergencia adicional encontrada foi de subscriptions default incompletas antes do A1.

## Atualização Documental

- atualizar `Status`;
- marcar checklist de `005-00` somente apos nova revalidacao fisica confirmar se subscriptions default completas destravam o A1 ou, em caso negativo, isolem novo erro especifico alem do timeout atual.

## Validação Final

- confirmar que nenhum dado real sensível entrou em docs ou fixtures versionadas.
- Nesta sessão, nenhuma fixture com dado real foi versionada e nenhum payload, BPM, MAC ou bytes crus foram adicionados em docs.
