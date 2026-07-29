# 013-09 — Implementar captura de rota e métricas de caminhada/corrida

- **Ticker:** `013`
- **Número:** `09`
- **Status:** pending

## Objetivo e resultado esperado

Conectar fonte GPS ao serviço para persistir pontos aceitos em pequenos lotes, segmentar gaps e calcular métricas live. Resultado: walking/running recuperáveis com rota local íntegra.

## Contexto

Readiness/filtro vêm de `013-06`; schema/repository de `013-05`; FGS de `013-08`. Aquecimento anterior ao zero nunca persiste.

## Documentos obrigatórios

Spec 013; ADR-014; `013-05/06/08`; política de privacidade atualizada.

## Dependências

`013-08` concluída. Pode avançar em paralelo com 013-10/11 sem editar contratos/repositories simultaneamente.

## Arquivos permitidos

Serviço/coordinator da sessão; `core/location`; route repository/DAOs somente APIs já definidas; estado/tela ativa de métricas GPS; logging seguro; task/overview.

## Arquivos e símbolos prováveis

`WorkoutRecordingService`, `WorkoutLocationSource`, `RoutePointFilter`, `WorkoutRouteAccumulator`, `WorkoutRouteRepository`, `WorkoutRouteDao` e estado de métricas da sessão ativa.

## Arquivos proibidos

BLE live, tipos sem GPS, finalização em atividade, Health Connect, mapa/export e testes.

## Requisitos cobertos

Captura incremental; campos de ponto; filtros; distância; ritmo/velocidade; segmentos; pause/loss/recovery; falha ao persistir.

## Escopo

- começar persistência só após ACTIVE;
- pequenos lotes/checkpoints;
- sequence/segment determinísticos;
- distância acumulada somente accepted;
- ritmo atual/médio e velocidade;
- signal lost/indisponível sem encerrar sessão;
- novo segmento em resume/gap/process recovery;
- degradação segura em falha de persistência.

## Fora do escopo

Mapa, route simplification, export, sync HC e correlação.

## Passos de implementação

1. Criar rota única ao iniciar walking/running.
2. Iniciar location source pela geração ativa.
3. Filtrar e acumular accepted; contar rejected por categoria segura.
4. Persistir em lote curto com sequence/segment e distância acumulada.
5. Pausa para source/persistência e fecha segmento.
6. Retomada/readiness e recuperação abrem segmento novo.
7. Em loss prolongado, sinalizar UI e não conectar gap.
8. Atualizar agregados/checkpoints e tela ativa.
9. Falha de write pausa captura/mostra erro seguro, sem perder atividade silenciosamente.
10. Atualizar overview.

## Critérios de aceite

- zero ponto de warmup persiste;
- rejected não altera distância;
- gap/pausa nunca forma linha reta;
- processo morto preserva últimos lotes;
- sequência/segmentos sobreviverem à recuperação;
- UI não mostra coordenadas;
- livre/esteira não entram neste caminho.

## Validação manual

Caminhada/corrida externa, pausa/retomada, perda/recuperação GPS, background, tela apagada, process death/recovery e falha de persistência reproduzível; comparar distância com referência aproximada sem tratar precisão como garantia.

## Comandos

```bash
./gradlew lint assembleDebug assembleRelease
./gradlew installDebug
```

## Evidências

Contagens sanitizadas de pontos/segmentos, distância final, transições GPS, ausência de coordenadas no Logcat, comandos e screenshots.

## Screenshots

Obrigatórias: ativa GPS, pausada, sinal perdido/recuperado e métricas; sem mapa/coordenadas.

## Definição de pronto

Walking/running persistem rota recuperável e métricas corretas; overview atualizado.

## Riscos e cuidados

Bateria, lotes grandes e callback tardio. Não persistir rejected. Não ajustar threshold sem registrar evidência física.

## Rollback

Desabilitar tipos GPS na seleção e parar capture; preservar dados v6 para descarte/forward fix.

Não criar, alterar ou executar testes automatizados.
