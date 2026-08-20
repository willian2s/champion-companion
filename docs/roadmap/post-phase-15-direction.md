# Direção autoritativa após a Fase 15

- **Status:** encerrada; roadmap completo
- **Data da decisão original:** 2026-08-08
- **Última revisão:** 2026-08-20
- **Escopo:** registro histórico e encerramento da direção macro posterior à
  Fase 15

Este documento registrou a direção futura das antigas seções de Fases 16–25 de
`champion-companion-roadmap.md` e agora registra seu encerramento. O roadmap
histórico continua preservado para contexto das decisões anteriores.

As fases posteriores foram avaliadas até a conclusão da Fase 16 e encerradas em
2026-08-20 por decisão explícita de maturidade do produto. A Fase 16 foi
executada pela Spec `023-personal-metrics-calibration`; não há fase macro futura
planejável sem reabertura formal da governança.

## Regra de governança: fases e tickers são independentes

Número de fase e ticker de spec/task não possuem relação obrigatória.

- **Fase** representa uma etapa macro da evolução do produto.
- **Ticker** identifica uma spec/task executável, corretiva, evolutiva ou documental.
- Coincidências como Fase 014 + ticker `014` são conveniência histórica, não regra.
- O trabalho `021` de normalização D4 e integridade do calendário não executou nem substituiu a Fase 21 histórica.
- A Fase 16 foi executada pela Spec `023` por escolha desta entrega; isso não
  cria regra `fase == ticker`.
- Um trabalho futuro pode receber qualquer ticker disponível sem alterar a sequência das fases.
- Fases removidas não são renumeradas e seus números não são reciclados.

## Estado consolidado até a Fase 15

- Fases 0–12: concluídas conforme histórico do projeto.
- Fase 13: `closed` / `partially_delivered` / `product_reorientation`.
- Fase 14: concluída; direção watch-first, derivação D3/D4 e sincronização granular consolidada.
- Fase 15: concluída; calendário e histórico local consolidados.
- Specs/tasks independentes posteriores, incluindo `016`, `017`, `018`, `021` e `022`, não alteram essa sequência de fases.
- Spec `022` de bicicleta está concluída e não representa a antiga Fase 22 removida.

## Encerramento do roadmap

**Decisão final:** Fases 17–25 estão `closed` em 2026-08-20 pelo motivo
`product_maturity_no_further_phases`. Não serão planejadas ou executadas como
fases macro do produto.

- Fase 17: `closed` — backup e restauração não serão desenvolvidos como fase;
- Fase 18: `closed` — transição sem migração permaneceu obsoleta;
- Fase 19: `closed` — diagnóstico adicional não justifica fase futura;
- Fase 20: `closed` — revisão adicional de segurança não justifica fase futura;
- Fase 21: `closed` — testes e qualidade continuam somente quando uma task
  específica exigir;
- Fase 22: `closed` — validação adicional continua somente quando uma task
  específica exigir;
- Fase 23: `closed` — paridade formal deixou de ser objetivo;
- Fase 24: `closed` — beta e uso paralelo não serão gates;
- Fase 25: `closed` — lançamento público não é objetivo.

`closed` não significa `completed`: Fases 17–25 não recebem crédito por
entregas não realizadas. O encerramento fecha o backlog macro por decisão de
produto. Specs corretivas independentes continuam possíveis sem reabrir fases.

Código legado, migrations, schemas, dados, permissões históricas e evidências
permanecem preservados. Limpeza técnica futura exige task explícita.

## Direção de produto vigente

O Champion Companion é um aplicativo pessoal para uso com o Champion C001. Não existe objetivo de distribuição pública, beta formal, matriz de fabricantes ou lançamento comercial.

O fluxo principal permanece:

```text
Champion C001
    ↓ BLE
Champion Companion
    ↓ persistência/derivação local
Revisão
    ↓ ação explícita
Health Connect
```

Treinos iniciados pelo celular, GPS, localização de sessão, rota, mapa e `ExerciseRoute` não fazem parte da direção futura. Infraestrutura histórica existente da Fase 13 permanece preservada até trabalho explícito de limpeza; essa preservação não autoriza nova implementação ou expansão.

## Fase 16 — Perfil, métricas pessoais e calibração

**Decisão:** concluída pela Spec 023.

- **Status:** `completed`.
- **Spec executável:** `docs/specs/023-personal-metrics-calibration.md`.
- **Branch planejada:** `feat/023-personal-metrics-calibration`.
- **ADR principal:** ADR-022.

- **Validação:** concluída em dispositivo real e banco preservado; evidência em
  `docs/integration/023-07-phase-16-physical-validation.md`.
- **Limitação registrada:** migration foi exercitada em fixture v12
  reconstruída, pois não há arquivo histórico v12 original.

A auditoria confirmou que parte da antiga Fase 16 já foi entregue: altura e comprimento de passo base já existem, há sugestão baseada em altura, peso possui persistência/leitura parcial e a meta diária de passos já vive no DataStore. Portanto, a fase não recria esses contratos do zero.

Escopo vigente:

- tornar altura editável no Perfil;
- tornar meta diária de passos editável;
- permitir cadastro manual e histórico local de peso;
- preservar comprimento de passo base manual/sugerido;
- adicionar calibração por distância externa de referência para corrida/caminhada;
- manter perfis aprendidos separados para `RUNNING` e `WALKING`;
- calcular perfil aprendido por `Σ distância / Σ passos`;
- usar perfis aprendidos em novos treinos compatíveis, com fallback para BASE;
- permitir que atividade elegível usada como referência seja rederivada explicitamente antes da sincronização;
- preservar snapshots históricos e fail-closed para atividades com conflito remoto;
- manter `TREADMILL` e resumo diário usando BASE nesta fase;
- manter `CYCLING` fora da derivação por passos conforme Spec 022.

Não fazem parte da Fase 16:

- nome;
- nascimento;
- sexo;
- meta de peso;
- unidades configuráveis/imperial;
- zonas de frequência cardíaca;
- GPS, rota, mapa ou coleta de localização;
- integração direta com Adidas/Strava;
- ML/regressão de passada por pace/cadência;
- reconciliação pós-sync genérica.

Alterações de perfil/calibração nunca reescrevem silenciosamente snapshots históricos.

## Fase 17 — Backup e restauração local — `closed`

**Decisão histórica:** manter, simplificar antes do planejamento. **Encerramento
final:** não planejada por maturidade do produto.

A direção pretendida é backup manual/local dos dados do Champion Companion, sem transformar o app em serviço de sincronização entre dispositivos.

Escopo histórico originalmente previsto, não executado:

- exportação manual;
- arquivo versionado;
- checksum/integridade;
- validação antes da importação;
- restauração explícita;
- proteção contra restauração incompatível ou parcial.

Fora da direção:

- Android Auto Backup;
- device transfer automático;
- nuvem obrigatória;
- compatibilidade com banco/exportações do MotionLog;
- sincronização entre aparelhos.

`android:allowBackup="false"` e as regras atuais de backup/device transfer permanecem vigentes até decisão explícita em contrário.

## Fase 18 — Transição sem migração — `closed`

**Decisão:** removida do roadmap ativo e encerrada.

Motivo: o requisito estrutural já está resolvido. Champion Companion e MotionLog usam identidades/prefixos próprios e mantêm armazenamento/banco independentes. Não existe entrega futura que justifique uma fase de transição.

A fase não deve ser marcada como concluída artificialmente; permanece apenas como item histórico removido por obsolescência do requisito.

## Fase 19 — Diagnóstico e manutenção — `closed`

**Decisão histórica:** manter, revisar antes do planejamento. **Encerramento
final:** não planejada por maturidade do produto.

O projeto já possui infraestrutura diagnóstica, payloads persistidos e exportações seguras usados durante investigação real de D1/D3/D4 e reprocessamento. Portanto, a fase não deve recriar diagnóstico do zero.

O planejamento histórico previa auditoria e lacunas úteis para manutenção
pessoal, mas esse trabalho não será aberto como fase macro:

- versão do app;
- versão do parser;
- versão do schema Room;
- estado BLE/dispositivo;
- última importação D1 e D3/D4;
- estado resumido da fila Health Connect;
- diagnóstico sanitizado de falhas;
- exportação controlada de evidência técnica;
- ações explícitas de manutenção/reprocessamento quando houver requisito real.

Não criar plataforma extensa de logging, telemetria ou observabilidade remota.

## Fase 20 — Segurança, permissões e dados locais — `closed`

**Decisão histórica:** manter, reescrever antes do planejamento. **Encerramento
final:** não planejada por maturidade do produto.

A antiga fase de segurança e privacidade ainda é útil, mas o escopo ligado a GPS/rota e distribuição pública está obsoleto.

Direção histórica registrada, não executada como fase macro:

- revisar somente permissões realmente utilizadas;
- manter Health Connect com permissões mínimas por record type;
- manter dados de saúde no armazenamento privado do app;
- não enviar dados para servidores;
- não adicionar analytics/telemetria desnecessários;
- manter logs e diagnósticos sem payload BLE, BPM, IDs pessoais, tokens ou outros dados sensíveis;
- revisar retenção dos payloads diagnósticos locais;
- manter mecanismo explícito para apagar dados locais;
- manter opção de desabilitar/desconectar integração Health Connect conforme contratos existentes;
- revisar regras de backup/restauração em conjunto com a Fase 17;
- remover permissões/código de localização remanescentes somente por trabalho explícito de limpeza, preservando migrations e dados históricos quando necessário.

Fora da direção:

- GPS;
- rota;
- mapas;
- `ExerciseRoute`;
- política pública de distribuição como requisito de produto;
- requisitos de loja, analytics de produção ou compliance voltado a usuários externos.

## Fase 21 — Testes e qualidade — `closed`

**Decisão:** removida como fase do roadmap e encerrada.

O projeto continua podendo criar testes quando um trabalho específico justificar, mas não haverá uma fase dedicada a construir cobertura ampla, fakes BLE ou matriz de testes como requisito de conclusão do aplicativo.

O Champion Companion é pessoal e a validação prática já ocorre continuamente durante as tasks. As regras de `AGENTS.md` permanecem: testes não são etapa padrão e não devem ser criados/executados sem pedido explícito.

O ticker `021` já utilizado por uma spec corretiva é independente desta fase removida.

## Fase 22 — Validação em dispositivos reais — `closed`

**Decisão:** removida e encerrada.

Não existe requisito de matriz multi-device ou multi-fabricante. O aplicativo é pessoal, não será distribuído e a validação no dispositivo de uso ocorre continuamente durante o desenvolvimento.

Validações físicas continuam sendo usadas quando uma task específica precisar comprovar BLE, Health Connect, migration ou comportamento Android; não constituem uma fase futura separada.

A Spec `022` concluída de bicicleta é independente desta fase removida.

## Fase 23 — Paridade funcional — `closed`

**Decisão:** removida e encerrada.

MotionLog deixou de ser meta formal de paridade. Ele permanece somente como referência histórica quando útil. O Champion Companion pode divergir quando a direção atual for melhor ou quando funcionalidades antigas tiverem sido deliberadamente abandonadas, como treinos iniciados pelo celular/GPS.

O ticker `023` usado para executar a Fase 16 é independente desta fase removida.

## Fase 24 — Beta e uso paralelo — `closed`

**Decisão:** removida e encerrada.

Não haverá programa de beta ou gate formal de uso paralelo. A instalação paralela pode continuar existindo tecnicamente por application IDs diferentes, mas não é uma etapa do roadmap.

## Fase 25 — Lançamento — `closed`

**Decisão:** removida e encerrada.

Não existe objetivo de distribuição pública. Assinatura de produção, changelog de lançamento, política pública, plano de rollout/rollback e congelamento formal do MotionLog não são gates do projeto.

Builds `debug` e `release` continuam úteis como validação técnica do projeto, independentemente de publicação.

## Roadmap encerrado após esta decisão

```text
Fases concluídas ou fechadas
0–12
13 — closed / partially_delivered
14 — completed
15 — completed
16 — Perfil, métricas pessoais e calibração / Spec 023 completed

Fases fechadas por decisão de produto
17–25 — closed / product_maturity_no_further_phases
```

A remoção de uma fase não implica remoção automática de código legado. Limpeza técnica deve ser solicitada e planejada separadamente.

## Próximo passo

Não há próxima fase macro. Manutenção, correções ou limpeza técnica somente
ocorrem por decisão explícita e task independente, sem reabrir o roadmap
automaticamente.
