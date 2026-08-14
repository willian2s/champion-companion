# Direção autoritativa após a Fase 15

- **Status:** vigente
- **Data da decisão original:** 2026-08-08
- **Última revisão:** 2026-08-12
- **Escopo:** direção macro do produto após a conclusão da Fase 15

Este documento substitui como direção futura as antigas seções de Fases 16–25 de `champion-companion-roadmap.md` quando houver divergência. O roadmap histórico continua preservado para contexto das decisões anteriores.

As fases posteriores são planejadas somente quando houver auditoria do estado real e decisão explícita. A Fase 16 já passou por essa auditoria e possui plano executável na Spec `023-personal-metrics-calibration`.

## Regra de governança: fases e tickers são independentes

Número de fase e ticker de spec/task não possuem relação obrigatória.

- **Fase** representa uma etapa macro da evolução do produto.
- **Ticker** identifica uma spec/task executável, corretiva, evolutiva ou documental.
- Coincidências como Fase 014 + ticker `014` são conveniência histórica, não regra.
- O trabalho `021` de normalização D4 e integridade do calendário não executou nem substituiu a Fase 21 histórica.
- A Fase 16 será executada pela Spec `023` por escolha desta entrega; isso não cria regra `fase == ticker`.
- Um trabalho futuro pode receber qualquer ticker disponível sem alterar a sequência das fases.
- Fases removidas não são renumeradas e seus números não são reciclados.

## Estado consolidado até a Fase 15

- Fases 0–12: concluídas conforme histórico do projeto.
- Fase 13: `closed` / `partially_delivered` / `product_reorientation`.
- Fase 14: concluída; direção watch-first, derivação D3/D4 e sincronização granular consolidada.
- Fase 15: concluída; calendário e histórico local consolidados.
- Specs/tasks independentes posteriores, incluindo `016`, `017`, `018`, `021` e `022`, não alteram essa sequência de fases.
- Spec `022` de bicicleta está concluída e não representa a antiga Fase 22 removida.

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

## Fase 17 — Backup e restauração local

**Decisão:** manter, simplificar antes do planejamento.

A direção pretendida é backup manual/local dos dados do Champion Companion, sem transformar o app em serviço de sincronização entre dispositivos.

Escopo futuro provável:

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

## Fase 18 — Transição sem migração

**Decisão:** removida do roadmap ativo.

Motivo: o requisito estrutural já está resolvido. Champion Companion e MotionLog usam identidades/prefixos próprios e mantêm armazenamento/banco independentes. Não existe entrega futura que justifique uma fase de transição.

A fase não deve ser marcada como concluída artificialmente; permanece apenas como item histórico removido por obsolescência do requisito.

## Fase 19 — Diagnóstico e manutenção

**Decisão:** manter, revisar antes do planejamento.

O projeto já possui infraestrutura diagnóstica, payloads persistidos e exportações seguras usados durante investigação real de D1/D3/D4 e reprocessamento. Portanto, a fase não deve recriar diagnóstico do zero.

O planejamento futuro deve começar por auditoria e completar somente lacunas úteis para manutenção pessoal, como:

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

## Fase 20 — Segurança, permissões e dados locais

**Decisão:** manter, reescrever antes do planejamento.

A antiga fase de segurança e privacidade ainda é útil, mas o escopo ligado a GPS/rota e distribuição pública está obsoleto.

Direção futura:

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

## Fase 21 — Testes e qualidade

**Decisão:** removida como fase do roadmap.

O projeto continua podendo criar testes quando um trabalho específico justificar, mas não haverá uma fase dedicada a construir cobertura ampla, fakes BLE ou matriz de testes como requisito de conclusão do aplicativo.

O Champion Companion é pessoal e a validação prática já ocorre continuamente durante as tasks. As regras de `AGENTS.md` permanecem: testes não são etapa padrão e não devem ser criados/executados sem pedido explícito.

O ticker `021` já utilizado por uma spec corretiva é independente desta fase removida.

## Fase 22 — Validação em dispositivos reais

**Decisão:** removida.

Não existe requisito de matriz multi-device ou multi-fabricante. O aplicativo é pessoal, não será distribuído e a validação no dispositivo de uso ocorre continuamente durante o desenvolvimento.

Validações físicas continuam sendo usadas quando uma task específica precisar comprovar BLE, Health Connect, migration ou comportamento Android; não constituem uma fase futura separada.

A Spec `022` concluída de bicicleta é independente desta fase removida.

## Fase 23 — Paridade funcional

**Decisão:** removida.

MotionLog deixou de ser meta formal de paridade. Ele permanece somente como referência histórica quando útil. O Champion Companion pode divergir quando a direção atual for melhor ou quando funcionalidades antigas tiverem sido deliberadamente abandonadas, como treinos iniciados pelo celular/GPS.

O ticker `023` usado para executar a Fase 16 é independente desta fase removida.

## Fase 24 — Beta e uso paralelo

**Decisão:** removida.

Não haverá programa de beta ou gate formal de uso paralelo. A instalação paralela pode continuar existindo tecnicamente por application IDs diferentes, mas não é uma etapa do roadmap.

## Fase 25 — Lançamento

**Decisão:** removida.

Não existe objetivo de distribuição pública. Assinatura de produção, changelog de lançamento, política pública, plano de rollout/rollback e congelamento formal do MotionLog não são gates do projeto.

Builds `debug` e `release` continuam úteis como validação técnica do projeto, independentemente de publicação.

## Roadmap ativo após esta decisão

```text
Fases concluídas
0–12
13 — closed / partially_delivered
14 — completed
15 — completed
16 — Perfil, métricas pessoais e calibração / Spec 023 completed

Direção futura a revisar antes de planejar
17 — Backup e restauração local
19 — Diagnóstico e manutenção
20 — Segurança, permissões e dados locais

Removidas do roadmap ativo
18, 21, 22, 23, 24, 25
```

A remoção de uma fase não implica remoção automática de código legado. Limpeza técnica deve ser solicitada e planejada separadamente.

## Próximo passo

Antes de planejar Fase 17, auditar estado real, simplificar escopo e registrar
decisão explícita. Não reexecutar `023-01`: auditoria, implementação e validação
da Fase 16 já estão encerradas pela Spec 023.
