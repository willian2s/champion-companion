# Direção autoritativa após a Fase 15

- **Status:** vigente
- **Data da decisão:** 2026-08-08
- **Escopo:** direção macro do produto após a conclusão da Fase 15

Este documento substitui como direção futura as antigas seções de Fases 16–25 de `champion-companion-roadmap.md` quando houver divergência. O roadmap histórico continua preservado para contexto das decisões anteriores.

Nenhuma fase nova é planejada por este documento. Ele apenas registra quais fases continuam fazendo sentido, quais foram removidas e quais premissas devem orientar o planejamento posterior.

## Regra de governança: fases e tickers são independentes

Número de fase e ticker de spec/task não possuem relação obrigatória.

- **Fase** representa uma etapa macro da evolução do produto.
- **Ticker** identifica uma spec/task executável, corretiva, evolutiva ou documental.
- Coincidências como Fase 014 + ticker `014` são conveniência histórica, não regra.
- O trabalho `021` de normalização D4 e integridade do calendário não executou nem substituiu a Fase 21 histórica.
- Um trabalho futuro pode receber qualquer ticker disponível sem alterar a sequência das fases.
- Fases removidas não são renumeradas e seus números não são reciclados.

## Estado consolidado até a Fase 15

- Fases 0–12: concluídas conforme histórico do projeto.
- Fase 13: `closed` / `partially_delivered` / `product_reorientation`.
- Fase 14: concluída; direção watch-first, derivação D3/D4 e sincronização granular consolidada.
- Fase 15: concluída; calendário e histórico local consolidados.
- Specs/tasks corretivas posteriores, incluindo `016`, `017`, `018` e `021`, não alteram essa sequência de fases.

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

## Fase 16 — Perfil, metas e métricas pessoais

**Decisão:** manter, mas revisar antes do planejamento.

A antiga Fase 16 continua funcionalmente relevante, porém parte do escopo já foi entregue por trabalhos posteriores, principalmente `stepLengthMeters` persistido/editável e sugestão baseada em altura.

O planejamento futuro deve começar por auditoria do estado atual e manter somente dados com uso real no aplicativo. Itens candidatos:

- altura, quando necessária para sugestão de passada;
- peso e histórico de peso;
- meta de peso, somente se houver uso real;
- meta diária de passos;
- comprimento de passo pessoal;
- unidades;
- zonas de frequência cardíaca, somente se forem consumidas por UI/cálculos.

Nome, nascimento, sexo e outros dados pessoais não devem ser adicionados apenas porque existiam no roadmap antigo. Devem existir somente se houver requisito funcional explícito.

Alterações de perfil não podem reescrever silenciosamente snapshots históricos de treinos já derivados.

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

## Fase 23 — Paridade funcional

**Decisão:** removida.

MotionLog deixou de ser meta formal de paridade. Ele permanece somente como referência histórica quando útil. O Champion Companion pode divergir quando a direção atual for melhor ou quando funcionalidades antigas tiverem sido deliberadamente abandonadas, como treinos iniciados pelo celular/GPS.

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

Direção futura a revisar antes de planejar
16 — Perfil, metas e métricas pessoais
17 — Backup e restauração local
19 — Diagnóstico e manutenção
20 — Segurança, permissões e dados locais

Removidas do roadmap ativo
18, 21, 22, 23, 24, 25
```

A remoção de uma fase não implica remoção automática de código legado. Limpeza técnica deve ser solicitada e planejada separadamente.

## Próximo passo documental

Antes de planejar qualquer nova fase, usar este documento para auditar o estado real do código e evitar reimplementar funcionalidades já entregues por specs/tasks independentes.
