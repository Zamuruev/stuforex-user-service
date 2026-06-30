-- ── Specializations reference table ──────────────────────────────────────────
CREATE TABLE IF NOT EXISTS specializations (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code         VARCHAR(20),
    name         VARCHAR(500) NOT NULL,
    degree_level VARCHAR(30)  NOT NULL,
    field_name   VARCHAR(255),
    created_at   TIMESTAMP DEFAULT NOW()
);
CREATE UNIQUE INDEX IF NOT EXISTS idx_spec_code ON specializations(code) WHERE code IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_spec_level ON specializations(degree_level);
CREATE INDEX IF NOT EXISTS idx_spec_name  ON specializations(name);

-- ── User education records ────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS user_education (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id             UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    university_id       UUID REFERENCES universities(id) ON DELETE SET NULL,
    university_name     VARCHAR(500),
    institute_id        UUID REFERENCES institutes(id) ON DELETE SET NULL,
    specialization_id   UUID REFERENCES specializations(id) ON DELETE SET NULL,
    specialization_name VARCHAR(500),
    degree_type         VARCHAR(30) NOT NULL,
    enrollment_year     SMALLINT,
    graduation_year     SMALLINT,
    is_current          BOOLEAN NOT NULL DEFAULT TRUE,
    is_primary          BOOLEAN NOT NULL DEFAULT FALSE,
    created_at          TIMESTAMP DEFAULT NOW(),
    updated_at          TIMESTAMP DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_user_edu_user ON user_education(user_id);

-- ── Specializations seed data ─────────────────────────────────────────────────

-- IT — Бакалавриат
INSERT INTO specializations (code, name, degree_level, field_name) VALUES
('09.03.01', 'Информатика и вычислительная техника', 'BACHELOR', 'Информатика и вычислительная техника'),
('09.03.02', 'Информационные системы и технологии', 'BACHELOR', 'Информатика и вычислительная техника'),
('09.03.03', 'Прикладная информатика', 'BACHELOR', 'Информатика и вычислительная техника'),
('09.03.04', 'Программная инженерия', 'BACHELOR', 'Информатика и вычислительная техника'),
('10.03.01', 'Информационная безопасность', 'BACHELOR', 'Информационная безопасность'),
('10.03.02', 'Информационная безопасность телекоммуникационных систем', 'BACHELOR', 'Информационная безопасность')
ON CONFLICT DO NOTHING;

-- IT — Магистратура
INSERT INTO specializations (code, name, degree_level, field_name) VALUES
('09.04.01', 'Информатика и вычислительная техника', 'MASTER', 'Информатика и вычислительная техника'),
('09.04.02', 'Информационные системы и технологии', 'MASTER', 'Информатика и вычислительная техника'),
('09.04.03', 'Прикладная информатика', 'MASTER', 'Информатика и вычислительная техника'),
('09.04.04', 'Программная инженерия', 'MASTER', 'Информатика и вычислительная техника'),
('10.04.01', 'Информационная безопасность', 'MASTER', 'Информационная безопасность')
ON CONFLICT DO NOTHING;

-- Математика/Физика — Бакалавриат
INSERT INTO specializations (code, name, degree_level, field_name) VALUES
('01.03.01', 'Математика', 'BACHELOR', 'Математика и механика'),
('01.03.02', 'Прикладная математика и информатика', 'BACHELOR', 'Математика и механика'),
('01.03.04', 'Прикладная математика', 'BACHELOR', 'Математика и механика'),
('03.03.01', 'Прикладные математика и физика', 'BACHELOR', 'Физика и астрономия'),
('03.03.02', 'Физика', 'BACHELOR', 'Физика и астрономия')
ON CONFLICT DO NOTHING;

-- Математика/Физика — Магистратура
INSERT INTO specializations (code, name, degree_level, field_name) VALUES
('01.04.01', 'Математика', 'MASTER', 'Математика и механика'),
('01.04.02', 'Прикладная математика и информатика', 'MASTER', 'Математика и механика'),
('03.04.01', 'Прикладные математика и физика', 'MASTER', 'Физика и астрономия'),
('03.04.02', 'Физика', 'MASTER', 'Физика и астрономия')
ON CONFLICT DO NOTHING;

-- Экономика — Бакалавриат
INSERT INTO specializations (code, name, degree_level, field_name) VALUES
('38.03.01', 'Экономика', 'BACHELOR', 'Экономика и управление'),
('38.03.02', 'Менеджмент', 'BACHELOR', 'Экономика и управление'),
('38.03.03', 'Управление персоналом', 'BACHELOR', 'Экономика и управление'),
('38.03.04', 'Государственное и муниципальное управление', 'BACHELOR', 'Экономика и управление'),
('38.03.05', 'Бизнес-информатика', 'BACHELOR', 'Экономика и управление'),
('38.03.06', 'Торговое дело', 'BACHELOR', 'Экономика и управление')
ON CONFLICT DO NOTHING;

-- Экономика — Магистратура
INSERT INTO specializations (code, name, degree_level, field_name) VALUES
('38.04.01', 'Экономика', 'MASTER', 'Экономика и управление'),
('38.04.02', 'Менеджмент', 'MASTER', 'Экономика и управление'),
('38.04.03', 'Управление персоналом', 'MASTER', 'Экономика и управление'),
('38.04.04', 'Государственное и муниципальное управление', 'MASTER', 'Экономика и управление')
ON CONFLICT DO NOTHING;

-- Юриспруденция
INSERT INTO specializations (code, name, degree_level, field_name) VALUES
('40.03.01', 'Юриспруденция', 'BACHELOR', 'Юриспруденция'),
('40.04.01', 'Юриспруденция', 'MASTER', 'Юриспруденция'),
('40.05.01', 'Правовое обеспечение национальной безопасности', 'SPECIALIST', 'Юриспруденция'),
('40.05.02', 'Правоохранительная деятельность', 'SPECIALIST', 'Юриспруденция')
ON CONFLICT DO NOTHING;

-- Медицина — Специалитет
INSERT INTO specializations (code, name, degree_level, field_name) VALUES
('31.05.01', 'Лечебное дело', 'SPECIALIST', 'Клиническая медицина'),
('31.05.02', 'Педиатрия', 'SPECIALIST', 'Клиническая медицина'),
('31.05.03', 'Стоматология', 'SPECIALIST', 'Клиническая медицина'),
('31.05.04', 'Медико-профилактическое дело', 'SPECIALIST', 'Клиническая медицина'),
('32.05.01', 'Медико-профилактическое дело', 'SPECIALIST', 'Науки о здоровье')
ON CONFLICT DO NOTHING;

-- Медицина — Ординатура
INSERT INTO specializations (code, name, degree_level, field_name) VALUES
('31.08.01', 'Акушерство и гинекология', 'RESIDENCY', 'Клиническая медицина'),
('31.08.02', 'Анестезиология-реаниматология', 'RESIDENCY', 'Клиническая медицина'),
('31.08.08', 'Кардиология', 'RESIDENCY', 'Клиническая медицина'),
('31.08.10', 'Неврология', 'RESIDENCY', 'Клиническая медицина'),
('31.08.12', 'Педиатрия', 'RESIDENCY', 'Клиническая медицина'),
('31.08.14', 'Психиатрия', 'RESIDENCY', 'Клиническая медицина'),
('31.08.39', 'Онкология', 'RESIDENCY', 'Клиническая медицина'),
('31.08.49', 'Терапия', 'RESIDENCY', 'Клиническая медицина'),
('31.08.64', 'Хирургия', 'RESIDENCY', 'Клиническая медицина')
ON CONFLICT DO NOTHING;

-- Медицина — Аспирантура
INSERT INTO specializations (code, name, degree_level, field_name) VALUES
('31.06.01', 'Клиническая медицина', 'PHDS', 'Клиническая медицина'),
('32.06.01', 'Общественное здоровье и здравоохранение', 'PHDS', 'Науки о здоровье')
ON CONFLICT DO NOTHING;

-- Строительство и архитектура
INSERT INTO specializations (code, name, degree_level, field_name) VALUES
('07.03.01', 'Архитектура', 'BACHELOR', 'Архитектура'),
('07.03.04', 'Градостроительство', 'BACHELOR', 'Архитектура'),
('07.04.01', 'Архитектура', 'MASTER', 'Архитектура'),
('08.03.01', 'Строительство', 'BACHELOR', 'Техника и технологии строительства'),
('08.04.01', 'Строительство', 'MASTER', 'Техника и технологии строительства')
ON CONFLICT DO NOTHING;

-- Машиностроение и техника
INSERT INTO specializations (code, name, degree_level, field_name) VALUES
('15.03.01', 'Машиностроение', 'BACHELOR', 'Машиностроение'),
('15.03.02', 'Технологические машины и оборудование', 'BACHELOR', 'Машиностроение'),
('15.03.04', 'Автоматизация технологических процессов и производств', 'BACHELOR', 'Машиностроение'),
('15.04.01', 'Машиностроение', 'MASTER', 'Машиностроение')
ON CONFLICT DO NOTHING;

-- Электроника и энергетика
INSERT INTO specializations (code, name, degree_level, field_name) VALUES
('11.03.01', 'Радиотехника', 'BACHELOR', 'Электроника, радиотехника и системы связи'),
('11.03.02', 'Инфокоммуникационные технологии и системы связи', 'BACHELOR', 'Электроника, радиотехника и системы связи'),
('11.03.04', 'Электроника и наноэлектроника', 'BACHELOR', 'Электроника, радиотехника и системы связи'),
('13.03.01', 'Теплоэнергетика и теплотехника', 'BACHELOR', 'Электро- и теплоэнергетика'),
('13.03.02', 'Электроэнергетика и электротехника', 'BACHELOR', 'Электро- и теплоэнергетика')
ON CONFLICT DO NOTHING;

-- Химия и биология
INSERT INTO specializations (code, name, degree_level, field_name) VALUES
('04.03.01', 'Химия', 'BACHELOR', 'Химические науки'),
('04.03.02', 'Химия, физика и механика материалов', 'BACHELOR', 'Химические науки'),
('06.03.01', 'Биология', 'BACHELOR', 'Биологические науки'),
('19.03.01', 'Биотехнология', 'BACHELOR', 'Промышленная экология и биотехнологии'),
('20.03.01', 'Техносферная безопасность', 'BACHELOR', 'Техносферная безопасность и природообустройство')
ON CONFLICT DO NOTHING;

-- Транспорт
INSERT INTO specializations (code, name, degree_level, field_name) VALUES
('23.03.01', 'Технология транспортных процессов', 'BACHELOR', 'Техника и технологии наземного транспорта'),
('23.03.02', 'Наземные транспортно-технологические комплексы', 'BACHELOR', 'Техника и технологии наземного транспорта'),
('23.03.03', 'Эксплуатация транспортно-технологических машин и комплексов', 'BACHELOR', 'Техника и технологии наземного транспорта')
ON CONFLICT DO NOTHING;

-- Гуманитарные науки
INSERT INTO specializations (code, name, degree_level, field_name) VALUES
('37.03.01', 'Психология', 'BACHELOR', 'Психологические науки'),
('37.04.01', 'Психология', 'MASTER', 'Психологические науки'),
('39.03.01', 'Социология', 'BACHELOR', 'Социология и социальная работа'),
('39.03.02', 'Социальная работа', 'BACHELOR', 'Социология и социальная работа'),
('41.03.01', 'Зарубежное регионоведение', 'BACHELOR', 'Политические науки и регионоведение'),
('41.03.04', 'Политология', 'BACHELOR', 'Политические науки и регионоведение'),
('42.03.01', 'Реклама и связи с общественностью', 'BACHELOR', 'СМИ и информационно-библиотечное дело'),
('42.03.02', 'Журналистика', 'BACHELOR', 'СМИ и информационно-библиотечное дело'),
('44.03.01', 'Педагогическое образование', 'BACHELOR', 'Образование и педагогические науки'),
('44.03.02', 'Психолого-педагогическое образование', 'BACHELOR', 'Образование и педагогические науки'),
('44.03.04', 'Профессиональное обучение', 'BACHELOR', 'Образование и педагогические науки'),
('45.03.01', 'Филология', 'BACHELOR', 'Языкознание и литературоведение'),
('45.03.02', 'Лингвистика', 'BACHELOR', 'Языкознание и литературоведение'),
('46.03.01', 'История', 'BACHELOR', 'История и археология'),
('47.03.01', 'Философия', 'BACHELOR', 'Философия, этика и религиоведение')
ON CONFLICT DO NOTHING;

-- Аспирантура (общие)
INSERT INTO specializations (code, name, degree_level, field_name) VALUES
('09.06.01', 'Информатика и вычислительная техника', 'PHDS', 'Информатика и вычислительная техника'),
('38.06.01', 'Экономика', 'PHDS', 'Экономика и управление'),
('40.06.01', 'Юриспруденция', 'PHDS', 'Юриспруденция'),
('44.06.01', 'Образование и педагогические науки', 'PHDS', 'Образование и педагогические науки')
ON CONFLICT DO NOTHING;
