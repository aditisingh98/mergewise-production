# MergeWise AI Review Platform

Enterprise AI-powered pull request review for GitHub.

> **Deploy publicly:** see [DEPLOYMENT.md](DEPLOYMENT.md) for Render, Railway, AWS, and Docker steps.

## Features

- **Advanced PR review engine**: functional, code quality, runtime risk, performance, security, database, testing, and architecture analysis
- **AI summary engine**: executive summary, quality/security/performance/maintainability/complexity scores, merge confidence
- **Auto fix suggestions**: root cause, production impact, fix recommendation, code examples, confidence scores
- **Merge decision engine**: `APPROVE`, `APPROVE_WITH_WARNINGS`, `NEEDS_CHANGES`, `BLOCK_MERGE` with reasoning
- **Production ready**: Docker, CORS, health checks, OpenAPI/Swagger, structured logging

## API

### Analyze PR

```http
POST /api/pr/analyze
Content-Type: application/json

{
  "prUrl": "https://github.com/owner/repo/pull/123"
}
```

### Response (includes legacy fields + enterprise enrichments)

All existing fields are preserved (`repo`, `prNumber`, `fileChanges`, `reviewIssues`, `overallScore`, `riskLevel`, `finalDecision`, etc.) plus:

- `executiveSummary`
- `qualityScore`, `securityScore`, `performanceScore`, `maintainabilityScore`, `complexityScore`, `mergeConfidence`
- `reviewSuggestions`
- `architectureRecommendations`, `testingRecommendations`
- `deploymentInfo`
- `decisionReasoning`

**Backward compatibility**: when decision is `NEEDS_CHANGES`, `metadata.legacyFinalDecision` is set to `CHANGES_REQUIRED`.

## Public URLs (after deployment)

| Endpoint | Path |
|----------|------|
| Analyze API | `{PUBLIC_URL}/api/pr/analyze` |
| Health check | `{PUBLIC_URL}/actuator/health` |
| Swagger UI | `{PUBLIC_URL}/swagger-ui.html` |
| OpenAPI JSON | `{PUBLIC_URL}/api-docs` |

Set `MERGEWISE_PUBLIC_URL` to your deployed HTTPS domain (e.g. `https://api.mergewise.example.com`).

## Environment variables

| Variable | Required | Description |
|----------|----------|-------------|
| `GITHUB_TOKEN` | Recommended | GitHub API token for private repos and higher rate limits |
| `OPENAI_API_KEY` | Optional | API key for OpenAI-compatible LLM provider |
| `OPENAI_BASE_URL` | Optional | LLM base URL (OpenAI, Groq, Gemini, Ollama) |
| `OPENAI_MODEL` | Optional | Model name |
| `MERGEWISE_PUBLIC_URL` | Prod | Public API base URL returned in `deploymentInfo` |
| `MERGEWISE_CORS_ORIGINS` | Prod | Comma-separated allowed origins (default `*` in dev) |
| `KAFKA_BOOTSTRAP_SERVERS` | Optional | Kafka broker (default `localhost:9092`) |
| `PORT` | Optional | Server port (default `8090`) |

## Local setup

### Prerequisites

- JDK 17+
- Maven 3.9+
- Optional: Docker, Kafka, GitHub token, LLM API key

### Run with Maven

```bash
export GITHUB_TOKEN=ghp_...
export OPENAI_API_KEY=...

mvn spring-boot:run
```

### Run with Docker

```bash
docker build -t mergewise:1.0.0 .
docker run -p 8090:8090 \
  -e GITHUB_TOKEN=... \
  -e OPENAI_API_KEY=... \
  -e MERGEWISE_PUBLIC_URL=http://localhost:8090 \
  mergewise:1.0.0
```

### Run with Docker Compose (app + Kafka)

```bash
export GITHUB_TOKEN=...
export OPENAI_API_KEY=...
docker compose up --build
```

## Build

```bash
mvn clean package -DskipTests
java -jar target/mergewise-1.0.0.jar
```

## Deployment flow

1. Build container image (`docker build` or CI workflow).
2. Deploy to your platform (AWS ECS, GCP Cloud Run, Azure, Railway, Render, etc.).
3. Set environment variables in the platform secret store.
4. Map HTTPS load balancer to container port `8090`.
5. Set `MERGEWISE_PUBLIC_URL` to the public HTTPS URL.
6. Configure `MERGEWISE_CORS_ORIGINS` for your frontend domain.
7. Verify health: `GET /actuator/health`.
8. Integrate frontend: `POST {PUBLIC_URL}/api/pr/analyze`.

## Frontend integration

```javascript
const API_BASE = 'https://api.mergewise.example.com';

const response = await fetch(`${API_BASE}/api/pr/analyze`, {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({ prUrl: 'https://github.com/owner/repo/pull/42' })
});

const result = await response.json();
console.log(result.finalDecision, result.executiveSummary, result.reviewSuggestions);
```

## Architecture

```
PRController → PRAnalysisService → GitHubService
                                 → AgentOrchestrator
                                       → PlannerAgent
                                       → CodeReviewAgent (heuristics + AI)
                                       → NpeAgent
                                       → QualityAgent
                                       → AdvancedReviewEngine (8 category analyzers)
                                       → MergeDecisionEngine
                                 → AISummaryService
                                 → PRAnalysisResponseMapper
```

## License

Proprietary — MergeWise
