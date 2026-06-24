# Event Ledger System

A production-ready distributed microservices system for processing financial transaction events with strict idempotency guarantees, out-of-order tolerance, and comprehensive observability.

**Table of Contents:**
- [Overview](#overview)
- [Architecture](#architecture)
- [Features](#features)
- [System Components](#system-components)
- [Technology Stack](#technology-stack)
- [Security](#security)
- [API Documentation](#api-documentation)
- [Getting Started](#getting-started)
- [Docker Setup](#docker-setup)
- [Development](#development)
- [Testing](#testing)
- [Monitoring & Observability](#monitoring--observability)
- [Production Deployment](#production-deployment)
- [Troubleshooting](#troubleshooting)
- [Contributing](#contributing)

---

## Overview

The Event Ledger system is designed to reliably process financial transaction events from multiple upstream systems that may have network issues causing:

- **Out-of-order events** — transactions with earlier timestamps arriving after later ones
- **Duplicate events** — the same event sent multiple times due to retries
- **Partial failures** — some events succeeding while others fail

The system ensures:
- ✅ **Idempotency** - Same event processed only once regardless of retries
- ✅ **Correctness** - Out-of-order events applied correctly to account balances
- ✅ **High Availability** - Resilience patterns prevent cascading failures
- ✅ **Observability** - Complete distributed tracing and metrics collection
- ✅ **Security** - OAuth2-based authentication and authorization

### Key Guarantees

| Guarantee | Implementation |
|-----------|-----------------|
| **Idempotency** | Event ID deduplication before processing |
| **Correctness** | Events reordered chronologically before balance calculation |
| **Resilience** | Circuit breaker, retry, and timeout patterns |
| **Traceability** | Distributed trace IDs across service boundaries |
| **Security** | OAuth2 Bearer tokens with scope-based access control |

---

## Architecture

### High-Level System Architecture
