# EventLedger
Event Ledger – Distributed Financial Transaction Processing System
A microservices-based event ledger system that processes financial transactions with high reliability and observability. The system handles out-of-order events, enforces idempotency, and maintains accurate account balances across distributed services.

Key Features
🔄 Out-of-order event handling – Events processed in chronological order regardless of arrival sequence
🛡️ Idempotent transactions – Duplicate submissions are safely rejected
📊 Distributed tracing – Request correlation across microservices with OpenTelemetry
📈 Observability – Structured logging, health checks, and custom metrics
🔌 Resilient architecture – Circuit breaker pattern for graceful degradation
📦 Docker ready – Complete Docker Compose setup for local development
Architecture
Event Gateway API (public-facing) – Receives and validates transaction events
Account Service (internal) – Manages account balances and transaction history
Tech Stack
Language: Java 17
Framework: Spring Boot 4.1.0
Build Tool: Gradle
Database: H2 (embedded)
API Spec: OpenAPI 3.0
Containerization: Docker & Docker Compose
Observability: OpenTelemetry, Structured Logging
