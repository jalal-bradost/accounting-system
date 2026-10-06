-- Rates are "units of currency per 1 base unit"; 6 decimals loses accuracy for weak-base
-- companies (1500 IQD per USD is 0.000666666667, not 0.000667).
ALTER TABLE currency_rates MODIFY rate DECIMAL(19, 12) NOT NULL;
ALTER TABLE company_currencies MODIFY rate_per_base DECIMAL(19, 12) NOT NULL;
