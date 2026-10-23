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
- **История проверок** — каждый запуск сверки сохраняется в PostgreSQL (`analysis_run` и
  `analysis_field_result`), результат доступен по `GET /api/analyses/{id}`.
- **Веб-интерфейс** — загрузка `.docx` и JSON-заявки, протокол сверки в виде side-by-side таблицы
  с подсветкой статусов и фильтром «только расхождения»; результат открывается по ссылке с `?runId=`.
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
├── AGENTS.md                         # правила для ИИ-агентов (коммиты, README)
├── backend/
│   ├── .mvn/wrapper/                 # Maven Wrapper
│   ├── spotbugs-exclude.xml          # исключения SAST для DI/JPA-шаблонов
│   ├── src/main/java/com/mortgage/reconciliation/
│   │   ├── config/                   # CORS
│   │   ├── controller/               # REST-эндпоинты
│   │   ├── domain/                   # JPA-сущности: шаблоны и запуски анализа
│   │   ├── dto/                      # DocBlock, ExtractedField, ComparisonResult, AnalysisRunResponse
│   │   ├── repository/               # Spring Data репозитории
│   │   └── service/                  # чтение .docx, нормализация, извлечение, сравнение, сохранение
│   ├── src/main/resources/
│   │   ├── application.yml
│   │   └── db/migration/             # Flyway: V1..V5
│   └── src/test/
│       ├── java/                     # unit- и регрессионные тесты
│       └── resources/corpus/         # Golden Corpus: ddu/ и dkp/ (.docx + .json)
├── frontend/
│   ├── eslint.config.js              # ESLint + eslint-plugin-security
│   ├── src/
│   │   ├── api.ts                    # запросы к бэкенду и типы ответов
│   │   ├── components/               # UploadForm, ResultsTable
│   │   ├── App.tsx                   # переключение «форма → результаты», runId в URL
│   │   └── App.css, index.css        # стили
│   └── vite.config.ts                # прокси /api и /actuator на бэкенд
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

Отладочные эндпоинты (`/api/debug/**`, `/api/extract/**`) отключены по умолчанию. Чтобы их включить:

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
# или: java -jar target/*.jar --spring.profiles.active=dev
```

### 3. Фронтенд

```bash
cd frontend
npm install
npm run dev
```

Страница доступна на `http://localhost:5173`. Vite проксирует `/api/*` и `/actuator/*`
на `http://localhost:8080`, поэтому CORS для локальной разработки не задействуется.

Проверка production-сборки:

```bash
cd frontend
npm run build      # tsc -b && vite build → frontend/dist
```

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
Доступен **только в профиле `dev`** (см. «Безопасность»), иначе — `404`.

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
curl -F "file=@contract.docx" http://localhost:8080/api/debug/docx
```

```json
[
  { "type": "PARAGRAPH", "text": "Договор долевого участия" },
  { "type": "TABLE_CELL", "text": "Сумма", "tableIndex": 0, "rowIndex": 0, "colIndex": 0 }
]
```

### `POST /api/extract/docx`

Промежуточный сценарий: разбор файла и извлечение полей по активному шаблону указанного типа.
Доступен **только в профиле `dev`**, как и `/api/debug/docx`.

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

### `POST /api/analyses`

Полный сценарий сверки с сохранением результата: разбор `.docx` → извлечение полей → нормализация →
сравнение с заявкой → запись в БД.

Параметры формы: `file` (`.docx`), `docType` (например `DDU`), `application` — JSON-объект
«код поля → значение» (числа допускаются как строки и как JSON-числа).

```bash
curl -F "file=@contract.docx" \
     -F "docType=DDU" \
     -F 'application={"loan_amount":"1 000 000,50","interest_rate":"15","borrower_fio":"иванов иван иваныч"}' \
     http://localhost:8080/api/analyses
```

```json
{
  "runId": 1,
  "docType": "DDU",
  "fileName": "contract.docx",
  "status": "NEEDS_REVIEW",
  "createdAt": "2026-10-23T20:09:48.303835Z",
  "results": [
    {
      "fieldCode": "loan_amount",
      "title": "Сумма кредита",
      "appValue": "1000000.5",
      "contractValue": "1000000.5",
      "status": "OK",
      "message": "Значения совпадают",
      "location": "paragraph 1"
    },
    {
      "fieldCode": "interest_rate",
      "title": "Процентная ставка",
      "appValue": "15",
      "contractValue": "12.5",
      "status": "ERROR",
      "message": "Значения различаются: заявка = 15, договор = 12.5",
      "location": "paragraph 2"
    }
  ]
}
```

### `GET /api/analyses`

История запусков с пагинацией, сначала новые. Возвращает краткую информацию без вложенных
результатов — для списка в интерфейсе и навигации к `GET /api/analyses/{id}`.

| Параметр | По умолчанию | Описание |
|---|---|---|
| `page` | `0` | номер страницы с нуля |
| `size` | `20` | размер страницы (не больше 100) |

```bash
curl "http://localhost:8080/api/analyses?page=0&size=20"
```

```json
{
  "items": [
    {
      "runId": 4,
      "docType": "DDU",
      "fileName": "contract.docx",
      "status": "DONE",
      "createdAt": "2026-10-23T20:20:11.512Z"
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 1,
  "totalPages": 1
}
```

### `GET /api/analyses/{id}`

Возвращает сохранённый запуск вместе с результатами по полям — та же структура, что и в ответе
`POST /api/analyses`. Результаты читаются одним запросом с `JOIN FETCH`, поэтому
`LazyInitializationException` не возникает.

```bash
curl http://localhost:8080/api/analyses/1
```

Статус поля (`results[].status`):

| Статус | Когда выставляется |
|---|---|
| `OK` | значения после нормализации совпадают |
| `WARN` | поле есть в договоре, но отсутствует в заявке |
| `ERROR` | оба значения есть, но они различаются |
| `MANUAL` | в договоре несколько разных значений поля или значение не нормализуется |
| `MISSING` | поле не найдено в договоре |

Общий статус запуска (`status`): `DONE`, если все поля `OK`, иначе `NEEDS_REVIEW`.

Коды ответов:

| Код | Причина |
|---|---|
| `200` | документ разобран, сверен и сохранён; либо запуск найден |
| `400` | файл пустой, не `.docx` или повреждён; не передан `docType` или `application`; `application` не JSON-объект |
| `404` | для указанного `docType` нет активного шаблона; запуск с таким `id` не найден |

## Интерфейс

Одностраничное приложение на React (без UI-библиотек, только обычный CSS).

**Форма загрузки** (`frontend/src/components/UploadForm.tsx`):

- выбор типа документа — `DDU` (ДДУ) или `DKP` (ДКП), значения совпадают с `docType` на бэкенде;
- `<input type="file" accept=".docx">` для договора;
- `<textarea>` для JSON-строки заявки (с примером по умолчанию);
- кнопка «Проанализировать» отправляет `FormData` (`file`, `docType`, `application`)
  на `POST /api/analyses`; на время запроса показывается «Анализ документа…», ошибки — красным
  текстом под формой.

**Протокол сверки** (`frontend/src/components/ResultsTable.tsx`):

| Колонка | Содержимое |
|---|---|
| Поле | `title` и `fieldCode` |
| Заявка | `appValue` |
| Договор | `contractValue` |
| Статус | бейдж `OK` / `WARN` / `ERROR` / `MANUAL` / `MISSING` |
| Локация | `location` (`paragraph 2`, `table 0, row 1, cell 2`) |
| Комментарий | `message` |

Подсветка строк: `OK` — без фона, `WARN` — жёлтый, `ERROR` — красный, `MANUAL` — серый,
`MISSING` — синий. Сверху — счётчики по статусам, общий статус запуска и чекбокс
«Показывать только расхождения» (скрывает `OK`). Кнопка «Новый анализ» возвращает к форме.

**Ссылка на результат.** После успешного анализа `runId` попадает в адрес (`/?runId=12`),
поэтому страницу можно перезагрузить или передать коллеге: при открытии выполняется
`GET /api/analyses/{runId}` и таблица восстанавливается из БД.

Запросы вынесены в `frontend/src/api.ts` (`analyzeDocument`, `getAnalysis`, `describeError`),
типы ответов описаны там же.

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
| `V3__analysis_results.sql` | `analysis_run`, `analysis_field_result` — история сверок |
| `V4__dkp_template.sql` | шаблон `DKP` (договор купли-продажи) с теми же тремя полями |
| `V5__fix_fio_regex.sql` | исправление regex ФИО: поддержка двойной фамилии через дефис |

Схема:

- `template` — `id`, `doc_type`, `name`, `is_active`; частичный уникальный индекс гарантирует
  один активный шаблон на `doc_type`;
- `template_field` — `id`, `template_id` (FK, `ON DELETE CASCADE`), `field_code`, `title`,
  `regex_pattern`, `anchor_phrases`, `normalization_rule`; уникальность `(template_id, field_code)`;
- `analysis_run` — `id`, `doc_type`, `file_name`, `status` (`DONE` / `NEEDS_REVIEW`),
  `created_at TIMESTAMPTZ DEFAULT now()`;
- `analysis_field_result` — `id`, `run_id` (FK, `ON DELETE CASCADE`), `field_code`, `title`,
  `app_value`, `contract_value`, `status`, `message`, `location`.

`spring.jpa.hibernate.ddl-auto=validate`: схема создаётся только миграциями, сущности обязаны ей
соответствовать.

## Тестирование

### Запуск

```bash
cd backend
./mvnw test                  # Windows: .\mvnw.cmd test
```

Тесты не требуют PostgreSQL: шаблоны для регрессионных прогонов описываются прямо в тесте,
база не поднимается. Фронтенд проверяется типами, линтером и сборкой:

```bash
cd frontend
npm run lint       # ESLint (typescript-eslint + eslint-plugin-security), 0 warnings
npm run build      # tsc -b && vite build
```

`npm run lint` проверяет `src/**/*.{ts,tsx}`: обязательные правила ESLint и typescript-eslint,
react-hooks, а также правила безопасности из `eslint-plugin-security`
(`detect-eval-with-expression`, `detect-non-literal-regexp`, `detect-possible-timing-attacks`
и другие). Сборка падает при любом предупреждении (`--max-warnings=0`).

### Golden Corpus

Набор эталонных документов лежит в `backend/src/test/resources/corpus`:

```text
corpus/
├── ddu/   # договоры долевого участия
│   ├── ddu-01-standard.docx  + .json
│   ├── ddu-02-table.docx     + .json
│   └── ddu-03-long.docx      + .json
└── dkp/   # договоры купли-продажи
    ├── dkp-01-standard.docx  + .json
    ├── dkp-02-table.docx     + .json
    └── dkp-03-sentence.docx  + .json
```

Документы покрывают разные форматы записи: `1 000 000 руб.`, `1 000 000,50 руб.`,
`5 000 000.00 руб.`, `12,5 %`, `12.5%`, суммы с пояснением словами, ФИО с двойной фамилией,
а также значения в ячейках таблиц. Рядом с каждым `.docx` лежит `.json` с «золотой правдой»:

```json
{
  "docType": "DDU",
  "application": {
    "loan_amount": "1 000 000,00",
    "interest_rate": "12.50",
    "borrower_fio": "иванов иван иванович"
  },
  "expected": {
    "loan_amount": "1000000",
    "interest_rate": "12.5",
    "borrower_fio": "Иванов Иван Иванович"
  }
}
```

- `application` — данные заявки, с которыми документ должен совпасть;
- `expected` — значения, которые обязана извлечь цепочка (сравниваются после нормализации,
  поэтому `1000000` и `1 000 000,00` считаются равными).

`AnalysisIntegrationTest` параметризован по всем `.docx` корпуса: документ проходит через
`DocxReaderService` → `RegexExtractorService` → `ComparatorService`, после чего проверяется, что
все поля совпали с эталоном, а статусы сравнения — `OK`. Отдельные тесты следят, что у каждого
документа есть `.json` и что корпус содержит по 3–4 документа каждого типа.

### Метрики точности

| Метрика | Значение |
|---|---|
| Precision по критическим полям (`loan_amount`, `interest_rate`) | **100%** на Golden Corpus |
| Precision по всем полям корпуса | **100%** |
| Документов в корпусе | 6 (3 DDU + 3 DKP), 40 тестов в наборе |

Если паттерн в БД перестанет находить значение, тест падает с сообщением вида
`loan_amount: ожидалось 1000000, извлечено []`.

### Как добавить документ в корпус

1. Положите `.docx` в `backend/src/test/resources/corpus/ddu` или `.../dkp`
   (имя — латиницей, например `ddu-04-table-headers.docx`); текст должен содержать поля шаблона
   с якорными фразами из `template_field`.
2. Рядом создайте `.json` с тем же именем и полями `docType`, `application`, `expected`
   (см. пример выше).
3. Запустите `.\mvnw.cmd test -Dtest=AnalysisIntegrationTest`. Если значение не извлекается —
   скорректируйте паттерн/якоря миграцией `V{n}__...sql`, а не ожидаемый результат.
4. Если новый формат требует поддержки в нормализации, добавьте правило и тест в
   `NormalizationServiceTest`.

Покрыто тестами: разбор `.docx` (порядок блоков, координаты ячеек), нормализация текста,
regex-извлечение (якорь, fallback, несколько значений, дедупликация, битый regex), отладочный
контроллер, нормализация значений (`MONEY`, `PERCENT`, `FIO`, `DATE`), сравнение (все пять
статусов, нечёткое сравнение ФИО, конфликт значений, передача `location`), маппинг запуска в
`AnalysisRunResponse`, страница истории и весь корпус документов.

## Безопасность

### SBOM (CycloneDX)

Бэкенд: плагин `org.cyclonedx:cyclonedx-maven-plugin` привязан к фазе `package`, поэтому SBOM
создаётся при обычной сборке:

```bash
cd backend
./mvnw package -DskipTests
# → target/bom.json (CycloneDX 1.5, JSON, все runtime-зависимости)
```

Фронтенд:

```bash
cd frontend
npm run sbom        # npm sbom --sbom-format=cyclonedx, JSON в stdout
```

`npm sbom` появился в npm 9.8. Если установлен более старый npm (например 8.x), используйте
`npx --yes npm@11 sbom --sbom-format=cyclonedx --json` или обновите npm.

### Проверка уязвимостей зависимостей (OWASP Dependency Check)

```bash
cd backend
./mvnw dependency-check:check
```

Плагин `org.owasp:dependency-check-maven` настроен на `failBuildOnCVSS=7`: сборка падает при
любой уязвимости с CVSS ≥ 7. Отчёты (`HTML` и `JSON`) и база NVD складываются в
`backend/target/dependency-check-data`, поэтому каталог не засоряет домашнюю директорию.
Плагин намеренно не привязан к `verify`, чтобы обычная сборка не зависела от загрузки NVD.

Первый запуск скачивает базу NVD (несколько сотен мегабайт). Без API-ключа NVD загрузка
сильно ограничена по скорости — если указать ключ, проверка проходит заметно быстрее:

```bash
./mvnw dependency-check:check -Dnvd.api.key=<ключ>      # ключ: nvd.nist.gov/developers/request-an-api-key
```

### SAST (SpotBugs)

```bash
cd backend
./mvnw spotbugs:check
```

Плагин `spotbugs-maven-plugin` запускается с `effort=Max`, `threshold=Medium` и падает при
находках. Исключения (только ложные срабатывания фреймворк-кода) описаны в
`backend/spotbugs-exclude.xml`: `EI_EXPOSE_REP2` для constructor injection в контроллерах и
`EI_EXPOSE_REP*` для двусторонних JPA-связей. Реальные замечания исправлены: DTO и коллекции
сущностей отдают неизменяемые копии (`List.copyOf`), поэтому наружу не утекает изменяемое
состояние.

### Линтер фронтенда

`eslint.config.js` подключает `typescript-eslint`, `react-hooks` и `eslint-plugin-security`;
запуск — `npm run lint` (падает при предупреждениях).

### Отладочные эндпоинты

`DebugController` (`POST /api/debug/docx`) и `ExtractionController` (`POST /api/extract/docx`)
помечены `@Profile("dev")` и в обычном профиле не регистрируются — запросы получают `404`.
Основной API (`/api/analyses`) доступен всегда; CORS по-прежнему ограничен
`http://localhost:5173` и `http://127.0.0.1:5173`.

## Соглашения

- **Коммиты** — [Conventional Commits](https://www.conventionalcommits.org/en/v1.0.0/) в формате
  RS School: `<тип>[(область)]: <описание>`, тип в нижнем регистре, описание в императиве и на
  английском, без точки в конце. Примеры: `feat(backend): add docx field extraction endpoint`,
  `fix(backend): update spring boot parent version to 3.5.5`, `docs: update readme`.
- **Java 17 без preview** — не используются API Java 21 (`SequencedCollection` и т.п.).
- **Без хардкода секретов** — только переменные окружения.
- **Только параметризованные запросы** — JPQL с `@Param` / Spring Data, без конкатенации SQL.
- **Документирование кода: Javadoc для Java, JSDoc для TypeScript. Комментарии объясняют «почему»,
  а не «что»** — публичные классы и методы сервисов/контроллеров/репозиториев описаны в Javadoc
  (`@param`, `@return`, `@throws`), сложные регулярные выражения и эвристики (порог похожести ФИО,
  окно после якоря, разбор разделителей тысяч) прокомментированы по месту, React-компоненты —
  через JSDoc с описанием пропсов и состояния. Очевидные комментарии и закомментированный код
  запрещены; полный стандарт — в `AGENTS.md`, раздел 3.

## Прогресс разработки

| Этап | Содержание | Статус |
|---|---|---|
| 0 | Каркас монорепозитория: Spring Boot, React + Vite, PostgreSQL в Docker | ✅ ЗАВЕРШЕН |
| 1 | Интеграция с Word: чтение `.docx`, DTO блоков, нормализация, отладочный эндпоинт | ✅ ЗАВЕРШЕН |
| 2 | Реестр шаблонов в PostgreSQL и regex-экстрактор полей | ✅ ЗАВЕРШЕН |
| 3 | Нормализация данных и компаратор: сверка договора с заявкой | ✅ ЗАВЕРШЕН |
| 4 | REST API и сохранение результатов анализа в БД | ✅ ЗАВЕРШЕН |
| 5 | Интерфейс: загрузка файла и таблица сверки | ✅ ЗАВЕРШЕН |
| 6 | Golden Corpus и регрессионное тестирование | ✅ ЗАВЕРШЕН |
| 7 | Подготовка к безопасности (SBOM, SAST, DAST) | ✅ ЗАВЕРШЕН |

Этапы 0–7 соответствуют поставленным задачам и все завершены. Следующий этап в README появится
после того, как задача будет сформулирована. Сквозные требования (стандарт комментирования,
линтер, проверки безопасности) не выделены в отдельный этап и описаны выше — в разделах
«Безопасность», «Тестирование» и «Соглашения».

Что добавлено по этапам:

- **Этап 0.** `MortgageReconciliationApplication`, `CorsConfig`, `application.yml`, стартовая
  страница фронтенда (`frontend/src/App.tsx`), `infra/docker-compose.yml`, миграция `V1__init.sql`
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
- **Этап 4.** Миграция `V3__analysis_results.sql` с таблицами `analysis_run` и
  `analysis_field_result`, сущности `AnalysisRun` / `AnalysisFieldResult`, репозитории
  `AnalysisRunRepository` (с `JOIN FETCH`) и `AnalysisFieldResultRepository`,
  `AnalysisPersistenceService`, DTO `AnalysisRunResponse` и `FieldResultResponse`;
  `AnalysisController` переведён на `POST /api/analyses` (с сохранением в БД) и
  `GET /api/analyses/{id}` (история). В `ComparisonResult` добавлено поле `location`.
- **Этап 5.** `frontend/src/api.ts` (типы и запросы `POST /api/analyses`, `GET /api/analyses/{id}`),
  компоненты `UploadForm` и `ResultsTable`, переключение экранов и `?runId=` в `App.tsx`,
  стили `App.css` / `index.css`, прокси `/api` в `vite.config.ts`; подсветка статусов
  (`OK`/`WARN`/`ERROR`/`MANUAL`/`MISSING`), фильтр «только расхождения», индикатор загрузки
  и вывод ошибок.
- **Этап 6.** Golden Corpus в `backend/src/test/resources/corpus` (6 документов: 3 DDU + 3 DKP,
  каждый с эталонным `.json`), `AnalysisIntegrationTest` — параметризованный прогон
  «чтение → извлечение → сравнение» с проверкой Precision 100% по критическим полям;
  миграции `V4__dkp_template.sql` (шаблон DKP) и `V5__fix_fio_regex.sql` (поддержка двойной
  фамилии через дефис — баг найден корпусом).
- **Этап 7.** Плагины `cyclonedx-maven-plugin` (SBOM `target/bom.json`), OWASP
  `dependency-check-maven` (`failBuildOnCVSS=7`), `spotbugs-maven-plugin` и
  `spotbugs-exclude.xml`; ESLint с `typescript-eslint` и `eslint-plugin-security`
  (`npm run lint`), скрипт `npm run sbom`; эндпоинт `GET /api/analyses` с пагинацией;
  отладочные контроллеры переведены на `@Profile("dev")`, а неизменяемые DTO и коллекции
  сущностей (`List.copyOf`) устранили замечания SAST.
- **Инфраструктура репозитория.** `AGENTS.md` — системные правила для ИИ-агентов
  (Conventional Commits и обязательное обновление README).

## Ограничения

- Извлечение основано на регулярных выражениях и якорных фразах — сторонние NLP-библиотеки
  не используются, сложные формулировки могут требовать доработки паттернов в БД.
- Суммы и ставки распознаются только в цифровой записи; суммы словами («один миллион рублей»)
  не извлекаются — в корпусе такие документы сопровождаются цифровым значением.
- Аутентификации и авторизации нет: сервис рассчитан на локальный запуск; отладочные эндпоинты
  включаются только профилем `dev`.
- Интерфейс не умеет ничего редактировать: значения `MANUAL` только подсвечиваются, ручная правка
  и экспорт результата пока не реализованы.
- Интерфейс не показывает список проверок: `GET /api/analyses` уже отдаёт историю с пагинацией,
  но экран истории в UI ещё не сделан.
- Правила нормализации ограничены `MONEY`, `PERCENT`, `FIO`, `DATE`; остальные значения сравниваются
  как строки без учёта регистра.
