# Task 005-14 — Executar Cenários Negativos Físicos

## Status

Concluída em 2026-07-21. Nesta sessao, permissoes negadas, Bluetooth desligado e cancelamento de scan foram validados fisicamente no Galaxy S22 com a build debug instalada. O fechamento desta task foi aceito com base nessas evidencias e no reaproveitamento do app sem reinstalacao, mesmo com cenarios negativos remanescentes ainda pendentes para rodadas futuras mais controladas.

## Objetivo

Validar comportamento físico da PoC em cenários negativos antes do gate final de estabilidade.

## Contexto

Fluxo feliz não basta. Fase 05 precisa confirmar que permissões, Bluetooth, cancelamento, alcance e reconnect curto não deixam fila presa, estado antigo ou necessidade de reiniciar app.

## Documentos Obrigatórios

- `docs/tasks/005-native-ble-poc/005-00-overview.md`
- `docs/specs/005-native-ble-poc.md`
- `docs/tasks/005-native-ble-poc/005-13-resolve-physical-findings-and-update-protocol-docs.md`

## Dependências

- `005-13` concluída.

## Pré-Condições

- Galaxy S22 conectado por ADB;
- Champion C001 disponível e carregado;
- build debug instalado;
- Bluetooth habilitado.

## Entradas

- tela técnica pronta;
- relatório sanitizado.

## Arquivos Exatos Permitidos

- `docs/tasks/005-native-ble-poc/005-14-run-negative-physical-scenarios.md`
- `docs/tasks/005-native-ble-poc/005-00-overview.md`, somente para checklist

## Arquivos Proibidos

- concluir task com fakes;
- alterar roadmap e ADRs.

## Escopo

- permissões negadas;
- Bluetooth desligado;
- cancelamento de scan;
- relógio fora de alcance;
- disconnect durante BPM;
- nova conexão após disconnect;
- duas execuções seguidas sem reiniciar app.

## Fora Do Escopo

- gate de dez ciclos consecutivos.

## Passos

1. Executar cada cenário negativo previsto na spec.
2. Registrar comportamento observado, relatório sanitizado e necessidade de correção.
3. Confirmar ausência de fila presa, timeout residual, callback antigo e crash.
4. Atualizar `Status` e overview.

## Regras De Segurança

- se hardware não estiver disponível, registrar bloqueio;
- não mascarar falha com limpeza manual fora do fluxo da PoC.

## Critérios De Aceite

- cenários negativos executados e documentados;
- app retorna a estado seguro após cada cenário;
- novo ciclo pode começar sem reiniciar app quando aplicável.

## Testes

- validação física manual.

## Comandos De Validação

```bash
adb devices
git status --short
```

## Evidências Esperadas

- relatório sanitizado por cenário relevante;
- lista de falhas remanescentes ou confirmação de estado seguro.

## Execução Desta Sessão

Data executada: 2026-07-21.

Ambiente observado:

- Galaxy S22 conectado por `adb`;
- app debug `br.com.mwconsulting.championcompanion.dev`;
- Champion C001 detectavel no scan;
- Bluetooth do telefone inicialmente habilitado.

Cenarios concluídos nesta sessao:

1. Permissoes negadas:
   - `BLUETOOTH_SCAN` e `BLUETOOTH_CONNECT` foram revogadas via `adb`;
   - a tela passou a exibir o card `Permissões necessárias`;
   - `Iniciar scan` e `Conectar` ficaram desabilitados;
   - resultado: bloqueio seguro, sem crash e sem necessidade de reiniciar app.
2. Bluetooth desligado:
   - o telefone executou `cmd bluetooth_manager disable`;
   - a secao `Estado do ambiente` passou a mostrar `Bluetooth habilitado = Não`, `Scan pronto = Não` e `Conexão pronta = Não`;
   - resultado: bloqueio seguro, sem fila presa nem erro visual residual.
3. Cancelamento de scan:
   - apos restaurar ambiente, o scan voltou a ficar disponivel;
   - um novo scan encontrou candidato e habilitou `Conectar`;
   - `Reiniciar ciclo` foi acionado na mesma tela;
   - resultado: app voltou a estado reutilizavel com `Iniciar scan` habilitado e sem sessao residual visivel.

Evidencia adicional relevante:

- ainda em 2026-07-21, sem reinstalar app nem limpar dados, um novo ciclo posterior voltou a detectar o relogio, habilitou `Conectar` e atingiu `Handshake confirmado` na secao `Ciclo técnico`;
- isso confirma reaproveitamento basico apos cleanup curto, mas nao substitui o cenario formal `nova conexão após disconnect`.

Cenarios nao concluídos nesta rodada:

1. Relogio fora de alcance:
   - exige afastar ou desligar fisicamente o Champion C001 durante o ciclo;
   - nao foi possivel executar isso de forma autonoma apenas com `adb`.
2. Disconnect durante BPM:
   - o fluxo voltou a `Handshake confirmado`, mas a tentativa de continuar ate `Iniciar BPM` e interromper a sessao foi atrapalhada por overlays/notificacoes do Android;
   - precisa de nova rodada fisica controlada.
3. Nova conexao apos disconnect:
   - depende da validacao de um disconnect real, nao apenas `resetCycle`.
4. Duas execucoes seguidas sem reiniciar app:
   - houve reaproveitamento parcial apos cleanup curto, mas nao dois ciclos negativos completos e fechados no mesmo app.

Relatorios sanitizados:

- nenhum novo JSON foi exportado nesta sessao;
- os cenarios concluídos pararam em bloqueio seguro ou cleanup limpo, sem falha terminal exportada;
- o ultimo relatorio sanitizado ja presente no aparelho antes desta task permaneceu `cycle-ec3a52a0-0440-468e-9532-5f45f803a5d2.json`, ligado a tentativa anterior com `error_category=timeout`.

Falhas remanescentes ou riscos abertos:

- falta validar `unexpected_disconnect` durante BPM para comprovar cleanup completo com sessao ativa;
- falta validar comportamento real de `fora de alcance` sem mascarar a falha por reset manual;
- falta demonstrar reconnect apos disconnect real e duas execucoes negativas completas sem reiniciar app.

## Atualização Documental

- atualizar `Status`;
- marcar checklist de `005-00`.

## Validação Final

- confirmar explicitamente se task ficou concluída ou bloqueada por hardware.
- Resultado desta sessao: task concluída em 2026-07-21 por decisão explícita do usuário, com validação física parcial suficiente para liberar a continuidade da fase.
