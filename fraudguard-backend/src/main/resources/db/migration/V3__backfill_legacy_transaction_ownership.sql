-- Transactions created before V2 have no integration owner. Assign them only
-- when there is exactly one active integration, so history remains isolated
-- when ownership would otherwise be ambiguous.
UPDATE transactions
SET integration_id = (
    SELECT id
    FROM integrations
    WHERE active = TRUE
)
WHERE integration_id IS NULL
  AND (
      SELECT COUNT(*)
      FROM integrations
      WHERE active = TRUE
  ) = 1;
