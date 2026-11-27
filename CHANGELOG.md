# Changelog

Notable changes are recorded here using [Keep a Changelog 1.1.0](https://keepachangelog.com/en/1.1.0/). See the [changelog archive](docs/CHANGELOG-ARCHIVE.md) for releases through 1.4.84.

## [Unreleased]

## [1.4.86] - 2026-11-27

### Changed

- Cache Dawnbreaker power and projectile identifiers to avoid rebuilding them during rendering.
- Use imported Kotlin and Java names throughout the source and Gradle script. Avoid allocating a fallback set on successful Origin config lookups.
- Refresh the README and storefront descriptions with current mechanics, visual settings, and optional integrations.
- Use the same categorized release notes for build validation, local packages, and all three publishing destinations.

## [1.4.85] - 2026-11-19

### Changed

- Extend standard and Champion Arachne cobweb movement, climbing, sensing and attack-through checks to Chipped cobweb variants through an optional shared block tag. Chipped remains optional.
- Restore a balanced enhanced presentation across all standard/Champion Origins: Standard and Cinematic use textured MRPG/Spell Engine water, bubbles, mist, sparks and family effects at moderated density, with clean animated contours as accents/fallbacks. Minimal stays restrained. Every preset has finite effects and per-entity burst limits; optional libraries remain optional.
- Make Begone immediately displace its player or living-mob target exactly 500 horizontal blocks at unchanged height, without a safe-landing search or search text. Restore “Begone.” for the caster and “You have been banished.” for the player target. Add a tiny caster-only teleport cue and retain the recipient-only heartbeat, without public endpoint particles or sound.
- Make grounded Measured Step follow facing direction across terrain rather than shortening when looking down. Render its 450 ms departure silhouette independently of arrival visibility, including the first-person world pass, with optional Afterimages compositing and a bundled fallback. Add brief Step and Stillwater activation/first-capture/volley sounds without idle audio loops.
- Animate family contours, wings and Guard accents. Replace line-based Dawnbreaker impact frames with filled gold/black eclipse, pressure and rupture compositions, preserving server switches and local opt-in, intensity and duration.
- Retain native particle/sound codecs and essential marks, web/petrification overlays and aquatic feedback. Bound sphere/trail density and particle viewing distance, provide reduced-flash cosmetic lightning, and update the reviewed Selkie cosmetic baseline without changing its powers or hydration mechanics.

### Fixed

- Fix Selkie preservation validation failing on GitHub Linux runners due to Windows JSON line endings; actual JSON content and binary asset edits remain protected.
- Remove coplanar overlap between Dawnbreaker feather fills/spines and halo bands without reducing wing detail or animation. Separate Wyverian gust layers along their travel direction and move the vacuum layer outward instead of stacking stationary billboards at the caster. Particle counts, ability costs and cooldowns are unchanged.
- Keep Fenrkin Hunt’s original activation howl, remove unintended fallback growls from silent repeated pulses, and make standard/Champion Hunt sniffing quieter and less frequent. Preserve the existing highlight and gameplay.
- Fix `/andromedaorigins_visuals` closing immediately after typed chat submission. The client-only Fzzy menu now opens after chat closes and retains local contour, Afterimages and impact-frame preferences.
- Restore core ability sound signatures outside the optional-layer limiter. Give Dawnbreaker Transference and Sovereign Gaze clearer cast cues plus target-position impacts that remain audible to distant victims. Preserve sound opt-out independently of particles, including resource-pack sound support.

[Unreleased]: https://github.com/BareMinimumStudios/andromeda-origins/compare/v1.4.86%2B1.21.1...overgrown-1.21.1
[1.4.86]: https://github.com/BareMinimumStudios/andromeda-origins/compare/v1.4.85%2B1.21.1...v1.4.86%2B1.21.1
[1.4.85]: https://github.com/BareMinimumStudios/andromeda-origins/releases/tag/v1.4.85%2B1.21.1
