# Event Ledger System

A distributed microservices system for processing financial transaction events with strict idempotency guarantees, out-of-order tolerance, and comprehensive observability.

## Overview

The Event Ledger system handles financial transaction events from multiple upstream systems that may arrive:
- **Out of order** — events with earlier timestamps arriving after later ones
- **Duplicated** — the same event sent multiple times

The system ensures correctness despite these challenges while maintaining high availability and operational visibility.


### Microservices

#### Event Gateway API (Port 8000)
The public-facing entry point for all client requests.

**Responsibilities:**
- Receives and validates transaction events
- Enforces idempotency using event IDs
- Stores event records in local database
- Calls Account Service to apply transactions
- Implements circuit breaker for resilience
- Propagates trace IDs for distributed tracing

**Endpoints:**
| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/events` | Submit a transaction event |
| `GET` | `/events/{id}` | Retrieve a single event by ID |
| `GET` | `/events?account={accountId}` | List events for an account (ordered by eventTimestamp) |
| `GET` | `/health` | Health check with diagnostics |

#### Account Service (Port 8001)
Internal-only service managing account state and balances.

**Responsibilities:**
- Manages account balances
- Applies transactions to accounts
- Maintains transaction history
- Provides balance queries

**Endpoints:**
| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/accounts/{accountId}/transactions` | Apply a transaction to an account |
| `GET` | `/accounts/{accountId}/balance` | Get current balance for an account |
| `GET` | `/accounts/{accountId}` | Get account details and recent transactions |
| `GET` | `/health` | Health check with diagnostics |

## Event Payload

Submitted to `POST /events` on the Gateway:

```json
{
  "eventId": "evt-001",
  "accountId": "acct-123",
  "type": "CREDIT",
  "amount": 150.00,
  "currency": "USD",
  "eventTimestamp": "2026-05-15T14:02:11Z",
  "metadata": {
    "source": "mainframe-batch",
    "batchId": "B-9042"
  }
}