# 013-13 — Correlacionar importações D3/D4 e suprimir duplicatas

- **Ticker:** `013`
- **Número:** `13`
- **Status:** pending

## Objetivo e resultado esperado

Integrar importação posterior do relógio à atividade phone compatível, sem criar duplicata e sem substituir GPS. Resultado: merge seguro transacional; fallback independente em ausência/ambiguidade.

## Contexto

Dedup por CRC e fallback por início já existem para watch. Nova correlação ocorre depois da chave CRC e antes de criar nova atividade. D3/D4 puro deve permanecer funcional.

## Documentos obrigatórios

Spec 013; specs 010/011; ADR-011/014; `013-03/05/11/12`; protocolo D3/D4.

## Dependências

`013-12` concluída. Pode avançar em paralelo com 013-14, evitando `RoomRepositories` simultâneo.

## Arquivos permitidos

`ChampionWatchWorkoutImportCoordinator.kt`; parser adapters sem alterar protocolo; repositories/DAOs/mappers de import/correlação; domínio de merge; UI de resultado/revisão; logging seguro; task/overview.

## Arquivos e símbolos prováveis

`ChampionWatchWorkoutImportCoordinator`, `WorkoutActivityRepository.persistImported`, `WatchWorkoutImportLookup`, query de candidatos phone, `WorkoutCorrelationDecision` e merge transacional em `RoomRepositories`.

## Arquivos proibidos

GPS capture/filter, FGS, Health Connect route, A8/D8, testes.

## Requisitos cobertos

Regra 60s; device/sobreposição/candidato único; merge; ambiguidade; reimport CRC; source priority; atividade somente watch.

## Escopo

- CRC lookup primeiro;
- candidatos phone não descartados sem import correlacionado;
- `abs(start)<=60 s`, sobreposição relevante e device quando associado;
- merge import/intervalos/BPM/passos/calorias elegíveis;
- dedup amostras real-time versus D4;
- estado correlacionado seguro para UI.

## Fora do escopo

Autoimportação, iniciar watch sport, substituir rota/GPS e sync remoto.

## Passos de implementação

1. Preservar caminho de reimport por CRC existente.
2. Consultar candidatos phone antes da criação watch nova.
3. Aplicar janela, device, overlap, não descartada e unicidade.
4. Candidato único: transação vincula `WatchWorkoutImport` à atividade phone.
5. Mesclar somente BPM/passos e calorias ausentes/válidas conforme tabela de prioridade.
6. Deduplicar HR por timestamp/sessão/atividade com tolerância contratual definida, sem apagar amostra válida.
7. Nunca substituir start/end/type phone, GPS, rota, distância, ritmo ou velocidade.
8. Sem candidato: fluxo watch normal.
9. Ambíguo: criar watch `PENDING_REVIEW`, diagnóstico sanitizado e aviso.
10. Reimport mesmo CRC localiza import correlacionado e não cria nova atividade.
11. Atualizar overview.

## Critérios de aceite

- correlação segura aparece uma vez;
- atividade phone mantém identidade e métricas prioritárias;
- import conserva CRC/parser/metadata;
- ambiguidade não descarta nada;
- atividade somente watch continua importada;
- CRC repetido não duplica;
- calendário/listas não exibem duplicata correlacionada.

## Validação manual

Watch-only; phone-only; inícios <=60s e >60s; device igual/diferente; overlap; candidato único/ambíguo; novo import mesmo CRC; comparar calendário/detalhes.

## Comandos

```bash
./gradlew lint assembleDebug assembleRelease
./gradlew installDebug
```

## Evidências

Matriz de candidatos/decisão, contagem de atividades/imports, prioridade preservada, logs sanitizados, comandos/screenshots.

## Screenshots

Obrigatórias: atividade correlacionada única, ambiguidade pendente e atividade watch independente.

## Definição de pronto

Correlação segura/fallback funcionam sem regressão D3/D4; overview atualizado.

## Riscos e cuidados

Drift do relógio e dois treinos próximos. Não ampliar 60s silenciosamente. Não correlacionar sem candidato único.

## Rollback

Desabilitar lookup de candidato e voltar ao fluxo watch independente; não desfazer merges já persistidos sem operação de dados explícita.

Não criar, alterar ou executar testes automatizados.
