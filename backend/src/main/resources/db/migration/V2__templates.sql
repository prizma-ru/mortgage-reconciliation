-- Реестр шаблонов документов и полей шаблона

CREATE TABLE template (
    id        BIGSERIAL    PRIMARY KEY,
    doc_type  VARCHAR(32)  NOT NULL,
    name      VARCHAR(255) NOT NULL,
    is_active BOOLEAN      NOT NULL DEFAULT TRUE
);

-- Для одного типа документа допускается только один активный шаблон
CREATE UNIQUE INDEX uq_template_active_doc_type
    ON template (doc_type)
    WHERE is_active;

CREATE TABLE template_field (
    id                 BIGSERIAL     PRIMARY KEY,
    template_id        BIGINT        NOT NULL REFERENCES template (id) ON DELETE CASCADE,
    field_code         VARCHAR(64)   NOT NULL,
    title              VARCHAR(255)  NOT NULL,
    regex_pattern      VARCHAR(2000) NOT NULL,
    anchor_phrases     VARCHAR(1000),
    normalization_rule VARCHAR(32),
    CONSTRAINT uq_template_field_code UNIQUE (template_id, field_code)
);

CREATE INDEX idx_template_field_template_id ON template_field (template_id);

INSERT INTO template (doc_type, name, is_active)
VALUES ('DDU', 'Договор долевого участия', TRUE);

INSERT INTO template_field (template_id, field_code, title, regex_pattern, anchor_phrases, normalization_rule)
SELECT t.id,
       'loan_amount',
       'Сумма кредита',
       '(?<value>\d[\d\s]*(?:[.,]\d{1,2})?)\s*(?:руб\.?|₽|RUB)',
       'сумма кредита,кредит предоставляется в размере,сумма займа',
       'MONEY'
FROM template t
WHERE t.doc_type = 'DDU';

INSERT INTO template_field (template_id, field_code, title, regex_pattern, anchor_phrases, normalization_rule)
SELECT t.id,
       'interest_rate',
       'Процентная ставка',
       '(?<value>\d{1,2}(?:[.,]\d{1,4})?)\s*%\s*(?:годовых|годовая|год)?',
       'процентная ставка,ставка по кредиту,годовых',
       'PERCENT'
FROM template t
WHERE t.doc_type = 'DDU';

INSERT INTO template_field (template_id, field_code, title, regex_pattern, anchor_phrases, normalization_rule)
SELECT t.id,
       'borrower_fio',
       'ФИО заемщика',
       '(?<value>[А-ЯЁ][а-яё-]+(?:\s+[А-ЯЁ][а-яё-]+){1,2})',
       'заемщик,фио,фамилия имя отчество',
       'FIO'
FROM template t
WHERE t.doc_type = 'DDU';
