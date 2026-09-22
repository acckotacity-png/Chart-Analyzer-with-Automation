# Supabase and Upstox setup

Keep your GitHub repository. Select the intended Supabase project explicitly. The URL supplied for this update matches the project's previous URL; a different project has not been created.

1. Complete the Auth/database setup in README.md. For an existing database, inspect its schema and migrate legacy identities before deploying. The migration also archives legacy profiles safely when no Auth accounts exist; users must re-register. Existing Auth accounts require a separate identity migration.
2. Set config.js to the selected project's URL and public key, then run node scripts/build-web.cjs.
3. Link and deploy to that SAME project:

```sh
supabase login
supabase link --project-ref YOUR_PROJECT_REF
supabase secrets set UPSTOX_ACCESS_TOKEN=YOUR_CURRENT_TOKEN
supabase functions deploy upstox-market-data --project-ref YOUR_PROJECT_REF
```

Keep broker tokens only in Edge Function secrets. Renew the token as required by Upstox. The function uses the platform-provided SUPABASE_URL and SUPABASE_ANON_KEY to validate the user's session and approved profile on EVERY request. Gateway JWT verification is disabled in config.toml because authentication is performed inside the function; this does not make data anonymous.

Supported requests are POST `ping`, `market_status`, and `get_snapshot` with a current NSE equity symbol and timeframe (1m, 5m, 15m, 1h, 1D). The function resolves symbols from the Upstox instrument directory and returns broker candles, quote timestamp, and NSE market status. Send apikey plus Authorization: Bearer USER_ACCESS_TOKEN. An approved, unexpired app plan is required. Unsupported symbols/actions return an error.

The function obtains NSE market status before automatic refresh. On `NORMAL_CLOSE`, a background refresh returns a paused status and makes no candle/quote request. Manual snapshots may show the last broker quote with its exchange timestamp. Historical and intraday candles use Upstox endpoints and are merged only after validation. Missing tokens, upstream failures, invalid exchange data, and empty responses are errors, never fabricated success. Ping reports token presence, not token validity; test an authenticated snapshot to verify Upstox access.

Deployment validation: confirm signed-out requests return 401, pending users return 403, approved users can obtain candles, users cannot read other profiles or change approval/role, and only the approved administrator can approve users. Validate both deployed Pages and Render assets after changing project configuration.

References: [Upstox V3 historical candles](https://upstox.com/developer/api-documentation/v3/get-historical-candle-data/), [Supabase RLS](https://supabase.com/docs/guides/database/postgres/row-level-security), [Supabase profiles](https://supabase.com/docs/guides/auth/managing-user-data).
