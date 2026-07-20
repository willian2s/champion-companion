# Task 001-08 — Configurar Variantes

## Status

Pendente.

## Dependência

- `001-03` concluída.

## Objetivo

Separar debug e release sem alterar ID produção.

## Pode modificar

- `app/build.gradle.kts`
- Recursos específicos variante.
- Testes instrumentados afetados.
- Este arquivo.

## Passos

1. Definir `debug` com `applicationIdSuffix ".dev"` e versão `-dev`.
2. Definir nome debug `Champion Companion Dev` via recurso variante.
3. Manter ID, nome e versão release atuais; sem signingConfig produção.
4. Rodar `assembleDebug` e `assembleRelease`.

## Aceite

- Debug instala ao lado release.
- Nenhum segredo ou assinatura produção adicionada.
