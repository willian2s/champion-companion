# 013-13 — Associar importações D3/D4 e suprimir duplicatas

- **Ticker:** `013`
- **Número:** `13`
- **Status:** pending

## Objetivo e resultado esperado

Associar importação posterior do relógio à atividade phone compatível sem criar
atividade duplicada nem fazer merge de métricas. Resultado: atividade phone
permanece única e dona de sessão, distância, calorias, tempo e intervalos; o
relógio pode fornecer somente BPM real elegível, enquanto `WatchWorkoutImport`
fica retido para proveniência e idempotência.

## Contexto

Dedup por CRC e fallback por início já existem para watch. Nova associação ocorre
depois da chave CRC e antes de criar nova atividade. D3/D4 sem candidato phone
deve permanecer funcional como atividade independente.

## Documentos obrigatórios

Spec 013; specs 010/011; ADR-011/014; `013-03/05/11/12`; protocolo D3/D4.

## Dependências

`013-12` concluída. Pode avançar em paralelo com 013-14, evitando `RoomRepositories` simultâneo.

## Arquivos permitidos

`ChampionWatchWorkoutImportCoordinator.kt`; parser adapters sem alterar protocolo; repositories/DAOs/mappers de import/correlação; domínio de associação; UI de resultado/revisão; logging seguro; task/overview.

## Arquivos e símbolos prováveis

`ChampionWatchWorkoutImportCoordinator`, `WorkoutActivityRepository.persistImported`, `WatchWorkoutImportLookup`, query de candidatos phone, `WorkoutCorrelationDecision` e associação transacional em `RoomRepositories`.

## Arquivos proibidos

GPS capture/filter, FGS, Health Connect route, A8/D8, testes.

## Requisitos cobertos

Regra 60s; device/sobreposição/candidato único; supressão; ambiguidade; reimport CRC; BPM como única contribuição do relógio; atividade somente watch.

## Escopo

- CRC lookup primeiro;
- candidatos phone não descartados sem import associado;
- `abs(start)<=60 s`, sobreposição relevante e device quando associado;
- associação técnica do import sem merge de métricas;
- somente BPM real elegível, com dedup de amostras real-time versus D4;
- nenhuma cópia de passos, calorias, distância, tempo, tipo, ritmo, velocidade, intervalos ou rota do relógio;
- ausência de dado phone não autoriza fallback para métrica do relógio;
- estado correlacionado seguro para UI.

## Fora do escopo

Autoimportação, iniciar watch sport, substituir rota/GPS e sync remoto.

## Passos de implementação

1. Preservar caminho de reimport por CRC existente.
2. Consultar candidatos phone antes da criação watch nova.
3. Aplicar janela, device, overlap, não descartada e unicidade.
4. Candidato único: transação vincula `WatchWorkoutImport` à atividade phone e não cria atividade watch.
5. Associar somente BPM real elegível conforme janela temporal e origem segura.
6. Deduplicar HR por timestamp/sessão/atividade com tolerância contratual definida, sem apagar amostra válida.
7. Nunca substituir start/end/type phone, GPS, rota, distância, ritmo, velocidade, calorias ou intervalos.
8. Sem candidato: fluxo watch normal.
9. Ambíguo: criar watch `PENDING_REVIEW`, diagnóstico sanitizado e aviso.
10. Reimport mesmo CRC localiza import correlacionado e não cria nova atividade.
11. Atualizar overview.

## Critérios de aceite

- correlação segura aparece como uma única atividade phone;
- atividade phone mantém identidade e todas as métricas prioritárias, exceto eventual BPM real associado;
- import conserva CRC/parser/metadata;
- nenhum intervalo, passo, caloria, distância, tempo ou agregado do relógio é copiado para atividade phone;
- associação sem BPM elegível ainda suprime atividade duplicada e conserva CRC;
- ambiguidade não descarta nada;
- atividade somente watch continua importada;
- CRC repetido não duplica;
- calendário/listas não exibem duplicata correlacionada.

## Validação manual

Watch-only; phone-only; inícios <=60s e >60s; device igual/diferente; overlap; candidato único/ambíguo; novo import mesmo CRC; verificar que somente BPM pode ser associado; comparar calendário/detalhes.

## Comandos

```bash
./gradlew lint assembleDebug assembleRelease
./gradlew installDebug
```

## Evidências

Matriz de candidatos/decisão, contagem de atividades/imports, campos phone preservados, BPM associado quando elegível, zero métricas D3/D4 copiadas, logs sanitizados, comandos/screenshots.

## Screenshots

Obrigatórias: atividade correlacionada única, ambiguidade pendente e atividade watch independente.

## Definição de pronto

Associação phone-first e fallback independente funcionam sem regressão D3/D4; somente BPM pode enriquecer atividade phone; overview atualizado.

## Riscos e cuidados

Drift do relógio, dois treinos próximos e divergência de amostras BPM. Não ampliar 60s silenciosamente, não copiar métricas watch por ausência phone e não associar sem candidato único.

## Rollback

Desabilitar lookup de candidato e voltar ao fluxo watch independente; não desfazer associações já persistidas sem operação de dados explícita.

Não criar, alterar ou executar testes automatizados.

## Atualização de planejamento — 2026-08-03

- **Status:** pending; planejamento ajustado, implementação não iniciada.
- **Arquivos alterados:** esta subtarefa, `013-14`, `013-00-overview.md`,
  spec 013, ADR-014, ADR-012, spec 012 e baseline de mapeamento Health Connect.
- **Decisões e desvios:** substituído merge de métricas por associação técnica
  phone-first. Candidato único não cria atividade watch; `WatchWorkoutImport`
  permanece para CRC/proveniência/idempotência. Somente BPM real elegível pode
  enriquecer atividade phone. Passos, calorias, distância, tempo, tipo, ritmo,
  velocidade, intervalos e rota do relógio são suprimidos.
- **Comandos executados:** `git diff --check`.
- **Resultados e evidências:** passou; diff documental confirma fonte phone
  exclusiva, supressão sem BPM e ausência de alteração no checklist/progresso.
  Nenhum build ou teste automatizado é pertinente a esta alteração de plano.
- **Riscos residuais:** semântica de associação BPM, retenção do import e
  reabertura de operação `HEART_RATE` após sync exigem implementação e gate
  físico. Ambiguidade continua fallback independente; janela de 60 s não foi
  ampliada.
