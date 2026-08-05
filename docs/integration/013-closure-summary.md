# Resumo de encerramento da Fase 013

- **Fase:** `013`
- **Status:** `closing`
- **Resultado previsto:** `partially_delivered`
- **Motivo:** `product_reorientation`
- **Data do registro:** 2026-08-05

## Decisão de produto

Novos treinos iniciados pelo celular foram descontinuados como direção ativa.
Não continuar correções ou expansão de GPS, rota, `ExerciseRoute` ou BLE live em
sessão phone. O app deve priorizar importação D3/D4, derivação de métricas,
revisão e sincronização com Health Connect de treinos iniciados pelo relógio.

Esta decisão encerra novos gates físicos da direção phone. A desativação
operacional foi executada nesta rodada, sem remoção definitiva do legado.
Migrations, schemas, sessões, dados e evidências continuam preservados.

## Desativação operacional executada

- `PhoneWorkoutPolicy` centraliza `DISCONTINUED` e bloqueia criação, retomada,
  foreground service, ownership, GPS, rota e participação BLE de sessões phone.
- UI, navegação, back stack restaurado, Activity e intents antigos não iniciam
  seleção, preparação, countdown ou sessão ativa phone.
- Sessões históricas permanecem legíveis. `CREATED`, `ACTIVE`, `PAUSED` e
  `AWAITING_RECOVERY` exibem recovery descontinuado e permitem somente descarte
  confirmado após shutdown seguro. `FINISHING` permanece preservada e sem ação.
- `WorkoutRecordingService`, fontes de localização, coordenadores BLE e
  ownership foram mantidos para compatibilidade histórica, mas operam em modo
  fail-closed sob a política.
- Importação/revisão D3/D4, tela Dispositivo, D1, atividades existentes,
  calendário, histórico e sincronizações sem sessão phone não receberam o guard.

Estruturas deliberadamente preservadas: entidades, tabelas, colunas, migrations,
schemas, dados de sessão/atividade/rota, permissões, classes legadas, parser D3/D4,
política de distância e evidências sanitizadas.

### Validação da desativação

- `git diff --check`: PASS.
- `./gradlew lint assembleDebug assembleRelease`: PASS.
- `./gradlew installDebug`: PASS em dispositivo Galaxy S22.
- Gate A: PASS; CTA phone ausente e busca no relógio disponível.
- Gate B: PASS parcial; intent externo da Activity antiga rejeitado e destinos
  restaurados convertidos com segurança. Back stack físico não reproduzido.
- Gate C: PASS; tentativa direta de `ACTION_START` não produziu foreground,
  notificação de treino, request ativo de localização ou owner visível.
- Gate D: PASS/N/A; não havia sessão phone histórica elegível e o cenário não
  pode ser produzido com segurança sem manipular o banco.
- Gate E: PASS; Champion associado, busca no relógio executada com treino
  disponível, `inseridos 1`, `parciais 1`, `inválidos 0`, e atividade pendente
  de revisão; nenhuma sessão phone foi iniciada.
- Testes automatizados não foram criados, alterados ou executados.

## Entregas aproveitáveis

- `013-01..07`: auditoria, política documental, contratos, configuração,
  migration/schema, readiness/filtro e preparação/countdown preservados como
  entregas históricas.
- `013-09`: captura GPS, rota, segmentação e métricas implementadas e aceitas
  com o waiver documentado; dados e infraestrutura permanecem preservados, sem
  nova expansão phone.
- `013-08`: base de foreground service, sessão persistida, ownership,
  recuperação, notificações e compensações parcialmente implementadas; não
  houve aceite integral da subtarefa.
- `013-10`: política e superfície sem GPS para treino livre/esteira parcialmente
  entregues; o handoff de esteira permaneceu bloqueado.
- `013-11`: ownership BLE, participação explícita, cleanup e preservação da
  sessão em desconexão parcialmente entregues; métricas live continuam sem
  origem esportiva comprovada.
- Migrations, schemas, contratos, fila Health Connect, dados existentes,
  referências de baseline e evidências sanitizadas são infraestrutura
  potencialmente reutilizável, não autorização de continuação phone.

## Incompletas

- `013-08` permanece com bloqueios físicos e evidências incompletas de CAS,
  Room, relógio civil, tipos FGS dinâmicos e demais itens registrados em seus
  documentos de integração.
- `013-10` não comprovou sessão ativa de esteira, pause/resume e screenshots
  correspondentes.
- `013-11` registrou divergência de estado após desconexão; reconexão, conflito,
  rollover, recuperação, handoff e screenshots permanecem sem aceite.
- A desativação operacional está implementada, mas `013-17` aguarda revisão
  independente e evidência manual dos gates D/E antes de ser concluída.

Incompleto não significa aprovado. Os bloqueios permanecem riscos residuais e
não devem ser reabertos como backlog ativo da Fase 013.

## Canceladas

- `013-08`, `013-10` e `013-11`: status `cancelled` por reorientação, com
  execução, riscos, bloqueios e evidências físicas preservados.
- `013-12..16`: status `cancelled`; não executadas ou não concluídas. Não há
  alegação de finalização phone, correlação, `ExerciseRoute`, integração de
  superfícies ou gate físico integral.

O refinamento futuro de importação, derivação, revisão e Health Connect para
treinos do relógio será redefinido conceitualmente na Fase 014. WorkManager
permanece somente detalhe técnico de tarefas finitas e adiáveis.

## Riscos residuais

- classes e consumidores legados phone ainda existem no código por preservação;
  devem permanecer protegidos pelo guard e não podem ser tratados como novas
  entradas de produto;
- O gate histórico D foi encerrado como não aplicável por ausência de sessão;
  consumidores legados phone ainda existem no código por preservação e devem
  permanecer protegidos pelo guard central;
- dados de rota/sessão existentes exigem retenção e compatibilidade até task
  explícita de remoção;
- evidências físicas inconclusivas não podem ser usadas como prova de sucesso;
- protocolo esportivo live não comprovado mantém BPM/passos live ausentes;
- distância canônica futura de treinos do relógio depende de passos válidos e
  comprimento de passada pessoal; distância original do relógio fica
  diagnóstica;
- Health Connect granular para a direção do relógio ainda exige decisão,
  implementação e validação próprias;
- `Phase003PrivacyGuardTest` continua dívida conhecida, sem alteração,
  contorno ou execução.

## Evidências preservadas

- `docs/integration/013-08-evidence.md` e
  `docs/integration/013-08-evidence/` preservam dumps e capturas sanitizados;
- `docs/integration/013-08-foreground-service-recovery-follow-up.md` preserva
  bloqueios de CAS, Room, relógio civil, insets e tipos FGS;
- `docs/integration/013-11-evidence/` preserva conexão, desconexão, estado stale,
  handoff e ausência segura de BPM/passos;
- históricos das tasks, overview, spec, baseline, ADRs, migrations, schemas e
  dados não foram apagados ou reclassificados;
- os documentos de integração `013-08-*` e `013-11-*` não foram alterados neste
  fechamento documental.

## Decisão sobre novos gates físicos

Encerrar novos gates físicos de treinos iniciados pelo celular na Fase 013.
Bloqueios e resultados existentes permanecem evidência histórica; não recebem
waiver implícito nem são convertidos em aprovação. Gates futuros devem pertencer
à direção redefinida de treinos iniciados pelo relógio e ter autorização
explícita, sem reabrir automaticamente a Fase 013.
