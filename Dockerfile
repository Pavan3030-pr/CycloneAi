# CycloneAI — one image, one URL.
#
# Three stages: build the console, build the service with the console inside it, run it. The result
# is a single Cloud Run service that answers the public site, the console and the API on the same
# origin, so the browser never makes a cross-origin request and no CORS policy has to be right at
# deploy time.
#
# Build from the repository root:  docker build -t cycloneai .
#
# The console is compiled without VITE_API_BASE_URL, which makes every API call relative. That is
# what makes same-origin work; setting it here would reintroduce the cross-origin deployment this
# image exists to avoid.

# ---------------------------------------------------------------- stage 1: console
FROM node:22-alpine AS web
WORKDIR /web

COPY frontend/package.json frontend/package-lock.json ./
RUN npm ci --no-fund --no-audit

COPY frontend/ ./
RUN npm run build

# ---------------------------------------------------------------- stage 2: service + site
FROM maven:3.9-eclipse-temurin-21 AS java
WORKDIR /workspace

# Dependencies first, so a source edit does not re-download the world.
COPY backend/pom.xml ./
RUN mvn -B -q dependency:go-offline

COPY backend/src ./src

# The built console is packaged as classpath resources. StaticSiteConfiguration picks it up and
# registers the client-side routes; the uncompressed jar is left alone otherwise.
COPY --from=web /web/dist ./src/main/resources/static

RUN mvn -B -q clean package -DskipTests

# ---------------------------------------------------------------- stage 3: runtime
FROM eclipse-temurin:21-jre
WORKDIR /app

RUN useradd --system --uid 10001 --create-home appuser
COPY --from=java /workspace/target/*.jar app.jar
RUN chown -R appuser:appuser /app
USER appuser

# Cloud Run injects PORT; application.yml reads SERVER_PORT, so the two are bridged here rather
# than making every deployment set both.
ENV SERVER_PORT=8080
EXPOSE 8080

ENTRYPOINT ["sh", "-c", "exec java -XX:MaxRAMPercentage=75 -Dserver.port=${PORT:-8080} -jar /app/app.jar"]
