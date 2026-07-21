# Task 005-07 — Implementar Sessão D0/F002 De BPM Técnico

## Status

Planejada.

## Objetivo

Implementar sessão técnica de D0/F002 com start/stop, parser D0, parser F002, buffer limitado e regras de sessão atual.

## Contexto

Medição técnica depende de A1 confirmado, F002 subscribed antes do start e limpeza estrita do estado da sessão anterior. Cleanup não pode depender do sucesso do stop.

## Documentos Obrigatórios

- `docs/tasks/005-native-ble-poc/005-00-overview.md`
- `docs/specs/005-native-ble-poc.md`
- `docs/protocol/heart-rate-d0-f002.md`
- `docs/protocol/ble-command-mapping.md`

## Dependências

- `005-06` concluída.

## Pré-Condições

- A1 confirmado e subscription FEA1 já funcionais.

## Entradas

- transporte ativo;
- capabilities F002;
- coordenador da PoC.

## Arquivos Exatos Permitidos

- `app/src/main/java/br/com/mwconsulting/championcompanion/core/bluetooth/**`
- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/device/**`
- `app/src/test/java/br/com/mwconsulting/championcompanion/**`
- `docs/tasks/005-native-ble-poc/005-07-implement-d0-f002-heart-rate-session.md`
- `docs/tasks/005-native-ble-poc/005-00-overview.md`, somente para checklist

## Arquivos Proibidos

- `core/database/**`
- `data/**`
- `docs/protocol/*.md`

## Escopo

- garantir F002 subscribe antes de D0 start;
- enviar `D0 01` e `D0 00` pela fila;
- interpretar D0 ao vivo da sessão atual;
- interpretar stream F002 em memória;
- limitar buffer;
- tentar stop em `finally`.

## Fora Do Escopo

- estimador final F002;
- persistência de BPM;
- tela final de saúde.

## Passos

1. Validar capability F002 e assinar via hub antes do start.
2. Limpar estado da sessão anterior e registrar instante inicial.
3. Enviar `D0 01` pela fila.
4. Interpretar pacotes D0 válidos somente após início atual.
5. Interpretar F002 em pares `uint16 little-endian`, ignorando byte ímpar final.
6. Limitar buffer em memória e contadores.
7. Enviar `D0 00` em `finally` quando transporte ainda estiver disponível.
8. Garantir que falha de stop não bloqueia cleanup.
9. Cobrir fluxo com testes.
10. Atualizar `Status` e overview.

## Regras De Segurança

- F002 deve ser assinada antes do start;
- valores de sessão anterior devem ser limpos;
- nenhum sinal bruto vai para Logcat;
- callbacks tardios não atualizam nova sessão.

## Critérios De Aceite

- `D0 01` só sai após A1 confirmado e F002 subscribed;
- `D0 00` é tentado em encerramento seguro;
- parser D0 só aceita sessão atual;
- buffer F002 é limitado e efêmero;
- testes cobrem stop, cleanup e stale callback.

## Testes

- JVM: parser D0, parser F002, byte ímpar, buffer limitado, reset entre sessões.
- integração: capability ausente, nenhum BPM disponível, falha de stop, disconnect durante D0.

## Comandos De Validação

```bash
./gradlew app:testDebugUnitTest --tests "*D0*"
./gradlew app:testDebugUnitTest --tests "*F002*"
```

## Evidências Esperadas

- testes do lifecycle start/stop e do gate F002-before-start.

## Atualização Documental

- atualizar `Status`;
- marcar checklist de `005-00`.

## Validação Final

- confirmar que cleanup independe de sucesso do stop.
