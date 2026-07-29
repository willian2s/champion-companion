# 013-11 — Integrar BPM e passos do relógio à sessão do celular

- **Ticker:** `013`
- **Número:** `11`
- **Status:** pending

## Objetivo e resultado esperado

Adicionar participação opcional e conservadora do Champion C001, com owner BLE próprio, amostras vinculadas e desconexão tolerada. Resultado: dados live somente quando origem esportiva for observável com segurança; sessão nunca depende do relógio.

## Contexto

Gate não possui owner app workout. D0/F002 atual é manual; A8/D8 não confirmados; FEA1 não carrega contexto esportivo. Não iniciar modo esporte nem D0 automaticamente.

## Documentos obrigatórios

Spec 013; ADR-008/014; `013-01/03/08`; protocolos D0/F002, D3/D4, transporte, comandos e Champion C001.

## Dependências

`013-08` concluída. Paralela a 013-09/10 com contratos/repositories congelados.

## Arquivos permitidos

`core/bluetooth/**`; gate/contracts; coordinator/session service; persistência de `APP_WORKOUT_SESSION` por API existente; feature device/exercises/health para conflitos/status; docs protocol somente com evidência; task/overview.

## Arquivos e símbolos prováveis

`ForegroundBleOperationOwner`, `InMemoryForegroundBleOperationGate`, `ChampionConnectionManager`, `ChampionNotificationHub`, `ChampionManualHeartRateProtocol`, `HeartRateMeasurement`, `HeartRateSource.APP_WORKOUT_SESSION` e coordinator da sessão.

## Arquivos proibidos

A8/D8 ou comando não confirmado; start D0 automático; importar D1/D3/D4 durante sessão; GPS/rota; Health Connect; testes.

## Requisitos cobertos

Relógio opcional; owner; BPM/passos; dedup; desconexão/reconexão; bloqueio de operações concorrentes; orientação ao usuário.

## Escopo

- owner `APP_WORKOUT_SESSION` durante participação BLE;
- status associado/conectado/modo esportivo observável/desconectado;
- ingestão segura de BPM/passos já emitidos pelo relógio no contexto comprovado;
- persistir HR como `APP_WORKOUT_SESSION`, sessão+timestamp;
- preservar amostras e permitir reconexão limitada/controlada;
- mensagens de conflito em Home/Saúde/Exercícios/Dispositivo.

## Fora do escopo

Iniciar esporte do relógio, D0 fallback, BLE permanente, retry infinito, D3/D4 merge e sync HC.

## Passos de implementação

1. Adicionar owner e atualizar `when`/mensagens dos owners existentes.
2. Adquirir gate somente quando sessão usar BLE; liberar em final/interrupção/descarte.
3. Reusar sessão saudável/A1/subscriptions sem comando esportivo novo.
4. Definir condição observável segura para atribuir BPM/passos; se ausente, manter dados ausentes.
5. Persistir BPM incremental com dedup sessão/timestamp; passos por delta seguro quando protocolo permitir.
6. Não apagar dados em disconnect; atualizar status e seguir treino.
7. Implementar ação controlada de reconectar, sem loop.
8. Bloquear D1, D3/D4, manual e refresh conflitantes com copy amigável.
9. Mostrar orientação para iniciar treino também no relógio.
10. Registrar achados físicos no protocolo somente quando observados e atualizar overview.

## Critérios de aceite

- treino funciona sem device;
- nenhum D0/A8/D8 automático é enviado;
- amostras têm owner/session/timestamp e não duplicam;
- disconnect não encerra treino nem apaga amostras;
- operações BLE conflitantes são recusadas;
- incerteza do protocolo produz ausência, não atribuição especulativa.

## Validação manual

Sem relógio; conectado sem modo esporte; modo esporte com BPM quando observado; passos quando disponíveis; disconnect/reconnect; tentativa de manual/D1/D3D4/refresh durante sessão; inspeção de comandos sanitizada.

## Comandos

```bash
./gradlew lint assembleDebug assembleRelease
./gradlew installDebug
```

## Evidências

Matriz cenário→dados/status, lista sanitizada de códigos de comando enviados, dedup observada, comandos e screenshots.

## Screenshots

Obrigatórias: status sem relógio, conectado, desconectado e conflito; sem BPM identificável se screenshot sair do ambiente privado.

## Definição de pronto

Integração opcional segura concluída ou ausência protocolar explicitamente aceita; overview atualizado.

## Riscos e cuidados

Maior risco: atribuir D0 manual à sessão errada. Falta de protocolo comprovado pode limitar entrega a status/owner e correlação posterior; registrar sem inventar.

## Rollback

Desabilitar participação BLE durante phone workout, liberar gate e preservar sessão/GPS; não apagar amostras já válidas.

Não criar, alterar ou executar testes automatizados.
