-- Rates are "units of currency per 1 base unit"; 6 decimals loses accuracy for weak-base
-- companies (1500 IQD per USD is 0.000666666667, not 0.000667).
ALTER TABLE currency_rates ALTER COLUMN rate TYPE NUMERIC(19, 12);
ALTER TABLE company_currencies ALTER COLUMN rate_per_base TYPE NUMERIC(19, 12);
