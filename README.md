# Zevenen

Android score keeper for the card game Zevenen (trick prediction game).

## Features

- 2 to 8 players, names entered at the start of a new game
- Configurable round structure ("mountain"):
  - maximum cards in a round (typically 7 or 13)
  - count up first (1 → max → 1) or down first (max → 1 → max)
  - whether the turning round is dealt twice in a row ("2, 1, 1, 2" vs "2, 1, 2")
  - optional extra final round with a single card
  - default: 7 down to 1, 1 repeated, back up to 7, plus a final single-card round
    (15 rounds: 7-6-5-4-3-2-1-1-2-3-4-5-6-7-1)
- Configurable scoring:
  - correct prediction: bonus (default 10) + points per trick won (default 2)
  - wrong prediction: penalty per difference (default 2) times the miss, as negative points
- Random dealer for round 1, rotating afterwards; the dealer predicts last and may
  not predict the value that would make the total of all predictions equal the number
  of cards dealt
- Tricks entered per round must sum exactly to the number of cards dealt
- Full scoreboard, round summaries and final winner; the game survives the app
  being killed mid-game (state is persisted)

## Building

Requires JDK 17+ (Gradle auto-provisions a JDK 17 toolchain) and an Android SDK.

```
./gradlew assembleDebug
```

APK: `app/build/outputs/apk/debug/app-debug.apk` — install with `adb install app-debug.apk`
or open the project in Android Studio and press Run.

Run the unit tests (game rules, scoring, dealer constraint):

```
./gradlew test
```

## Project layout

```
app/src/main/java/com/zevenen/app/
├── MainActivity.kt          # screen wiring (setup / predict / tricks / summary)
├── GameViewModel.kt        # state holder + persistence
├── game/
│   ├── GameRules.kt        # round structure and scoring
│   └── GameState.kt        # immutable game state machine
└── ui/                     # Compose screens (setup, prediction, tricks, summary)
app/src/test/               # JUnit tests for the game logic
```

## License

This project is released under the [MIT License](LICENSE).

## AI disclosure

This codebase was developed with the assistance of AI coding tools
(Mistral Vibe). All code was reviewed, tested and shaped by the maintainer,
who is responsible for it — but reviewers and contributors should be aware
of its AI-assisted origin.

