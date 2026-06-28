# Offline-first: bundle questions as a JSON asset, retire the Heroku REST API

The app was built to fetch questions from a remote REST API
(`https://passmath-api.herokuapp.com/api/v1/questions` via Retrofit `MathService`), cached
in Room. That Heroku backend is dead — Heroku discontinued free dynos in November 2022 and
the app is a hobby project with no paid backend. The network path is therefore broken.

We ship the converted question bank as `app/src/main/assets/questions/questions_1993_2015.json`
bundled in the APK, and seed the existing Room `questions` table from that asset on first
launch. The app becomes fully offline-first, the curated content lives version-controlled
in this repo alongside the code, and there is no server to maintain.

## Considered Options

- **Revive / pay for the `passmath-api` Heroku backend.** Rejected: ongoing cost and
  maintenance for a hobby app; past-paper users often lack connectivity anyway.
- **Bundled JSON asset + Room seed (chosen).** Offline, self-contained, content in git.
- **Static JSON on a host (GitHub Pages / S3).** Rejected: keeps a network dependency and
  a deploy step for no benefit over bundling.

## Consequences

- New one-time seed step reads the asset and inserts into the `questions` table (gated so
  it runs once, not on every launch).
- The Retrofit / `MathService` / `APIUtils` network path is abandoned in practice (left in
  place for now, not wired into the loading flow).
- Adding more question banks later = add another JSON asset and extend the seed.