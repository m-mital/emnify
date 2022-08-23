CREATE TABLE PRODUCT_OFFER_AGGREGATION(
    product_code VARCHAR NOT NULL UNIQUE,
    min_price NUMERIC NOT NULL,
    max_price NUMERIC NOT NULL,
    avg_price NUMERIC NOT NULL,
    no_of_offers BIGINT NOT NULL,
    state INTEGER NOT NULL DEFAULT 1,
    PRIMARY KEY (product_code)
);
