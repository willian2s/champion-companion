# 014-06 — Persistir e editar comprimento pessoal do passo

- **Ticker:** `014`
- **Número:** `06`
- **Status:** pending

## Objetivo e resultado esperado

Permitir valor manual e sugestão baseada na altura, com confirmação explícita,
persistindo `stepLengthMeters`, origem e atualização. Resultado: perfil fornece
um valor fixo elegível sem alterar atividades antigas.

## Contexto

`UserProfile` persiste somente altura. `DailySummaryPolicy` calcula
`strideLengthMeters` por `altura × 0,415`, mas não registra aceite nem snapshot
para treino.

## Requisitos cobertos

- R2 e R9;
- terminologia normativa da spec.

## Dependências

- `014-03` concluída.

## Arquivos permitidos

- contratos de perfil em `core/model`;
- entidades/DAO/mappers/repository de perfil criados em `014-03`;
- `feature/profile` e navegação já existente para perfil;
- recursos de texto relacionados;
- overview.

Símbolos prováveis: `UserProfile`, `PersonalMetricsSnapshot`, repositório de
perfil, `ProfileViewModel`/tela equivalente e `StepLengthSource`.

## Arquivos proibidos

- parser D4, derivação de atividades, Health Connect writer;
- Gradle, manifesto, migrations e schemas;
- UI phone, GPS, rota e Fase 013.

## Escopo

- entrada manual validada;
- sugestão por altura como proposta não persistida;
- confirmação e origem `HEIGHT_SUGGESTION_ACCEPTED`;
- ausência segura e edição futura.

## Fora do escopo

- fluxo de `REFERENCE_DISTANCE_CALIBRATION`;
- passada variável por ritmo/cadência;
- recalcular atividades existentes;
- sincronizar passo pessoal com Health Connect;
- testes automatizados.

## Passos de implementação

1. Expor `stepLengthMeters` e origem no contrato/repositório de perfil.
2. Definir limites de validação coerentes e erro de UI seguro.
3. Mostrar sugestão derivada da altura sem salvá-la automaticamente.
4. Exigir confirmação explícita para salvar sugestão.
5. Salvar entrada manual com origem `MANUAL`.
6. Usar texto **Comprimento do passo**; não introduzir `stride` em contrato novo.
7. Garantir que edição apenas atualize perfil.
8. Atualizar overview.

## Critérios de aceite

- Valor finito/positivo manual persiste e reabre corretamente.
- Sugestão não aceita não muda perfil.
- Sugestão aceita registra origem correta.
- Sem altura, sugestão permanece indisponível sem default.
- `0,8818 m` não aparece como constante/fallback.
- Atividades antigas não mudam após edição.

## Validação manual

- Salvar manual, reiniciar app e conferir valor/origem.
- Gerar sugestão com altura, cancelar e confirmar ausência de mudança.
- Aceitar sugestão e conferir persistência.
- Editar perfil com atividades existentes e confirmar métricas congeladas.

## Comandos permitidos

```bash
./gradlew lint assembleDebug assembleRelease
./gradlew installDebug
git diff --check
git status --short
```

Não criar nem executar testes automatizados.

## Evidências

- screenshots da mudança visual;
- roteiro manual com origens e persistência;
- builds e reinício do app.

## Definição de pronto

Perfil editável e confirmado, sem efeito retroativo, screenshots registradas e
item `014-06` marcado.

## Riscos e cuidados

- Fórmula de altura existente pode ser chamada de `stride`; adaptar terminologia
  sem alterar silenciosamente estimativas diárias fora da task.
- Passo pessoal é dado sensível: não logar nem usar em ID.
- Unidade de UI precisa converter sem perda para metros persistidos.

## Rollback

Ocultar edição/sugestão e parar novas gravações, preservando valores já
persistidos como dados desconhecidos até migration forward.

## Atualização obrigatória do overview

Atualizar `014-00-overview.md`, marcar `014-06` e registrar fórmula/política de
sugestão efetivamente adotada.
