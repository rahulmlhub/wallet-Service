# Wallet System – Spring Boot

A production-grade digital wallet system that supports deposits, withdrawals, transfers, real-time balance accuracy, concurrent transaction safety, and transaction auditing.

## Features

- Create wallet for user
- Deposit funds
- Withdraw funds
- Transfer funds atomically
- View wallet balance
- Get transaction history (paginated)
- Idempotency protection (referenceId)
- Wallet freeze/unfreeze control
- Transaction reversal (bonus)
- Strong concurrency control using PESSIMISTIC_WRITE locking
- Complete audit trail for all operations

---

## Tech Stack

- Java 17
- Spring Boot 3.x
- Spring Data JPA
- PostgreSQL / H2
- Maven
- Swagger/OpenAPI 3
- JUnit + Mockito

---

## API Endpoints

### Wallet Operations
Method | Endpoint | Description
-------|----------|-------------
POST | `/api/wallets` | Create new wallet
GET | `/api/wallets/{walletId}` | Get wallet details
GET | `/api/wallets/{walletId}/balance` | Get current balance
POST | `/api/wallets/{walletId}/deposit` | Deposit funds
POST | `/api/wallets/{walletId}/withdraw` | Withdraw funds
POST | `/api/wallets/transfer` | Transfer between wallets
POST | `/api/wallets/{walletId}/freeze` | Freeze wallet
POST | `/api/wallets/{walletId}/unfreeze` | Unfreeze wallet

### Transactions
Method | Endpoint | Description
-------|----------|-------------
GET | `/api/wallets/{walletId}/transactions` | Get transaction history (paginated)
GET | `/api/wallets/transactions/{transactionId}` | Get individual transaction
POST | `/api/wallets/transactions/{transactionId}/reverse` | Reverse transaction

---

## Database Schema

### Wallet Table
- id (UUID)
- user_id
- balance (BigDecimal)
- currency
- status (ACTIVE / FROZEN)
- created_at
- updated_at

### Transaction Table
- id (UUID)
- wallet_id
- type (DEPOSIT/WITHDRAW/TRANSFER_IN/TRANSFER_OUT/REVERSAL)
- amount
- balance_after
- reference_id
- remarks
- timestamp

---

## How to Run

### Prerequisites
- Java 17+
- Maven 3.x

### Steps
```sh
git clone <repo-url>
cd wallet-system
mvn clean install
mvn spring-boot:run
