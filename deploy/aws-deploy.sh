#!/usr/bin/env bash
# Build and push MergeWise image to Amazon ECR, then deploy via App Runner.
# Prerequisites: AWS CLI, Docker, authenticated AWS credentials.
set -euo pipefail

AWS_REGION="${AWS_REGION:-us-east-1}"
AWS_ACCOUNT_ID="${AWS_ACCOUNT_ID:?Set AWS_ACCOUNT_ID}"
ECR_REPO="${ECR_REPO:-mergewise}"
IMAGE_TAG="${IMAGE_TAG:-latest}"

ECR_URI="${AWS_ACCOUNT_ID}.dkr.ecr.${AWS_REGION}.amazonaws.com/${ECR_REPO}"

echo "==> Logging in to ECR"
aws ecr get-login-password --region "${AWS_REGION}" | docker login --username AWS --password-stdin "${AWS_ACCOUNT_ID}.dkr.ecr.${AWS_REGION}.amazonaws.com"

echo "==> Ensuring ECR repository exists"
aws ecr describe-repositories --repository-names "${ECR_REPO}" --region "${AWS_REGION}" 2>/dev/null \
  || aws ecr create-repository --repository-name "${ECR_REPO}" --region "${AWS_REGION}"

echo "==> Building Docker image"
docker build -t "${ECR_REPO}:${IMAGE_TAG}" .

echo "==> Tagging and pushing"
docker tag "${ECR_REPO}:${IMAGE_TAG}" "${ECR_URI}:${IMAGE_TAG}"
docker push "${ECR_URI}:${IMAGE_TAG}"

echo "==> Image pushed: ${ECR_URI}:${IMAGE_TAG}"
echo "Create/update App Runner service using deploy/aws-apprunner-service.json"
echo "Set environment variables: SPRING_PROFILES_ACTIVE=prod, GITHUB_TOKEN, OPENAI_API_KEY, MERGEWISE_CORS_ORIGINS"
