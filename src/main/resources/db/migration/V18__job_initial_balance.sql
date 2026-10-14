-- The amount a site was taken on for ("bu şantiyeyi 1.000.000 TL'ye aldım").
-- The summary subtracts what has been charged to the customer so far from it,
-- so the user sees how much of the agreed figure is still untouched.
-- NULL means no figure was agreed; the site then shows no balance at all
-- rather than a misleading zero. Services do not use it.
ALTER TABLE job ADD COLUMN initial_balance TEXT;
