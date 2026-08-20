# Evidência final — validação visual 024-18

- **Ticker:** `024`
- **Subtarefa:** `024-18`
- **Data:** 2026-08-20
- **Dispositivo:** Galaxy S22 `SM-S901E`, variante debug

## Inventário consolidado

| Área | Resultado |
|---|---|
| Primitives | `ChampionSpacing`, shapes, tipografia, tema, paletas e elevação permanecem com consumidores reais. |
| Componentes compartilhados | `HealthCard`, `MetricCard`, estados de feedback, `PrimaryActionButton` e `SyncStatusBadge` possuem consumidores reais em múltiplas superfícies. |
| Duplicações locais | Headers, cards e `MetricCard` local de Exercícios não foram promovidos: não há equivalência de API comprovada entre duas superfícies. |
| Legado removido | `LocalChampionSpacing` e `LocalChampionElevation` não tinham consumidor em código de produção, preview ou teste; foram removidos. |
| Legado preservado | `HealthChart`, `CalibrationBanner`, `ConnectionStatus` e `ActivityStatusBadge` permanecem por terem cobertura de testes/previews, apesar de não possuírem consumidor de superfície. Remoção exigiria alteração de testes, fora do escopo. |
| Phone/GPS/rota/mapa | Preservados; nenhum símbolo histórico foi alterado. |

## Evidência física reutilizada

As capturas individuais sanitizadas permanecem na pasta da task e cobrem shell e
treze superfícies: `024-03` (portrait/landscape), `024-05` a `024-17` e
`024-19`. Nenhuma captura nova contém métricas, títulos livres, identificadores,
payload BLE ou dados de conta.

## Smoke integrado

- Início; Saúde e detalhes de frequência cardíaca, peso, passos e distância;
  Calendário e detalhe diário; Exercícios e detalhe; Dispositivo; Perfil;
  Configurações; menu `Mais`; back e restauração: **passou**.
- Portrait escuro com fonte `1,0x`, portrait claro com fonte `1,3x` e landscape
  claro com fonte `1,3x`: **passou** no smoke final do shell; PNGs foram
  inspecionados somente em memória para confirmar renderização e dimensões
  `1080x2340` e `2340x1080`.
- Seleção nominal do shell e retorno por back: **passou** no smoke final; as
  capturas individuais continuam sendo a evidência visual sanitizada versionada.
- Fallback sem dynamic color em API inferior a 31: **N/A**; aparelho disponível
  usa API 36 e nenhum emulador foi iniciado.
- Estados raros, importação, medição, sincronização, exclusão e permissões
  mutáveis: **N/A**; não foram disparados para produzir evidência.

## Privacidade e restauração

- Hierarquias ADB foram usadas somente para labels genéricos, bounds e estrutura;
  dumps temporários foram removidos.
- Aparelho restaurado para fonte `1,0x`, portrait e tema escuro.

## Riscos residuais

- Fallback físico inferior à API 31 não foi observado.
- Estados transitórios e operações com efeitos permanecem `N/A`.
- A referência Fitbit pode variar por versão, região, conta e estado de dados.
- Revisão independente final: `PASS`; nenhum bloqueador ou achado importante.
