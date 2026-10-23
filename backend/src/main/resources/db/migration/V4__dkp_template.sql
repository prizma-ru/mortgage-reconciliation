-- Шаблон для договора купли-продажи (DKP) с ипотекой

INSERT INTO template (doc_type, name, is_active)
VALUES ('DKP', 'Договор купли-продажи', TRUE);

INSERT INTO template_field (template_id, field_code, title, regex_pattern, anchor_phrases, normalization_rule)
SELECT t.id,
       'loan_amount',
       'Цена договора',
       '(?<value>\d[\d\s]*(?:[.,]\d{1,2})?)\s*(?:руб\.?|₽|RUB)',
       'цена договора,стоимость квартиры,сумма кредита',
       'MONEY'
FROM template t
WHERE t.doc_type = 'DKP';

INSERT INTO template_field (template_id, field_code, title, regex_pattern, anchor_phrases, normalization_rule)
SELECT t.id,
       'interest_rate',
       'Процентная ставка',
       '(?<value>\d{1,2}(?:[.,]\d{1,4})?)\s*%\s*(?:годовых|годовая|год)?',
       'процентная ставка,ставка по кредиту,годовых',
       'PERCENT'
FROM template t
WHERE t.doc_type = 'DKP';

INSERT INTO template_field (template_id, field_code, title, regex_pattern, anchor_phrases, normalization_rule)
SELECT t.id,
       'borrower_fio',
       'ФИО покупателя',
       '(?<value>[А-ЯЁ][а-яё]*(?:-[А-ЯЁ][а-яё]*)*(?:\s+[А-ЯЁ][а-яё]*(?:-[А-ЯЁ][а-яё]*)*){1,2})',
       'покупатель,заемщик,фио',
       'FIO'
FROM template t
WHERE t.doc_type = 'DKP';
