-- Seed loan types if not already present
INSERT INTO loan_types (category, annual_interest_rate, max_amount, max_term_months)
SELECT 'CONSUMER', 0.0850, 50000.00, 84
WHERE NOT EXISTS (SELECT 1 FROM loan_types WHERE category = 'CONSUMER');

INSERT INTO loan_types (category, annual_interest_rate, max_amount, max_term_months)
SELECT 'MORTGAGE', 0.0450, 500000.00, 360
WHERE NOT EXISTS (SELECT 1 FROM loan_types WHERE category = 'MORTGAGE');
