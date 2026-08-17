# 024-15 — Refatorar Perfil

- **Ticker:** `024`
- **Número:** `15`
- **Status:** pending

## Objetivo e resultado esperado

Refatorar Perfil para organizar métricas pessoais, passo base, perfis aprendidos
e histórico de calibração. Resultado: edição e provenance compreensíveis sem
reescrever snapshots ou misturar RUNNING/WALKING.

## Requisitos e critérios cobertos

- R4–R9; critérios 5, 7–12 da spec.

## Escopo incluído

- altura, meta diária e comprimento de passo;
- sugestão por altura e confirmação;
- perfis aprendidos RUNNING/WALKING;
- histórico, correção/remoção de calibração, mensagens e dialogs;
- abertura de atividade relacionada.

## Escopo excluído

- novos campos pessoais, unidade imperial ou regra de calibração;
- alteração de repository, snapshots, reprocessamento ou sync.

## Dependências

- `024-14` concluída.

## Arquivos e símbolos prováveis

- `feature/profile/ProfileScreen.kt`;
- `ProfileScreen`, editores, cards de perfil e histórico;
- `ProfileViewModel` somente como referência.

## Passos de implementação

1. Priorizar métricas editáveis e passo base atual.
2. Separar perfis aprendidos por modalidade com origem/data.
3. Organizar histórico e ações de correção/remoção sem perder contexto.
4. Preservar sugestões, confirmações, mensagens e abertura de atividade.
5. Validar cancelamento de edição e dialogs sem persistir dado artificial.
6. Conferir fonte ampliada, teclado, landscape e conteúdo longo.
7. Registrar evidências e overview.

## Testes e comandos de validação

Sem testes automatizados.

```bash
git diff --check
./gradlew lint assembleDebug assembleRelease
./gradlew installDebug
adb devices -l
adb shell am start -n br.com.mwconsulting.championcompanion.dev/br.com.mwconsulting.championcompanion.MainActivity
```

Smoke: abrir/fechar editores, cancelar dialogs, abrir atividade, voltar; não
salvar valor apenas para screenshot.

## Definição de pronto

- métricas, passo base, perfis e histórico têm hierarquia clara;
- RUNNING/WALKING permanecem independentes;
- snapshots e dados históricos não são reescritos;
- adaptação/teclado validados;
- comandos, capturas sanitizadas e overview registrados.

## Riscos e cuidados

- dados antropométricos e calibrações são pessoais;
- remover calibração pode ser destrutivo;
- copy de origem não pode prometer reprocessamento automático;
- não alterar regras da Spec 023.
