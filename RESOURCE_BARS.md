# Custom Resource Bars

**Current release: v1.4.73.** The authoritative HUD mapping remains **25 styles on sheet 1 + 6 styles on sheet 2**. Champion Veilborn reuses the existing Auroral Mirage cooldown bar for its 5-second cooldown; sprite indices are unchanged.

Andromeda Origins uses two 256×256 Origins HUD sprite sheets and a global **01–31** art catalog. The global style number is for documentation; Origins itself uses zero-based `bar_index` values within each individual sheet.

## Sheet layout

- `andromeda_origins:textures/gui/resource_bars_1.png` — global styles **01–25**, `bar_index` **0–24**.
- `andromeda_origins:textures/gui/resource_bars_2.png` — global styles **26–31**, `bar_index` **0–5**.

Global style 25 is the final style on sheet 1 (`bar_index: 24`). Global style 26 then restarts at `bar_index: 0` on sheet 2.

## Current catalog

| Global | Sheet | `bar_index` | Style | Current use |
|---:|---|---:|---|---|
| 01 | `resource_bars_1.png` | 0 | Stamina Bar | Arachne Eightfold Swiftness; Fenrkin Stamina Surge; Gorgon Transference speed-buff duration; Humanity Indomitable active duration; Manticore Ravenous Lunge; Satyr Momentum; Selkie Surging Tides |
| 02 | `resource_bars_1.png` | 1 | Wetness Bar | Nereid Wet duration; Selkie Wetness / land-weakness recovery |
| 03 | `resource_bars_1.png` | 2 | Wet Status Bar | Manticore Wet / Cramped weakness duration; Nereid Convalescing Wet effect (catalog intent; see shared-resource note below); Veilborn Wet / Unstable duration; Wyverian weakness-state duration |
| 04 | `resource_bars_1.png` | 3 | Cobweb Bar | Arachne Weaver's Nest |
| 05 | `resource_bars_1.png` | 4 | Cooldown 1 Bar | Arachne Cobweb Fatigue; Fenrkin On the Hunt cooldown; Humanity Indomitable cooldown; Manticore Beast of Blood cooldown; Nereid Submersion cooldown; Selkie Coastal Phalanx cooldown |
| 06 | `resource_bars_1.png` | 5 | Flutter Bar | Faerie air-jump charges |
| 07 | `resource_bars_1.png` | 6 | Trickster Bar | Faerie Fae Illusions cooldown |
| 08 | `resource_bars_1.png` | 7 | Dust Bar | Faerie Dust / Flutter resource |
| 09 | `resource_bars_1.png` | 8 | Visionary Bar | Faerie Undetectable state; Satyr Vigil Perception state |
| 10 | `resource_bars_1.png` | 9 | Hunting Bar | Fenrkin On the Hunt active duration |
| 11 | `resource_bars_1.png` | 10 | Wolf Bar | Fenrkin Mark of Fenrir progress; Fenrkin marked-target duration |
| 12 | `resource_bars_1.png` | 11 | Blue Cooldown Bar | Faerie Concealment cooldown; Fenrkin Mark of Fenrir cooldown; Selkie Sealskin Bastion cooldown |
| 13 | `resource_bars_1.png` | 12 | Shield Bar | Fenrkin Adrenaline active duration |
| 14 | `resource_bars_1.png` | 13 | Resurrection Bar | Fenrkin Adrenaline cooldown; Lichling Death's Defiance cooldown |
| 15 | `resource_bars_1.png` | 14 | Eye Bar | Gorgon Ophidian Gaze charges; recharge; Petrified duration |
| 16 | `resource_bars_1.png` | 15 | Transference Bar | Gorgon Transference charges; recharge; target debuff duration |
| 17 | `resource_bars_1.png` | 16 | Skull Bar | Lichling Energy / Chimes of Necros; Doom duration |
| 18 | `resource_bars_1.png` | 17 | Weakness Bar | Lichling weakness-state duration |
| 19 | `resource_bars_1.png` | 18 | Blood Moon Bar | Manticore Beast of Blood active duration |
| 20 | `resource_bars_1.png` | 19 | Veil Swap Bar | Veilborn Veil Transposition cooldown |
| 21 | `resource_bars_1.png` | 20 | Veil Curtain Bar | Veilborn Auroral Mirage cooldown |
| 22 | `resource_bars_1.png` | 21 | Reality Shatter Bar | Veilborn Reality Shatter duration (standard self-stack capped at 90s) |
| 23 | `resource_bars_1.png` | 22 | Arrow Right Bar | Satyr Rush cooldown |
| 24 | `resource_bars_1.png` | 23 | Fire Bar | Wyverian Ember resource |
| 25 | `resource_bars_1.png` | 24 | Wings Bar | Wyverian Gusts of Freedom flight resource |
| 26 | `resource_bars_2.png` | 0 | Halo Bar | Nereid Convalescing Aura cooldown |
| 27 | `resource_bars_2.png` | 1 | Bubble Bar | Nereid Submersion active duration; Nereid Submersion debuff duration |
| 28 | `resource_bars_2.png` | 2 | Heart Bar | Selkie Coastal Phalanx active duration |
| 29 | `resource_bars_2.png` | 3 | Gold Seal Bar | Selkie Sealskin Bastion active duration |
| 30 | `resource_bars_2.png` | 4 | Infatuation Bar | Siren Infatuation cooldown; Infatuated duration |
| 31 | `resource_bars_2.png` | 5 | Waves Bar | Siren Shrieking Wail cooldown; Wail debuff duration |

## Nereid Wet-resource exception

Nereid Convalescing Aura and Submersion currently extend the same underlying `nereidwet` duration resource. One resource can only render one HUD style at a time. The shared Nereid Wet timer therefore uses **global style 02 (Wetness Bar)**. A separate Convalescing-only Wet Status style would require splitting that mechanic into a separate resource.

## Champion mirrors

Champion resources that mirror standard visible HUD resources use the same custom style where the Champion has its own copy of that resource. This is currently relevant to mirrored Faerie, Fenrkin, Satyr, and Selkie HUD state.

## Extra/internal HUDs

A handful of internal/legacy visible resources were never assigned a dedicated style in the 01–31 art catalog and intentionally remain on upstream Origins sheets rather than being guessed into a custom slot. These include:

- Fenrkin helper activation animation,
- Selkie retaliated timer.

## Editing rules

- Keep both sheets at **256×256**.
- Keep each Origins indexed row aligned to the sheet layout used by the existing PNG.
- Sheet 2 restarts its Origins index at `0`.
- Do not renumber the global 01–31 catalog when adding another sheet; continue the global art numbering and document the new sheet/index mapping here.


## v1.4.56 Wet accumulation fix
- Veilborn Wet/Unstable uses `veilborn/helper/watered_remove` on sheet 1 `bar_index: 2`. While exposure continues, the resource now gains 3 seconds every second up to 90 seconds; once exposure ends, it counts down normally.
- The shared `common/silenced_sources` reference counter is internal-only and no longer renders a fallback/default HUD bar.
