# syntax=docker/dockerfile:1
FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /app
COPY . /app

RUN chmod +x ./gradlew && ./gradlew :server:installDist --no-daemon --console=plain

FROM eclipse-temurin:21-jre-alpine AS runner

WORKDIR /app
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser

COPY --from=builder /app/server/build/install/server /app/

ENV PORT=8080
ENV HOST=0.0.0.0
ENV LOG_LEVEL=INFO

EXPOSE 8080

HEALTHCHECK --interval=15s --timeout=3s --start-period=10s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://127.0.0.1:${PORT}/health || exit 1

ENTRYPOINT ["/app/bin/server"]

