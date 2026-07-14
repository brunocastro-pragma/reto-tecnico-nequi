DROP TABLE IF EXISTS product;
DROP TABLE IF EXISTS branch;
DROP TABLE IF EXISTS franchise;

CREATE TABLE franchise (
    id   VARCHAR(36) PRIMARY KEY,
    name VARCHAR(120) NOT NULL UNIQUE
);

CREATE TABLE branch (
    id           VARCHAR(36) PRIMARY KEY,
    name         VARCHAR(120) NOT NULL,
    franchise_id VARCHAR(36) NOT NULL REFERENCES franchise (id) ON DELETE CASCADE
);

CREATE TABLE product (
    id        VARCHAR(36) PRIMARY KEY,
    name      VARCHAR(120) NOT NULL,
    stock     INTEGER NOT NULL CHECK (stock >= 0),
    branch_id VARCHAR(36) NOT NULL REFERENCES branch (id) ON DELETE CASCADE
);
