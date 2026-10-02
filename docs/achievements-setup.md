# Setting up achievements in Google Play Games

Leak Out decides, stores and shows achievements on the device. When a player signs in to Google Play Games, the app also pushes them to Play Games, so they show up in the player's Play Games profile. That second part needs a **Play Games Services (PGS) project** in the Play Console, with the 25 achievements defined there and their generated ids copied into the app. This guide walks through that setup, from an app that already exists in the Play Console to a tester seeing *Wet Feet* in Play Games.

You only do this once. Later achievements, added with new level packs, follow [Adding achievements later](#adding-achievements-later).

<<<<<<< Updated upstream
How achievements work inside the app is in [achievements-technical.md](achievements-technical.md). The release pipeline and signing keys are explained in [deployment.md](deployment.md).
=======
The design behind the achievements is in the OpenSpec change [`app-game-achievements`](../openspec/changes/app-game-achievements/design.md). The release pipeline and signing keys are explained in [deployment.md](deployment.md).
>>>>>>> Stashed changes

- [How it works](#how-it-works)
- [What you need](#what-you-need)
- [1. Review the icons](#1-review-the-icons)
- [2. Create the Play Games Services project](#2-create-the-play-games-services-project)
- [3. Set up the OAuth consent screen](#3-set-up-the-oauth-consent-screen)
- [4. Add a credential for every signing key](#4-add-a-credential-for-every-signing-key)
- [5. Create the 25 achievements](#5-create-the-25-achievements)
- [6. Add testers](#6-add-testers)
- [7. Export the ids into the app](#7-export-the-ids-into-the-app)
- [8. Smoke test](#8-smoke-test)
- [9. Publish the Play Games configuration](#9-publish-the-play-games-configuration)
- [Adding achievements later](#adding-achievements-later)
- [Troubleshooting](#troubleshooting)
- [Reference: the launch catalogue](#reference-the-launch-catalogue)

## How it works

```
 Leak Out (device)                                   Google Play Games
 ─────────────────                                   ─────────────────
 game engine ──▶ AchievementRules ──▶ Room tables      PGS project (app_id)
                                        │              25 achievements (ids)
                                        │                    ▲
                                        └─ AchievementSyncer ┘  unlock / setSteps
                                           (only while signed in)
```

- **The device is the source of truth.** Achievements are decided and stored locally, whether or not the player is signed in. Signing in later pushes everything that was earned before.
- **The sync only ever sets.** Standard achievements get an *unlock* call and incremental ones a *set steps* call with the local progress, never *increment*. Pushing the same state twice changes nothing, so the app pushes everything on every sign-in and after every local change.
- **The link is `games-ids.xml`.** This resource file holds the PGS `app_id` and one `achievement_<key>` string per achievement, with the id the Play Console generated. Until the real file is in place, the app ships with a placeholder `app_id` of `0`. With the placeholder, the app does not start the Play Games SDK, Settings shows Play Games as unavailable, and achievements work locally only. Debug and CI builds never need the Play Console.

Three Google pieces are involved:

- **The PGS project** in the Play Console. It holds the game's settings, the achievements and the testers, and it has a numeric project id: the `app_id`.
- **A Google Cloud project** linked to it. It holds the OAuth consent screen and the OAuth clients.
- **One Android OAuth client per signing key.** Google only lets an APK sign in if its package name and signing certificate SHA-1 match a registered credential. Leak Out is signed by three different keys (see [step 4](#4-add-a-credential-for-every-signing-key)), so it needs three credentials.

## What you need

- The app **Leak Out** (`nl.hexmaster.leakout`) in the Play Console, with at least one build uploaded, so Play App Signing is set up. [deployment.md](deployment.md) covers this.
- The **Owner or Admin** role in the Play Console.
- A **JDK** for `keytool`, to read SHA-1 fingerprints. Android Studio ships one: `<Android Studio>/jbr/bin/keytool`.
- The **upload keystore** (`leak-out-upload.jks`) and its password from your password manager, or access to the Play Console's App integrity page, which shows the same fingerprint.
<<<<<<< Updated upstream
- The icons in [`assets/achievements/png/`](../assets/achievements/png/).
=======
- The icons in [`openspec/changes/app-game-achievements/icons/png/`](../openspec/changes/app-game-achievements/icons/png/).
>>>>>>> Stashed changes
- One or more **Google accounts** for testers. Your own account is enough to start.

> The Play Console moves its menus around from time to time. If a menu path below doesn't match exactly, look for the same page name under **Grow users** (or **Grow**) → **Play Games Services**.

## 1. Review the icons

You upload the icons to the Play Console in [step 5](#5-create-the-25-achievements), and changing them later means uploading them again. Check them first:

<<<<<<< Updated upstream
1. Open [`assets/achievements/preview.html`](../assets/achievements/preview.html) in a browser. It is a contact sheet of all 25 icons plus the hidden placeholder.
2. Reject any icon you don't like. The SVG sources are written by [`assets/achievements/generate-icons.mjs`](../assets/achievements/generate-icons.mjs). After a change, run `node generate-icons.mjs .` and then `node render-icons.js <key>` in that folder to render the PNG again (see [achievements-technical.md](achievements-technical.md#icons)).
3. Check that each PNG in `assets/achievements/png/` is 512 × 512. That is the size the Play Console wants.
=======
1. Open [`openspec/changes/app-game-achievements/icons/preview.html`](../openspec/changes/app-game-achievements/icons/preview.html) in a browser. It is a contact sheet of all 25 icons plus the hidden placeholder.
2. Reject any icon you don't like. The icons are generated by [`icons/generate-icons.mjs`](../openspec/changes/app-game-achievements/icons/generate-icons.mjs) from SVG sources in the same folder. After a change, run `node generate-icons.mjs` there to render the PNGs again.
3. Check that each PNG in `icons/png/` is 512 × 512. That is the size the Play Console wants.
>>>>>>> Stashed changes

Play Games crops icons to a circle. All icon content stays inside a disc with a radius of 200 px, so nothing is cut off.

`achievement_hidden.png` is only used in the app, for locked hidden achievements. Don't upload it to the Console: Play Games uses its own placeholder for hidden achievements.

## 2. Create the Play Games Services project

1. Open the [Play Console](https://play.google.com/console) and select **Leak Out**.
2. Go to **Grow users → Play Games Services → Setup and management → Configuration**.
3. When asked whether the game already uses Google APIs, choose **No, my game doesn't use Google APIs**, and create a new project. This creates a new Google Cloud project, linked to the PGS project.

   You could instead choose **Yes** and link the Cloud project that holds the deployment service account from [deployment.md](deployment.md). That works too, but a separate project keeps the release robot and the player sign-in apart, so revoking one never affects the other.

4. Fill in **Properties**:
   - **Display name**: Leak Out
   - **Description**: a short description of the game. Players see it in Play Games.
   - **Category**: Puzzle
   - **Graphic assets**: the app icon (512 × 512) and a feature graphic (1024 × 500). You can reuse the ones from the store listing.
5. Click **Save**.

Write down the **project id** shown at the top of the Configuration page, a 12-digit number. That is the `app_id`. You also find it in the exported `games-ids.xml` in [step 7](#7-export-the-ids-into-the-app).

## 3. Set up the OAuth consent screen

Before you can add credentials, the Cloud project needs an OAuth consent screen.

1. On the PGS **Configuration** page, under **Credentials**, follow the link to the Google Cloud project (or open the [Google Cloud console](https://console.cloud.google.com/) and select the project created in step 2).
2. Go to **APIs & Services → OAuth consent screen** (in newer consoles: **Google Auth Platform → Branding**).
3. Choose **External** as the user type.
4. Fill in:
   - **App name**: Leak Out
   - **User support email**: your email address
   - **Developer contact email**: your email address
5. Don't add any scopes. Play Games sign-in needs none beyond the defaults.
6. Save, and return to the Play Console.

## 4. Add a credential for every signing key

An APK can only sign in to Play Games when its package name and signing certificate SHA-1 are registered as an **Android** credential. Three keys sign Leak Out, and each one needs its own credential:

| Key | Signs | Where to find the SHA-1 |
|---|---|---|
| **App signing key** | every build testers and players install from Google Play | Play Console → **Test and release → Setup → App integrity → Play app signing** → *App signing key certificate* → **SHA-1 certificate fingerprint** |
| **Upload key** | the bundle CI uploads, and any APK you build from it yourself | Same page → *Upload key certificate*, or run `keytool` (below) |
| **Debug key** | `assembleDebug` builds and the emulator | Run `keytool` (below) on your machine's debug keystore |

Read the upload key's SHA-1:

```sh
keytool -list -v -keystore leak-out-upload.jks -alias upload
```

```powershell
& "$env:JAVA_HOME\bin\keytool.exe" -list -v -keystore leak-out-upload.jks -alias upload
```

Read the debug key's SHA-1. The debug keystore's password is always `android`:

```sh
keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android
```

```powershell
& "$env:JAVA_HOME\bin\keytool.exe" -list -v -keystore "$env:USERPROFILE\.android\debug.keystore" -alias androiddebugkey -storepass android
```

The debug keystore is per machine. Every developer machine, and every machine that runs the instrumented tests with sign-in, has its own debug key and needs its own credential. If you only want to test with builds from Google Play, you can skip the debug key.

For **each** of the three SHA-1s:

1. On the PGS **Configuration** page, under **Credentials**, click **Add credential**.
2. **Type**: Android.
3. **Name**: something you recognise later, for example `Leak Out – app signing`, `Leak Out – upload` or `Leak Out – debug (<machine name>)`.
4. Under **Authorization**, click **Create OAuth client**. This opens the Cloud console:
   1. **Application type**: Android.
   2. **Name**: the same name as above.
   3. **Package name**: `nl.hexmaster.leakout`
   4. **SHA-1 certificate fingerprint**: the SHA-1 you just read, in the `AA:BB:CC:…` form `keytool` prints.
   5. Click **Create**.
5. Back in the Play Console, click **Refresh OAuth clients**, pick the client you just created, and click **Save changes**.

You should end up with three Android credentials. Don't add a *Game server* credential: Leak Out has no server.

## 5. Create the 25 achievements

Go to **Play Games Services → Setup and management → Achievements** and click **Add achievement** for each row in [the launch catalogue](#reference-the-launch-catalogue). Enter them in the order of the table, so the **List order** in Play Games matches the app.

For every achievement, fill in:

| Field | What to enter |
|---|---|
| **Name** | The *Name* column. |
| **Description** | The *Description* column. |
<<<<<<< Updated upstream
| **Icon** | `assets/achievements/png/achievement_<key>.png`, where `<key>` is the *Key* column. |
=======
| **Icon** | `icons/png/achievement_<key>.png`, where `<key>` is the *Key* column. |
>>>>>>> Stashed changes
| **Incremental achievement** | On only when the *Steps* column has a number. Then enter that number as **How many steps are needed?** |
| **Initial state** | **Hidden** when the *Hidden* column has a ✔, **Revealed** otherwise. |
| **Points** | The *XP* column. |
| **List order** | Leave the default when you add them in table order. |

Then click **Save as draft**.

Be exact with the steps, the hidden flags and the points:

- **The app pins them.** The `core` catalogue test checks every published achievement's key, steps, hidden flag and XP. A Console value that differs from the app means the progress or the points players see in Play Games are wrong.
- **They lock when you publish.** Once you [publish](#9-publish-the-play-games-configuration) the Play Games configuration, you can't delete an achievement or change whether it is incremental, its number of steps or its points. You can still change its name, description and icon.
- **The points must add up to 595.** Play Games allows 1,000 points per game. The other 405 are kept for achievements that come with new level packs.

The names and descriptions are also the app's English string resources. When you change the wording in the Console, change the strings in the app too, so the two match.

## 6. Add testers

While the Play Games configuration is not published, only tester accounts can sign in. Everybody else sees Play Games sign-in fail, and the app keeps working without it.

1. Go to **Play Games Services → Setup and management → Testers**.
2. Click **Add testers** and add the Google accounts that should test: at least your own account, and the testers of the closed test.
3. Save.

Testers of the app's testing tracks are **not** automatically PGS testers. Add them here as well.

## 7. Export the ids into the app

1. Go to **Play Games Services → Setup and management → Achievements**.
2. Click **Get resources** and choose the **Android** tab. You get a file like this:

   ```xml
   <?xml version="1.0" encoding="utf-8"?>
   <resources>
     <string name="app_id" translatable="false">123456789012</string>
     <string name="package_name" translatable="false">nl.hexmaster.leakout</string>
     <string name="achievement_first_leak_fixed" translatable="false">CgkI…EAI</string>
     <string name="achievement_getting_the_hang_of_it" translatable="false">CgkI…EAM</string>
     …
   </resources>
   ```

3. **Rename the resources whose name doesn't match the key.** The Console makes the resource names from the achievement *names*. The app looks them up by *key* (`achievement_<key>`), and for five achievements the two differ:

   | Console name | Rename to |
   |---|---|
   | `achievement_getting_the_hang_of_it` | `achievement_levels_finished_1` |
   | `achievement_star_collector_i` | `achievement_star_collector_1` |
   | `achievement_star_collector_ii` | `achievement_star_collector_2` |
   | `achievement_perfectionist_i` | `achievement_perfectionist_1` |
   | `achievement_the_long_way_round` | `achievement_long_way_round` |

   Check the other 20 as well: each one must read `achievement_<key>` with a key from the catalogue. Keep the ids (the `CgkI…` values) exactly as exported.

4. Replace `src/app/src/main/res/values/games-ids.xml` with the result.
5. Run the resource check and the unit tests from `src/`:

   ```sh
   ./gradlew :app:testDebugUnitTest
   ./gradlew :app:connectedDebugAndroidTest   # includes the check that every catalogue key has an achievement_<key> string
   ```

6. Commit the file on a feature branch and merge it into `development` as usual. The ids and the `app_id` are not secrets: they end up in every APK anyway, so committing them is fine.

Once `app_id` is a positive number, the app starts the Play Games SDK and shows the Sign in button in Settings.

## 8. Smoke test

Test before you publish, because publishing locks the achievements.

1. Install a build with the real `games-ids.xml`:
   - a **debug build** on the emulator or a phone (needs the debug credential from step 4), or
   - a **closed-test build** from Google Play (needs the app signing credential), after the change has gone out through `main`.
2. On the device, use a **tester account** from step 6. On the emulator, use a system image *with Google Play* and sign in to that account in the Play Store.
3. Open the app and go to **Settings**. The Play Games panel shows *Signed out*, or *Signed in as …* if the silent sign-in already worked. If signed out, tap **Sign in** and finish the Play Games sign-in.
4. Start a level and let the liquid leak out, so you lose it. The level lost dialog shows *Achievement unlocked · Wet Feet*.
5. Go to **Settings → Achievements** and tap **Play Games**. Wet Feet is unlocked in the Play Games achievements list.
6. Check an incremental one: after placing some sections, *Apprentice* shows the same progress in Play Games as on the app's achievements screen.
7. Optional: check the backfill. Sign out of Play Games on a device that already has local achievements, sign in again, and check that they all appear in Play Games.

## 9. Publish the Play Games configuration

When the smoke test passes:

1. Go to **Play Games Services → Review and publish**.
2. Fix anything the page lists as missing.
3. Click **Publish**.

After publishing, every player who installs Leak Out from Google Play can sign in, not only testers. The 25 achievements are then permanent: they can't be deleted, and their type, steps and points can't change.

Publishing the Play Games configuration is separate from publishing a release. You can publish it while the app is still in closed testing.

## Adding achievements later

<<<<<<< Updated upstream
New achievements come with new level packs, each as its own OpenSpec change. The growth rules are in [achievements.md](achievements.md). In short:
=======
New achievements come with new level packs, each as its own OpenSpec change. The growth rules are in the [design](../openspec/changes/app-game-achievements/design.md) (decision 1a). In short:
>>>>>>> Stashed changes

1. **Add only.** Never remove an achievement, and never change a published key, steps, hidden flag or XP.
2. **Only achievements that can be earned.** A milestone may not need more levels or stars than are bundled, and a pack achievement only exists for a full library page.
3. **Mind the points.** The total stays at or below 1,000.

For each new achievement:

1. Add it to the `Achievement` enum in `core`, with its strings, and add it to the pinned list in the catalogue test.
<<<<<<< Updated upstream
2. Make its icon with `assets/achievements/generate-icons.mjs` and render it with `assets/achievements/render-icons.js`.
=======
2. Make its icon with `icons/generate-icons.mjs`.
>>>>>>> Stashed changes
3. Add it in the Play Console as in [step 5](#5-create-the-25-achievements), and test it as in [step 8](#8-smoke-test).
4. Export `games-ids.xml` again as in [step 7](#7-export-the-ids-into-the-app), rename any resource whose name doesn't match its key, and replace the file. The existing ids stay the same.
5. Publish the Play Games configuration.

Players who already meet the new condition get the achievement on their next app start, through the same backfill that gave existing players their progress achievements.

## Troubleshooting

### Settings says Play Games is unavailable

The app finds no valid `app_id`: `games-ids.xml` still has the placeholder `0`, or the value isn't a number. Check `src/app/src/main/res/values/games-ids.xml`.

### Sign in fails right away, or the log shows status 10 (`DEVELOPER_ERROR`)

The APK's package name and SHA-1 match no Android credential. Find out which key signed the build you're running:

- installed from Google Play → app signing key;
- built by you with `assembleRelease` or from CI's bundle → upload key;
- `assembleDebug` or run from Android Studio → the debug key of the machine that built it.

Compare that key's SHA-1 with the credentials in [step 4](#4-add-a-credential-for-every-signing-key) and add the missing one. It can take a few minutes, sometimes up to an hour, before a new credential works.

### Sign in fails with "This game is not yet available", or for some accounts only

The Play Games configuration isn't published and the account isn't a tester. Add it in [step 6](#6-add-testers), or publish ([step 9](#9-publish-the-play-games-configuration)).

### The app crashes at start with "A fatal developer error has occurred"

The `app_id` in `games-ids.xml` doesn't belong to a PGS project linked to `nl.hexmaster.leakout`. Export the file again from the right project.

### Signed in, but nothing appears in Play Games

- The ids may belong to another PGS project, or a resource name may be wrong so the app uses a placeholder id. Export `games-ids.xml` again and check the names against [step 7](#7-export-the-ids-into-the-app).
- The device may be offline. Failed pushes leave the local data alone and are retried on the next sign-in or the next local change.
- The Play Games app can take a moment to refresh. Close its achievements list and open it again.

### The resource test fails: a catalogue key has no `achievement_<key>` string

A resource name in `games-ids.xml` doesn't match a catalogue key. Usually it is one of the five renames in [step 7](#7-export-the-ids-into-the-app).

### Points in the Console don't add up to 595

A value was entered wrong. Correct it *before* publishing. After publishing, points can't be changed.

## Reference: the launch catalogue

<<<<<<< Updated upstream
These are the 25 launch achievements, in list order. Key, steps, hidden flag and XP come from the [catalogue](achievements-technical.md#launch-catalogue) and may never change once published. The descriptions are the English text the Console and the app's string resources share.
=======
These are the 25 launch achievements, in list order. Key, steps, hidden flag and XP come from the [`game-achievements` spec](../openspec/changes/app-game-achievements/specs/game-achievements/spec.md) and may never change once published. The descriptions are the English text the Console and the app's string resources share.
>>>>>>> Stashed changes

| # | Key | Name | Steps | Hidden | XP | Description |
|---|---|---|---|---|---|---|
| 1 | `first_leak_fixed` | First Leak Fixed | | | 10 | Finish level 1. |
| 2 | `levels_finished_1` | Getting the Hang of It | 10 | | 15 | Finish 10 levels. |
| 3 | `star_collector_1` | Star Collector I | 25 | | 15 | Collect 25 stars. |
| 4 | `star_collector_2` | Star Collector II | 60 | | 30 | Collect 60 stars. |
| 5 | `perfectionist_1` | Perfectionist I | 10 | | 30 | Get 3 stars on 10 levels. |
| 6 | `pack_1_complete` | Pack 1 Complete | 24 | | 30 | Finish every level on page 1. |
| 7 | `pack_1_perfect` | Pack 1 Perfect | 24 | | 40 | Get 3 stars on every level on page 1. |
| 8 | `zero_waste` | Zero Waste | | | 30 | Win a level with no unused sections. |
| 9 | `double_cross` | Double Cross | | | 20 | Send the liquid through both channels of a cross. |
| 10 | `crossroads_king` | Crossroads King | | | 40 | Send the liquid through both channels of 3 crosses in one attempt. |
| 11 | `ahead_of_the_flow` | Ahead of the Flow | | | 30 | Complete the route before the liquid starts flowing. |
| 12 | `full_throttle` | Full Throttle | | | 15 | Win a level with fast forward on. |
| 13 | `measure_twice` | Measure Twice | | | 20 | Win a level without replacing a section. |
| 14 | `steady_hands` | Steady Hands | | | 15 | Win a level without pausing or leaving the game. |
| 15 | `long_way_round` | The Long Way Round | | | 30 | Win with the liquid flowing through 30 or more sections. |
| 16 | `big_spender` | Big Spender | | | 30 | Win a level with a score of 500 or more. |
| 17 | `apprentice` | Apprentice | 100 | | 10 | Place 100 sections. |
| 18 | `journeyman` | Journeyman | 1000 | | 25 | Place 1,000 sections. |
| 19 | `guildmaster` | Guildmaster | 5000 | | 40 | Place 5,000 sections. |
| 20 | `never_give_up` | Never Give Up | | | 20 | Win a level after losing it 3 times in a row. |
| 21 | `personal_best` | Personal Best | | | 15 | Beat your own highscore on a level. |
| 22 | `wet_feet` | Wet Feet | | | 5 | Lose a level for the first time. |
| 23 | `instant_flood` | Instant Flood | | ✔ | 20 | Lose before the liquid reaches any section you placed. |
| 24 | `wall_to_wall` | Wall to Wall | | ✔ | 30 | Win with every free cell filled. |
| 25 | `photo_finish` | Photo Finish | | ✔ | 30 | Complete the route on the very tile the liquid was about to enter. |

Points per group: progress 170 (rows 1–7), skill 230 (rows 8–16), lifetime 115 (rows 17–22), hidden 80 (rows 23–25). Total: **595**.
