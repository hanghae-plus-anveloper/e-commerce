
```bash
# 1. 기존 컨테이너/네트워크 완전 삭제
docker compose -f docker-compose-redis.yml down -v
```

```bash
docker compose -f docker-compose-redis.yml up -d
```

```bash
docker exec -it redis-7001 \
  redis-cli --cluster create \
  host.docker.internal:7001 \
  host.docker.internal:7002 \
  host.docker.internal:7003 \
  host.docker.internal:7004 \
  host.docker.internal:7005 \
  host.docker.internal:7006 \
  --cluster-replicas 1
```

```bash
docker exec -it redis-7001 redis-cli -p 7001 cluster nodes
```


```bash
docker run -d --name redis \
-p 6379:6379 \
redis:7-alpine
docker logs -f redis
redis-cli -h 127.0.0.1 -p 6379 ping
```