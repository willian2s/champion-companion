# 023-07 — Validar e encerrar a Fase 16

- **Status:** `pending`
- **Depends on:** `023-06`

## Objetivo

Validar em banco preservado e dispositivo real os fluxos novos da Fase 16 e
fechar documentação somente com evidências observadas.

## Validação técnica

Executar:

```bash
git diff --check
./gradlew lint assembleDebug assembleRelease
./gradlew installDebug
```

Não criar testes automatizados novos nem executar suites adicionais sem pedido
explícito.

## Banco

Sobre instalação existente:

- confirmar migration v12 -> v13;
- `PRAGMA user_version = 13`;
- `PRAGMA integrity_check` válido;
- dados anteriores preservados;
- `manualDistanceMeters` da Spec 022 preservado;
- tabela de calibrações criada vazia no upgrade;
- nenhum snapshot criado pela migration;
- nenhuma operação HC criada pela migration.

## Perfil

Validar:

- editar altura;
- reiniciar e confirmar persistência;
- sugestão de passo muda, mas não é aceita automaticamente;
- editar meta diária de passos;
- Home/Saúde/Calendário refletem a meta;
- comprimento base manual/altura continua funcional.

## Peso

Validar:

- registrar duas medições em kg;
- reiniciar app;
- último peso correto;
- histórico preservado;
- comportamento local correto com HC indisponível/desabilitado;
- quando HC estiver habilitado e autorizado, confirmar somente o fluxo write-only
  esperado, sem readback.

## Calibração de corrida

Usar uma atividade D3/D4 real ainda elegível:

1. confirmar `RUNNING`;
2. registrar distância externa de referência;
3. conferir passos usados;
4. conferir comprimento observado;
5. aplicar calibração;
6. conferir distância canônica fechando na referência;
7. conferir pace/velocidade/splits coerentes;
8. confirmar uma única evidência no perfil RUNNING.

Adicionar segunda corrida calibrada e validar manualmente:

```text
learned = Σ distância / Σ passos
```

## Calibração de caminhada

Validar uma atividade `WALKING` e confirmar:

- perfil WALKING independente;
- nenhuma alteração no perfil RUNNING;
- novo treino de caminhada usa walking learned quando disponível.

## Correção e remoção

- corrigir uma distância de referência existente;
- confirmar substituição sem dupla contagem;
- remover uma evidência;
- confirmar recomputação do perfil futuro;
- confirmar que snapshots históricos não mudaram automaticamente.

## Guard de sincronização

- atividade `SYNCED` não pode ser reescrita silenciosamente;
- `SYNCING`/efeito parcial segue fail-closed conforme contrato congelado;
- calibração não dispara sync automaticamente;
- sync explícito após calibração usa projeções esperadas;
- nenhuma leitura Health Connect é adicionada.

## Regressões essenciais

Confirmar pelo menos:

- corrida sem calibração continua funcionando com BASE/fallback;
- caminhada sem calibração continua funcionando;
- TREADMILL continua usando BASE;
- Daily Summary continua usando BASE;
- CYCLING continua usando distância manual opcional da Spec 022 e nunca passos;
- parser continua `d4-v3`.

## Documentação de evidência

Criar:

```text
docs/integration/023-07-phase-16-physical-validation.md
```

Registrar somente cenários realmente exercitados, com N/A para o que não foi
possível validar.

## Fechamento documental

Após aceite:

- marcar Spec 023 como `completed`;
- atualizar `023-00-overview.md` para `7/7`;
- atualizar `docs/baseline/current-baseline.md` com fatos efetivamente entregues;
- atualizar `docs/roadmap/post-phase-15-direction.md` marcando Fase 16 concluída;
- registrar Room/derivation versions finais reais;
- não alterar o roadmap histórico para fingir execução de itens removidos.

## Checklist

- [ ] build/lint/install concluídos
- [ ] migration/banco preservados
- [ ] altura/meta validadas
- [ ] peso/histórico validados
- [ ] corrida calibrada validada
- [ ] segunda corrida confirma aprendizado ponderado
- [ ] caminhada independente validada
- [ ] correção/remoção validada
- [ ] guards de sync validados
- [ ] regressão CYCLING/TREADMILL/DailySummary conferida
- [ ] evidência física documentada
- [ ] baseline/roadmap atualizados
- [ ] spec e overview em `completed`

## Aceite final da Fase 16

A fase encerra quando o perfil aprendido melhora novos treinos de corrida e
caminhada sem GPS interno, enquanto atividades antigas permanecem reproduzíveis
pelos snapshots que congelaram.