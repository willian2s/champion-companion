# Task 005-12 — Executar Primeira Validação Física Exploratória

## Status

Planejada.

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

## Atualização Documental

- atualizar `Status`;
- marcar checklist de `005-00` somente se execução física ocorreu.

## Validação Final

- confirmar explicitamente se task ficou concluída ou bloqueada por hardware.
