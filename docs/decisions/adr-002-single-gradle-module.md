# ADR-002 — Manter módulo Gradle único na fase atual

## Status

Aceita

## Contexto

O roadmap define início com um único módulo Gradle e posterga modularização física
até existir necessidade concreta.

Implementação atual confirma essa escolha: `settings.gradle.kts` inclui apenas `:app`.
Infraestrutura, domínio, dados e features já estão separados por pacotes dentro do
módulo Android.

Adicionar módulos agora aumentaria custo de configuração de AGP, KSP, Hilt,
testes e cache sem evidência de ganho proporcional.

## Decisão

Manter somente o módulo `:app` nesta fase.

Organização interna continuará por pacotes:

- `core/*` para infraestrutura compartilhada;
- `data/` para implementações persistentes;
- `feature/*` para superfícies funcionais.

Modularização Gradle só deverá ser reavaliada quando houver necessidade concreta,
como isolamento de build, fronteiras de dependência, ownership ou impacto real
em tempo de compilação.

## Consequências

### Positivas

- build, IDE e testes permanecem mais simples;
- configuração de Hilt, KSP e Compose fica centralizada;
- evolução inicial do app exige menos coordenação estrutural.

### Negativas

- fronteiras entre camadas não são impostas pelo Gradle;
- crescimento do módulo único pode aumentar acoplamento e tempo de build;
- futura extração de módulos exigirá refatoração planejada.

## Alternativas consideradas

- iniciar já com múltiplos módulos por camada;
- modularizar por feature desde a fundação;
- separar imediatamente `core`, `data` e `feature` em módulos distintos.
