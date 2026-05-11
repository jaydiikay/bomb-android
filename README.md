# Bomb Card Game (Android)

Android implementation using Kotlin + Jetpack Compose.

## Requirements

- Android Studio Hedgehog or later
- Android SDK 34
- Min SDK 26 (Android 8)

## Setup

1. Clone repo
2. Open in Android Studio (File > Open)
3. Sync Gradle
4. Run on device or emulator (API 26+)

## Rules

- Standard 52-card deck, 2-7 players, each dealt 7 cards
- Play a card matching the top card's suit or rank
- **2**: next player draws 2 (stackable with other 2s)
- **4**: reverses direction for one turn (or current player plays again with 2 players)
- **8 / J**: must play a second card of the same suit or rank
- **7 of Hearts (Bomb)**: ends the game immediately
- First player to empty their hand wins
- Highest score (sum of card values in hand) loses

## Scoring

| Card | Points |
|------|--------|
| A    | 1      |
| 2    | 20     |
| 3    | 3      |
| 4    | 20     |
| 5    | 5      |
| 6    | 6      |
| 7 (non-Hearts) | 7 |
| 7 of Hearts | 500 |
| 8    | 8      |
| 9    | 9      |
| 10   | 10     |
| J    | 45     |
| Q    | 2      |
| K    | 4      |

## Architecture

MVVM: `GameViewModel` + `GameState` (pure Kotlin game logic) + Jetpack Compose UI + Room DB

- `game/` — Pure Kotlin game logic (no Android deps)
- `data/` — Room database for persisting game results
- `ui/screens/` — Compose screens (Auth, Setup, Game, Score)
- `ui/components/` — Reusable Compose components
- `ui/theme/` — Material3 theming
