# 013-07 — Implementar preparação, countdown e fluxo de permissão

- **Ticker:** `013`
- **Número:** `07`
- **Status:** pending

## Objetivo e resultado esperado

Entregar seleção e preparação acessíveis com countdown cancelável, GPS aquecido sem persistência e status opcional do relógio. Resultado: comando de start emitido somente em zero e com pré-condições válidas.

## Contexto

Shells/destinos e preferência vêm de `013-04`; readiness vem de `013-06`. Preparação não pode criar atividade, rota ou FGS por abertura.

## Documentos obrigatórios

Spec 013; `013-04/06`; padrões visuais/a11y existentes; AGENTS.

## Dependências

`013-06` concluída.

## Arquivos permitidos

`feature/exercises/**`; navigation mínima; resources; preferences para update; estado do dispositivo somente por contratos existentes; task/overview.

## Arquivos e símbolos prováveis

`ExercisesFeatureContract`, `ExercisesViewModel`, `ExercisesScreen`, novos estados/intents de preparação/countdown, destinos de treino e `AppPreferencesRepository.update`.

## Arquivos proibidos

Room route writes, FGS real além de interface de start, D0/A8, Health Connect, testes.

## Requisitos cobertos

Tela de seleção/preparação; tipos; countdown 0/3/5/10; cancelamento; GPS/relógio; permissão; voltar sem atividade; a11y.

## Escopo

- CTA `Iniciar treino` na entrada de Exercícios;
- seleção dos quatro tipos;
- preparação por tipo;
- solicitação contextual de `ACCESS_FINE_LOCATION` só para walking/running;
- detecção de aproximada/serviço off;
- warming GPS descartável;
- countdown visível, anunciado com moderação e persistido.

## Fora do escopo

Sessão ativa completa, notificação, captura/persistência e sync.

## Passos de implementação

1. Expandir estado/intents ViewModel para seleção/preparação/countdown.
2. Integrar preferência e allowlist.
3. Iniciar warming ao entrar em preparação GPS, sem repository de rota.
4. Modelar mensagens/ações para permissão e serviços.
5. Habilitar start GPS somente bom/forte; sem GPS para livre/esteira.
6. Após toque, contar; cancelar retorna espera.
7. Se readiness cair durante countdown GPS, cancelar/pausar e retornar espera.
8. Em zero, emitir uma única solicitação idempotente de início para `013-08`.
9. Cleanup warming ao sair; voltar sem criar nada.
10. Validar fonte aumentada/landscape e atualizar overview.

## Critérios de aceite

- abrir/voltar não cria sessão/atividade/FGS/rota;
- todos countdowns e último valor funcionam;
- 0 inicia sem atraso artificial após toque;
- cancelamento não cria atividade;
- treino sem GPS não pede localização;
- perda de GPS impede start;
- status e ações têm descrição acessível.

## Validação manual

Countdown 0/3/5/10, cancelamento, permissão negada/aproximada, localização desligada, estados GPS e degradação; livre/esteira sem prompt; back; rotação, 1,3x e landscape.

## Comandos

```bash
./gradlew lint assembleDebug assembleRelease
./gradlew installDebug
```

## Evidências

Roteiro preenchido, confirmação de ausência de writes/service antes de zero, comandos e screenshots.

## Screenshots

Obrigatórias: seleção; preparação GPS em estados principais; preparação sem GPS; countdown; fonte aumentada/landscape representativos.

## Definição de pronto

Preparação funcional emite exatamente um start válido e overview está atualizado.

## Riscos e cuidados

Recomposição não pode duplicar timer/start. Live region não deve anunciar cada frame. Não expor coordenadas em UI/erro.

## Rollback

Remover destinos/CTA novos e conservar preference inócua.

Não criar, alterar ou executar testes automatizados.
