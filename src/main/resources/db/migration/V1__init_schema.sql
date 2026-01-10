CREATE EXTENSION IF NOT EXISTS "uuid-ossp";


CREATE TABLE merchant_accounts (
    id BIGSERIAL PRIMARY KEY,
    merchant_id VARCHAR(100) NOT NULL UNIQUE,
    store_name VARCHAR(255) NOT NULL,
    secret_key TEXT NOT NULL,
    callback_url TEXT,
    status VARCHAR(20) NOT NULL CHECK (status IN ('ACTIVE', 'SUSPENDED')),
    bank_account_number VARCHAR(50),
    bank_routing_number VARCHAR(50),
    commission_type VARCHAR(20) NOT NULL DEFAULT 'PERCENTAGE' CHECK (commission_type IN ('PERCENTAGE', 'FIXED')),
    commission_value DECIMAL(10,4) NOT NULL DEFAULT 0.0,
    min_commission DECIMAL(15,2) DEFAULT 0.0,
    max_commission DECIMAL(15,2),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP

);


CREATE TABLE payment_intents (
    id BIGSERIAL PRIMARY KEY,
    merchant_id VARCHAR(100) NOT NULL,
    terminal_id VARCHAR(100) NOT NULL,
    session_id VARCHAR(100) NOT NULL UNIQUE,
    amount DECIMAL(15,2) NOT NULL,
    currency VARCHAR(10) DEFAULT 'USD',
    status VARCHAR(20) NOT NULL CHECK (status IN ('PENDING', 'COMPLETED', 'FAILED', 'CANCELLED')),
    expiry_time TIMESTAMP NOT NULL,
    transaction_ref VARCHAR(100) NOT NULL,
    callback_url TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (merchant_id) REFERENCES merchant_accounts(merchant_id)
);


CREATE TABLE payment_events (
    id BIGSERIAL PRIMARY KEY,
    payment_intent_id BIGINT NOT NULL,
    event_type VARCHAR(50) NOT NULL,
    payload JSONB,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (payment_intent_id) REFERENCES payment_intents(id)
);


CREATE TABLE rail_transfers (
    id BIGSERIAL PRIMARY KEY,
    payment_intent_id BIGINT NOT NULL,
    from_account VARCHAR(50) NOT NULL,
    to_account VARCHAR(50) NOT NULL,
    amount DECIMAL(15,2) NOT NULL,
    status VARCHAR(20) NOT NULL CHECK (status IN ('REQUESTED', 'ACKNOWLEDGED', 'SETTLED', 'FAILED')),
    settlement_date TIMESTAMP,
    response_payload JSONB,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (payment_intent_id) REFERENCES payment_intents(id)
);


CREATE TABLE fraud_checks (
    id BIGSERIAL PRIMARY KEY,
    payment_intent_id BIGINT NOT NULL,
    risk_score INTEGER DEFAULT 0,
    risk_level VARCHAR(20) CHECK (risk_level IN ('LOW', 'MEDIUM', 'HIGH')),
    rules_triggered JSONB,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (payment_intent_id) REFERENCES payment_intents(id)
);


CREATE TABLE failed_callbacks (
    id BIGSERIAL PRIMARY KEY,
    payment_intent_id BIGINT NOT NULL,
    callback_url TEXT NOT NULL,
    payload JSONB NOT NULL,
    retry_count INTEGER DEFAULT 0,
    last_attempt TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    error_message TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (payment_intent_id) REFERENCES payment_intents(id)
);


CREATE TABLE ledger_entries (
    id BIGSERIAL PRIMARY KEY,
    payment_intent_id BIGINT NOT NULL,
    account_number VARCHAR(50) NOT NULL,
    entry_type VARCHAR(10) NOT NULL CHECK (entry_type IN ('CREDIT', 'DEBIT')),
    amount DECIMAL(15,2) NOT NULL,
    balance_after DECIMAL(15,2),
    description VARCHAR(100) NOT NULL,
    reference_id VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (payment_intent_id) REFERENCES payment_intents(id)
);


CREATE TABLE transaction_commissions (
    id BIGSERIAL PRIMARY KEY,
    payment_intent_id BIGINT NOT NULL,
    transaction_amount DECIMAL(15,2) NOT NULL,
    commission_amount DECIMAL(15,2) NOT NULL,
    net_amount DECIMAL(15,2) NOT NULL,
    commission_rate DECIMAL(10,4),
    commission_type VARCHAR(20),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (payment_intent_id) REFERENCES payment_intents(id)
);


CREATE INDEX idx_payment_intents_session_id ON payment_intents(session_id);
CREATE INDEX idx_payment_intents_status ON payment_intents(status);
CREATE INDEX idx_payment_intents_merchant_id ON payment_intents(merchant_id);
CREATE INDEX idx_payment_events_payment_intent_id ON payment_events(payment_intent_id);
CREATE INDEX idx_payment_events_created_at ON payment_events(created_at);
CREATE INDEX idx_rail_transfers_payment_intent_id ON rail_transfers(payment_intent_id);
CREATE INDEX idx_rail_transfers_status ON rail_transfers(status);
CREATE INDEX idx_ledger_entries_account ON ledger_entries(account_number);
CREATE INDEX idx_ledger_entries_payment_intent ON ledger_entries(payment_intent_id);
CREATE INDEX idx_transaction_commissions_payment_intent ON transaction_commissions(payment_intent_id);


CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';


CREATE TRIGGER update_merchant_accounts_updated_at
    BEFORE UPDATE ON merchant_accounts
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_payment_intents_updated_at
    BEFORE UPDATE ON payment_intents
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_rail_transfers_updated_at
    BEFORE UPDATE ON rail_transfers
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();