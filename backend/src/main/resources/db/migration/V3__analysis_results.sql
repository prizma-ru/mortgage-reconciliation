-- Запуски анализа и результаты сверки по полям

CREATE TABLE analysis_run (
    id         BIGSERIAL   PRIMARY KEY,
    doc_type   VARCHAR(32) NOT NULL,
    file_name  VARCHAR(255) NOT NULL,
    status     VARCHAR(32) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE INDEX idx_analysis_run_created_at ON analysis_run (created_at DESC);

CREATE TABLE analysis_field_result (
    id             BIGSERIAL    PRIMARY KEY,
    run_id         BIGINT       NOT NULL REFERENCES analysis_run (id) ON DELETE CASCADE,
    field_code     VARCHAR(64)  NOT NULL,
    title          VARCHAR(255) NOT NULL,
    app_value      TEXT,
    contract_value TEXT,
    status         VARCHAR(32)  NOT NULL,
    message        TEXT,
    location       VARCHAR(255)
);

CREATE INDEX idx_analysis_field_result_run_id ON analysis_field_result (run_id);
