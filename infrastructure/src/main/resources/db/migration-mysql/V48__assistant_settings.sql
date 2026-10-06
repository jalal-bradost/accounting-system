-- AI assistant provider settings (per company).
CREATE TABLE IF NOT EXISTS platform_assistant_settings (
    company_id              CHAR(36) NOT NULL PRIMARY KEY,
    assistant_enabled       TINYINT(1) NOT NULL DEFAULT 0,
    mode                    VARCHAR(32) NOT NULL DEFAULT 'hybrid',
    google_enabled          TINYINT(1) NOT NULL DEFAULT 0,
    google_api_key          VARCHAR(512) NULL,
    google_model            VARCHAR(128) NOT NULL DEFAULT 'gemini-3.8-flash',
    ollama_enabled          TINYINT(1) NOT NULL DEFAULT 1,
    ollama_base_url         VARCHAR(512) NOT NULL DEFAULT 'http://localhost:11434',
    ollama_model            VARCHAR(128) NOT NULL DEFAULT 'qwen3:4b',
    openai_enabled          TINYINT(1) NOT NULL DEFAULT 0,
    openai_api_key          VARCHAR(512) NULL,
    openai_model            VARCHAR(128) NOT NULL DEFAULT 'gpt-4o-mini',
    updated_at              TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
