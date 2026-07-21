# Task 005-12 — Executar Primeira Validação Física Exploratória

## Status

Concluída em 2026-07-21. Houve ciclo físico exploratório comprovado no Galaxy S22 com exportação do relatório sanitizado `cycle-a86ae7d2-6b02-4890-a330-2babd26ae2c0.json`. O ciclo confirmou scan real e falha reproduzível na conexão, com erro sanitizado `Canal Bluetooth incompatível com operação`, `error_category` igual a `incompatible_capability`, sem `transport_ready` e sem handshake A1.

## Objetivo

Executar primeira validação física exploratória para confirmar scan real, discovery, properties, A1, FEA1, D0, F002, stop e disconnect antes do gate final.

## Contexto

Esta task não fecha fase. Ela coleta primeiras evidências reais e descobre divergências entre MotionLog e Champion Companion sem forçar comportamento antigo.

## Documentos Obrigatórios

- `docs/tasks/005-native-ble-poc/005-00-overview.md`
- `docs/specs/005-native-ble-poc.md`
- `docs/protocol/champion-c001.md`
- `docs/protocol/handshake-a1.md`
- `docs/protocol/heart-rate-d0-f002.md`

## Dependências

- `005-11` concluída.

## Pré-Condições

- Galaxy S22 conectado por ADB;
- Champion C001 disponível e carregado;
- build debug instalado;
- Bluetooth habilitado.

## Entradas

- tela técnica da PoC;
- relatório sanitizado por ciclo.

## Arquivos Exatos Permitidos

- `docs/tasks/005-native-ble-poc/005-12-run-first-physical-protocol-validation.md`
- `docs/tasks/005-native-ble-poc/005-00-overview.md`, somente para checklist

## Arquivos Proibidos

- marcar task como concluída com fakes;
- alterar protocolo como confirmado sem evidência física;
- alterar roadmap e ADRs.

## Escopo

- primeira execução física exploratória;
- coletar evidências sanitizadas;
- registrar divergências e bloqueios.

## Fora Do Escopo

- gate de dez ciclos;
- correções de código nesta task;
- fechar fase.

## Passos

1. Confirmar pré-condições físicas.
2. Validar scan e nome/localName observados.
3. Validar services, characteristics e properties reais.
4. Validar F008 notify, A1, FEA1, D0 start, D0 resposta, F002, D0 stop e disconnect.
5. Exportar relatório sanitizado do ciclo.
6. Registrar divergências, comportamentos inesperados e bloqueios.
7. Atualizar `Status` e overview.

## Regras De Segurança

- se hardware não estiver disponível, registrar bloqueio e manter task pendente;
- não mascarar falhas com retries ocultos;
- não logar payload bruto ou valor de saúde em documento público.

## Critérios De Aceite

- pelo menos um ciclo exploratório executado no hardware alvo;
- evidência sanitizada exportada;
- divergências físicas registradas para correção posterior.

## Testes

- validação física manual.

## Comandos De Validação

```bash
./gradlew assembleDebug
adb devices
git status --short
```

## Evidências Esperadas

- relatório sanitizado do ciclo;
- lista de properties e comportamentos confirmados/divergentes;
- bloqueios reproduzíveis, se houver.

## Execução Desta Sessão

1. Pré-condições confirmadas:
   - `adb devices` retornou um dispositivo `device`, compatível com Galaxy S22 conectado por ADB.
   - `./gradlew assembleDebug` ficou verde em 2026-07-21.
   - permissão de dispositivo por perto foi concedida no aparelho.
   - scan manual na tela técnica encontrou dispositivo e transicionou para `device_found`.
2. Ciclo físico exportado:
   - relatório sanitizado: `cycle-a86ae7d2-6b02-4890-a330-2babd26ae2c0.json`;
   - `started_at`: `2026-07-21T21:28:29.675879Z`;
   - `finished_at`: `2026-07-21T21:28:53.684398Z`;
   - `duration_seconds`: `24`;
   - `state_sequence`: `idle -> ready_to_scan -> scanning -> device_found -> connecting -> failed`.
3. Resultado funcional observado:
   - `transport_ready`: `false`;
   - `handshake_confirmed`: `false`;
   - `steps_capability`: `not_observed`;
   - `heart_rate_result`: `unavailable`;
   - erro sanitizado visto no app: `Canal Bluetooth incompatível com operação`.

## Divergências Observadas

- divergência física confirmada entre scan e conexão: o relógio é encontrado, mas a tentativa de conectar encerra em falha antes de `transport_ready`;
- capabilities esperadas da PoC não ficaram disponíveis no hardware testado nesta execução;
- nenhuma etapa de A1, FEA1, D0 ou F002 foi observada, porque o ciclo falhou ainda em `connecting`.

## Bloqueios

- falha reproduzível de conexão física com `error_category` `incompatible_capability`;
- sem `transport_ready`, próximas confirmações de A1, FEA1, D0 e F002 ficam bloqueadas até correção na `005-13`.

## Atualização Documental

- atualizar `Status`;
- marcar checklist de `005-00`, porque execução física ocorreu com evidência sanitizada exportada.

## Validação Final

- Task satisfez critérios de aceite em 2026-07-21: houve pelo menos um ciclo exploratório no hardware alvo, relatório sanitizado exportado e divergência física registrada para correção posterior em `005-13`.
