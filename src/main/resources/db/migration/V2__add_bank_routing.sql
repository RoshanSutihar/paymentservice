ALTER TABLE ledger_entries ADD COLUMN source_routing_number VARCHAR(9);
CREATE INDEX idx_ledger_source ON ledger_entries (source_routing_number);