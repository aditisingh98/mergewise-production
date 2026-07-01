# MergeWise AI Review Platform

Enterprise AI-powered pull request review for **GitHub** and **GitLab** — public and private repositories.

**Live API:** `https://mergewise-api.onrender.com`

> **Deploy publicly:** see [DEPLOYMENT.md](DEPLOYMENT.md) for Render, Railway, AWS, and Docker steps.

---

## Quick start

| What | URL |
|------|-----|
| API root | `GET /` |
| Supported providers | `GET /api/pr/providers` |
| Analyze PR/MR | `POST /api/pr/analyze` |
| Health | `GET /actuator/health` |
| Swagger UI | `GET /swagger-ui/index.html` |

The provider is detected automatically from `prUrl`. You do **not** need to send `githubToken` for GitLab URLs or `gitlabToken` for GitHub URLs.

---

## Request body

| Field | Required | Used for |
|-------|----------|----------|
| `prUrl` | Yes | GitHub PR or GitLab merge request URL |
| `githubToken` | No | Private **GitHub** repos only |
| `gitlabToken` | No | Private **GitLab** merge requests only |
| `accessToken` | No | Unified token for whichever provider is detected |

**Alternative:** send the token in the header instead of the body:

```http
Authorization: Bearer <your-token>
```

**Server-side fallback (Render / Docker):** set `GITHUB_TOKEN` and/or `GITLAB_TOKEN` in environment variables so callers can send only `prUrl`.

---

## Sample requests

Base URL for all examples below:

```text
https://mergewise-api.onrender.com
```

### 1. GitHub — public repository

No token needed for public GitHub pull requests.

**Request**

```http
POST /api/pr/analyze
Content-Type: application/json

{
  "prUrl": "https://github.com/spring-projects/spring-petclinic/pull/1"
}
```

**cURL**

```bash
curl -X POST "https://mergewise-api.onrender.com/api/pr/analyze" \
  -H "Content-Type: application/json" \
  -d '{
    "prUrl": "https://github.com/spring-projects/spring-petclinic/pull/1"
  }'
```

**URL format**

```text
https://github.com/{owner}/{repo}/pull/{number}
```

---

### 2. GitHub — private repository

Provide a GitHub Personal Access Token (classic or fine-grained) with `repo` read access.

**Option A — token in body**

```http
POST /api/pr/analyze
Content-Type: application/json

{
  "prUrl": "https://github.com/your-org/your-private-repo/pull/42",
  "githubToken": "ghp_your_github_token_here"
}
```

**Option B — token in header**

```bash
curl -X POST "https://mergewise-api.onrender.com/api/pr/analyze" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer ghp_your_github_token_here" \
  -d '{
    "prUrl": "https://github.com/your-org/your-private-repo/pull/42"
  }'
```

**Option C — unified `accessToken` field**

```json
{
  "prUrl": "https://github.com/your-org/your-private-repo/pull/42",
  "accessToken": "ghp_your_github_token_here"
}
```

**Option D — server token (no token in request)**

Set `GITHUB_TOKEN` on the server, then send only:

```json
{
  "prUrl": "https://github.com/your-org/your-private-repo/pull/42"
}
```

---

### 3. GitLab — public merge request

No token needed for public projects on GitLab.com (and many public self-hosted instances).

**Request**

```http
POST /api/pr/analyze
Content-Type: application/json

{
  "prUrl": "https://gitlab.com/gitlab-org/gitlab/-/merge_requests/1"
}
```

**cURL**

```bash
curl -X POST "https://mergewise-api.onrender.com/api/pr/analyze" \
  -H "Content-Type: application/json" \
  -d '{
    "prUrl": "https://gitlab.com/gitlab-org/gitlab/-/merge_requests/1"
  }'
```

**URL format**

```text
https://{gitlab-host}/{group}/{project}/-/merge_requests/{iid}
```

Examples:

- GitLab.com: `https://gitlab.com/group/project/-/merge_requests/123`
- Self-hosted: `https://gitlab.example.com/group/project/-/merge_requests/7485`

---

### 4. GitLab — private merge request

Use a GitLab Personal Access Token with `read_api` (or `api`) scope.  
**Do not send `githubToken` for GitLab URLs** — it will cause authentication errors.

**Option A — token in body**

```http
POST /api/pr/analyze
Content-Type: application/json

{
  "prUrl": "https://gitlab.intelligrape.net/bharti-axa/proposal-service/-/merge_requests/7485",
  "gitlabToken": "glpat-your_gitlab_token_here"
}
```

**Option B — token in header**

```bash
curl -X POST "https://mergewise-api.onrender.com/api/pr/analyze" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer glpat-your_gitlab_token_here" \
  -d '{
    "prUrl": "https://gitlab.intelligrape.net/bharti-axa/proposal-service/-/merge_requests/7485"
  }'
```

**Option C — unified `accessToken` field**

```json
{
  "prUrl": "https://gitlab.example.com/group/private-project/-/merge_requests/100",
  "accessToken": "glpat-your_gitlab_token_here"
}
```

**Option D — server token (no token in request)**

Set `GITLAB_TOKEN` on the server (e.g. in Render env vars), then send only:

```json
{
  "prUrl": "https://gitlab.example.com/group/private-project/-/merge_requests/100"
}
```

---

## Sample response

All requests return the same response shape. Example (truncated):

```json
{
  "repo": "owner/repo",
  "prNumber": 42,
  "finalDecision": "APPROVE_WITH_WARNINGS",
  "decisionReasoning": "No blocking issues found; minor quality improvements suggested.",
  "overallScore": 78,
  "riskLevel": "MEDIUM",
  "executiveSummary": {
    "summary": "The change set is generally sound with a few areas to harden before merge.",
    "keyFindings": ["Missing null checks in service layer", "No unit tests for new endpoint"]
  },
  "qualityScore": { "score": 75, "grade": "B" },
  "securityScore": { "score": 82, "grade": "B+" },
  "performanceScore": { "score": 80, "grade": "B" },
  "mergeConfidence": { "score": 72, "grade": "B-" },
  "reviewSuggestions": [
    {
      "category": "SECURITY",
      "severity": "MEDIUM",
      "title": "Validate user input",
      "recommendation": "Add server-side validation before persisting."
    }
  ],
  "metadata": {
    "vcsProvider": "GITHUB",
    "tokenField": "githubToken",
    "authMode": "PUBLIC"
  },
  "deploymentInfo": {
    "apiBaseUrl": "https://mergewise-api.onrender.com",
    "analyzeEndpoint": "https://mergewise-api.onrender.com/api/pr/analyze",
    "healthCheckUrl": "https://mergewise-api.onrender.com/actuator/health",
    "swaggerUrl": "https://mergewise-api.onrender.com/swagger-ui/index.html"
  }
}
```

**`metadata.authMode` values**

| Value | Meaning |
|-------|---------|
| `PUBLIC` | No token was used (public repo/MR) |
| `USER_TOKEN` | Token came from request body or `Authorization` header |
| `SERVER_TOKEN` | Token came from `GITHUB_TOKEN` or `GITLAB_TOKEN` on the server |

**Merge decisions:** `APPROVE`, `APPROVE_WITH_WARNINGS`, `NEEDS_CHANGES`, `BLOCK_MERGE`

---

## List supported providers

```bash
curl "https://mergewise-api.onrender.com/api/pr/providers"
```

Returns URL patterns, token field names, and JSON examples for each provider.

---

## Token cheat sheet

| Scenario | Send in request | Server env fallback |
|----------|-----------------|---------------------|
| Public GitHub PR | `prUrl` only | — |
| Private GitHub PR | `githubToken` or `accessToken` or `Authorization: Bearer` | `GITHUB_TOKEN` |
| Public GitLab MR | `prUrl` only | — |
| Private GitLab MR | `gitlabToken` or `accessToken` or `Authorization: Bearer` | `GITLAB_TOKEN` |

---

## Features

- **Multi-VCS**: GitHub pull requests and GitLab merge requests (including self-hosted GitLab)
- **Public + private**: optional per-request tokens or server-side token fallback
- **Advanced PR review engine**: functional, code quality, runtime risk, performance, security, database, testing, and architecture analysis
- **AI summary engine**: executive summary, category scores, merge confidence
- **Merge decision engine**: `APPROVE`, `APPROVE_WITH_WARNINGS`, `NEEDS_CHANGES`, `BLOCK_MERGE`
- **Production ready**: Docker, CORS, health checks, OpenAPI/Swagger, structured logging

---

## Environment variables

| Variable | Required | Description |
|----------|----------|-------------|
| `GITHUB_TOKEN` | Optional | Server fallback for private GitHub repos |
| `GITLAB_TOKEN` | Optional | Server fallback for private GitLab merge requests |
| `OPENAI_API_KEY` | Optional | API key for OpenAI-compatible LLM provider |
| `OPENAI_BASE_URL` | Optional | LLM base URL (OpenAI, Groq, Gemini, Ollama) |
| `OPENAI_MODEL` | Optional | Model name |
| `MERGEWISE_PUBLIC_URL` | Prod | Public API base URL returned in `deploymentInfo` |
| `MERGEWISE_CORS_ORIGINS` | Prod | Comma-separated allowed origins (default `*` in dev) |
| `MERGEWISE_KAFKA_ENABLED` | Optional | Set to `false` to disable Kafka (default in prod) |
| `PORT` | Optional | Server port (default `8090`) |

---

## Local setup

### Prerequisites

- JDK 17+
- Maven 3.9+
- Optional: Docker, GitHub/GitLab tokens, LLM API key

### Run with Maven

```bash
export GITHUB_TOKEN=ghp_...
export GITLAB_TOKEN=glpat-...
export OPENAI_API_KEY=...

mvn spring-boot:run
```

### Run with Docker

```bash
docker build -t mergewise:1.0.0 .
docker run -p 8090:8090 \
  -e GITHUB_TOKEN=ghp_... \
  -e GITLAB_TOKEN=glpat-... \
  -e OPENAI_API_KEY=... \
  -e MERGEWISE_PUBLIC_URL=http://localhost:8090 \
  mergewise:1.0.0
```

### Build

```bash
mvn clean package -DskipTests
java -jar target/mergewise-1.0.0.jar
```

---

## Frontend integration

```javascript
const API_BASE = 'https://mergewise-api.onrender.com';

// Public GitHub PR
const publicGh = await fetch(`${API_BASE}/api/pr/analyze`, {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({
    prUrl: 'https://github.com/owner/repo/pull/42'
  })
});

// Private GitLab MR (token in body)
const privateGl = await fetch(`${API_BASE}/api/pr/analyze`, {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({
    prUrl: 'https://gitlab.example.com/group/project/-/merge_requests/100',
    gitlabToken: 'glpat-...'
  })
});

const result = await publicGh.json();
console.log(result.finalDecision, result.executiveSummary);
```

---

## Architecture

```
PRController → PRAnalysisService → VcsProviderDetector
                                 → GitHubService (GitHub)
                                 → GitLabService (GitLab)
                                 → AgentOrchestrator
                                       → AdvancedReviewEngine
                                       → MergeDecisionEngine
                                 → AISummaryService
                                 → PRAnalysisResponseMapper
```

---

## Troubleshooting

| Error | Likely cause | Fix |
|-------|--------------|-----|
| `401 Unauthorized` on GitHub | Invalid or expired `githubToken` | Remove token for public repos; use a valid PAT for private |
| `401 Unauthorized` on GitLab | `githubToken` sent for a GitLab URL | Use `gitlabToken` or `accessToken` instead |
| `404 Not Found` | Wrong URL or no access to project | Verify `prUrl`; add token for private repos |
| `429` from AI provider | LLM rate limit | Heuristic review still runs; retry later or change model |

---

## License

Proprietary — MergeWise
