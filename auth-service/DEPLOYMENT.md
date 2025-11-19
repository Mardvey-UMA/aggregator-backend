# Auth Service Deployment Guide

## Prerequisites

- Docker 20.10+
- Kubernetes 1.25+ (for K8s deployment)
- kubectl configured with cluster access
- PostgreSQL 15+
- Redis 7+

## Environment Variables

### Required Variables

| Variable | Description | Example |
|----------|-------------|---------|
| `DB_HOST` | PostgreSQL host | `postgres-service` |
| `DB_PORT` | PostgreSQL port | `5432` |
| `DB_NAME` | Database name | `authdb` |
| `DB_USERNAME` | Database username | `postgres` |
| `DB_PASSWORD` | Database password | `secure_password` |
| `REDIS_HOST` | Redis host | `redis-service` |
| `REDIS_PORT` | Redis port | `6379` |
| `REDIS_PASSWORD` | Redis password (optional) | `redis_password` |
| `JWT_SECRET` | Base64 encoded JWT secret (min 256 bits) | `base64_encoded_secret` |

### Optional Variables

| Variable | Description | Default |
|----------|-------------|---------|
| `SPRING_PROFILES_ACTIVE` | Active Spring profile | `prod` |
| `SERVER_PORT` | Application port | `8080` |
| `DB_POOL_SIZE` | Database connection pool size | `20` |
| `JWT_ACCESS_TOKEN_EXPIRATION` | Access token TTL (ms) | `900000` (15 min) |
| `JWT_REFRESH_TOKEN_EXPIRATION` | Refresh token TTL (ms) | `604800000` (7 days) |
| `FRONTEND_URL` | Frontend URL for CORS | `https://app.contentaggregation.com` |
| `VK_CLIENT_ID` | VK OAuth2 client ID | - |
| `VK_CLIENT_SECRET` | VK OAuth2 client secret | - |
| `VK_REDIRECT_URI` | VK OAuth2 redirect URI | - |

## Local Development

### Using Docker Compose

1. Copy environment file:
   ```bash
   cp .env.example .env
   ```

2. Update `.env` with your values

3. Start services:
   ```bash
   docker-compose up -d
   ```

4. Check logs:
   ```bash
   docker-compose logs -f auth-service
   ```

5. Access the API:
   - API: http://localhost:8080
   - Swagger UI: http://localhost:8080/swagger-ui.html

### Without Docker

1. Start PostgreSQL and Redis manually

2. Set environment variables or update `application-dev.yml`

3. Run the application:
   ```bash
   ./gradlew :auth-service:bootRun
   ```

## Kubernetes Deployment

### 1. Create Namespace (optional)

```bash
kubectl create namespace auth-system
```

Update namespace in all K8s manifests if using custom namespace.

### 2. Create Secrets

**Important**: Replace placeholder values with actual production secrets.

```bash
# Create secrets from literals (recommended for production)
kubectl create secret generic auth-service-secrets \
  --from-literal=DB_HOST=postgres-service \
  --from-literal=DB_USERNAME=postgres \
  --from-literal=DB_PASSWORD=your_secure_password \
  --from-literal=REDIS_HOST=redis-service \
  --from-literal=REDIS_PASSWORD=your_redis_password \
  --from-literal=JWT_SECRET=your_base64_encoded_secret \
  --from-literal=VK_CLIENT_ID=your_vk_client_id \
  --from-literal=VK_CLIENT_SECRET=your_vk_client_secret \
  --from-literal=VK_REDIRECT_URI=https://api.contentaggregation.com/api/v1/auth/oauth2/callback/vk
```

Or apply from file (for non-production):
```bash
kubectl apply -f k8s/secret.yaml
```

### 3. Create ConfigMap

```bash
kubectl apply -f k8s/configmap.yaml
```

### 4. Deploy Application

```bash
kubectl apply -f k8s/deployment.yaml
kubectl apply -f k8s/service.yaml
kubectl apply -f k8s/hpa.yaml
```

### 5. Verify Deployment

```bash
# Check pods
kubectl get pods -l app=auth-service

# Check service
kubectl get svc auth-service

# Check HPA
kubectl get hpa auth-service-hpa

# View logs
kubectl logs -l app=auth-service -f

# Describe deployment
kubectl describe deployment auth-service
```

### 6. Access the Service

#### Internal Access (within cluster)
```
http://auth-service:8080
```

#### External Access

Create an Ingress resource:

```yaml
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: auth-service-ingress
  annotations:
    kubernetes.io/ingress.class: nginx
    cert-manager.io/cluster-issuer: letsencrypt-prod
spec:
  tls:
    - hosts:
        - api.contentaggregation.com
      secretName: auth-service-tls
  rules:
    - host: api.contentaggregation.com
      http:
        paths:
          - path: /api/v1/auth
            pathType: Prefix
            backend:
              service:
                name: auth-service
                port:
                  number: 8080
```

## Health Checks

### Endpoints

- **Liveness**: `GET /actuator/health/liveness`
  - Returns 200 if the application is running
  - Used by Kubernetes to restart unhealthy pods

- **Readiness**: `GET /actuator/health/readiness`
  - Returns 200 if the application can handle requests
  - Checks database and Redis connectivity
  - Used by Kubernetes to route traffic

- **Full Health**: `GET /actuator/health`
  - Returns detailed health information
  - Includes database, Redis, and disk space status

### Example Response

```json
{
  "status": "UP",
  "components": {
    "db": {
      "status": "UP",
      "details": {
        "database": "PostgreSQL",
        "validationQuery": "isValid()"
      }
    },
    "redis": {
      "status": "UP",
      "details": {
        "response": "PONG"
      }
    },
    "diskSpace": {
      "status": "UP"
    }
  }
}
```

## Scaling

### Manual Scaling

```bash
kubectl scale deployment auth-service --replicas=5
```

### Auto Scaling

The HPA is configured to:
- Minimum replicas: 2
- Maximum replicas: 10
- Scale up at 70% CPU utilization
- Scale up at 80% memory utilization

View HPA status:
```bash
kubectl get hpa auth-service-hpa -w
```

## Monitoring

### Metrics Endpoint

```
GET /actuator/metrics
GET /actuator/metrics/{metric-name}
```

### Prometheus Integration

Add these annotations to the deployment for Prometheus scraping:

```yaml
metadata:
  annotations:
    prometheus.io/scrape: "true"
    prometheus.io/port: "8080"
    prometheus.io/path: "/actuator/prometheus"
```

### Key Metrics to Monitor

- `jvm_memory_used_bytes` - JVM memory usage
- `hikaricp_connections_active` - Active DB connections
- `http_server_requests_seconds` - Request latency
- `process_cpu_usage` - CPU usage
- `logback_events_total` - Log events by level

## Troubleshooting

### Pod Not Starting

1. Check pod events:
   ```bash
   kubectl describe pod <pod-name>
   ```

2. Check logs:
   ```bash
   kubectl logs <pod-name>
   ```

3. Common issues:
   - Database connection refused: Check DB_HOST and network policies
   - Redis connection failed: Check REDIS_HOST and password
   - JWT secret invalid: Ensure it's Base64 encoded and min 256 bits

### High Memory Usage

1. Check current memory:
   ```bash
   kubectl top pods -l app=auth-service
   ```

2. Adjust JVM settings in deployment:
   ```yaml
   env:
     - name: JAVA_OPTS
       value: "-XX:MaxRAMPercentage=75.0 -XX:+UseG1GC"
   ```

### Slow Responses

1. Check database connection pool:
   ```bash
   curl http://auth-service:8080/actuator/metrics/hikaricp.connections.active
   ```

2. Increase pool size if needed:
   ```yaml
   data:
     DB_POOL_SIZE: "30"
   ```

### Database Migrations Failed

1. Check Flyway status:
   ```bash
   kubectl logs <pod-name> | grep -i flyway
   ```

2. Manual migration:
   ```bash
   kubectl exec -it <pod-name> -- java -jar app.jar --spring.flyway.repair=true
   ```

## Rollback

### Rollback to Previous Version

```bash
kubectl rollout undo deployment/auth-service
```

### Rollback to Specific Revision

```bash
# View history
kubectl rollout history deployment/auth-service

# Rollback to revision
kubectl rollout undo deployment/auth-service --to-revision=2
```

## Security Recommendations

1. **Secrets Management**: Use external secret management (Vault, AWS Secrets Manager)

2. **Network Policies**: Restrict pod-to-pod communication

3. **TLS**: Always use HTTPS in production

4. **JWT Secret Rotation**: Implement secret rotation strategy

5. **Resource Limits**: Always set CPU/memory limits

6. **Security Contexts**: Run as non-root user (already configured)

7. **Image Scanning**: Scan container images for vulnerabilities

## Backup and Recovery

### Database Backup

```bash
kubectl exec -it postgres-pod -- pg_dump -U postgres authdb > backup.sql
```

### Database Restore

```bash
kubectl exec -i postgres-pod -- psql -U postgres authdb < backup.sql
```

## Updating the Application

### Rolling Update

```bash
# Update image
kubectl set image deployment/auth-service \
  auth-service=contentaggregation/auth-service:v2.0.0

# Watch rollout
kubectl rollout status deployment/auth-service
```

### Blue-Green Deployment

1. Deploy new version with different name
2. Test new deployment
3. Switch service selector
4. Delete old deployment
