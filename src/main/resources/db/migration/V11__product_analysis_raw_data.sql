ALTER TABLE product
    ADD COLUMN opening_period VARCHAR(255);

ALTER TABLE product
    ADD COLUMN usage_timing_changed_at DATE;

ALTER TABLE product
    ADD CONSTRAINT product_opening_period_check
        CHECK (opening_period IN (
                                  'RECENT',
                                  'ONE_TO_THREE_MONTHS',
                                  'SIX_MONTHS_OR_MORE'
            ));
