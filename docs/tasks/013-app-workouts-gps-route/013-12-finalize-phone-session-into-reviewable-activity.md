# 013-12 — Finalizar sessão do celular em atividade revisável

- **Ticker:** `013`
- **Número:** `12`
- **Status:** cancelled

## Objetivo e resultado esperado

Implementar finalização local idempotente e transacional, transformando sessão phone em `WorkoutActivity` única, vinculando rota/amostras e abrindo revisão/calibração existente. Resultado: retry de finalização não duplica atividade.

## Contexto

Frentes GPS, tipos sem GPS e relógio convergem aqui. Serviço deve bloquear callbacks, flushar e consolidar antes de parar. Atividade generalizada já está em schema/contrato.

## Documentos obrigatórios

Spec 013; ADR-011/012/014; `013-03/05/08/09/10/11`; fluxo de revisão/calibração atual.

## Dependências

`013-09`, `013-10` e `013-11` concluídas ou integração BLE formalmente degradada para ausência segura.

## Arquivos permitidos

Session service/coordinator; `RoomRepositories`, DAOs/mappers somente transação final já prevista; `WorkoutActivity` factories; `feature/exercises` revisão/navegação; task/overview.

## Arquivos e símbolos prováveis

`AppWorkoutSession`, estado `FINISHING`, factory phone de `WorkoutActivity`, `RoomRepositories.finalizePhoneWorkout` equivalente, `AppWorkoutSessionDao`, route/HR DAOs e `ExercisesViewModel`.

## Arquivos proibidos

Correlação D3/D4, Health Connect route, mapa/export, testes.

## Requisitos cobertos

Ordem final; atividade única; workflows; rota/amostras; descarte; source priority; serviço encerrado.

## Escopo

- estado `FINISHING` recuperável;
- flush final de rota/BPM/agregados;
- métricas por prioridade;
- criação de atividade phone e vínculo one-to-one;
- transferir/associar amostras sem duplicar;
- workflow PENDING_REVIEW e esteira→calibração após revisão;
- descarte pré-atividade com deleção física transitória.

## Fora do escopo

Import merge e sync remoto.

## Passos de implementação

1. Serializar finalize e rejeitar duplo toque/callback tardio.
2. Persistir checkpoint e `FINISHING`.
3. Parar producers fora da transação e flushar último lote válido.
4. Consolidar tempo, distância GPS, ritmo, velocidade, BPM/passos/calorias conforme prioridade.
5. Em uma transação: finalizar rota, criar ou reutilizar atividade pelo vínculo, associar sessão/amostras e marcar `FINISHED`.
6. Após commit, liberar gate, parar FGS e navegar ao detalhe/revisão.
7. Se transaction falhar, manter `FINISHING` recuperável e retry idempotente.
8. Implementar descarte antes da atividade: parar producers, apagar rota/pontos/amostras transitórias e marcar sessão.
9. Atualizar read models/queries mínimos para nova atividade e overview.

## Critérios de aceite

- ordem obrigatória é respeitada;
- retry não cria segunda atividade;
- phone activity não exige import/device;
- rota permanece vinculada via sessão;
- GPS ganha da distância watch;
- livre/walk/run abrem revisão; esteira chega ao fluxo de calibração correto;
- descarte remove dados transitórios sensíveis;
- FGS só para após commit ou estado recuperável seguro.

## Validação manual

Finalizar todos tipos, duplo toque, falha de persistência, process death em FINISHING, recovery/retry, descarte e reinício; verificar listas/detalhes/calendário sem duplicata.

## Comandos

```bash
./gradlew lint assembleDebug assembleRelease
./gradlew installDebug
```

## Evidências

IDs opacos/contagens sanitizadas, state transitions, activity count, deleção de route no descarte, comandos e screenshots.

## Screenshots

Obrigatórias: confirmação finalizar/descartar, revisão phone, esteira/calibração e erro recuperável.

## Definição de pronto

Sessões finalizam/descarte com atomicidade local; 013-13/14 liberadas; overview atualizado.

## Riscos e cuidados

Provider não participa desta transação. Não mover metadados import-only para atividade phone. Não apagar atividade após criada por descarte pré-atividade.

## Rollback

Desabilitar finalização nova e manter sessões `FINISHING/AWAITING_RECOVERY`; corrigir por forward fix. Sem downgrade v6.

Não criar, alterar ou executar testes automatizados.

## Encerramento da Fase 013 — cancelada e não executada

- **Status atual:** `cancelled`.
- **Motivo:** `product_reorientation`; a finalização de novos treinos iniciados
  pelo celular deixou de ser direção ativa.
- **Execução:** esta subtarefa não foi executada nem concluída. Não houve
  implementação, aceite ou evidência de finalização transacional, atividade
  phone revisável, descarte de rota ou calibração derivados deste escopo.
- **Histórico e riscos:** objetivo, dependências, critérios, riscos e limites
  originais permanecem preservados acima; a ausência de execução não é sucesso.
- **Destino:** requisitos de revisão, derivação e sincronização de treinos do
  relógio podem ser redefinidos conceitualmente na Fase 014. Nenhum backlog
  phone ativo é aberto por este registro.
