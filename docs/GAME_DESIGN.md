# Jolt Time — MVP Game Design

## Core loop

The Keeper taps the Time Core for Time Shards, coins, and XP. XP raises the Keeper level and opens epochs. Coins buy infrastructure upgrades. Taps and expeditions uncover artifact fragments; completed artifacts permanently improve production and museum completion. Passive production makes every return valuable.

## Economy and currencies

- **Time Shards** are the primary score and tap/passive resource. Each tap grants current tap power.
- **Coins** fund upgrades. A tap grants one coin and expeditions grant 75.
- **XP** advances player level. Taps grant 4 XP; the next level costs `level × 100` XP.

There is no premium currency, monetization, trading, or server economy.

## Epochs and artifacts

Egypt is available at level 1; Greece, Rome, Vikings, and Japan unlock at levels 5, 10, 15, and 20. Each has five artifacts—one in every rarity from Common through Mythic. An artifact needs 3, 5, 7, 9, or 11 fragments based on rarity. Random discoveries target an incomplete artifact in an unlocked epoch. Completion adds a permanent tap or passive-production bonus and raises total museum progress.

## Upgrades

- **Stronger Tap** adds tap power.
- **Time Engine** adds one shard per second and offline-production rate per level.
- **Archaeologist Tools** adds 1.5 percentage points to fragment discovery chance per level.
- **Museum Wing** records an extensible museum bonus for future prestige rewards.

Upgrade prices use `base cost × multiplier^level`, producing a controlled exponential sink.

## Expeditions

One expedition can run at a time in any unlocked epoch. It lasts 30 seconds and persists absolute start/end timestamps, so closing the process cannot pause it. Claiming yields 75 coins, 30 XP, and a fragment from the selected civilization.

## Daily chronology

The seven-day local cycle grants increasing shards, coins, and XP. Days four and seven also provide a fragment. An ISO local date is persisted to prevent a second claim on the same calendar day.

## Offline income

On restoration, elapsed wall-clock seconds are multiplied by current passive income. Elapsed time is clamped to six hours (21,600 seconds). A modal reports the collected amount. The latest background timestamp is persisted when the activity stops.

## Future roadmap

Potential post-MVP work includes balanced museum prestige, richer expedition choices, duplicate-fragment conversion, achievements, accessible sound assets, animated artifact reveals, more curated epochs, optional cloud backup, deterministic daily streak validation, and Play Store release signing. These are deliberately excluded until retention and economy data validate the complete offline MVP.
