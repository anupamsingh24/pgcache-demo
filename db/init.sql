-- Schema
CREATE EXTENSION IF NOT EXISTS pg_trgm;

CREATE TABLE categories (
    id BIGSERIAL PRIMARY KEY,
    name TEXT NOT NULL
);

CREATE TABLE products (
    id BIGSERIAL PRIMARY KEY,
    name TEXT NOT NULL,
    description TEXT,
    category_id BIGINT NOT NULL REFERENCES categories(id),
    price NUMERIC(10, 2) NOT NULL,
    stock INT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_products_category ON products (category_id);
CREATE INDEX idx_products_name_trgm ON products USING gin (name gin_trgm_ops);

CREATE TABLE customers (
    id BIGSERIAL PRIMARY KEY,
    name TEXT NOT NULL,
    email TEXT NOT NULL UNIQUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE orders (
    id BIGSERIAL PRIMARY KEY,
    customer_id BIGINT NOT NULL REFERENCES customers(id),
    status TEXT NOT NULL DEFAULT 'completed',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_orders_customer ON orders (customer_id);

CREATE TABLE order_items (
    id BIGSERIAL PRIMARY KEY,
    order_id BIGINT NOT NULL REFERENCES orders(id),
    product_id BIGINT NOT NULL REFERENCES products(id),
    quantity INT NOT NULL,
    unit_price NUMERIC(10, 2) NOT NULL
);
CREATE INDEX idx_order_items_order ON order_items (order_id);
CREATE INDEX idx_order_items_product ON order_items (product_id);

-- Seed: categories
INSERT INTO categories (name)
SELECT unnest(ARRAY[
    'Footwear', 'Outerwear', 'Accessories', 'Activewear', 'Formalwear',
    'Knitwear', 'Denim', 'Swimwear', 'Underwear', 'Kidswear'
]);

-- Seed: products (~50,000)
INSERT INTO products (name, description, category_id, price, stock, created_at)
SELECT
    'Product ' || g,
    'Description for product ' || g,
    (SELECT id FROM categories ORDER BY random() LIMIT 1),
    round((random() * 190 + 10)::numeric, 2),
    (random() * 500)::int,
    now() - (random() * interval '365 days')
FROM generate_series(1, 50000) AS g;

-- Seed: customers (~5,000)
INSERT INTO customers (name, email, created_at)
SELECT
    'Customer ' || g,
    'customer' || g || '@example.com',
    now() - (random() * interval '365 days')
FROM generate_series(1, 5000) AS g;

-- Seed: orders (~200,000)
INSERT INTO orders (customer_id, status, created_at)
SELECT
    (floor(random() * 5000) + 1)::bigint,
    (ARRAY['completed', 'completed', 'completed', 'cancelled', 'pending'])[floor(random() * 5) + 1],
    now() - (random() * interval '365 days')
FROM generate_series(1, 200000) AS g;

-- Seed: order_items (~1-3 items per order, ~400,000 rows)
INSERT INTO order_items (order_id, product_id, quantity, unit_price)
SELECT
    o.id,
    (floor(random() * 50000) + 1)::bigint,
    (floor(random() * 4) + 1)::int,
    round((random() * 190 + 10)::numeric, 2)
FROM orders o
CROSS JOIN LATERAL generate_series(1, (floor(random() * 3) + 1)::int) AS item_num;

ANALYZE;
