-- Records a user has starred (marked as a favourite), per company and model.
CREATE TABLE platform_user_star (
    id UUID NOT NULL,
    company_id UUID NOT NULL,
    user_id UUID NOT NULL,
    model_name VARCHAR(100) NOT NULL,
    record_id UUID NOT NULL,
    created_at TIMESTAMP NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_user_star UNIQUE (company_id, user_id, model_name, record_id)
);

CREATE INDEX ix_user_star_user_model ON platform_user_star(company_id, user_id, model_name);
