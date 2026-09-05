# Discord Patches

Morphe patches for Discord (Stable builds).

### How to use these patches

Click here to add these patches to Morphe: https://morphe.software/add-source?github=jrddupont/discord-patches

## 🩹 Patches list

<!-- PATCHES_START EXPANDED -->
> **[v1.0.2](https://github.com/jrddupont/discord-patches/releases/tag/v1.0.2)**&nbsp;&nbsp;•&nbsp;&nbsp;`main`&nbsp;&nbsp;•&nbsp;&nbsp;1 patches total
<details open>
<summary>📦 Discord&nbsp;&nbsp;•&nbsp;&nbsp;1 patch</summary>
<br>

**🎯 Supported versions:**

| 343.12 - Stable | 342.16 - Stable |
| :---: | :---: |

| 💊&nbsp;Patch | 📜&nbsp;Description | ⚙️&nbsp;Options |
|----------|----------------|-----------|
| [Hide quest promo banner](#hide-quest-promo-banner) | Hides the quest promo banner at the top of the server channel list. |  |

</details>

<!-- PATCHES_END -->

### 🛠️ Building locally

- Run `./gradlew buildAndroid` (in this environment use the local Gradle
  directly — see HANDOFF notes in the parent workspace — not `./gradlew`,
  since the wrapper cannot download its distribution here)
- The built patches .mpp file is found in `patches/build/libs/patches-*.mpp`
- Apply the mpp file using [Morphe-Desktop](https://github.com/MorpheApp/morphe-desktop)
  like any other patch bundle.

See the [Morphe documentation](https://github.com/MorpheApp/morphe-documentation) for more information.

## 📜 License

Discord Patches are licensed under the [GNU General Public License v3.0](LICENSE)
