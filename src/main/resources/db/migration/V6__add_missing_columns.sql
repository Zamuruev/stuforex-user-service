-- task_boosts — добавить недостающие колонки
ALTER TABLE task_boosts
    ADD COLUMN IF NOT EXISTS status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    ADD COLUMN IF NOT EXISTS payment_provider VARCHAR(50) DEFAULT 'MOCK',
    ADD COLUMN IF NOT EXISTS created_at TIMESTAMP DEFAULT NOW();

-- escrow_accounts
CREATE TABLE IF NOT EXISTS escrow_accounts (
                                               id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    task_id UUID NOT NULL,
    customer_id UUID NOT NULL,
    executor_id UUID,
    amount NUMERIC(10,2) NOT NULL,
    advance_percent INTEGER DEFAULT 0,
    advance_amount NUMERIC(10,2) DEFAULT 0,
    platform_fee_percent NUMERIC(5,2) DEFAULT 10,
    platform_fee NUMERIC(10,2) DEFAULT 0,
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    payment_id VARCHAR(255),
    created_at TIMESTAMP DEFAULT NOW(),
    funded_at TIMESTAMP,
    completed_at TIMESTAMP
    );
CREATE INDEX IF NOT EXISTS idx_escrow_task ON escrow_accounts(task_id);

-- user_subscriptions — добавить payment_provider если отсутствует
ALTER TABLE user_subscriptions
    ADD COLUMN IF NOT EXISTS payment_provider VARCHAR(50) DEFAULT 'MOCK';
