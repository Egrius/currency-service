CREATE TABLE prices (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    symbol VARCHAR(50) NOT NULL,
    price NUMERIC(20, 8) NOT NULL,
    fetched_at TIMESTAMPTZ NOT NULL,

    CONSTRAINT symbol_fetched_at_unique UNIQUE(symbol, fetched_at)
);