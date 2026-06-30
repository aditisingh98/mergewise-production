# MergeWise Deployment Guide

Complete step-by-step instructions to deploy MergeWise publicly so frontends can call `POST /api/pr/analyze`.

## Project analysis

| Item | Value |
|------|-------|
| Framework | Spring Boot 3.2.5 (Java 17) |
| Build system | Maven (`pom.xml`) |
| Packaging | Executable JAR via `spring-boot-maven-plugin` |
| Container | `Dockerfile` (multi-stage Maven build) |
| Database | **None** (stateless API) |
| Message queue | Kafka (**optional**, disabled in production by default) |
| Start command | `java -jar app.jar` (Docker handles `PORT`) |
| Health check | `GET /actuator/health` |
| API docs | `GET /swagger-ui.html` |
| Main API | `POST /api/pr/analyze` |

### Required environment variables

| Variable | Required | Description |
|----------|----------|-------------|
| `GITHUB_TOKEN` | Recommended | GitHub API access (required for private repos) |
| `OPENAI_API_KEY` | Optional | Enables AI review + executive summary |
| `SPRING_PROFILES_ACTIVE` | Prod | Set to `prod` |
| `MERGEWISE_CORS_ORIGINS` | Prod | Frontend origin(s), comma-separated |
| `MERGEWISE_PUBLIC_URL` | Optional | Auto-detected on Render/Railway if unset |

---

## Option A — Render (recommended, free tier)

### 1. Push code to GitHub

```bash
git add .
git commit -m "Add production deployment configuration"
git push -u origin cursor/enterprise-pr-review-platform
# Merge to main, or deploy branch directly
```

### 2. Create Render service

1. Go to [https://dashboard.render.com](https://dashboard.render.com)
2. **New → Blueprint** (or **New → Web Service**)
3. Connect repository: `aditisingh98/mergewise-production`
4. Render detects `render.yaml` automatically

### 3. Set secrets in Render dashboard

| Key | Value |
|-----|-------|
| `GITHUB_TOKEN` | Your GitHub PAT |
| `OPENAI_API_KEY` | Your LLM API key |

### 4. Deploy

Click **Deploy**. Render builds the Docker image and assigns a public URL.

### 5. Expected URLs

```
https://mergewise-api.onrender.com/api/pr/analyze      POST
https://mergewise-api.onrender.com/actuator/health     GET
https://mergewise-api.onrender.com/swagger-ui.html     GET
https://mergewise-api.onrender.com/api/deployment      GET
```

`MERGEWISE_PUBLIC_URL` is auto-populated from `RENDER_EXTERNAL_URL`.

### Build / start (handled by Render)

| Setting | Value |
|---------|-------|
| Runtime | Docker |
| Dockerfile | `./Dockerfile` |
| Health check | `/actuator/health` |

---

## Option B — Railway

### 1. Push to GitHub (same as above)

### 2. Create Railway project

1. Go to [https://railway.app](https://railway.app)
2. **New Project → Deploy from GitHub repo**
3. Select `mergewise-production`
4. Railway reads `railway.toml`

### 3. Environment variables (Railway → Variables)

```
SPRING_PROFILES_ACTIVE=prod
GITHUB_TOKEN=<secret>
OPENAI_API_KEY=<secret>
MERGEWISE_CORS_ORIGINS=*
MERGEWISE_KAFKA_ENABLED=false
```

### 4. Generate public domain

Railway → Service → **Settings → Networking → Generate Domain**

Public URL auto-detected via `RAILWAY_PUBLIC_DOMAIN`.

### Build / start

| Setting | Value |
|---------|-------|
| Builder | Dockerfile |
| Health check | `/actuator/health` |

---

## Option C — AWS App Runner

### 1. Build and push image

```bash
export AWS_ACCOUNT_ID=123456789012
export AWS_REGION=us-east-1
chmod +x deploy/aws-deploy.sh
./deploy/aws-deploy.sh
```

### 2. Create App Runner service

1. AWS Console → **App Runner → Create service**
2. Source: ECR image from `deploy/aws-deploy.sh` output
3. Port: `8090` (App Runner maps to HTTPS automatically)
4. Health check: HTTP `/actuator/health`
5. Environment variables:

```
SPRING_PROFILES_ACTIVE=prod
GITHUB_TOKEN=<secret>
OPENAI_API_KEY=<secret>
MERGEWISE_CORS_ORIGINS=https://your-frontend.com
AWS_APP_RUNNER_SERVICE_URL=<assigned after create>
```

Reference: `deploy/aws-apprunner-service.json`

---

## Option D — Docker (any VPS / cloud VM)

```bash
docker build -t mergewise:prod .
docker run -d \
  --name mergewise \
  -p 8090:8090 \
  -e SPRING_PROFILES_ACTIVE=prod \
  -e MERGEWISE_PUBLIC_URL=https://api.yourdomain.com \
  -e MERGEWISE_CORS_ORIGINS=https://app.yourdomain.com \
  -e GITHUB_TOKEN=ghp_... \
  -e OPENAI_API_KEY=... \
  mergewise:prod
```

Put Nginx/Caddy/ALB in front for HTTPS.

---

## Vercel

**Not applicable.** MergeWise is a Java Spring Boot backend. Use Render, Railway, AWS, or Docker instead.

---

## Frontend integration

```javascript
const API_URL = 'https://mergewise-api.onrender.com'; // your public URL

const response = await fetch(`${API_URL}/api/pr/analyze`, {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({
    prUrl: 'https://github.com/owner/repo/pull/1'
  })
});

if (!response.ok) throw new Error(await response.text());
const result = await response.json();

console.log(result.finalDecision);
console.log(result.executiveSummary);
console.log(result.deploymentInfo);
```

### CORS

Set `MERGEWISE_CORS_ORIGINS` to your frontend origin:

```
MERGEWISE_CORS_ORIGINS=https://myapp.vercel.app,http://localhost:3000
```

Use `*` only for development.

---

## Validation checklist

After deploy, run:

```bash
# Health
curl -s https://YOUR_PUBLIC_URL/actuator/health

# Deployment info
curl -s https://YOUR_PUBLIC_URL/api/deployment

# Analyze (public PR)
curl -s -X POST https://YOUR_PUBLIC_URL/api/pr/analyze \
  -H "Content-Type: application/json" \
  -d '{"prUrl":"https://github.com/spring-projects/spring-boot/pull/1"}'
```

Expected health response:

```json
{"status":"UP"}
```

---

## CI/CD

GitHub Actions workflow `.github/workflows/deploy.yml` builds and tests on every push. Connect Render/Railway auto-deploy to `main` for continuous deployment.

---

## Troubleshooting

| Issue | Fix |
|-------|-----|
| App won't start | Check logs; ensure `SPRING_PROFILES_ACTIVE=prod` |
| 502 on Render free tier | Cold start — wait 30–60s, retry health check |
| CORS errors | Set `MERGEWISE_CORS_ORIGINS` to exact frontend origin |
| GitHub 404 | Set `GITHUB_TOKEN` with `repo` scope |
| Kafka errors | Keep `MERGEWISE_KAFKA_ENABLED=false` (default) |
