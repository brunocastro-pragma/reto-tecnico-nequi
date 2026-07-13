-- Runs on every startup, on every task: IF NOT EXISTS keeps a second task starting in parallel
-- from crash-looping.
--
-- Ids are VARCHAR(36), not UUID: they are generated in the domain, which knows nothing about
-- PostgreSQL types.

CREATE TABLE IF NOT EXISTS franchise (
    id   VARCHAR(36) PRIMARY KEY,
    name VARCHAR(120) NOT NULL UNIQUE
);

CREATE TABLE IF NOT EXISTS branch (
    id           VARCHAR(36) PRIMARY KEY,
    name         VARCHAR(120) NOT NULL,
    franchise_id VARCHAR(36) NOT NULL REFERENCES franchise (id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS product (
    id        VARCHAR(36) PRIMARY KEY,
    name      VARCHAR(120) NOT NULL,
    -- Also enforced by StockRule in the domain. Here it guards the data against anything that
    -- does not go through this service.
    stock     INTEGER NOT NULL CHECK (stock >= 0),
    branch_id VARCHAR(36) NOT NULL REFERENCES branch (id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_branch_franchise ON branch (franchise_id);
CREATE INDEX IF NOT EXISTS idx_product_branch ON product (branch_id);
