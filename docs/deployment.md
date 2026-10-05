# Deploying to Google Play

Every push to `main` builds a signed release of Leak Out and uploads it to the Play Console's private testing track, so testers get the new build within minutes. This guide explains how that works and how to set up every key, account, secret and variable it needs, from an empty Play Console to a green run.

You only do this setup once. After that, releasing is merging `development` into `main`.

- [How it works](#how-it-works)
- [What you need](#what-you-need)
- [1. Create the upload keystore](#1-create-the-upload-keystore)
- [2. Create the app in the Play Console and upload the first build by hand](#2-create-the-app-in-the-play-console-and-upload-the-first-build-by-hand)
- [3. Add testers](#3-add-testers)
- [4. Create a Google Cloud service account](#4-create-a-google-cloud-service-account)
- [5. Give the service account access in the Play Console](#5-give-the-service-account-access-in-the-play-console)
- [6. Create the GitHub environment and secrets](#6-create-the-github-environment-and-secrets)
- [7. Set the repository variables](#7-set-the-repository-variables)
- [8. Run the workflow](#8-run-the-workflow)
- [Versioning](#versioning)
- [Release notes](#release-notes)
- [Troubleshooting](#troubleshooting)
- [Maintenance](#maintenance)
- [Reference: every secret and variable](#reference-every-secret-and-variable)

## How it works

The workflow is [`.github/workflows/deploy-play-internal.yml`](../.github/workflows/deploy-play-internal.yml). It runs on every push to `main`, and you can also start it by hand. Only one deployment runs at a time; a newer one waits for the running one to finish.

On each run it:

1. checks that all five secrets are set (and stops with the names of any that are missing);
2. runs the JVM unit tests (`:core:test`, `:app:testDebugUnitTest`) and stops if any fail;
3. works out the **version** with GitVersion (`versionName` `x.y.z`) and the **version code** from it (see [Versioning](#versioning));
4. writes the **release notes** from the commits since the previous release (see [Release notes](#release-notes));
5. decodes the upload keystore and builds `app-release.aab` signed with the **upload key**;
6. checks that the bundle really is signed;
7. keeps the bundle as a workflow artifact for 14 days;
8. uploads it with the release notes to the Play Console track in `PLAY_TRACK` (default: internal testing), then tags the commit `v<x.y.z>`;
9. deletes the decoded keystore, whether the run succeeded or not.

Three keys and accounts are involved. It helps to keep them apart:

```
 GitHub Actions                         Google Play                         Testers' phones
 ──────────────                         ───────────                         ───────────────
 upload keystore (secret) ──signs──▶  app-release.aab ──Play re-signs──▶  APK signed with the
                                       checked against      with the          APP SIGNING KEY
                                       your UPLOAD KEY      APP SIGNING KEY
                                       certificate          (kept by Google)

 service account JSON (secret) ──authenticates the upload──▶ Play Developer API
```

- **Upload key**: yours. It lives in a keystore file that you create in step 1. It proves to Google that an upload comes from you. If it is lost or leaks, Google can reset it (see [Maintenance](#maintenance)).
- **App signing key**: Google's, through *Play App Signing*. Google signs what reaches testers' phones with it. You never handle it, so it cannot be lost.
- **Service account**: a Google Cloud robot account. Its JSON key lets the workflow call the Play Developer API for this one app.

Local builds are not affected. Without the `LEAKOUT_UPLOAD_*` environment variables, `:app:bundleRelease` makes an unsigned bundle, exactly as before, and the version code is the one in `src/app/build.gradle.kts`. You never need the keystore to develop.

## What you need

- A **Google Play developer account** with the Owner or Admin role, so you can create the app and invite users.
- **Admin** access to the GitHub repository, to create environments, secrets and variables.
- A **Google account** that can create a Google Cloud project (any Google account can; a company account may have restrictions, see [Troubleshooting](#troubleshooting)).
- A **JDK** (17 or later) for `keytool`. Android Studio ships one: `<Android Studio>/jbr/bin/keytool`.
- A **password manager** to store the keystore, its password and the service-account key.
- Optional: the [GitHub CLI](https://cli.github.com/) (`gh`). It makes adding secrets easier, and the commands below use it.

## 1. Create the upload keystore

Run this **outside the repository**, in a folder you will clean up afterwards. Keystores must never be committed; `*.jks` and `*.keystore` are in `.gitignore` as a safety net, but don't rely on it.

```sh
keytool -genkeypair -v -keystore leak-out-upload.jks -storetype PKCS12 -alias upload -keyalg RSA -keysize 4096 -validity 10000

& "$env:JAVA_HOME\bin\keytool.exe" -genkeypair -v -keystore leak-out-upload.jks -storetype PKCS12 -alias upload -keyalg RSA -keysize 4096 -validity 10000
```

On Windows PowerShell, put the command on one line or replace each `\` with a backtick (`` ` ``).

`keytool` asks for:

- **A keystore password.** Use a long random password from your password manager.
- **Your name, organisation, city, country.** These end up in the certificate. Testers never see them, but use real values.

About these choices:

- `-validity 10000` is about 27 years. Google Play requires the key to be valid until at least 22 October 2033, and a key that expires stops working for uploads.
- `-storetype PKCS12` is the standard format (and `keytool`'s default). **With PKCS12 the key password is the same as the keystore password.** `keytool` does not ask for a separate one. You will put the same value in both `PLAY_UPLOAD_KEYSTORE_PASSWORD` and `PLAY_UPLOAD_KEY_PASSWORD`.
- `upload` is the alias. Any name works; it goes into `PLAY_UPLOAD_KEY_ALIAS`.

Check the result:

```sh
keytool -list -v -keystore leak-out-upload.jks -alias upload
& "$env:JAVA_HOME\bin\keytool.exe"  -list -v -keystore leak-out-upload.jks -alias upload
```

You should see one `PrivateKeyEntry` with a `Valid until` date decades from now. Note the **SHA-1** fingerprint. It is useful when [the Play Console complains about the wrong key](#the-android-app-bundle-was-signed-with-the-wrong-key).

**Back it up now.** Store `leak-out-upload.jks` itself (as an attachment), the password and the alias in your password manager. Losing the upload key is recoverable, but only through Google support, which takes days.

## 2. Create the app in the Play Console and upload the first build by hand

The Play Developer API cannot create an app, and the first upload decides which key is your upload key. So the first build goes up by hand.

### 2.1 Create the app

1. Open the [Play Console](https://play.google.com/console) and choose **Create app**.
2. Fill in the app name (*Leak Out*), default language, *App or game*: **Game**, *Free or paid*, and accept the declarations.
3. The package name is not asked here. It is fixed by the first bundle you upload, and it must be **`nl.hexmaster.leakout`** (the `applicationId` in `src/app/build.gradle.kts`). Once set, it can never change.

### 2.2 Build a signed bundle locally

From the repository's `src/` folder, point the build at the keystore from step 1 and use **version code 1**.

Windows PowerShell:

```powershell
$env:LEAKOUT_UPLOAD_STORE_FILE     = "D:\projects\github.com\hexmasternl\leak-out\src\leak-out-upload.jks"
$env:LEAKOUT_UPLOAD_STORE_PASSWORD = "BK^OZM*3YVWAzGks^4fv@1iHkyxf1kvL"
$env:LEAKOUT_UPLOAD_KEY_ALIAS      = "upload"
$env:LEAKOUT_UPLOAD_KEY_PASSWORD   = "BK^OZM*3YVWAzGks^4fv@1iHkyxf1kvL"
.\gradlew.bat :app:bundleRelease -PversionCode=1
```

macOS / Linux:

```sh
export LEAKOUT_UPLOAD_STORE_FILE=/path/to/leak-out-upload.jks
export LEAKOUT_UPLOAD_STORE_PASSWORD='<keystore password>'
export LEAKOUT_UPLOAD_KEY_ALIAS=upload
export LEAKOUT_UPLOAD_KEY_PASSWORD='<keystore password>'
./gradlew :app:bundleRelease -PversionCode=1
```

The bundle is `src/app/build/outputs/bundle/release/app-release.aab`. Confirm it is signed: `jarsigner -verify src/app/build/outputs/bundle/release/app-release.aab` must print `jar verified.` (the warnings about a self-signed certificate are normal).

Close the terminal afterwards (or remove the four variables), so the passwords don't linger in your shell.

### 2.3 Upload it to internal testing

1. In the app, go to **Test and release → Testing → Internal testing** and choose **Create new release**.
2. Under *App signing* (or *App integrity*), accept **Play App Signing** with a **Google-generated app signing key**. This is the default and the recommended choice. Google now keeps the app signing key, and the key that signed this upload becomes your registered **upload key**.
3. Upload `app-release.aab`, give the release a name (for example `0.1.0 (1)`), and save.
4. The **Dashboard** lists the setup tasks the Console requires before a release can go out (such as app access, ads, content rating, target audience, data safety and a privacy policy). Complete those it marks as required.
5. Back on the release, choose **Review release → Start rollout to Internal testing**.

Rolling this first release out matters. Until an app has had a release rolled out, it is a *draft app*, and the API only accepts releases with status `draft` for it. If you cannot roll out yet, set the variable `PLAY_RELEASE_STATUS` to `draft` in step 7 for now.

## 3. Add testers

The internal testing track is private: only people on its tester list can install builds, and new builds reach them within minutes, without review. It allows up to 100 testers.

1. In **Test and release → Testing → Internal testing**, open the **Testers** tab.
2. Create an email list (for example *Leak Out testers*), add the testers' Google account emails, and select the list.
3. Copy the **opt-in link** (*Join on the web*) and send it to the testers. Each tester opens it once, accepts, and can then install Leak Out from the Play Store on a phone signed in with that account.

**Prefer a named closed track** (for example a *Private preview* track for a larger group)? Create it under **Testing → Closed testing → Create track**, add testers there, and set the variable `PLAY_TRACK` to the track's name in step 7. Builds on closed tracks go through Google's review, so they take longer to appear.

## 4. Create a Google Cloud service account

The workflow authenticates as a service account. Create one in a Google Cloud project; any project you control works, and a dedicated one keeps things tidy.

1. Open the [Google Cloud console](https://console.cloud.google.com/) and create a project (for example `leak-out-play-publishing`), or pick an existing one.
2. Enable the API: **APIs & Services → Library**, search for **Google Play Android Developer API**, and choose **Enable**.
3. Create the account: **IAM & Admin → Service accounts → Create service account**. Name it for example `github-play-publisher`. Skip the optional *grant access to project* and *grant users access* steps: it needs **no** Cloud roles, only Play Console permissions (next step).
4. Copy its email address. It looks like `github-play-publisher@leak-out-play-publishing.iam.gserviceaccount.com`.
5. Create a key: open the service account, go to **Keys → Add key → Create new key → JSON**. A `.json` file downloads.

The JSON key grants upload access to your app. Keep it out of the repository, store it in your password manager, and **delete the downloaded file** once it is in GitHub (step 6).

## 5. Give the service account access in the Play Console

1. In the Play Console, go to **Users and permissions** (in the account menu, not inside the app) and choose **Invite new users**.
2. Enter the service account's **email address** from step 4.
3. Under **App permissions**, choose **Add app** and select **Leak Out**. Grant only:
   - **View app information (read-only)**
   - **Release apps to testing tracks**

   Leave the *Account permissions* empty. The service account should not be able to release to production or touch other apps.
4. Choose **Invite user** and confirm. A service account doesn't need to accept the invitation.

New permissions can take some time to apply, sometimes up to a day. If the first run fails with a permission error right after this step, see [Troubleshooting](#the-caller-does-not-have-permission).

If your Play Console still has an **API access** page under *Setup* asking you to link a Google Cloud project, link the project from step 4 there too. Newer consoles don't need this.

## 6. Create the GitHub environment and secrets

The secrets live in a GitHub **environment** called `play-store`. Only workflow runs on `main` can deploy to it, so a workflow edited on any other branch cannot read the signing key or the Play credentials.

### 6.1 Create the environment

1. In the repository, go to **Settings → Environments → New environment** and name it exactly **`play-store`**.
2. Under **Deployment branches and tags**, choose **Selected branches and tags**, then **Add deployment branch or tag rule**, and enter `main`.
3. Optional: under **Required reviewers**, add yourself if every deployment should wait for your approval.

Create the environment **before** the first run. If a run finds no `play-store` environment, GitHub creates one without the branch rule.

### 6.2 Add the five secrets

Add these under **Settings → Environments → play-store → Environment secrets → Add environment secret**. Do not add them as repository secrets.

| Secret | What to put in it |
| --- | --- |
| `PLAY_UPLOAD_KEYSTORE_BASE64` | The keystore file from step 1, base64-encoded (commands below) |
| `PLAY_UPLOAD_KEYSTORE_PASSWORD` | The keystore password from step 1 |
| `PLAY_UPLOAD_KEY_ALIAS` | The alias from step 1 (`upload` if you followed the command) |
| `PLAY_UPLOAD_KEY_PASSWORD` | The key password. For a PKCS12 keystore this is the **same** as the keystore password |
| `PLAY_SERVICE_ACCOUNT_JSON` | The whole content of the JSON key file from step 4, as is |

**Encoding the keystore.** GitHub secrets are text, so the binary keystore is stored base64-encoded. The workflow strips whitespace before decoding, so line breaks don't break it, but one line is cleanest:

- Windows PowerShell (copies to the clipboard):
  ```powershell
  [Convert]::ToBase64String([IO.File]::ReadAllBytes("C:\path\to\leak-out-upload.jks")) | Set-Clipboard
  ```
- macOS (copies to the clipboard):
  ```sh
  base64 -i leak-out-upload.jks | tr -d '\n' | pbcopy
  ```
- Linux:
  ```sh
  base64 -w 0 leak-out-upload.jks
  ```

Paste the result into the secret.

**With the GitHub CLI**, run these from the repository folder. Each command prompts for the value, or reads it from a file, so nothing ends up in your shell history:

```sh
base64 -w 0 leak-out-upload.jks | gh secret set PLAY_UPLOAD_KEYSTORE_BASE64 --env play-store   # macOS: base64 -i … | tr -d '\n'
gh secret set PLAY_UPLOAD_KEYSTORE_PASSWORD --env play-store
gh secret set PLAY_UPLOAD_KEY_ALIAS --env play-store
gh secret set PLAY_UPLOAD_KEY_PASSWORD --env play-store
gh secret set PLAY_SERVICE_ACCOUNT_JSON --env play-store < github-play-publisher-key.json
```

On Windows PowerShell, pipe the keystore the same way: `[Convert]::ToBase64String([IO.File]::ReadAllBytes("leak-out-upload.jks")) | gh secret set PLAY_UPLOAD_KEYSTORE_BASE64 --env play-store`, and use `Get-Content github-play-publisher-key.json -Raw | gh secret set PLAY_SERVICE_ACCOUNT_JSON --env play-store` for the JSON key.

Check with `gh secret list --env play-store`: all five names must be listed. GitHub never shows the values again.

Now delete the local copies of the base64 text and the JSON key file. The keystore stays in your password manager.

## 7. Set the repository variables

These two settings are not secret. They are **repository variables**: **Settings → Secrets and variables → Actions → Variables tab → New repository variable**. Each one has a default, so you only need to set those you want to change.

| Variable | Default when not set | When to change it |
| --- | --- | --- |
| `PLAY_TRACK` | `internal` | Set it to the name of a closed testing track to release there instead (see [step 3](#3-add-testers)). |
| `PLAY_RELEASE_STATUS` | `completed` | Set it to `draft` while the app has never had a release rolled out (a *draft app*). Remove it, or set `completed`, once the first rollout is done. With `draft`, each upload waits in the Console until you roll it out by hand. |

With the GitHub CLI:

```sh
gh variable set PLAY_TRACK --body internal
gh variable set PLAY_RELEASE_STATUS --body draft       # only while the app is a draft app
```

## 8. Run the workflow

The deployment starts on its own when `development` is merged into `main`. To run it without a new merge, open the **Actions** tab, choose **Deploy to Play internal testing**, then **Run workflow** with branch `main`. The *Run workflow* button only appears once the workflow file is on `main`.

A successful run:

- shows every step green, including *Verify bundle signature*, *Upload to Google Play* and *Remove upload keystore*;
- has a **summary** with the version, the version code, the track, the release status and the commit;
- has an artifact `leak-out-<version>-<version code>` with the uploaded `.aab`, kept for 14 days.

In the Play Console, the release appears under **Test and release → Testing → Internal testing → Releases**, named after its version (for example `1001 (0.1.1)`). Testers who opted in get the update through the Play Store, usually within minutes.

## Versioning

The workflow sets both versions of every release; nothing is bumped by hand.

| | What it is | Where it comes from |
| --- | --- | --- |
| `versionName` | The version testers see: `<major>.<minor>.<patch>`, for example `0.2.1` | [GitVersion](https://gitversion.net) 6 (`MajorMinorPatch`), configured in [`GitVersion.yml`](../GitVersion.yml) |
| `versionCode` | The number Google Play orders releases by; every upload must be higher than all earlier ones | `major × 1 000 000 + minor × 1 000 + patch`, so `0.2.1` → `2001` and `1.0.0` → `1000000` |
| Git tag | `v<major>.<minor>.<patch>` on the released commit | Pushed by the workflow after a successful upload |

GitVersion uses the GitHub Flow workflow: on `main` it starts from the newest `v*` tag and increments the **patch** (the very first release, with no tag yet, is `next-version` from `GitVersion.yml`: `0.1.0`). Because each release is tagged, each merge into `main` gives the next patch:

| Merge into `main` | Version | Version code |
| --- | --- | --- |
| first release | `0.1.0` | `1000` |
| a fix | `0.1.1` | `1001` |
| a feature with `+semver: minor` in a commit message or the PR title | `0.2.0` | `2000` |
| another fix | `0.2.1` | `2001` |
| a breaking change with `+semver: major` | `1.0.0` | `1000000` |

To bump the minor or major version, put `+semver: minor` or `+semver: major` in any commit message that reaches `main` (a squash-merge PR title counts). You can also raise `next-version` in `GitVersion.yml`; it only takes effect when it is above the newest tag. `+semver: none` (or `skip`) keeps the version, which makes the run fail, so don't use it for a release.

**Why the version code always increases.** The formula keeps the order of the versions as long as minor and patch stay at or below 999 and major at or below 2099 (the largest code, `2099999999`, is just under Play's limit of `2100000000`). GitVersion only moves forward from the newest tag, and before building, [`scripts/compute-release-version.sh`](../scripts/compute-release-version.sh) refuses a version that doesn't fit the formula, is below the newest release tag, or whose tag already sits on a different commit. So the run fails before uploading rather than sending Play a code that is too low. Its tests: `bash scripts/compute-release-version.test.sh`.

Rules that keep this working:

- Don't delete or move `v*` tags, and don't create them by hand: the workflow owns them.
- A release tag is only pushed after the upload succeeds. A failed run leaves no tag, so the next run gets the same version again.
- Re-running a run that already uploaded gets the same version and code, and Play rejects it as already used. Merge a new commit instead.

Local builds keep `versionName` `0.1.0` and `versionCode` `1` unless you pass `-PversionName=<x.y.z>` and `-PversionCode=<n>`.

## Release notes

Every release in the Play Console gets a "What's new" text that the workflow writes from the commits it releases. Nobody types them in by hand.

**Where they come from.** [`scripts/generate-release-notes.sh`](../scripts/generate-release-notes.sh) looks at the commits since the previous `v*` release tag (the whole history for the very first release):

| In the range | Becomes |
| --- | --- |
| A merged pull request from `feature/<change-name>` | One note: the PR title without the `<change-name>:` prefix, issue references (`(issue #64)`, `(#64)`) and `+semver:` markers, starting with a capital |
| A `Release-note: <text>` trailer on a commit in that pull request | Replaces the PR title. Several trailers give several notes |
| `Release-note: none` on a commit in that pull request | Leaves the pull request out |
| A `Release-note: <text>` trailer on a commit that reached `main` without a feature pull request | A note of its own |
| Anything else: merges of `development`, `Merge remote-tracking branch …`, commits without a trailer | Nothing |

Notes are listed oldest first and each text only once. The PR title `app-game-more-achievements: 14 new achievements (issue #64)` becomes:

```text
• 14 new achievements
```

**Write PR titles for players.** The title after the prefix is what testers and players read, so describe what they will notice: `app-game-pause: pause the game at any time`, not `app-game-pause: add PauseState and wire the overlay`. GitHub's *Create a merge commit* puts the title in the merge commit; squash or rebase merges aren't recognised as pull requests, so they only show up through a trailer.

**Trailers** go in the last paragraph of a commit message, so they can be added at any time before the pull request is merged into `development`:

```sh
git commit --allow-empty -m "app-game-pause: word the release note" -m "Release-note: Pause the game at any time"
git commit --allow-empty -m "app-ci: keep out of the release notes" -m "Release-note: none"
```

`none` is not case-sensitive. A trailer on the merge commit itself counts for its pull request.

**Limits.** The Play Console accepts at most 500 characters per language. When the notes are longer, whole lines are dropped from the end and a final `• …and more improvements` line is added; a note is never cut in half. When nothing qualifies, the notes are `Bug fixes and improvements.`. The notes are written for `en-US`, the app's default language.

**Where to see them.** In the run's summary (*Release notes (en-US)*), in the release in the Play Console, and in the tag's message (`git show v<x.y.z>`). To preview the notes of the next release, run from the repository root:

```sh
git fetch origin --tags
bash scripts/generate-release-notes.sh origin/main
```

That prints the notes of the latest commit on `main` since the release before it; run it on a merge of `development` you haven't pushed yet to see the next one. Its tests: `bash scripts/generate-release-notes.test.sh`.

**Release notes never stop a deployment.** If the tests or the script fail, the run logs a warning and uploads with `Bug fixes and improvements.`. Fix the script on a feature branch; the next release gets proper notes again (they cover everything since the previous tag, so nothing is lost).

## Troubleshooting

Find the failing step in the run's log. The upload step prints Google's error message, which is usually one of these.

### Secret … is not set in the play-store environment

The *Check required secrets* step lists every secret that is empty. Add it as an **environment** secret of `play-store` (step 6.2), not as a repository secret, and check the name is spelled exactly as shown.

### Branch "…" is not allowed to deploy to play-store

The run was not on `main`, and the environment's branch rule refused it. That is intended. Run the workflow from `main`.

### A unit test fails

Nothing was uploaded. Fix the test on a feature branch, merge it through `development` into `main`, and the next run deploys.

### Version code … has already been used

Every upload needs a version code higher than every earlier one, on any track. This happens when a run that already uploaded is re-run, or when a bundle was uploaded by hand with a higher code. Look up the highest version code in **Test and release → Latest releases and bundles** (or *App bundle explorer*). For a re-run, merge a new commit into `main` instead. For a higher code from elsewhere, bump the version past it with `+semver: minor` or `+semver: major` (see [Versioning](#versioning)), then merge.

### Version … is below the newest release / does not fit the version code / Tag … already exists on another commit

The *Compute version* step refused the version before anything was built. A `v*` tag was probably added, moved or deleted by hand. Restore the tags so that the newest one is the last release, or bump the version with `+semver: minor` (see [Versioning](#versioning)). When minor or patch would pass 999, bump the next part up.

### The release-notes tests failed / Generating the release notes failed

A warning, not an error: the release went out with `Bug fixes and improvements.` as its notes. The *Generate release notes* step prints the failing test or the script's `::error::`. Reproduce it with `bash scripts/generate-release-notes.test.sh` and fix it on a feature branch (see [Release notes](#release-notes)).

### The upload step rejects the release-notes language

The release notes are uploaded as `whatsnew-en-US`, and Play only accepts languages the store listing has. Check the default language under **Grow users → Store presence → Main store listing**: it must be English (United States) – en-US. If it is another language, either change it there or rename the file in the *Generate release notes* step to `whatsnew-<that language's code>`.

### Only releases with status draft may be created on draft app

The app has never had a release rolled out. Either roll out a release by hand once (step 2.3), or set `PLAY_RELEASE_STATUS` to `draft` until you have.

### Package not found: nl.hexmaster.leakout

The Play Console has no app with that package name yet, or the service account can't see it. Do the first manual upload (step 2), and check that the service account has app permissions for **Leak Out** (step 5).

### The caller does not have permission

The upload may also fail with *403*, *forbidden* or *insufficient permissions*. The service account lacks Play Console access. Check that its email is invited under **Users and permissions** with **Release apps to testing tracks** for this app (step 5). If you just granted it, wait: permissions can take up to a day to apply. Editing and saving the app's details in the Console sometimes speeds this up.

### Google Play Android Developer API has not been used in project … or it is disabled

Enable the **Google Play Android Developer API** in the Google Cloud project that owns the service account (step 4), wait a few minutes, and re-run.

### The Android App Bundle was signed with the wrong key

The keystore in `PLAY_UPLOAD_KEYSTORE_BASE64` is not the registered upload key. The error shows the expected and the found SHA-1 fingerprints. Compare them with `keytool -list -v -keystore <file> -alias <alias>`, and put the keystore whose fingerprint matches *expected* in the secret. If you no longer have that keystore, [reset the upload key](#lost-or-leaked-upload-key).

### keystore password was incorrect / Cannot recover key / No key with alias

`PLAY_UPLOAD_KEYSTORE_PASSWORD`, `PLAY_UPLOAD_KEY_PASSWORD` or `PLAY_UPLOAD_KEY_ALIAS` doesn't match the keystore. Remember that for a PKCS12 keystore both passwords are the same. Also check that `PLAY_UPLOAD_KEYSTORE_BASE64` is the encoding of the whole file: re-encode it with the commands in step 6.2.

### Key creation is not allowed on this service account

A company Google Cloud organisation may block JSON keys with the policy `iam.disableServiceAccountKeyCreation`. Ask the organisation admin for an exception for this project, create the project under a personal Google account, or use [Workload Identity Federation](#moving-to-workload-identity-federation) instead.

### Failed to find target with hash string 'android-37' / SDK platform missing

The runner image doesn't have the Android platform the app compiles against. Add a step before the build that runs `"$ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager" "platforms;android-37"`, with `working-directory: .`.

## Maintenance

### Rotate the service-account key

Do this once a year, or immediately if the JSON key may have leaked.

1. In the Google Cloud console, open the service account → **Keys → Add key → Create new key → JSON**.
2. Replace the `PLAY_SERVICE_ACCOUNT_JSON` secret with the new file's content.
3. Run the workflow once to confirm it works, then **delete the old key** on the same *Keys* page.

### Lost or leaked upload key

Your app is safe, because Google holds the app signing key, but you need a new upload key:

1. Create a new keystore as in step 1.
2. Export its certificate:
   ```sh
   keytool -export -rfc -keystore leak-out-upload-new.jks -alias upload -file upload_certificate.pem
   ```
3. In the Play Console, go to **Test and release → Setup → App signing** (or *App integrity*), choose **Request upload key reset**, and upload `upload_certificate.pem`.
4. Once Google confirms the reset (this takes a few days), replace `PLAY_UPLOAD_KEYSTORE_BASE64`, `PLAY_UPLOAD_KEYSTORE_PASSWORD`, `PLAY_UPLOAD_KEY_ALIAS` and `PLAY_UPLOAD_KEY_PASSWORD`, and store the new keystore in your password manager.

### Change the track

Set `PLAY_TRACK` to `internal`, `alpha` (the default closed track), `beta` (open testing) or the name of a custom closed track. Production is deliberately not a target: the service account has no permission for it. Promote builds to production by hand in the Console.

### Jump the version code

If version codes ever collide, bump the version (see [Version code … has already been used](#version-code--has-already-been-used)). Version codes only need to increase, so gaps are harmless.

### Moving to Workload Identity Federation

A JSON key is a long-lived secret. With Workload Identity Federation, GitHub instead proves the run's identity to Google Cloud and gets a short-lived token, so there is no key to leak or rotate. To switch:

1. In Google Cloud, create a workload identity pool and an OIDC provider for `https://token.actions.githubusercontent.com`, limited to this repository and the `main` branch (attribute condition on `assertion.repository` and `assertion.ref`).
2. Allow that provider to impersonate the service account (role **Workload Identity User** on the service account).
3. In the workflow, add `id-token: write` to `permissions`, add a [`google-github-actions/auth`](https://github.com/google-github-actions/auth) step (pinned by SHA) with `workload_identity_provider` and `service_account`, and pass the credentials file it creates to the upload step as `serviceAccountJson` instead of `serviceAccountJsonPlainText`.
4. Remove `PLAY_SERVICE_ACCOUNT_JSON` from the secret check and from the environment, and delete the service account's JSON keys.

This is a change to the workflow, so make it as its own OpenSpec change.

## Reference: every secret and variable

Environment secrets (`play-store`):

| Name | Used for |
| --- | --- |
| `PLAY_UPLOAD_KEYSTORE_BASE64` | Decoded to a temporary keystore file; Gradle reads it as `LEAKOUT_UPLOAD_STORE_FILE` |
| `PLAY_UPLOAD_KEYSTORE_PASSWORD` | `LEAKOUT_UPLOAD_STORE_PASSWORD` |
| `PLAY_UPLOAD_KEY_ALIAS` | `LEAKOUT_UPLOAD_KEY_ALIAS` |
| `PLAY_UPLOAD_KEY_PASSWORD` | `LEAKOUT_UPLOAD_KEY_PASSWORD` |
| `PLAY_SERVICE_ACCOUNT_JSON` | Authenticating the upload to the Play Developer API |

Repository variables:

| Name | Default | Used for |
| --- | --- | --- |
| `PLAY_TRACK` | `internal` | The Play track to upload to |
| `PLAY_RELEASE_STATUS` | `completed` | The release status (`draft` for a draft app) |

Gradle inputs (set by the workflow, or by you for a local signed build):

| Name | Kind | Effect |
| --- | --- | --- |
| `LEAKOUT_UPLOAD_STORE_FILE` | environment variable | Path to the keystore. When set, the release build is signed and the three below are required |
| `LEAKOUT_UPLOAD_STORE_PASSWORD` | environment variable | Keystore password |
| `LEAKOUT_UPLOAD_KEY_ALIAS` | environment variable | Key alias |
| `LEAKOUT_UPLOAD_KEY_PASSWORD` | environment variable | Key password |
| `versionCode` | Gradle property (`-PversionCode=<n>`) | Overrides the version code; must be a positive whole number |
| `versionName` | Gradle property (`-PversionName=<x.y.z>`) | Overrides the version name (default `0.1.0`); must be `<major>.<minor>.<patch>` |
