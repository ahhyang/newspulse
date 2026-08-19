-- Hibernate maps @Column(length = 64) to VARCHAR, not CHAR.
-- Align Flyway with JPA so ddl-auto=validate succeeds on Postgres.

ALTER TABLE articles
    ALTER COLUMN url_hash TYPE VARCHAR(64),
    ALTER COLUMN content_hash TYPE VARCHAR(64);
