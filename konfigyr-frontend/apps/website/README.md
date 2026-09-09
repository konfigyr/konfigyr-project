# Konfigyr website

The public marketing site (`konfigyr.com`): homepage, about, early access request form, privacy
policy, imprint. It is a separate [TanStack Start](https://tanstack.com/start) app from
`apps/console` (the authenticated product), with no shared auth, no query layer, and no
`@konfigyr/hooks` dependency — the only server-side logic it has is the early access form
submitting to HubSpot.

## Running it

This is an npm workspace member of `konfigyr-frontend`. Install from the workspace root, not from
inside `apps/website`:

```bash
cd konfigyr-frontend
npm install
```

### Dev server

From the workspace root, scoped to this app:

```bash
npm run dev --workspace=@konfigyr/website
```

Or from `apps/website` directly:

```bash
cd konfigyr-frontend/apps/website
npm run dev
```

Vite is configured to listen on **port 3001** (`vite.config.ts`). `apps/console` listens on port
3000, so both apps can run side by side without a port clash — no extra configuration needed to
run product and marketing site together locally.

### Build

```bash
npm run build
```

Runs `vite build` (client + server + Nitro bundles into `.output/`) followed by `tsc --noEmit`.
From the workspace root: `turbo run build` builds every workspace app, including this one; scope
it to just this app with `npm run build --workspace=@konfigyr/website` or
`turbo run build --filter=@konfigyr/website`.

### Test, lint, typecheck

```bash
npm test          # typecheck + lint + vitest run --coverage (turbo sequences typecheck/lint first)
npm run test:ui    # vitest UI, watch mode
npm run lint       # eslint . on its own
npm run lint:fix   # eslint . --fix
npm run typescript # tsc --noEmit on its own
```

`turbo.json` wires `test` to depend on `typescript` and `lint`, so `npm test` at the workspace
root (`turbo run test`) — or scoped with `turbo run test --filter=@konfigyr/website` — runs all
three for this app in the right order. `npm test` run directly inside `apps/website` invokes only
the `vitest run --coverage` script itself; typecheck/lint aren't sequenced automatically unless
you go through turbo.

## Environment variables

All variables are read via `process.env.*` inside `submitEarlyAccessRequest`
(`src/routes/early-access/-handler.ts`), never at module scope — this is a TanStack Start server
function, so that code only ever executes server-side and these values are never sent to the
browser bundle or exposed to the client.

| Variable | Required? | Purpose |
|---|---|---|
| `HUBSPOT_MOCK` | No (defaults to unset/`false`) | Set to `true` to log the submission (`early-access.mock-submit`) instead of calling HubSpot. Lets the form be built and tested before the HubSpot portal setup (below) is in place. `.env.test` sets this to `true` so the test suite never hits the real API by default. |
| `HUBSPOT_ACCESS_TOKEN` | **Yes**, once `HUBSPOT_MOCK` is not `true` | HubSpot Private App token, `forms` scope. **Secret — server-side only.** Never reference this from client code; it authenticates the Forms API submission and must never reach the browser. |
| `HUBSPOT_PORTAL_ID` | **Yes**, once `HUBSPOT_MOCK` is not `true` | HubSpot account/portal ID, part of the submission URL. |
| `HUBSPOT_FORM_GUID` | **Yes**, once `HUBSPOT_MOCK` is not `true` | GUID of the Early Access form created in the HubSpot portal, part of the submission URL. |

If `HUBSPOT_MOCK` isn't `true` and any of `HUBSPOT_ACCESS_TOKEN` / `HUBSPOT_PORTAL_ID` /
`HUBSPOT_FORM_GUID` is missing, the handler logs `early-access.missing-config` and returns a
"Server is misconfigured" error to the visitor rather than throwing — so a bad deploy fails
loudly on the first real submission instead of silently.

For local development, `.env` (git-ignored) already ships with `HUBSPOT_MOCK=true` and the three
HubSpot vars present but commented out — uncomment and fill them in once real values exist.
`.env.test` separately pins `HUBSPOT_MOCK=true` for the test suite, independently of whatever
`.env` has, so tests never hit the real API by accident.

### Prerequisite: HubSpot portal setup

The early access form **cannot submit real leads** until someone with admin access to EBF's
HubSpot portal has:

1. Created a Private App with the `forms` scope (yields `HUBSPOT_ACCESS_TOKEN`).
2. Built the actual Early Access form in HubSpot (yields `HUBSPOT_FORM_GUID`) and confirmed the
   portal ID (`HUBSPOT_PORTAL_ID`).
3. Defined custom Contact properties for the fields that aren't standard HubSpot fields:
   `team_size` and `spring_boot_service_count` at minimum (see the `fields` array in
   `-handler.ts` for the exact set submitted).

Until that's done, run locally with `HUBSPOT_MOCK=true`: every other page and the form's own
client-side validation, honeypot, and UI all work normally, but submissions only get logged
locally rather than reaching HubSpot. Setting `HUBSPOT_MOCK=false` (or unset) without the three
HubSpot vars in place fails every submission with the misconfiguration error above rather than
silently dropping data.

## Site scope: what's deliberately absent

This is a deliberately lean pre-launch site, not an unfinished one. `marketing/site-architecture.md`
made an explicit call to leave the following out for now — recorded here so nobody re-derives (or
undoes) that decision without the context:

| Section | Why deferred |
|---|---|
| **Pricing** | No pricing model exists yet (confirmed gap in `product-marketing.md` — no plan names, price points, or packaging defined). A pricing page would either be empty or invented. |
| **Customers / Case Studies** | Zero proof points exist — no customers, no named design partners, no metrics, no testimonials. Building this section now means faking social proof, which the brand voice and copywriting principles explicitly rule out. |
| **Blog** | No content strategy has been defined. An empty or single-post blog undermines credibility more than having no blog at all. |
| **Docs** | The product isn't public. Docs implies self-serve signup and a live product to document; neither exists yet at this stage. |
| **Features (as a separate section/page)** | With only one page's worth of real substance (the homepage), a dedicated Features section would just be the homepage's Solution/Benefits content re-published — a second page competing with the first rather than adding new depth. Revisit once there's enough product surface area (multiple modules with distinct enough audiences) to justify splitting it out. |
| **Terms of Service** | No self-serve signup, no billing, no account creation yet — nothing for a ToS to actually govern. Add it alongside Pricing/billing infrastructure. |
| **Security page** | No SOC 2/compliance certifications exist yet (per `product-marketing.md`, on-prem/compliance positioning is aspirational, not certified). A Security page today would either overstate posture or sit empty. |
| **Compare / vs. competitor pages** | The objection responses that exist (Vault, Doppler, "not a Spring Boot shop") are drafted but not sales-validated, and the cloud-PaaS objection is explicitly flagged as unvalidated. Naming competitors on dedicated, indexable pages is a bigger commitment than handling objections conversationally inside the homepage's Objection Handling section. Revisit once objection responses are validated against real prospect conversations. |

See `marketing/site-architecture.md` for the full page hierarchy, nav spec, and what's included
(Homepage, Early Access, About, Privacy, Imprint) and why.

## Outstanding content gates before launch

Two things on the built pages are placeholders, not oversights:

- **`[LEGAL: ...]` markers** in `src/components/legal/privacy-policy.tsx` (7 open items) and
  `src/components/legal/imprint.tsx` (1 open item) — retention periods, DPO contact details, and
  similar specifics that need EBF's data protection officer / legal sign-off before this content
  is accurate. Don't resolve these by guessing; get the real answer from EBF's legal/privacy team.
- **Screenshot placeholders** in `src/components/homepage/benefits-section.tsx` (2 of them, one
  per benefit pillar) — dashed, warning-colored boxes reserving a 16:9 slot for real product
  screenshots (the profile editor's inline validation state, and the service manifest/catalog
  view) that haven't been captured yet.
