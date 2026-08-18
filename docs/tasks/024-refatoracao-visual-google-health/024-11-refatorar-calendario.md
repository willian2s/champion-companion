# 024-11 — Refatorar Calendário

- **Ticker:** `024`
- **Número:** `11`
- **Status:** pending

## Objetivo e resultado esperado

Refatorar calendário mensal para melhorar navegação temporal, seleção e leitura
dos indicadores. Resultado: grade estável e hierarquia mais leve sem alterar
read model, seleção ou abertura do dia.

## Requisitos e critérios cobertos

- R4–R9; critérios 5, 7–12 da spec.
- Regra cromática comum: quando presentes, header azul-petróleo nominal do
  Champion com conteúdo branco; cards, indicadores e progresso com acentos
  nominais `primary`/`secondary`/`tertiary` da paleta clara/escura conforme o
  tema, seguindo o piloto `024-05`, sem alterar o tema raiz.

## Escopo incluído

- header, navegação mês anterior/próximo/hoje;
- grade de 42 células, seleção, indicadores e legenda;
- estados parcial, vazio, loading e erro;
- CTA de abrir dia.

## Escopo excluído

- read model, ViewModel, cálculo de indicadores ou detalhe diário;
- novo tipo de evento ou fonte de dados.

## Dependências

- `024-10` concluída.

## Arquivos e símbolos prováveis

- `feature/calendar/CalendarScreen.kt`;
- `CalendarScreen`, `CalendarHeader`, grade, célula, legenda e estados locais.

## Passos de implementação

1. Preservar estrutura de sete colunas e semântica de cada dia.
2. Reorganizar header e controles mensais para leitura rápida.
3. Tornar seleção e indicadores distinguíveis sem depender só de cor.
4. Compactar legenda e CTA sem ocultar estados parciais.
5. Validar meses com cinco/seis semanas e labels localizados.
6. Exercitar abrir dia, voltar e mudar mês.
7. Registrar capturas sanitizadas e overview.

## Testes e comandos de validação

Sem testes automatizados.

```bash
git diff --check
./gradlew lint assembleDebug assembleRelease
./gradlew installDebug
adb devices -l
adb shell am start -n br.com.mwconsulting.championcompanion.dev/br.com.mwconsulting.championcompanion.MainActivity
```

Smoke: meses anterior/próximo, hoje, seleção, abrir dia, portrait, fonte `1,3x`,
landscape e temas.

## Definição de pronto

- grade mantém sete colunas e 42 células sem corte;
- seleção, indicadores e legenda são acessíveis;
- navegação temporal e abertura do dia preservadas;
- estados locais equivalentes;
- comandos, capturas e overview registrados.

## Riscos e cuidados

- largura reduzida pode comprimir números e marcadores;
- fonte ampliada não deve quebrar grade;
- cores locais só migram para tokens quando semântica existir;
- não alterar candidate temporal ou read model.
