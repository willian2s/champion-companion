# 023-03 — Completar Perfil com altura e meta diária de passos

- **Status:** `pending`
- **Depends on:** `023-02`

## Objetivo

Transformar a tela Perfil em ponto de gestão dos dados básicos realmente usados
pelos cálculos, sem adicionar campos pessoais sem consumidor funcional.

## Altura

Adicionar edição explícita de altura:

- entrada em centímetros;
- valor opcional, positivo e finito;
- persistir no `UserProfile` existente;
- recalcular `heightSuggestionMeters` após persistência;
- não aceitar automaticamente a sugestão;
- não substituir `stepLengthProfile` atual;
- preservar `updatedAt` coerente;
- acionar o fluxo local-first de Height/Health Connect já existente somente após
  sucesso local e conforme configuração/permissões atuais.

A task deve adicionar API de repository específica para atualizar altura em vez
de reconstruir perfil na ViewModel.

## Meta diária de passos

Adicionar edição da meta já persistida em DataStore:

- valor inteiro positivo;
- usar o sanitizer/policy existente onde aplicável;
- Profile mostra a meta atual;
- salvar por `AppPreferencesRepository`/contrato existente, sem acesso direto ao
  DataStore pela UI;
- Home, Saúde e Calendário devem continuar consumindo a mesma fonte e refletir a
  nova meta naturalmente.

Não mover a meta para Room.

## Perfil base

Preservar os cards atuais:

- comprimento de passo base;
- origem `MANUAL`/`HEIGHT_SUGGESTION_ACCEPTED`;
- edição manual;
- sugestão baseada em altura.

Preparar espaço de UI/estado para os perfis aprendidos que serão conectados em
`023-05/023-06`, sem inventar valores quando não houver calibração.

## Fora do escopo

- peso (023-04);
- calibração de atividade (023-05);
- meta de peso;
- nome/nascimento/sexo;
- unidade imperial;
- zonas de FC.

## Checklist

- [ ] altura editável
- [ ] validação de altura segura
- [ ] sugestão recalculada sem aceite automático
- [ ] step length base preservado
- [ ] meta diária editável
- [ ] meta continua em DataStore
- [ ] consumers existentes refletem nova meta
- [ ] enqueue de Height reutiliza fluxo existente
- [ ] UI sem novos dados pessoais desnecessários
- [ ] spec/overview atualizados

## Aceite

Alterar altura e meta de passos, reiniciar o app e confirmar persistência. Alterar
altura não pode mudar silenciosamente o comprimento do passo já aceito.