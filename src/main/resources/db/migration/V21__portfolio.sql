-- ── Portfolio Cases ───────────────────────────────────────────────────────────
-- Хранит только метаданные кейса.
-- Основные данные (title, budget, skills, review) джойнятся из tasks/reviews/escrow.

CREATE TABLE IF NOT EXISTS public.portfolio_cases (
                                                      id            uuid    DEFAULT gen_random_uuid() NOT NULL,
    executor_id   uuid    NOT NULL,
    task_id       uuid    NOT NULL,
    -- PUBLIC | LINK | CLIENTS | HIDDEN
    visibility    varchar(20) DEFAULT 'PUBLIC'  NOT NULL,
    ai_summary    text    NULL,
    -- SHA-256(task_id + executor_id + completed_at + agreed_price)
    proof_hash    varchar(128) NOT NULL,
    view_count    int4    DEFAULT 0 NOT NULL,
    created_at    timestamp DEFAULT now() NOT NULL,

    CONSTRAINT portfolio_cases_pkey         PRIMARY KEY (id),
    CONSTRAINT portfolio_cases_task_id_key  UNIQUE (task_id),
    CONSTRAINT portfolio_cases_executor_fk  FOREIGN KEY (executor_id)
    REFERENCES public.users(id) ON DELETE CASCADE,
    CONSTRAINT portfolio_cases_task_fk      FOREIGN KEY (task_id)
    REFERENCES public.tasks(id) ON DELETE CASCADE,
    CONSTRAINT portfolio_cases_visibility_check
    CHECK (visibility IN ('PUBLIC','LINK','CLIENTS','HIDDEN'))
    );

CREATE INDEX IF NOT EXISTS idx_portfolio_executor
    ON public.portfolio_cases USING btree (executor_id);

CREATE INDEX IF NOT EXISTS idx_portfolio_executor_visibility
    ON public.portfolio_cases USING btree (executor_id, visibility);

CREATE INDEX IF NOT EXISTS idx_portfolio_task
    ON public.portfolio_cases USING btree (task_id);
