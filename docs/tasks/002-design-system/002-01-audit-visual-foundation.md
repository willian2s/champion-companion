# Task 002-01 — Auditar Fundação Visual

## Status

Pendente.

## Dependência

- `002-00` lida.

## Objetivo

Inventariar tema, recursos e cores existentes; registrar linha de base e exclusão explícita de identidade MotionLog.

## Pode modificar

- Este arquivo.

## Passos

1. Inventariar `ui/theme`, `AndroidManifest.xml`, recursos `drawable`, `mipmap`, `values` e telas Compose existentes.
2. Registrar Material 3/Material 2, esquema dinâmico, cores literais, fontes, ícones e recursos gerados presentes.
3. Buscar referências a MotionLog, cores/tokens copiados e nomes de cores acoplados a feature.
4. Registrar arquivos que serão migrados por `002-02`, `002-07` e `002-08`; não os alterar nesta task.
5. Registrar `git status --short` pré-existente e preservar alterações alheias.

## Aceite

- Inventário reproduzível, com caminhos e riscos de migração.
- Origem independente da paleta azul-petróleo registrada; nenhuma referência MotionLog aprovada.
