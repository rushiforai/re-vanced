If you wanna help me

<a href="https://www.buymeacoffee.com/daboynb" target="_blank"><img src="https://cdn.buymeacoffee.com/buttons/default-orange.png" alt="Buy Me A Coffee" height="41" width="174"></a>

# Instagram View-Once (ReVanced)

Custom ReVanced patch bundle that **saves Instagram view-once photos and videos**
to `Pictures/InstagramViewOnce` before they expire.

The build produces a standalone `.rvp` that you load into **ReVanced Manager**
alongside the official bundle. Only the *Save view-once media* patch is exposed.

## What you need

- **Docker** installed and running ([Get Docker](https://docs.docker.com/get-docker/))
- **A GitHub account** (free) — required to download the ReVanced Gradle plugin
  from GitHub Packages

## Setup

### 1. Get a GitHub token

1. Go to https://github.com/settings/tokens
2. Click **Generate new token (classic)**
3. Check only **read:packages**
4. Copy the token

### 2. Configure

```bash
cp .env.example .env
```

Open `.env` and put your GitHub username and the token:

```
GITHUB_PACKAGES_USERNAME=your_github_username
GITHUB_PACKAGES_PASSWORD=ghp_xxxxxxxxxxxxxxxxxxxx
```

## Build

```bash
docker compose run --rm slim
```

The first run takes a while (Android SDK + ReVanced Patches clone + Gradle
dependency download). Later runs are much faster thanks to the persistent
Gradle cache.

Output: `output/viewonce-standalone.rvp` (~2.4 MB).

## Install

1. Copy `output/viewonce-standalone.rvp` to your phone
2. Open **ReVanced Manager** → *Sources* → **+ Add bundle**
3. Pick the file — it appears as **daboynb / Instagram View-Once**
4. Patch Instagram as usual, selecting the **Save view-once media** patch

## Compatibility

Tested only on Instagram **416.0.0.47.66**. Other versions may work (the patch
uses runtime fingerprints, not hardcoded offsets) but are not verified.
