-- AI assistant provider settings (per company).
CREATE TABLE IF NOT EXISTS platform_assistant_settings (
    company_id              UUID PRIMARY KEY,
    assistant_enabled       BOOLEAN NOT NULL DEFAULT FALSE,
    mode                    VARCHAR(32) NOT NULL DEFAULT 'hybrid',
    google_enabled          BOOLEAN NOT NULL DEFAULT FALSE,
    google_api_key          VARCHAR(512),
    google_model            VARCHAR(128) NOT NULL DEFAULT 'gemini-3.8-flash',
    ollama_enabled          BOOLEAN NOT NULL DEFAULT TRUE,
    ollama_base_url         VARCHAR(512) NOT NULL DEFAULT 'http://localhost:11434',
    ollama_model            VARCHAR(128) NOT NULL DEFAULT 'qwen3:4b',
    openai_enabled          BOOLEAN NOT NULL DEFAULT FALSE,
    openai_api_key          VARCHAR(512),
    openai_model            VARCHAR(128) NOT NULL DEFAULT 'gpt-4o-mini',
    updated_at              TIMESTAMP NOT NULL
);
