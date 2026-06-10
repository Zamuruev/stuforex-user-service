CREATE TABLE IF NOT EXISTS partner_ads (
                                           id           UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    company      VARCHAR(100) NOT NULL,
    tagline      VARCHAR(100),
    description  TEXT,
    cta          VARCHAR(50)  NOT NULL DEFAULT 'Подробнее →',
    url          TEXT         NOT NULL,
    accent_color VARCHAR(200) NOT NULL DEFAULT 'bg-violet-600 hover:bg-violet-700',
    bg_color     VARCHAR(200) NOT NULL DEFAULT 'bg-violet-50 dark:bg-violet-900/10',
    border_color VARCHAR(200) NOT NULL DEFAULT 'border-violet-200 dark:border-violet-800/50',
    logo         VARCHAR(10),
    active       BOOLEAN      NOT NULL DEFAULT TRUE,
    sort_order   INTEGER      NOT NULL DEFAULT 0,
    created_at   TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMP    NOT NULL DEFAULT NOW()
    );

INSERT INTO partner_ads (company, tagline, description, cta, url, accent_color, bg_color, border_color, logo, active, sort_order) VALUES
                                                                                                                                      ('Skillbox',  'Онлайн-образование', 'Получи профессию и начни зарабатывать больше. Первый месяц бесплатно.', 'Попробовать →',    'https://skillbox.ru',  'bg-violet-600 hover:bg-violet-700', 'bg-violet-50 dark:bg-violet-900/10',  'border-violet-200 dark:border-violet-800/50', 'SK', true,  1),
                                                                                                                                      ('Tinkoff',   'Дебетовая карта',    'Получай кэшбэк за каждый заказ. Карта с бесплатным обслуживанием.',  'Оформить карту →', 'https://tinkoff.ru',   'bg-yellow-400 hover:bg-yellow-500',  'bg-yellow-50 dark:bg-yellow-900/10',  'border-yellow-200 dark:border-yellow-800/50', 'TK', true,  2),
                                                                                                                                      ('Нетология', 'IT-специальности',   'Стань разработчиком или аналитиком. Трудоустройство в подарок.',     'Выбрать курс →',   'https://netology.ru',  'bg-emerald-600 hover:bg-emerald-700','bg-emerald-50 dark:bg-emerald-900/10','border-emerald-200 dark:border-emerald-800/50','НТ', true,  3),
                                                                                                                                      ('GeekBrains','Программирование',   'Научись программировать с нуля. Гарантия трудоустройства.',           'Начать учиться →', 'https://gb.ru',        'bg-blue-600 hover:bg-blue-700',      'bg-blue-50 dark:bg-blue-900/10',     'border-blue-200 dark:border-blue-800/50',    'GB', false, 4);
