# Task 005-06 — Implementar Passos FEA1 Da PoC

## Status

Concluída em 2026-07-21.

## Objetivo

Assinar FEA1 via hub, interpretar pacote de passos em memória e expor evidência técnica segura na PoC.

## Contexto

FEA1 é opcional no core da Fase 04, mas obrigatório para ciclo vencedor da PoC. Etapa precisa falhar com erro específico se capability estiver ausente, sem redefinir `TransportReady`.

## Documentos Obrigatórios

- `docs/tasks/005-native-ble-poc/005-00-overview.md`
- `docs/specs/005-native-ble-poc.md`
- `docs/protocol/ble-command-mapping.md`
- `docs/protocol/champion-c001.md`

## Dependências

- `005-05` concluída.

## Pré-Condições

- A1 funcional disponível.

## Entradas

- capabilities da sessão;
- `ChampionNotificationHub`.

## Arquivos Exatos Permitidos

- `app/src/main/java/br/com/mwconsulting/championcompanion/core/bluetooth/**`
- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/device/**`
- `app/src/test/java/br/com/mwconsulting/championcompanion/**`
- `docs/tasks/005-native-ble-poc/005-06-implement-fea1-steps-poc.md`
- `docs/tasks/005-native-ble-poc/005-00-overview.md`, somente para checklist

## Arquivos Proibidos

- `core/database/**`
- `data/**`
- `docs/protocol/*.md`

## Escopo

- subscription FEA1;
- parser do pacote de quatro bytes;
- armazenamento em memória;
- erro específico para capability ausente;
- evidência sanitizada sem valor exato exportado.

## Fora Do Escopo

- `DailySummary`;
- persistência;
- distância e calorias.

## Passos

1. Validar capability FEA1 no ciclo atual.
2. Assinar FEA1 via `ChampionNotificationHub`.
3. Implementar parser `01 XX XX XX` com 24-bit little-endian.
4. Armazenar último valor e contagem de pacotes somente em memória.
5. Expor presença de passos e capability ausente como evidência sanitizada.
6. Cobrir parser, pacote inválido e capability ausente com testes.
7. Atualizar `Status` e overview.

## Regras De Segurança

- não persistir passos;
- não enviar passos exatos para Logcat ou relatório;
- não configurar CCCD fora do hub.

## Critérios De Aceite

- FEA1 usa `ChampionNotificationHub`;
- parser aceita somente pacote válido de 4 bytes;
- ausência de capability falha etapa de passos sem invalidar transporte;
- passos permanecem somente em memória.

## Testes

- JVM: parser FEA1, 24-bit little-endian, rejeição de pacote inválido.
- integração: capability ausente e fluxo de sucesso.

## Comandos De Validação

```bash
./gradlew app:testDebugUnitTest --tests "*FEA1*"
```

## Evidências Esperadas

- testes do parser e do gate de capability.

## Atualização Documental

- atualizar `Status`;
- marcar checklist de `005-00`.

## Validação Final

- confirmar que nenhuma entidade de banco foi tocada.
