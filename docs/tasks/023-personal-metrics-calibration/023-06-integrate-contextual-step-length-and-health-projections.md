# 023-06 — Integrar comprimento contextual à derivação e às projeções

- **Status:** `pending`
- **Depends on:** `023-05`

## Objetivo

Fazer novos treinos de corrida/caminhada usarem o perfil aprendido correto sem
alterar silenciosamente atividades históricas e alinhar consumers locais/Health
Connect com a nova provenance de calibração.

## Nova versão de derivação

Introduzir versão identificável para a política contextual:

```text
watch-d3-d4-v3
```

O parser permanece `d4-v3`.

Snapshots antigos `watch-d3-d4-v2` continuam legíveis e não são migrados.

## Resolver perfil efetivo

Implementar resolução central, fora da UI:

```text
RUNNING -> learned RUNNING ?: BASE
WALKING -> learned WALKING ?: BASE
TREADMILL -> BASE
FREE_WORKOUT -> comportamento vigente
CYCLING -> sem distância por passos
DailySummary -> BASE
```

A resolução precisa ocorrer de forma coerente com o momento em que o tipo foi
confirmado, conforme decisão congelada em `023-01`.

## Importação e revisão

Evitar usar perfil contextual errado antes de `confirmedType` existir.

Direção preferida:

- importação inicial mantém derivação/base atual quando necessário;
- revisão de RUNNING/WALKING pode rederivar explicitamente com perfil contextual
  antes do sync;
- nova atividade após perfil aprendido deve terminar com snapshot que identifique
  o valor/contexto realmente usado;
- mudança de tipo não reaproveita snapshot semântico incompatível.

## Consumers locais

Auditar/adaptar:

- lista de Exercícios;
- detalhe da atividade;
- Calendário/detalhe diário;
- históricos de distância;
- splits/pace/velocidade;
- qualquer read model que assuma um único `stepLengthMeters` atual.

Todos devem consumir o snapshot congelado da atividade, nunca o perfil pessoal
corrente para recalcular histórico em leitura.

## Perfil UI

Conectar os perfis aprendidos à tela Perfil:

```text
Corrida
0,846 m por passo
2 calibrações • 8,36 km de referência

Caminhada
0,721 m por passo
1 calibração • 3,12 km de referência
```

Sem evidência:

```text
Ainda sem calibrações de corrida.
Será usado o comprimento de passo base.
```

Mostrar acesso ao histórico/correção de evidências.

## Health Connect

Auditar o contrato pós-Spec 022 para atividades diretamente calibradas por
referência.

Requisitos:

- sessão/BPM/calorias mantêm provenance vigente;
- distância/velocidade diretamente baseadas em referência digitada não podem ser
  apresentadas como GPS coletado pelo app;
- preservar IDs determinísticos, versões monotônicas e frozen payloads;
- não duplicar `DistanceRecord`;
- não quebrar operações antigas;
- não criar efeitos remotos durante rederivação/calibração;
- sync continua explícito.

Se for necessário adicionar provenance ao frozen payload, preferir evolução
retrocompatível e documentar versão/decoder. Não aumentar schema por conveniência.

Treino futuro que apenas usa perfil aprendido continua seguindo a projeção
canônica automática vigente, pois a distância daquele treino não foi digitada.

## Daily Summary

Confirmar explicitamente que a estimativa diária continua usando `BASE`.

Não substituir por walking/running learned profile porque passos do dia não têm
contexto confiável.

## Compatibilidade 022

Confirmar:

- `CYCLING` continua com `manualDistanceMeters` opcional;
- nenhum learned step profile é consumido por bicicleta;
- nenhuma `DistanceInterval`/`SpeedSeries` por passos retorna para CYCLING.

## Checklist

- [ ] derivation version nova introduzida
- [ ] resolver contextual centralizado
- [ ] RUNNING usa learned/fallback corretamente
- [ ] WALKING usa learned/fallback corretamente
- [ ] TREADMILL continua BASE
- [ ] DailySummary continua BASE
- [ ] CYCLING permanece isolado
- [ ] consumers usam snapshot congelado
- [ ] Perfil mostra learned profiles/evidências
- [ ] HC provenance auditada e implementada
- [ ] payloads antigos continuam legíveis
- [ ] sync continua explícito
- [ ] spec/overview atualizados

## Aceite

Depois de duas calibrações de corrida, um novo treino RUNNING sem distância de
referência deve usar o perfil aprendido corrente e congelá-lo em seu próprio
snapshot, permanecendo imutável quando calibrações futuras alterarem o perfil.