# Fixtures Progressivas Do Protocolo Champion C001

## Objetivo

Guardar amostras reais de payloads BLE do Champion C001 para validar parsers, comandos e conversoes futuras do Champion Companion.

Estas fixtures sao progressivas. A Fase 0 nao exige amostras reais completas.

## Padrao De Nomes

Cada cenario deve usar tres arquivos com o mesmo prefixo:

```text
<fluxo>-<cenario>.hex
<fluxo>-<cenario>-expected.json
<fluxo>-<cenario>-notes.md
```

Exemplos:

```text
a1-handshake-sucesso.hex
a1-handshake-sucesso-expected.json
a1-handshake-sucesso-notes.md

d1-historico-bpm-um-dia.hex
d1-historico-bpm-um-dia-expected.json
d1-historico-bpm-um-dia-notes.md

d3-lista-treinos-completa.hex
d3-lista-treinos-completa-expected.json
d3-lista-treinos-completa-notes.md

d4-treino-corrida-valido.hex
d4-treino-corrida-valido-expected.json
d4-treino-corrida-valido-notes.md
```

## Arquivo `.hex`

O arquivo `.hex` deve conter payload bruto em hexadecimal, sem interpretacao manual.

Regras:

- uma linha por pacote ou frame capturado;
- bytes separados por espaco;
- letras hexadecimais em minusculo;
- sem prefixo `0x`;
- sem comentarios inline.

Exemplo:

```text
a1 00 01
d1 04 10 00
```

## Arquivo `-expected.json`

O arquivo `-expected.json` deve conter o objeto esperado depois do parser.

Regras:

- usar JSON valido;
- registrar unidades explicitamente nos nomes quando necessario;
- usar `null` quando valor existir no contrato mas nao estiver presente;
- nao preencher campos por inferencia sem registrar isso no arquivo de notas.

Exemplo:

```json
{
  "flow": "d1",
  "scenario": "historico-bpm-um-dia",
  "records": [
    {
      "timestamp": "2026-07-20T10:00:00-03:00",
      "bpm": 72
    }
  ]
}
```

## Arquivo `-notes.md`

O arquivo `-notes.md` deve registrar contexto da captura.

Campos recomendados:

- fluxo;
- cenario;
- data da captura;
- origem da captura;
- dispositivo;
- firmware do relogio, se conhecido;
- app usado na captura;
- branch e commit do MotionLog, quando aplicavel;
- comando enviado;
- characteristic usada;
- resposta observada;
- status da validacao;
- dados removidos por privacidade;
- lacunas e duvidas.

## Dados Sensiveis

Nao registrar:

- nome real do usuario;
- identificadores pessoais;
- latitude;
- longitude;
- rotas;
- polylines;
- coordenadas brutas;
- enderecos;
- chaves, tokens ou dados de conta.

Quando um dado for removido, registrar a remocao em `-notes.md`.

## Origem Da Captura

Cada fixture deve indicar se veio de:

- captura fisica com Champion C001;
- log local do MotionLog;
- comparacao entre MotionLog e Champion Companion;
- teste manual controlado.

Fixtures sinteticas devem ser marcadas claramente como sinteticas e nao podem substituir validacao fisica.

## Uso Futuro Em Testes Kotlin

As fixtures devem poder alimentar testes de parser no futuro app Android nativo.

Regras:

- `.hex` representa entrada do parser;
- `-expected.json` representa saida esperada;
- `-notes.md` preserva contexto e limitacoes;
- cada fixture deve ser pequena o bastante para isolar um comportamento;
- cenarios grandes podem ser divididos em varios prefixos.

## Fluxos Prioritarios

Prioridade inicial:

- `a1`: handshake;
- `d1`: historico de BPM;
- `d3`: listagem ou indice de treinos;
- `d4`: detalhes de treino.

Fluxos adicionais previstos:

- `d0`: leitura ou medicao relacionada a saude;
- `d8`: diagnostico ou suporte ao protocolo;
- `a8`: diagnostico ou suporte ao protocolo;
- `f002`: stream ou medicao manual de BPM;
- `fea1`: characteristic ou fluxo auxiliar identificado na analise.

## Status Atual

Nenhuma fixture real foi adicionada nesta task.

Esta pasta existe para receber fixtures progressivas conforme os comandos e parsers forem implementados e comparados com o dispositivo fisico e o MotionLog.
