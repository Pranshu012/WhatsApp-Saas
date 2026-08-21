-- F11/F12 Fix: Allow pre-tenant account resolution on whatsapp_accounts during incoming webhooks when app.tenant_id is not set.

DROP POLICY IF EXISTS whatsapp_accounts_tenant_isolation ON whatsapp_accounts;

CREATE POLICY whatsapp_accounts_tenant_isolation ON whatsapp_accounts
    USING (tenant_id = NULLIF(current_setting('app.tenant_id', true), '')::uuid OR NULLIF(current_setting('app.tenant_id', true), '') IS NULL)
    WITH CHECK (tenant_id = NULLIF(current_setting('app.tenant_id', true), '')::uuid);
