-- Import sample customer data
\copy customers(id,name,email) FROM '/docker-entrypoint-initdb.d/sample-customers.csv' DELIMITER ',' CSV HEADER;

-- Update created_at and updated_at for imported records
UPDATE customers SET 
    created_at = CURRENT_TIMESTAMP - INTERVAL '1 day' + (RANDOM() * INTERVAL '1 day'),
    updated_at = created_at
WHERE created_at IS NULL;

-- Reset sequence to continue from the last imported ID
SELECT setval('customers_id_seq', (SELECT MAX(id) FROM customers));
