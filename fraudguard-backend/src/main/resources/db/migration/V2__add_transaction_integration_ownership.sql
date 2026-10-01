-- Existing transactions predate integration ownership and remain unassigned.
-- New transactions are associated with their authenticated integration in the service layer.
ALTER TABLE transactions
    ADD COLUMN integration_id BIGINT;

ALTER TABLE transactions
    ADD CONSTRAINT fk_transactions_integration
        FOREIGN KEY (integration_id) REFERENCES integrations(id);

CREATE INDEX idx_transactions_integration_id
    ON transactions(integration_id);
