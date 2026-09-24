-- Align POS configs / open sessions that were created with the old USD UI default
-- to the company's configured currency (default_currency or accounting base currency).

UPDATE pos_config pc
SET currency_code = COALESCE(
    (SELECT c.default_currency FROM platform_company c WHERE c.id = pc.company_id),
    (SELECT cc.code FROM company_currencies cc
     WHERE cc.company_id = pc.company_id AND cc.base_currency = TRUE AND cc.active = TRUE
     LIMIT 1),
    pc.currency_code
)
WHERE UPPER(pc.currency_code) = 'USD'
  AND EXISTS (
      SELECT 1 FROM platform_company c
      WHERE c.id = pc.company_id
        AND c.default_currency IS NOT NULL
        AND UPPER(c.default_currency) <> 'USD'
  );

UPDATE pos_session ps
SET currency_code = COALESCE(
    (SELECT pc.currency_code FROM pos_config pc WHERE pc.id = ps.config_id),
    (SELECT c.default_currency FROM platform_company c WHERE c.id = ps.company_id),
    ps.currency_code
)
WHERE UPPER(ps.currency_code) = 'USD'
  AND UPPER(ps.state) = 'OPEN'
  AND EXISTS (
      SELECT 1 FROM platform_company c
      WHERE c.id = ps.company_id
        AND c.default_currency IS NOT NULL
        AND UPPER(c.default_currency) <> 'USD'
  );
