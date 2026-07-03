# InstaDrop

Android app that registers as a **share target** so you can send a link from
almost any app — **YouTube, Instagram (incl. Stories), TikTok, X, Facebook,
Reddit and ~1000 more** — straight to InstaDrop and save the media to your
gallery.

Native Android (Kotlin + Jetpack Compose, Material 3).
Share/paste → validate → resolve → preview → download → save to the gallery,
with persistent history, download notifications, and carousel "download all".

- **Package (code):** `com.instadrop.app`
- **applicationId:** `com.instadrop.myapp`

## Two ways it resolves media

| Source | How | Needs a server? |
| --- | --- | --- |
| **Instagram** posts / reels / photos / carousels | On-device via Instagram's public web GraphQL | ❌ No |
| **Everything else** (YouTube, TikTok, X, FB, Reddit, IG Stories, …) | Your self-hosted [Cobalt](backend/README.md) backend | ✅ Yes — see [`backend/`](backend) |

Universal, reliable extraction (especially YouTube signatures and logged-in
Stories) isn't feasible on-device, so those go through a small server **you**
run and update. Set its URL once in **Settings → Downloader server**. See
[`backend/README.md`](backend/README.md) — it's a two-line `docker compose up`.

> ⚠️ **Distribution:** Google Play **bans YouTube downloaders**, so a
> YouTube-capable build is for **sideloading / direct APK**, not the Play Store.
> Only download content you have the rights to, and respect each site's Terms.

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

## The resolver seam (now with a real public resolver)

Instagram has **no official download API**, so media extraction is isolated
behind a single interface and run through a fallback chain:

- [`MediaResolver`](app/src/main/java/com/instadrop/app/data/resolver/MediaResolver.kt)
  — `suspend fun resolve(url): InstaMedia`
- [`GraphQlResolver`](app/src/main/java/com/instadrop/app/data/resolver/GraphQlResolver.kt)
  — **the real one**. Calls Instagram's own web GraphQL endpoint (the request
  instagram.com makes to render a post) with the public web `X-IG-App-ID` and a
  `csrftoken` it picks up from a throwaway session. Returns the actual CDN
  `video_url` / `display_url` for public Reels, videos, photos and carousels —
  no login, no private credentials. Verified downloading real public posts.
- [`OpenGraphResolver`](app/src/main/java/com/instadrop/app/data/resolver/OpenGraphResolver.kt)
  — lightweight fallback that reads `og:` meta tags if the GraphQL shape changes.
- [`ResolverChain`](app/src/main/java/com/instadrop/app/data/resolver/ResolverChain.kt)
  — tries each resolver in order; first success wins. A private / Story / login-
  only post resolves to a clear error instead of a fake.

> ⚠️ The GraphQL `doc_id` and headers are undocumented and Instagram rotates them
> without notice, so extraction is best-effort and may need updating over time.

Wire your own backend in at the front of the chain in
[`ServiceLocator.init`](app/src/main/java/com/instadrop/app/data/ServiceLocator.kt).
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

Share-target intent-filter · URL validation · **real public Open Graph resolver
+ stub fallback chain** · preview · streaming download to gallery (scoped storage
on 10+, legacy permission below) · progress UI with speed · **persistent history
(Room)** · **swipe-to-delete + clear-all history** · **paste-from-clipboard** ·
**Settings: light / dark / system theme + Material You** · **branded splash
screen** · download notifications (progress + tap-to-open) · carousel "download
all" · open / share / copy-caption · hardened OkHttp client (browser UA,
timeouts) · **R8 minify + resource shrinking** for release · JVM unit tests for
URL parsing.

> A browser-like `User-Agent`/`Referer` is sent so Instagram's public CDN serves
> us the preview metadata and asset. Private/login-only posts simply fall back to
> the stub — see [`Http`](app/src/main/java/com/instadrop/app/data/Http.kt).

## Possible next steps

Built-in video player · concurrent batch queue ·
favorites / search in history · premium gating (no ads, unlimited, cloud backup).
