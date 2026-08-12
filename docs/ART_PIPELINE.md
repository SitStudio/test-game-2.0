# Jolt Time art pipeline

The visible Ancient Egypt slice is resource-backed. Gameplay IDs resolve through `ArtCatalog`; game rules never reference filenames or rendering primitives.

## Asset contracts

- `HeroArtAsset`: full-body battle/menu art and portrait art.
- `EnemyArtAsset`: battle sprite and portrait.
- `BossArtAsset`: dedicated boss sprite and portrait.
- `ArtifactArtAsset`: museum/reveal display art.
- `EnvironmentArtAsset`: Archive, campaign, story, and arena backgrounds.
- `UiDecorationAsset`: map-node frames and other replaceable embellishments.

Current temporary illustrations are original Android vector drawables under `app/src/main/res/drawable`. They are real resource assets rendered by `Image`/`painterResource` in Compose. `BattleArtCache` rasterizes the drawable resources once at bounded sizes, then `GameBattleView` blits cached bitmaps. Canvas remains responsible only for gameplay overlays: shadows, selection rings, HP/cooldown bars, telegraphs, hit effects, damage numbers, and controls.

## Replacing temporary art

Keep the resource name and replace its XML with an optimized WebP/PNG, or point the relevant `ArtCatalog` entry to a new drawable. No domain model, save ID, mission, or combat rule needs to change. Portrait and full-body fields may point to separate resources when final portrait art arrives. Sprite-sheet animation can be added behind the battle asset/cache boundary without modifying `ActionBattleEngine`.

Recommended production conventions:

- full-body heroes/enemies: transparent WebP, consistent feet anchor and padding;
- portraits: transparent or framed WebP with consistent eye line;
- environments: 16:9 opaque WebP with HUD-safe negative space;
- artifacts: transparent square WebP;
- keep source art outside the Android package and commit only optimized runtime exports.

Missing IDs use one centralized catalog fallback. Fallbacks are not used by the current four heroes, Egypt artifact set, normal enemies, or boss.
