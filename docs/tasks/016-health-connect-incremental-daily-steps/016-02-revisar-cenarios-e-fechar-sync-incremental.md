# Task 016-02 — Revisar cenários e fechar sync incremental

## Status

pending

## Objetivo

Revisar independentemente a implementação 016-01 contra os requisitos de
imutabilidade, concorrência, retry, novo dia e compatibilidade legada, sem
alterar código salvo achado novo e materialmente bloqueante.

## Resultado esperado

Cada cenário relevante possui evidência estática ou manual explicitamente
classificada; documentação final não declara testes ou validações não
executados.

## Escopo incluído

- revisão do diff e da migration;
- inspeção da tabela de cenários da spec;
- comandos de build permitidos;
- atualização da spec, subtarefa 016-01, esta subtarefa e overview;
- correção única se revisão encontrar falha material nova.

## Escopo excluído

- refatoração oportunista;
- novos testes;
- limpeza remota ou leitura do Health Connect;
- mudanças em BPM, exercícios, peso, altura, UI, BLE ou `DailySummary`.

## Dependências

- `016-01` concluída e documentação preenchida.

## Arquivos e símbolos prováveis

- diff de `016-01`;
- spec e overview 016;
- ADR-015;
- entities/DAOs/mappers/repository/coordinator tocados pela implementação.

## Passos de revisão

1. Confirmar que só uma subtarefa foi executada e que `DailySummary` continua
   agregado.
2. Verificar primeira operação, crescimentos sucessivos, igualdade, redução,
   zero, janela inválida e novo dia.
3. Verificar transação, concorrência, ordinal, ausência de sobreposição e
   derivação persistida após reinício.
4. Verificar payload, ID, versão e offsets constantes no retry.
5. Verificar ponte legada sem mistura de cumulativo e incremental.
6. Verificar que mapper não consulta total mutável para count.
7. Verificar preservação de outros records e ausência de testes alterados.
8. Executar comandos permitidos e registrar evidências e riscos residuais.

## Testes e comandos de validação

Não criar, alterar ou executar testes automatizados.

```bash
git diff --check
./gradlew lint assembleDebug assembleRelease
```

Validação manual física somente deve ser registrada se realmente executada.

## Definição de pronto

- cenários e critérios da spec estão classificados;
- riscos e desvios reais estão documentados;
- overview marca somente subtarefas realmente concluídas;
- não há alegação de evidência inexistente;
- build e diff check possuem resultado registrado.

## Riscos e cuidados

- Inspeção estática não prova comportamento do provider.
- Estado remoto anterior pode exigir limpeza manual fora do app.
- Não iniciar próxima implementação automaticamente.
