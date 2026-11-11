# Urban Risk Intelligence Platform

Global Solution 2026.2 – Advanced Programming and Mobile Dev | Turma 3ECR
554603 – Pedro Alvarez Certo | 555143 – Gustavo Demeis Peres

Plataforma integrada de monitoramento e análise de riscos cotidianos em áreas urbanas (alagamentos, ilhas de calor e interrupções de serviços essenciais), em um único repositório:

| Pasta | Conteúdo |
|---|---|
| `backend/` | API REST em **Java 21 + Spring Boot 3** (receber dados, persistir, priorizar alertas, informar origem e confiança) |
| `mobile/` | App **React Native (Expo)** que consome a API: painel, eventos, notificações, registro comunitário e ajustes |

## Visão geral

**Backend**
- Recebe dados de **sensores** (`POST /api/readings`), de **bases públicas** (`POST /api/public-data`) e de **registros comunitários** (`POST /api/community-reports`).
- **Persiste** tudo em banco (H2 em arquivo por padrão; PostgreSQL pelo perfil `postgres`).
- Expõe a API que **prioriza alertas**: cada evento sai com `priority`, `risk`, `status`, `origin` (origem) e `confidence`/`confidenceLevel` (nível de confiança).
- Arquitetura em camadas: `web` (controllers e DTOs) → `service` (regras) → `repository` → `domain` (entidades).
- Regras de negócio: confiança por origem e evidências independentes (RN-01/02), prioridade (RN-03), emissão de alerta só para evento confirmado (RN-04), classificação de risco por tipo (RN-05), fonte desatualizada (RN-06), eventos duplicados (RN-07), máquina de estados do evento.
- **Análise de apoio à priorização:** `TrendAnalyzer` ajusta uma reta às últimas medições dos sensores, detecta tendência de alta e projeta em quantos minutos o nível chega ao limite. Tendência de alta soma pontos à prioridade.
- **Tempo real:** um simulador (`SensorSimulator`) gera leituras a cada 5 s; o app consulta a API a cada 5 s.
- **LGPD:** o relato comunitário não coleta dados pessoais (só bairro e texto), exige consentimento e é validado no backend.
- **Disponibilidade:** `GET /actuator/health`; saúde e atraso das fontes em `GET /api/sources`.

**App mobile**
- Painel de acompanhamento (indicadores e alertas prioritários), lista de eventos com filtro, detalhe (origem, evidências, confiança, tendência e ciclo de vida), central de notificações, registro de ocorrência e ajustes.
- **Alertas em tempo real:** banner e vibração quando chega um novo alerta.
- **Sem conexão:** se o servidor cair, o app **não trava**: mostra um banner vermelho “Sem conexão…”, mantém os últimos dados na tela e continua tentando; ao voltar, exibe “Conexão restabelecida” e atualiza sozinho.
- Acessibilidade: rótulos de acessibilidade, textos legíveis e botões grandes.

## Como executar

### 1) Backend (precisa de Java 21; o Maven vem no wrapper)
```
cd backend
./mvnw spring-boot:run          # Windows: mvnw.cmd spring-boot:run
```
A API sobe em `http://localhost:8080` com dados de exemplo e o simulador ligado.
Teste rápido: `http://localhost:8080/api/events` e `http://localhost:8080/actuator/health`.

Testes automatizados (18 testes: regras de negócio e API):
```
./mvnw test
```

### 2) App mobile (precisa de Node 20+)
```
cd mobile
npm install
npx expo start
```
- **Celular:** instale o app **Expo Go**, leia o QR code e, em **Ajustes**, informe o endereço do computador na mesma rede Wi-Fi (ex.: `http://192.168.0.10:8080`). Libere a porta 8080 no firewall se necessário.
- **Emulador Android:** funciona sem ajuste (usa `http://10.0.2.2:8080`).
- **Navegador:** `npm run web` (usa `http://localhost:8080`).

## Roteiro da apresentação (API + app, com queda do backend)

1. Suba o backend e abra o app. Mostre o **Painel**, a lista de **Eventos** e um **Detalhe** (origem, confiança, tendência).
2. Mostre dados chegando em tempo real: o evento de **Porto Seco** sobe de nível a cada 5 s e dispara alerta quando passa do limite (banner roxo + vibração). Também dá para enviar uma leitura manualmente:
   ```
   curl -X POST http://localhost:8080/api/readings -H "Content-Type: application/json" -d "{\"sourceName\":\"Sensor S-099\",\"sourceGroup\":\"Sensores de nível\",\"type\":\"ALAGAMENTO\",\"neighborhood\":\"Ribeira\",\"value\":125}"
   ```
3. Em **Relatar**, registre uma ocorrência (com consentimento) e mostre-a entrando como “Em validação” com confiança baixa. Em **Eventos**, com perfil “Agente” (Ajustes), valide, confirme e emita alerta.
4. **Pare o backend** (Ctrl+C no terminal). Em até ~5 s o app mostra “Sem conexão”, **sem travar** e mantendo os dados.
5. **Suba o backend de novo.** O app exibe “Conexão restabelecida” e atualiza sozinho.

## Endpoints principais

| Método e caminho | Função |
|---|---|
| `GET /api/events?type=&risk=&status=` | Eventos priorizados, com origem e confiança |
| `GET /api/events/{id}` | Detalhe, evidências, histórico e tendência |
| `POST /api/events/{id}/transitions` | Ações: `VALIDAR`, `CONFIRMAR`, `ALERTAR`, `MONITORAR`, `ENCERRAR`, `DESCARTAR` |
| `POST /api/readings` · `/api/public-data` · `/api/community-reports` | Entrada de dados |
| `GET /api/dashboard` · `/api/notifications` · `/api/sources` | Painel, notificações e saúde das fontes |

## Indicadores

- **Tempo de atualização:** consulta a cada 5 s; saúde das fontes em `/api/sources`.
- **Qualidade dos alertas:** alerta só para evento confirmado; risco classificado por regras; confiança explícita.
- **Facilidade de uso:** poucas telas, filtros simples, textos claros e avisos de conexão.

## Limitações e próximos passos

- Autenticação (JWT) e perfis por usuário não estão implementados: o perfil “Agente” é uma escolha no app.
- Sensores e bases públicas são simulados; não há integração real com as fontes.
- Notificações são feitas dentro do app (banner e vibração), sem push do sistema operacional.
- A “fonte principal” da confiança (RN-01) é a primeira evidência registrada do evento.
- O banco H2 em arquivo (`backend/data/`) é para desenvolvimento; use PostgreSQL em produção.
