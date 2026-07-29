# Follow-up — validação Room v5 → v6

## Objetivo

Completar cobertura manual de cenários que não existem no banco v5 usado no
gate físico de `013-05`.

## Evidência já capturada

- Snapshot v5 e snapshot v6 foram capturados no pacote debug do dispositivo
  `SM-S901E`.
- Comparação sanitizada preservou linhas e hashes de atividades, importações,
  intervalos, amostras BPM, `heart_rate_measurements` e operações de sync.
- `user_version` passou de `5` para `6`; `foreign_key_check` não apontou
  violações.
- Exercícios, detalhe e Calendário abriram após migration.

## Pendências

1. Repetir gate com banco v5 contendo pelo menos uma calibração de esteira e
   sessões persistidas nos estados relevantes, incluindo uma sessão
   `DISCARDED` legada sem `endedAtEpochMillis` e outra descartada após início
   com durações acumuladas. Registrar somente contagens, hashes e divergências;
   não exportar IDs, coordenadas, horários ou payloads.
2. Comparar antes/depois de `treadmill_calibrations` e
   `app_workout_sessions`, incluindo vínculos, estados, agregados, tempos,
   revisão e geração. Confirmar término e última transição válidos, além do
   round-trip legível das sessões `DISCARDED`.
3. Quando houver rota real, exercitar `WorkoutRouteRepository.loadRecoverySnapshot`
   após reinício/process death e confirmar cursor, último ponto, segmentos,
   métricas e sequência antes de novo lote.
4. Alinhar invariantes de escrita e leitura de rota: validar `route.startedAt`
   contra `session.startedAt`, rejeitar primeiro ponto/segmento anterior à rota
   e impedir segmento novo iniciado antes do fim do segmento anterior. Garantir
   que todo append aceito seja aceito imediatamente por
   `loadRecoverySnapshot`.
5. Após corrigir invariantes, solicitar nova revisão independente do caminho
   append → recuperação e registrar decisão na task `013-05`.
6. Se qualquer divergência aparecer, abrir correção na subtarefa responsável;
   não fazer downgrade de banco v6.

## Restrições

- Não criar nem executar testes automatizados nesta fase.
- Executar somente em dispositivo de desenvolvimento.
- Não registrar conteúdo de pontos, localização, payload BLE, dados de saúde ou
  identificadores pessoais.
