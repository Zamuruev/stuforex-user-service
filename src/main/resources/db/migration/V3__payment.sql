-- Cross-service tables: task-service
CREATE TABLE IF NOT EXISTS categories (
    id    UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name  VARCHAR(255),
    slug  VARCHAR(255),
    icon  VARCHAR(255),
    color VARCHAR(255)
);

CREATE TABLE IF NOT EXISTS tasks (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    title        VARCHAR(255) NOT NULL,
    description  TEXT NOT NULL,
    customer_id  UUID,
    executor_id  UUID,
    category_id  UUID,
    budget_min   DECIMAL(10,2),
    budget_max   DECIMAL(10,2),
    agreed_price DECIMAL(10,2),
    deadline     TIMESTAMP,
    status       VARCHAR(50) DEFAULT 'DRAFT',
    subject      VARCHAR(255),
    tags         TEXT[],
    file_urls    TEXT[],
    created_at   TIMESTAMP DEFAULT NOW(),
    updated_at   TIMESTAMP DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS proposals (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    task_id       UUID NOT NULL,
    executor_id   UUID NOT NULL,
    price         DECIMAL(10,2) NOT NULL,
    message       TEXT,
    delivery_days INTEGER,
    status        VARCHAR(50) DEFAULT 'PENDING',
    created_at    TIMESTAMP DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS reviews (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    task_id     UUID NOT NULL,
    reviewer_id UUID NOT NULL,
    reviewee_id UUID NOT NULL,
    rating      INTEGER NOT NULL,
    comment     VARCHAR(1000),
    created_at  TIMESTAMP DEFAULT NOW(),
    UNIQUE (task_id, reviewer_id)
);

-- Планы подписки
CREATE TABLE IF NOT EXISTS subscription_plans (
                                                  id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name        VARCHAR(50) NOT NULL,
    price       DECIMAL(10,2) NOT NULL,
    proposals_per_month INTEGER,
    has_priority BOOLEAN DEFAULT FALSE,
    has_verification BOOLEAN DEFAULT FALSE,
    features    TEXT,
    created_at  TIMESTAMP DEFAULT NOW()
    );

CREATE TABLE IF NOT EXISTS user_subscriptions (
                                                  id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID NOT NULL REFERENCES users(id),
    plan_name       VARCHAR(50) NOT NULL,
    status          VARCHAR(50) DEFAULT 'ACTIVE',
    started_at      TIMESTAMP DEFAULT NOW(),
    expires_at      TIMESTAMP,
    proposals_used  INTEGER DEFAULT 0,
    created_at      TIMESTAMP DEFAULT NOW()
    );

CREATE TABLE IF NOT EXISTS escrow_accounts (
                                               id                   UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    task_id              UUID NOT NULL REFERENCES tasks(id),
    customer_id          UUID NOT NULL REFERENCES users(id),
    executor_id          UUID REFERENCES users(id),
    amount               DECIMAL(10,2) NOT NULL,
    advance_percent      INTEGER DEFAULT 0,
    advance_amount       DECIMAL(10,2) DEFAULT 0,
    platform_fee_percent DECIMAL(5,2) DEFAULT 10,
    platform_fee         DECIMAL(10,2) DEFAULT 0,
    status               VARCHAR(50) DEFAULT 'PENDING',
    payment_id           VARCHAR(255),
    created_at           TIMESTAMP DEFAULT NOW(),
    funded_at            TIMESTAMP,
    completed_at         TIMESTAMP
    );

CREATE TABLE IF NOT EXISTS transactions (
                                            id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    escrow_id   UUID REFERENCES escrow_accounts(id),
    user_id     UUID REFERENCES users(id),
    type        VARCHAR(50) NOT NULL,
    amount      DECIMAL(10,2) NOT NULL,
    description TEXT,
    external_id VARCHAR(255),
    created_at  TIMESTAMP DEFAULT NOW()
    );

CREATE TABLE IF NOT EXISTS task_boosts (
                                           id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    task_id     UUID NOT NULL REFERENCES tasks(id),
    customer_id UUID NOT NULL REFERENCES users(id),
    price       DECIMAL(10,2) NOT NULL,
    hours       INTEGER NOT NULL,
    started_at  TIMESTAMP DEFAULT NOW(),
    expires_at  TIMESTAMP NOT NULL
    );

CREATE TABLE IF NOT EXISTS verifications (
                                             id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id      UUID NOT NULL REFERENCES users(id),
    status       VARCHAR(50) DEFAULT 'PENDING',
    document_url TEXT,
    payment_id   VARCHAR(255),
    created_at   TIMESTAMP DEFAULT NOW(),
    reviewed_at  TIMESTAMP
    );

ALTER TABLE tasks ADD COLUMN IF NOT EXISTS advance_percent INTEGER DEFAULT 0;
ALTER TABLE tasks ADD COLUMN IF NOT EXISTS advance_amount DECIMAL(10,2);
ALTER TABLE tasks ADD COLUMN IF NOT EXISTS is_boosted BOOLEAN DEFAULT FALSE;
ALTER TABLE tasks ADD COLUMN IF NOT EXISTS boost_expires_at TIMESTAMP;

ALTER TABLE users ADD COLUMN IF NOT EXISTS subscription_plan VARCHAR(50) DEFAULT 'LITE';
ALTER TABLE users ADD COLUMN IF NOT EXISTS is_verified_specialist BOOLEAN DEFAULT FALSE;
ALTER TABLE users ADD COLUMN IF NOT EXISTS proposals_this_month INTEGER DEFAULT 0;
ALTER TABLE users ADD COLUMN IF NOT EXISTS proposals_reset_at TIMESTAMP;

INSERT INTO subscription_plans (name, price, proposals_per_month, has_priority, features) VALUES
                                                                                              ('LITE', 0,   5,    false, 'До 5 откликов в месяц, базовый профиль'),
                                                                                              ('BASE', 299, 30,   false, 'До 30 откликов, расширенный профиль, статистика'),
                                                                                              ('PRO',  799, NULL, true,  'Безлимит откликов, приоритет в выдаче, верификация включена')
    ON CONFLICT DO NOTHING;
