# Fase 16 — Direção de perfil, métricas pessoais e calibração

- **Fase:** 16
- **Status:** planejada
- **Spec executável:** `023-personal-metrics-calibration`
- **Base auditada:** `main` após conclusão da Spec 022

## Regra de governança

Fase e ticker são conceitos independentes. A Fase 16 será executada pela Spec
`023` por conveniência desta entrega; isso não cria regra `fase == ticker`.

## Estado real antes da fase

A auditoria da `main` confirma que parte da antiga Fase 16 já existe:

- `UserProfile` persiste altura e um `StepLengthProfile` base;
- o perfil já permite editar comprimento de passo manualmente;
- já existe sugestão de comprimento do passo baseada em altura, com aceite
  explícito;
- `StepLengthSource.REFERENCE_DISTANCE_CALIBRATION` já existe no domínio, mas
  não possui fluxo funcional;
- peso já possui entidade persistida, leitura do último valor e escrita para o
  Health Connect, porém cadastro/edição/histórico ainda não foram entregues na
  UI;
- a meta diária de passos já existe em `AppPreferences`/DataStore e é consumida
  por Home/Saúde/Calendário, mas não possui gestão completa pelo usuário;
- o banco está em Room v12 após a Spec 022;
- treinos do relógio usam snapshots imutáveis de derivação e não podem ser
  reescritos silenciosamente por mudanças posteriores de perfil.

## Objetivo revisado da Fase 16

Transformar o perfil atual em uma fonte pessoal realmente útil para os cálculos
do app e implementar calibração progressiva de comprimento do passo com
distâncias externas de referência, sem trazer GPS para dentro do Champion
Companion.

A fase deve permitir:

1. editar altura;
2. editar meta diária de passos;
3. registrar peso e consultar histórico local;
4. manter comprimento de passo base manual/sugerido;
5. aprender comprimentos de passo separados para corrida e caminhada a partir
   de atividades D3/D4 calibradas com distância de referência;
6. usar o perfil aprendido somente em treinos futuros compatíveis;
7. calibrar explicitamente a atividade usada como referência quando ela ainda
   puder ser alterada com segurança;
8. preservar snapshots e sincronizações históricas.

## Distância de referência

O Champion Companion não coleta GPS nesta fase.

A distância é digitada pelo usuário a partir de uma fonte externa confiável,
por exemplo:

- Adidas Running;
- Strava;
- GPS de outro relógio/dispositivo;
- ciclocomputador quando aplicável a outro fluxo;
- percurso medido conhecido.

Para corrida/caminhada a evidência de calibração é:

```text
observedStepLengthMeters = referenceDistanceMeters / decodedSteps
```

A distância de referência deve usar a soma dos `decodedSteps` elegíveis da
série D4, não um contador agregado incompatível.

## Aprendizado progressivo

Corrida e caminhada não compartilham o mesmo perfil aprendido.

Contextos previstos:

```text
BASE
RUNNING
WALKING
```

`BASE` continua sendo o perfil atual manual ou aceito pela sugestão de altura e
permanece fallback.

Os perfis aprendidos são calculados de forma determinística usando todas as
calibrações ativas do mesmo contexto:

```text
learnedStepLengthMeters = Σ referenceDistanceMeters / Σ decodedSteps
```

Isso equivale a ponderar naturalmente cada evidência pela quantidade de passos e
evita média simples de treinos curtos e longos.

Regras de resolução:

```text
RUNNING -> perfil RUNNING aprendido, se existir; senão BASE
WALKING -> perfil WALKING aprendido, se existir; senão BASE
TREADMILL -> BASE nesta fase
FREE_WORKOUT -> comportamento atual preservado
CYCLING -> política própria da Spec 022; nunca usar passos para distância
resumo diário -> BASE
```

A fase não deve inferir gait de `TREADMILL` nem misturar corrida/caminhada sem
requisito explícito.

## Atividade usada para calibrar

Quando uma atividade D3/D4 de corrida/caminhada ainda for elegível para alteração
local segura, o usuário pode aplicar a distância de referência à própria
atividade.

Nesse caso:

```text
stepLengthMetersUsed = referenceDistanceMeters / Σ decodedSteps
stepLengthSource = REFERENCE_DISTANCE_CALIBRATION
```

A nova derivação distribui a distância total pelos intervalos de acordo com os
passos D4 e mantém coerentes distância, velocidade, pace, acumulado, splits e
projeções futuras.

A mesma evidência entra no perfil aprendido do tipo correspondente.

Atividades já `SYNCED` ou com efeitos remotos parciais não são reescritas
silenciosamente. A fase não abre reconciliação pós-sync genérica.

## Histórico de calibração

Cada atividade pode contribuir no máximo uma calibração ativa para evitar peso
duplicado no aprendizado.

A evidência deve preservar pelo menos:

- activityId;
- contexto `RUNNING` ou `WALKING`;
- distância de referência em metros;
- passos D4 usados;
- comprimento observado calculado;
- instante de aceite.

O usuário deve conseguir corrigir/remover uma evidência antes de ela permanecer
como fonte permanente do perfil aprendido. Remover ou substituir evidência
recalcula o perfil futuro, mas não reescreve snapshots históricos.

## Perfil e peso

### Altura

- editar em centímetros;
- valor positivo e finito;
- alteração recalcula somente a sugestão de passo;
- não substitui automaticamente o comprimento de passo salvo;
- quando Health Connect estiver habilitado, reutilizar o fluxo write-only já
  existente para altura.

### Peso

- registrar nova medição manual em quilogramas;
- preservar histórico temporal;
- mostrar último peso e lista/histórico local;
- reutilizar o fluxo de sincronização write-only já existente;
- não criar meta de peso nesta fase.

### Meta diária de passos

- continuar em DataStore;
- edição explícita pelo usuário;
- valor positivo;
- consumidores existentes passam a refletir o novo valor sem migration Room.

## Fora do escopo

- nome;
- data de nascimento;
- sexo;
- meta de peso;
- preferências de unidades/imperial;
- zonas de frequência cardíaca;
- cálculo de FC máxima;
- GPS, localização, rota ou mapa;
- leitura de distância do Health Connect;
- importação automática de Adidas/Strava;
- regressão/ML para passada por pace, cadência ou inclinação;
- atualização silenciosa de atividades antigas;
- reconciliação genérica de atividades já sincronizadas;
- testes automatizados novos ou execução de suites sem pedido explícito.

## Critério macro de encerramento

A Fase 16 pode ser considerada concluída quando o perfil pessoal tiver gestão
funcional de altura, peso, meta diária de passos e comprimento de passo, e quando
corridas/caminhadas puderem acumular evidências explícitas de distância de
referência para melhorar deterministicamente os cálculos de treinos futuros sem
GPS.