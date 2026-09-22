# Chart Analyzer Pro

The existing GitHub repository is retained. GitHub Pages and Render serve the same public/ assets.
Root index.html, app.js, config.js and manifest.json are the source files. Run:

```sh
node scripts/build-web.cjs
node --test tests/web.test.cjs
```

## Supabase configuration

config.js contains only the project URL and public anon/publishable key. The supplied project is currently configured. Authentication and market data use this same project; old browser data-bridge overrides are ignored.

To use a DIFFERENT Supabase project, change both fields in config.js and rebuild. For GitHub Pages you can instead set repository Actions variables SUPABASE_URL and SUPABASE_ANON_KEY. Both must belong to that project. Keep the same GitHub repository. Never put service_role keys or broker tokens in browser files or repository variables used by the web build.

## Required database setup before deployment

The web app now requires Supabase Auth email/password accounts and app_users.id matching auth.users.id. Email-only legacy login and cached approval no longer grant access.

- For a NEW, empty project, apply supabase/migrations/202609210001_user_access.sql before registering users. It creates profiles, pending approval defaults, and row-level policies.
- For an EXISTING project with no Auth accounts, the migration preserves the old app_users table in the non-public app_private schema, revokes browser access to it, and creates the protected table. Users must register and verify new password accounts; old approval is not automatically trusted. The migration refuses conversion if Auth users already exist. The inspected project has 2 legacy profiles and 0 Auth accounts. The migration was approved and applied to qhqbporwncgccpcgrurl. Both legacy profiles are preserved privately; no Auth accounts have been registered yet.
- Enable Email authentication. Configure Auth Site URL and redirect URLs for the actual deployed website. Add the deployed GitHub Pages URL to both settings so email confirmation and the Forgot password recovery link return to the app. Users confirm their email, then sign in with their password.
- Register the intended administrator normally. From the trusted SQL editor, promote that VERIFIED Auth UUID:

```sql
update public.app_users set role='admin', status='approved'
where id = 'REPLACE_WITH_VERIFIED_AUTH_USER_UUID';
```

Other users remain pending until the administrator approves them. The frontend alone is not the access boundary: apply the SQL policies and authenticated Edge Function together.

## Deployment

GitHub Settings > Pages > Source: GitHub Actions. The existing pages.yml workflow builds and uploads ONLY public/, not the entire repository. Optional project variables are described above.

Render: Python service, start command python3 server.py. Rebuild public/ before pushing changes; Render serves committed public/ files. Browser configuration does not automatically read .env or Render environment variables.

See UPSTOX_SUPABASE_SETUP.md for Edge Function setup. No deployment, SQL migration, or GitHub push is performed by the local build.

## Current limits

The native Android client still uses the legacy email-only identity flow and must be migrated to Supabase Auth before it can use this protected schema. Do not weaken RLS to make that client work. Android builds were not verified in this change.

Prices and candles now come only from the authenticated Upstox Edge Function. The app checks the NSE market status first. When the exchange is closed or status is unavailable, automatic refresh pauses and the UI labels the last available broker value; it never creates a simulated tick. Manual refresh can retrieve the broker snapshot after close. Intraday candles are merged with historical candles; technical analysis needs at least 50 candles.

The manifest does not make this an offline-capable PWA; service worker and install icons remain future work.

## Verified deployment status

Supabase migrations 202609210001 and 202609220001 plus upstox-market-data version 2 are deployed. Transactional tests passed for user isolation, plan pricing, one-time trials, payment references, expiry boundaries, trade audit records, and administrator review; all fixtures were rolled back. Live anonymous and invalid-session requests return HTTP 401. UPSTOX_ACCESS_TOKEN is still missing. Frontend GitHub publication, first verified administrator registration, paid-plan prices, and Android Auth migration remain pending.

## Access plans and trade history

A user is granted 7 days of free access only when an administrator approves the `trial_7` plan. The trial cannot be reused. Paid 7-day and 30-day plans are disabled and unpriced by default. The administrator sets each INR price, enables the plan, records a confirmed payment reference, and activates or extends access. Every grant/revocation is kept in `access_events`. Access automatically expires at `access_until`; the database independently denies charts, history, edits, and Excel export after expiry.

Users can save their own buy/sell records, including date, quantity, price, charges, and notes. Administrators can review a user's saved history; users cannot access anyone else's data. All trade changes create an audit record. The `.xlsx` export includes a summary and raw `Trades` sheet, typed numeric cells, gross/net-cash formulas, weighted-average cost, and realized P&L. It flags a sell that lacks recorded buys instead of inventing a profit. The export is an analysis snapshot, not a tax calculation or broker order record.

Before real market data can load, configure the current Upstox access token as the `UPSTOX_ACCESS_TOKEN` secret in the linked Supabase project. Never put the token in `config.js`, GitHub, or this repository.
