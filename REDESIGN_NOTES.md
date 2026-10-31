# HerHub v2.1: redesign notes

## What changed
**Splash** (`SplashActivity`, launcher): animated HerHub-pink gradient, pulse rings, glowing logo pop-in, drifting leaves,
tagline "Your space. Your strength." as the title (the logo already carries the wordmark), loading dots. Android 12+ system
splash uses the same colour + white emblem, so there is no white flash. Dark mode switches palette automatically.

**Login / Registration** (`AuthActivity`): one shared design. Logo + tagline, icon fields, remember-me, forgot password,
**Continue with Google** (same button on both screens), **Continue as a guest** (works), switch link. No duplicate "HerHub" title.

**Dashboard**: welcome line is the only emoji in the app. Status card, SOS card (SOS + Call 112), 8 shortcuts with
8 different icons, mood icons, today overview, reminder, explore.

**SOS** (`SosPage`, `Siren`): three emergency lines (112 / 10111 / 10177) with their own icons; hands-free SOS; a real loud siren
(`res/raw/herhub_siren.wav`, alarm stream, volume forced to max, vibration) and the flashlight blinks exactly 10 times when it
starts; redesigned, themed "If you feel unsafe" guide (5 numbered steps, each with a unique icon and one-tap action).

**Icons**: the supplied drawable pack is merged. Every feature has its own icon (see `Features.icon()` and `U.ICONS`);
all emojis were removed from code, resources and notifications; every icon has a content description (label).
Bottom navigation uses `ic_nav_*` in the prototype order: Home, Safety, Health, Map, More.

**Assistant**: 116 topics / ~1,500 keywords in `app/src/main/assets/knowledge.json` (safety, GBV, legal rights, health,
pregnancy, mental health, money scams, digital safety, app help, small talk). Weighted phrase + word matching with light
stemming, crisis topics take priority, related-topic chips, tap-to-call buttons, typing indicator.
Edit `tools/knowledge/*.py` then run `python3 tools/knowledge/build_knowledge.py`.

## One-time Firebase setup for Google sign-in
1. Firebase Console > Authentication > Sign-in method: enable **Google** (and Email/Password).
2. Project settings > your Android app: add your **SHA-1 and SHA-256** (`./gradlew signingReport`).
3. Download a fresh `google-services.json` into `app/`. It then contains the web client, and the Google Services plugin
   generates `default_web_client_id`, which the app reads. Rebuild.

## Notes
* Helpline numbers should be re-verified before release.
* Add `READ`-free torch use needs no permission. The siren/flash also work from the hands-free service.
* Open in Android Studio (Ladybug+), let Gradle sync, run. I could not run a Gradle build in my environment, so the
  first build may show small compile errors to fix.
