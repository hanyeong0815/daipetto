-- 性別は任意項目（02_Requirement_Definition FR-PET-001・06_ERD §8）。V3のNOT NULLは設計と食い違い、
-- 性別を省略した登録がDB制約違反（500）になっていたため、設計に合わせてNULLを許可する
ALTER TABLE pets
    ALTER COLUMN gender DROP NOT NULL;
