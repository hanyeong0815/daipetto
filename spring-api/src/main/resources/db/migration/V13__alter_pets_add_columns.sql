ALTER TABLE pets
    ADD COLUMN breed            VARCHAR(100) NULL,
    ADD COLUMN neutered         BOOLEAN      NOT NULL DEFAULT FALSE,
    ADD COLUMN microchip_number VARCHAR(15)  NULL;
