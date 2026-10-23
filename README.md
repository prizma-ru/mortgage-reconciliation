# Mortgage Reconciliation

Сервис автоматической сверки ипотечных документов. Принимает `.docx`, разбирает его на блоки
(параграфы и ячейки таблиц) и извлекает значения полей по шаблонам, которые хранятся в PostgreSQL,
а не зашиты в код.

## Возможности

- **Разбор `.docx`** — Apache POI (`XWPFDocument`), блоки сохраняют исходный порядок; для ячеек
  таблиц сохраняются координаты `tableIndex`, `rowIndex`, `colIndex`.
- **Нормализация текста** — неразрывные и узкие пробелы, все виды тире и кавычек, многоточие,
  soft hyphen и zero-width символы приводятся к стандартному виду.
- **Реестр шаблонов в БД** — тип документа (`DDU`, `DKP`, …) определяет набор полей; для каждого поля
  задаются regex-паттерн, якорные фразы и правило нормализации. Новые правила добавляются миграцией,
  без правки кода.
- **Regex-экстрактор** — поиск по якорной фразе с окном в 1–2 предложения, при отсутствии якоря —
  по всему блоку; результат помечается уровнем уверенности.
- **Нормализация значений** — `MONEY` и `PERCENT` приводятся к `BigDecimal`, `FIO` — к единому
  регистру и пробелам, `DATE` разбирается из `dd.MM.yyyy` и `dd MMMM yyyy` в `LocalDate`.
- **Сверка с заявкой** — сравнение договора с JSON-заявкой по всем полям шаблона; каждое поле
  получает статус `OK`, `WARN`, `ERROR`, `MANUAL` или `MISSING`.
- **Отладочный эндпоинт** — отдаёт все блоки документа, чтобы проверить парсер.

## Стек

| Часть | Технологии |
|---|---|
| `backend/` | Java 17, Spring Boot 3.5.16 (Web, Data JPA, Validation, Actuator), PostgreSQL Driver, Flyway + `flyway-database-postgresql`, Apache POI 5.3.0, Maven Wrapper |
| `frontend/` | React 18, Vite 5, TypeScript 5 (strict) |
| `infra/` | Docker Compose, `postgres:16-alpine`, healthcheck через `pg_isready`, named volume |

## Структура репозитория

```text
.
├── backend/
│   ├── .mvn/wrapper/                 # Maven Wrapper
│   ├── src/main/java/com/mortgage/reconciliation/
│   │   ├── config/                   # CORS
│   │   ├── controller/               # REST-эндпоинты
│   │   ├── domain/                   # JPA-сущности Template, TemplateField
│   │   ├── dto/                      # DocBlock, ExtractedField
│   │   ├── repository/               # Spring Data репозитории
│   │   └── service/                  # чтение .docx, нормализация, извлечение
│   ├── src/main/resources/
│   │   ├── application.yml
│   │   └── db/migration/             # Flyway: V1__init.sql, V2__templates.sql
│   └── src/test/java/                # unit-тесты
├── frontend/
│   └── src/                          # React-страница со статусом бэкенда
└── infra/
    ├── docker-compose.yml
    └── .env.example
```

## Требования

- JDK 17 (без preview-фич);
- Docker (для PostgreSQL);
- Node.js 20+ и npm (для фронтенда).

## Запуск

### 1. База данных

```bash
cd infra
cp .env.example .env          # Windows PowerShell: Copy-Item .env.example .env
docker compose up -d
```

Compose читает `infra/.env`. Если файла нет, подставляются значения по умолчанию
(`mortgage_db` / `postgres` / `postgres`, порт `5432`). Проверка состояния:

```bash
docker compose ps             # STATUS должен быть healthy
```

### 2. Бэкенд

```bash
cd backend
./mvnw spring-boot:run        # Windows: .\mvnw.cmd spring-boot:run
```

При старте Flyway применяет миграции, Hibernate проверяет соответствие сущностей схеме
(`ddl-auto: validate`). Сервис слушает `http://localhost:8080`.

### 3. Фронтенд

```bash
cd frontend
npm install
npm run dev
```

Страница доступна на `http://localhost:5173` и показывает статус бэкенда: Vite проксирует
`/actuator/*` на `http://localhost:8080`.

## Конфигурация

Переменные окружения бэкенда (значения по умолчанию указаны в `application.yml`):

| Переменная | Назначение | По умолчанию |
|---|---|---|
| `DB_HOST` | хост PostgreSQL | `localhost` |
| `DB_PORT` | порт PostgreSQL | `5432` |
| `DB_NAME` | имя базы | `mortgage_db` |
| `DB_USERNAME` | пользователь БД | `postgres` |
| `DB_PASSWORD` | пароль БД | `postgres` |
| `SERVER_PORT` | порт приложения | `8080` |

Переменные `infra/.env` для контейнера: `POSTGRES_DB`, `POSTGRES_PORT`, `DB_USERNAME`, `DB_PASSWORD`.

Секреты в репозиторий не коммитятся: `infra/.env` в `.gitignore`, в репозитории только `.env.example`.
Учётные данные для локального запуска — дефолтные, для любых других окружений задаются переменными.

CORS открыт только для `http://localhost:5173` и `http://127.0.0.1:5173` (см. `CorsConfig`).

## API

### `GET /actuator/health`

Проверка живости сервиса и подключения к БД.

```json
{ "status": "UP" }
```

### `POST /api/debug/docx`

Отладочный разбор: возвращает блоки документа в исходном порядке.

```bash
curl -F "file=@contract.docx" http://localhost:8080/api/debug/docx
```

```json
[
  { "type": "PARAGRAPH", "text": "Договор долевого участия" },
  { "type": "TABLE_CELL", "text": "Сумма", "tableIndex": 0, "rowIndex": 0, "colIndex": 0 }
]
```

### `POST /api/extract/docx`

Основной сценарий: разбор файла и извлечение полей по активному шаблону указанного типа.

```bash
curl -F "file=@contract.docx" -F "docType=DDU" http://localhost:8080/api/extract/docx
```

```json
[
  { "fieldCode": "loan_amount",   "rawValue": "1 000 000", "location": "paragraph 1", "confidence": 0.9 },
  { "fieldCode": "interest_rate", "rawValue": "12,5",      "location": "paragraph 2", "confidence": 0.9 },
  { "fieldCode": "borrower_fio",  "rawValue": "Иванов Иван Иванович", "location": "paragraph 3", "confidence": 0.9 }
]
```

Если одно поле встречается в документе с разными значениями, в ответе будет несколько записей
с одинаковым `fieldCode` — такие значения предназначены для ручной проверки.

### `POST /api/analyze`

Полный сценарий сверки: разбор `.docx` → извлечение полей → нормализация → сравнение с заявкой.

Параметры формы: `file` (`.docx`), `docType` (например `DDU`), `applicationData` — JSON-объект
«код поля → значение» (числа допускаются как строки и как JSON-числа).

```bash
curl -F "file=@contract.docx" \
     -F "docType=DDU" \
     -F 'applicationData={"loan_amount":"1 000 000,50","interest_rate":"15","borrower_fio":"иванов иван иваныч"}' \
     http://localhost:8080/api/analyze
```

```json
[
  {
    "fieldCode": "loan_amount",
    "title": "Сумма кредита",
    "appValue": "1000000.5",
    "contractValue": "1000000.5",
    "status": "OK",
    "message": "Значения совпадают"
  },
  {
    "fieldCode": "interest_rate",
    "title": "Процентная ставка",
    "appValue": "15",
    "contractValue": "12.5",
    "status": "ERROR",
    "message": "Значения различаются: заявка = 15, договор = 12.5"
  },
  {
    "fieldCode": "borrower_fio",
    "title": "ФИО заемщика",
    "appValue": "Иванов Иван Иваныч",
    "contractValue": "Иванов Иван Иванович",
    "status": "OK",
    "message": "Значения совпадают (ФИО сравнено с учётом регистра и пробелов)"
  }
]
```

Статусы сравнения:

| Статус | Когда выставляется |
|---|---|
| `OK` | значения после нормализации совпадают |
| `WARN` | поле есть в договоре, но отсутствует в заявке |
| `ERROR` | оба значения есть, но они различаются |
| `MANUAL` | в договоре несколько разных значений поля или значение не нормализуется |
| `MISSING` | поле не найдено в договоре |

Коды ответов:

| Код | Причина |
|---|---|
| `200` | документ разобран и сверен |
| `400` | файл пустой, не `.docx` или повреждён; не передан `docType` или `applicationData`; `applicationData` не JSON-объект |
| `404` | для указанного `docType` нет активного шаблона |

## Как работает извлечение

1. Файл разбирается на блоки (`DocxReaderService`), текст нормализуется.
2. Для каждого поля шаблона берётся список якорных фраз (хранится строкой через запятую).
3. Если якорь найден в блоке — регулярное выражение применяется к тексту сразу после него
   (не более двух предложений), уверенность **0.9**.
4. Если якоря в блоке нет — выражение применяется ко всему тексту блока, уверенность **0.4**.
5. Из совпадения берётся named group `(?<value>…)`, иначе всё совпадение; пробелы схлопываются.
6. Одинаковые значения схлопываются (остаётся максимальная уверенность), разные — сохраняются все.

## Как работает сверка

1. `NormalizationService` приводит значения заявки и договора по правилу поля:
   - `MONEY` — убираются пробелы, «руб.»/«₽»/«RUB», запятая меняется на точку, результат `BigDecimal`
     (`"1 000 000,50 руб."` → `1000000.50`);
   - `PERCENT` — убираются `%` и пробелы, запятая меняется на точку (`"6,5 %"` → `6.5`);
   - `FIO` — схлопывание пробелов и приведение каждого слова к виду «С заглавной»;
   - `DATE` — `dd.MM.yyyy` или `dd MMMM yyyy` с русским месяцем → `LocalDate`.
2. `ComparatorService` проходит по полям шаблона и сравнивает значения:
   - `MONEY` и `PERCENT` — точное сравнение `BigDecimal` (`compareTo`, масштаб не важен);
   - `DATE` — равенство `LocalDate`;
   - `FIO` — сравнение без учёта регистра и лишних пробелов плюс нечёткое сравнение
     (расстояние Левенштейна, порог похожести 0.85);
   - остальные правила — сравнение строк без учёта регистра.
3. Если нормализовать значение не удалось, поле помечается `MANUAL`, а не `ERROR`.

## Данные и миграции

Flyway-миграции лежат в `backend/src/main/resources/db/migration`, имя — `V{номер}__{описание}.sql`.

| Версия | Содержимое |
|---|---|
| `V1__init.sql` | таблица `app_meta` |
| `V2__templates.sql` | `template`, `template_field`, тестовый шаблон `DDU` с полями `loan_amount`, `interest_rate`, `borrower_fio` |

Схема:

- `template` — `id`, `doc_type`, `name`, `is_active`; частичный уникальный индекс гарантирует
  один активный шаблон на `doc_type`;
- `template_field` — `id`, `template_id` (FK, `ON DELETE CASCADE`), `field_code`, `title`,
  `regex_pattern`, `anchor_phrases`, `normalization_rule`; уникальность `(template_id, field_code)`.

`spring.jpa.hibernate.ddl-auto=validate`: схема создаётся только миграциями, сущности обязаны ей
соответствовать.

## Тесты

```bash
cd backend
./mvnw test
```

Покрыто: разбор `.docx` (порядок блоков, координаты ячеек), нормализация текста, regex-извлечение
(якорь, fallback, несколько значений, дедупликация, битый regex), отладочный контроллер,
нормализация значений (`MONEY`, `PERCENT`, `FIO`, `DATE`) и сравнение (все пять статусов,
нечёткое сравнение ФИО, конфликт значений).

## Соглашения

- **Коммиты** — [Conventional Commits](https://www.conventionalcommits.org/en/v1.0.0/) в формате
  RS School: `<тип>[(область)]: <описание>`, тип в нижнем регистре, описание в императиве и на
  английском, без точки в конце. Примеры: `feat(backend): add docx field extraction endpoint`,
  `fix(backend): update spring boot parent version to 3.5.5`, `docs: update readme`.
- **Java 17 без preview** — не используются API Java 21 (`SequencedCollection` и т.п.).
- **Без хардкода секретов** — только переменные окружения.
- **Только параметризованные запросы** — JPQL с `@Param` / Spring Data, без конкатенации SQL.

## Прогресс разработки

| Этап | Содержание | Статус |
|---|---|---|
| 0 | Каркас монорепозитория: Spring Boot, React + Vite, PostgreSQL в Docker | ✅ ЗАВЕРШЕН |
| 1 | Интеграция с Word: чтение `.docx`, DTO блоков, нормализация, отладочный эндпоинт | ✅ ЗАВЕРШЕН |
| 2 | Реестр шаблонов в PostgreSQL и regex-экстрактор полей | ✅ ЗАВЕРШЕН |
| 3 | Нормализация данных и компаратор: сверка договора с заявкой | ✅ ЗАВЕРШЕН |
| 4 | Экспорт результата сверки и ручная проверка (MANUAL) в интерфейсе | ⏳ НЕ НАЧАТ |

Что добавлено по этапам:

- **Этап 0.** `MortgageReconciliationApplication`, `CorsConfig`, `application.yml`, страница
  статуса бэкенда (`frontend/src/App.tsx`), `infra/docker-compose.yml`, миграция `V1__init.sql`
  с таблицей `app_meta`.
- **Этап 1.** `DocxReaderService`, `TextNormalizer`, `DocBlock` + `BlockType`, `DebugController`
  (`POST /api/debug/docx`), тесты разбора и нормализации.
- **Этап 2.** Сущности `Template` и `TemplateField`, `TemplateRepository`, `RegexExtractorService`,
  DTO `ExtractedField`, `ExtractionController` (`POST /api/extract/docx`), миграция
  `V2__templates.sql` с таблицами `template` и `template_field` и тестовым шаблоном `DDU`.
- **Этап 3.** `NormalizationService` (`MONEY`, `PERCENT`, `FIO`, `DATE`), DTO `ApplicationData`,
  `ComparisonResult` и `ComparisonStatus`, `ComparatorService` (пять статусов, нечёткое сравнение
  ФИО через расстояние Левенштейна), `AnalysisController` (`POST /api/analyze`); новых таблиц БД
  не потребовалось — используются правила `normalization_rule` из `template_field`.
- **Инфраструктура репозитория.** `AGENTS.md` — системные правила для ИИ-агентов
  (Conventional Commits и обязательное обновление README).

## Ограничения

- Извлечение основано на регулярных выражениях и якорных фразах — сторонние NLP-библиотеки
  не используются, сложные формулировки могут требовать доработки паттернов в БД.
- Аутентификации и авторизации нет: сервис рассчитан на локальный запуск, отладочные эндпоинты открыты.
- Фронтенд пока показывает только статус бэкенда; загрузка файла и таблица сверки — на следующем этапе.
- Правила нормализации ограничены `MONEY`, `PERCENT`, `FIO`, `DATE`; остальные значения сравниваются
  как строки без учёта регистра.
