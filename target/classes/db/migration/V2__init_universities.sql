-- ============================================================
-- StudEx: Users table additions
-- ============================================================
ALTER TABLE users ADD COLUMN IF NOT EXISTS phone          VARCHAR(50);
ALTER TABLE users ADD COLUMN IF NOT EXISTS telegram       VARCHAR(100);
ALTER TABLE users ADD COLUMN IF NOT EXISTS skills         TEXT;
ALTER TABLE users ADD COLUMN IF NOT EXISTS specialization VARCHAR(255);

-- ============================================================
-- StudEx: Universities module
-- ============================================================
CREATE TABLE IF NOT EXISTS universities (
                                            id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name        VARCHAR(500) NOT NULL,
    short_name  VARCHAR(50),
    city        VARCHAR(255),
    type        VARCHAR(100),
    description TEXT,
    website     VARCHAR(500),
    color       VARCHAR(20)  DEFAULT '#534AB7',
    bg_color    VARCHAR(20)  DEFAULT '#EEEDFE',
    created_by  UUID REFERENCES users(id),
    created_at  TIMESTAMP DEFAULT NOW(),
    updated_at  TIMESTAMP DEFAULT NOW()
    );

CREATE TABLE IF NOT EXISTS institutes (
                                          id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    university_id UUID NOT NULL REFERENCES universities(id) ON DELETE CASCADE,
    name          VARCHAR(500) NOT NULL,
    created_at    TIMESTAMP DEFAULT NOW()
    );

CREATE TABLE IF NOT EXISTS departments (
                                           id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    institute_id UUID NOT NULL REFERENCES institutes(id) ON DELETE CASCADE,
    name         VARCHAR(500) NOT NULL,
    created_at   TIMESTAMP DEFAULT NOW()
    );

CREATE TABLE IF NOT EXISTS professors (
                                          id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    department_id UUID NOT NULL REFERENCES departments(id) ON DELETE CASCADE,
    name          VARCHAR(500) NOT NULL,
    subject       VARCHAR(500),
    difficulty    INTEGER DEFAULT 3 CHECK (difficulty >= 1 AND difficulty <= 5),
    is_fair       BOOLEAN DEFAULT TRUE,
    created_by    UUID REFERENCES users(id),
    created_at    TIMESTAMP DEFAULT NOW()
    );

CREATE TABLE IF NOT EXISTS university_reviews (
                                                  id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    university_id UUID NOT NULL REFERENCES universities(id) ON DELETE CASCADE,
    author_id     UUID NOT NULL REFERENCES users(id),
    rating        INTEGER NOT NULL CHECK (rating >= 1 AND rating <= 5),
    text          TEXT,
    created_at    TIMESTAMP DEFAULT NOW()
    );

CREATE TABLE IF NOT EXISTS professor_reviews (
                                                 id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    professor_id UUID NOT NULL REFERENCES professors(id) ON DELETE CASCADE,
    author_id    UUID NOT NULL REFERENCES users(id),
    rating       INTEGER NOT NULL CHECK (rating >= 1 AND rating <= 5),
    text         TEXT,
    created_at   TIMESTAMP DEFAULT NOW()
    );

-- Seed universities
INSERT INTO universities (name, short_name, city, type, color, bg_color) VALUES
                                                                             ('МГУ им. М.В. Ломоносова',           'МГУ',   'Москва',           'Классический',     '#185FA5', '#E6F1FB'),
                                                                             ('НИУ Высшая школа экономики',         'ВШЭ',   'Москва',           'Исследовательский','#0F6E56', '#E1F5EE'),
                                                                             ('Университет ИТМО',                   'ИТМО',  'Санкт-Петербург',  'Технический',      '#534AB7', '#EEEDFE'),
                                                                             ('МГТУ им. Н.Э. Баумана',             'МГТУ',  'Москва',           'Технический',      '#993556', '#FBEAF0'),
                                                                             ('СПбГУ',                             'СПбГУ', 'Санкт-Петербург',  'Классический',     '#854F0B', '#FAEEDA')
    ON CONFLICT DO NOTHING;
