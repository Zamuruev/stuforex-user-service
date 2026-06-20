-- ── University API Keys ──────────────────────────────────────────────────────
-- API-ключи для деканатов вузов. Один вуз — один активный ключ.

CREATE TABLE IF NOT EXISTS public.university_api_keys (
                                                          id            uuid    DEFAULT gen_random_uuid() NOT NULL,
    university_id uuid    NULL,          -- NULL если вуз ещё не в БД (только по названию)
    university_name varchar(500) NOT NULL,
    api_key       varchar(64)  NOT NULL,
    contact_email varchar(255) NOT NULL,
    position      varchar(255) NULL,
    comment       text         NULL,
    status        varchar(20)  DEFAULT 'PENDING' NOT NULL,  -- PENDING | ACTIVE | REVOKED
    created_at    timestamp    DEFAULT now() NOT NULL,
    approved_at   timestamp    NULL,
    last_used_at  timestamp    NULL,

    CONSTRAINT university_api_keys_pkey       PRIMARY KEY (id),
    CONSTRAINT university_api_keys_key_unique UNIQUE (api_key),
    CONSTRAINT university_api_keys_status_check
    CHECK (status IN ('PENDING','ACTIVE','REVOKED')),
    CONSTRAINT university_api_keys_university_fk
    FOREIGN KEY (university_id) REFERENCES public.universities(id) ON DELETE SET NULL
    );

CREATE INDEX IF NOT EXISTS idx_uni_api_keys_key
    ON public.university_api_keys USING btree (api_key);

CREATE INDEX IF NOT EXISTS idx_uni_api_keys_university
    ON public.university_api_keys USING btree (university_id);

CREATE INDEX IF NOT EXISTS idx_uni_api_keys_status
    ON public.university_api_keys USING btree (status);
