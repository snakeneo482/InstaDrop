# InstaDrop

Android app that registers as a **share target** so you can send an Instagram
Reel / Post / Story straight from Instagram → InstaDrop → saved to your gallery.

Native Android (Kotlin + Jetpack Compose, Material 3).
Share → validate → fetch → preview → download → save to `/Movies/InstaDrop`,
with persistent history, download notifications, and carousel "download all".

- **Package (code):** `com.instadrop.app`
- **applicationId (Play):** `com.instadrop.myapp`

## How the share flow works

```
Instagram → Share → InstaDrop (ACTION_SEND text/plain)
          → MainActivity.handleIntent → MainViewModel.onSharedText
          → InstagramUrl.extract  (pull + clean the URL)
          → MediaResolver.resolve (→ InstaMedia)
          → Preview → MediaDownloader (MediaStore) → Gallery
```

The intent-filter that makes InstaDrop appear in Instagram's share sheet lives in
[`app/src/main/AndroidManifest.xml`](app/src/main/AndroidManifest.xml).

## The one seam you need to fill in: the resolver

Instagram has **no official download API**, so the actual media extraction is
isolated behind a single interface:

- [`MediaResolver`](app/src/main/java/com/instadrop/app/data/resolver/MediaResolver.kt)
  — `suspend fun resolve(url): InstaMedia`
- [`StubMediaResolver`](app/src/main/java/com/instadrop/app/data/resolver/StubMediaResolver.kt)
  — the current stand-in. It validates the URL and returns a **publicly-hosted
  sample video**, so the whole app (preview, download, gallery, history) works
  end-to-end on a real device today.

When your real backend is ready, implement `MediaResolver` and swap it in one
place — [`ServiceLocator.init`](app/src/main/java/com/instadrop/app/data/ServiceLocator.kt).
Nothing else changes.

> ⚠️ Scraping Instagram violates their Terms of Service and breaks often. Keep
> extraction in a backend you control, and only download content you have the
> rights to.

## Build & run (debug)

Requires Android Studio (Ladybug+) or a local JDK 17 + Android SDK. The Gradle
wrapper (`gradlew` + `gradle-wrapper.jar`) is included, so the CLI works directly:

```bash
./gradlew installDebug      # build + install on a connected device/emulator
```

Test the share flow: open Instagram → a Reel → Share → **InstaDrop**.

## Building a signed release `.aab` (for Play)

### 1. Generate an upload keystore (once)

```bash
keytool -genkeypair -v \
  -keystore instadrop-release.jks \
  -alias instadrop \
  -keyalg RSA -keysize 2048 -validity 10000
```

Keep `instadrop-release.jks` **outside** the repo (it's git-ignored anyway) and
back it up — losing it means you can't update the app.

### 2. Create `keystore.properties`

```bash
cp keystore.properties.template keystore.properties
# then edit it with your real passwords / path
```

```properties
storeFile=../instadrop-release.jks
storePassword=********
keyAlias=instadrop
keyPassword=********
```

The build reads this automatically (see
[`app/build.gradle.kts`](app/build.gradle.kts)). It's git-ignored, so secrets
never get committed.

### 3. Build the bundle

```bash
./gradlew bundleRelease
```

Output (signed with your upload key):

```
app/build/outputs/bundle/release/app-release.aab
```

Upload that `.aab` to the Play Console. With **Play App Signing** (recommended),
Google manages the final app signing key; your `.jks` is just the upload key.

## What's in

Share-target intent-filter · URL validation · pluggable resolver seam · preview
· streaming download to gallery (scoped storage on 10+, legacy permission below)
· progress UI with speed · **persistent history (Room)** · **download
notifications** (progress + tap-to-open) · **carousel "download all"** ·
open / share / copy-caption.

## Possible next steps

Built-in video player · concurrent batch queue · in-app dark-mode toggle ·
favorites / search in history · premium gating (no ads, unlimited, cloud backup).
