# 018 — Integridade do calendário após importação de treino

- **Ticker:** `018`
- **Status:** concluída
- **Base:** Fases 15, 15A e 17 concluídas

## Contexto

Após importar treino D3/D4 do relógio, o Calendário pode exibir **Dados parciais
do mês**, mesmo quando o treino e suas operações locais aparecem sincronizados.
O banner é emitido quando o read model recebe uma issue de integridade ou
indisponibilidade de fonte; a mensagem não identifica a origem por privacidade.

Investigação no Galaxy S22 confirmou o comportamento com banco local real. A
reimportação preservava `reviewedAt`, mas atualizava `recordedAt`; atividades já
revisadas ficavam com ordem temporal inválida. O domínio rejeitava essas
atividades durante a reconstrução do índice semântico, deixando todos os seus
aggregates não resolvidos. O writer de Health Connect não é a origem.

## Objetivo

Eliminar falso diagnóstico de dados parciais no Calendário após reimportação de
treino, preservando invariantes temporais do domínio e degradações reais.

## Requisitos

1. Reproduzir o banner com banco local preservado e identificar a categoria da
   issue sem registrar IDs, BPM, passos, distância ou timestamps individuais.
2. Preservar `recordedAt` original ao reimportar atividade que mantém revisão e
   workflow.
3. Corrigir registros legados com `reviewedAt < recordedAt` somente quando
   `reviewedAt >= startedAt`, via migration estreita e determinística.
4. Manter invariantes do domínio; não aceitar ordem temporal inválida no mapper.
5. Preservar operações inválidas, aggregates órfãos e dados realmente
   inelegíveis como degradação observável.
6. Não alterar UI, texto, writer, coordinator, histórico dedicado de frequência
   cardíaca ou workflows.
7. Não iniciar BLE, Health Connect ou nova sincronização durante leitura do
   Calendário.

## Critérios de aceite

1. Causa é reproduzida no mesmo checkout e APK antes da edição.
2. Banner deixa de aparecer quando única causa era ordem temporal inválida de
   atividade reimportada.
3. Nova reimportação preserva `recordedAt` original e a revisão permanece
   válida.
4. Migration corrige somente linhas legadas elegíveis e mantém linhas sem
   reparo seguro para diagnóstico fail-closed.
5. Operação `SYNCED` semanticamente válida não produz issue no mês.
6. Falhas reais de fonte, operação inválida ou aggregate não resolvido continuam
   produzindo degradação segura.
7. Dados locais, detalhe diário, status de sync e histórico dedicado permanecem
   inalterados.
8. Nenhum dado sensível aparece em logs ou documentação.
9. `lint`, builds debug/release, instalação e smoke manual offline possuem
    evidências registradas.

## Abordagem

- Confirmar origem da issue no código e no APK instalado.
- Preservar `recordedAt` original em reimportação no domínio e na persistência.
- Corrigir legado somente por migration estreita e determinística.
- Manter amostras e operações somente leitura fora da migration.
- Validar no dispositivo com banco existente, sem fabricar registros.

## Alternativas descartadas

- Alterar ou esconder banner: removeria diagnóstico sem corrigir integridade.
- Relaxar invariantes temporais no mapper: mascararia corrupção persistida.
- Apagar ou normalizar amostras: perda de histórico e violação local-first.
- Reparar fila durante consulta: mistura leitura com escrita e sincronização.
- Reabrir Specs 015, 015A ou 017: fases já aceitas e fechadas.

## Arquivos prováveis

- `app/src/main/java/br/com/mwconsulting/championcompanion/data/RoomRepositories.kt`;
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/model/WatchWorkoutActivity.kt`;
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/database/ChampionDatabase.kt`;
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/database/ChampionMigrations.kt`;
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/database/DatabaseModule.kt`;
- `docs/decisions/adr-016-integridade-temporal-de-reimportacao-de-treino.md`;
- esta spec e documentação SDD da task 018.

## Riscos e mitigação

- APK instalado pode não corresponder ao checkout: confirmar revisão e reinstalar
  somente após alteração.
- Outro aggregate pode causar o banner: registrar apenas categorias seguras e
  manter diagnósticos reais.
- Migration pode corrigir somente inconsistências legadas conhecidas: exigir
  `reviewedAt >= startedAt` e manter falha segura nas demais.
- `RoomRepositories.kt` é hotspot de fases futuras: manter diff mínimo.

## Validação

Não criar, alterar ou executar testes automatizados. Usar:

```bash
git diff --check
./gradlew lint assembleDebug assembleRelease
./gradlew installDebug
```

No dispositivo, confirmar Calendário com Bluetooth desligado, relógio
desconectado e Health Connect indisponível ou sem permissão quando reproduzível.
Mudança visual exige screenshot; esta task não prevê mudança visual.

## Ordem das subtarefas

```text
018-01
```

## Fora do escopo

- BLE, importador D3/D4 e protocolo do relógio;
- provider ou escrita adicional no Health Connect;
- nova sincronização, reparo de fila ou alteração de estado;
- DAO, navegação e UI;
- GPS, rotas, WorkManager e testes automatizados.
