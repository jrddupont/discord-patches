# Changelog

## v1.1.0 (unreleased)

- New patch: Hide gift button — removes the gift button from the chat
  message composer (343.12 / 342.16 / 341.13 Stable). Skips the gift
  action's push in the Hermes bundle; the thread-button fallback and all
  other composer buttons are untouched.
- Hide quest promo banner now also supports 341.13 Stable (its gate is a
  hook-style variant, fn 58782 — same checks, different codegen, so it
  gets its own anchor).

## v1.0.2

- Stable-only: dropped the 345.2 Alpha target and its Hermes anchor. The
  Hide quest promo banner patch now supports 343.12 and 342.16 Stable.
- Repo cleanup: rewrote the README, renamed the Gradle project, fixed
  issue-template links, bundle contact/website metadata.
- Retired the broken v1.0.0 release (shipped without classes.dex, loaded
  zero patches); v1.0.1+ are the good builds.

## v1.0.1

- Fixed empty bundle: v1.0.0 shipped without dex entries so Manager
  loaded zero patches.
- Hides the Discord quest promo banner (343.12 / 342.16 Stable, 345.2
  Alpha).
