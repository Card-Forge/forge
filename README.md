# ⚔️  Forge: The Magic: The Gathering Rules Engine

Join the **Forge community** on [Discord](https://discord.gg/HcPJNyD66a)!

[![Test build](https://github.com/Card-Forge/forge/actions/workflows/test-build.yaml/badge.svg)](https://github.com/Card-Forge/forge/actions/workflows/test-build.yaml)

---

## ✨ Introduction

**Forge** is a dynamic and open-source **Rules Engine** tailored for **Magic: The Gathering** enthusiasts. Developed by a community of passionate programmers, Forge allows players to explore the rich universe of MTG through a flexible, engaging platform. 

**Note:** Forge operates independently and is not affiliated with Wizards of the Coast.

---

## 🌟 Key Features

- **🌐 Cross-Platform Support:** Play on **Windows, Mac, Linux,** and **Android**.
- **🔧 Extensible Architecture:** Built in **Java**, Forge encourages developers to contribute by adding features and cards.
- **🎮 Versatile Gameplay:** Dive into single-player modes or challenge opponents online!

---

## 🛠️ Installation Guide

### 📥 Desktop Installation
1. **Latest Releases:** Download the latest version [here](https://github.com/Card-Forge/forge/releases/latest).
2. **Snapshot Build:** For the latest development version, grab the `forge-gui-desktop` tarball from our [Snapshot Build](https://github.com/Card-Forge/forge/releases/tag/daily-snapshots).
   - **Tip:** Extract to a new folder to prevent version conflicts.
3. **User Data Management:** Previous players’ data is preserved during upgrades.
4. **Java Requirement:** Ensure you have **Java 17 or later** installed.

### 📱 Android Installation
- _(Note: **Android 11** is the minimum requirement with at least **6GB RAM** to run smoothly. You need to enable **"Install unknown apps"** for Forge to initialize and update itself)_
- Download the **APK** from the [Snapshot Build](https://github.com/Card-Forge/forge/releases/tag/daily-snapshots). On the first launch, Forge will automatically download all necessary assets.

---

## 🎮 Modes of Play

Forge offers various exciting gameplay options:

### 🌍 Adventure Mode
Embark on a thrilling single-player journey where you can:
- Explore an overworld map.
- Challenge diverse AI opponents.
- Collect cards and items to boost your abilities.

<img width="1282" height="752" alt="Shandalar World" src="https://github.com/user-attachments/assets/9af31471-d688-442f-9418-9807d8635b72" />

### 🎲 Randomized Adventure Mode
Play the Adventure mode on the Shandalar world but with randomized items & biomes. Unlock each region of the map as you progress.
- All cards outside your starting deck will be locked by default. 
- Biomes, cards and equipment unlocks are randomized and can be unlocked by completing various objectives in the game such as:
  - Collecting cards
  - Completing quests
  - Winning battles
  - Competing at Inn events
  - Defeating bosses and mini-bosses
- Equipment shop contents are fully randomized and (almost) any piece of equipment in game can show up.
- The Leather Boots in your starting equipment are removed regardless of difficulty, all entities' base speeds are increased to compensate.

### 🏝️ Archipelago Adventure Mode
Play the Randomized Adventure Mode using Archipelago to shuffle all items and locations amongst a pool of other Archipelago compatible games.
<br/>Archipelago adds the following locations to the pool:
- Buying items from equipment & item shops
- Defeating a mini-boss
- Defeating a castle boss
- Winning a certain number of battles per region
- Completing a certain number of quests per region
- Finishing a certain number of inn events per region
- Collecting a certain number of cards per rarity

This Archipelago implementation adds the following rewards and filler rewards to the pool:
- Region unlocks in the form of "region runes", one per color of region
- Gold in various amounts
- Mana Shards in various amounts
- Bronze/Silver/Gold Challenge Coins
- Sets of cards including a free matching booster pack
- Max life in various amounts
- Pieces of equipment for the player to wear

#### Step-by-step guide on how to set up Forge Archipelago Adventure Mode:
1. Download the [latest release](https://github.com/BramTeurlings/forge-archipelago/releases) of this repository
2. Download the [latest release](https://github.com/BramTeurlings/forge-APWorld/releases) of the Forge APWorld repository
3. Download the [latest release](https://github.com/ArchipelagoMW/Archipelago/releases) of the Archipelago Client
4. Install Card-Forge using the `forge-installer-<VERSION>-SNAPSHOT.jar` file bundled in this repository's release files
5. Install the Archipelago Client by opening the `Setup.Archipelago.<VERSION>.exe` bundled in Archipelago Client release files
6. Open the Archipelago Launcher and click "Open" on the "Install APWorld" option. 
7. When prompted, select the `forge.apworld` file downloaded from the Forge APWorld repository release files.
8. Once complete, restart the launcher.
9. Click "Open" on the "Options Creator" option.
10. Scroll down the list on the left side until you see "Forge" or "ForgeAP".
11. Etc

### 🔍 Quest Modes
Engage in focused gameplay without the overworld exploration—perfect for quick sessions!

<img width="1282" height="752" alt="Quest Duels" src="https://github.com/user-attachments/assets/b9613b1c-e8c3-4320-8044-6922c519aad4" />

### 🤖 AI Formats
Test your skills against AI in multiple formats:
- **Sealed**
- **Draft**
- **Commander**
- **Cube**

For comprehensive gameplay instructions, visit our [User Guide](https://github.com/Card-Forge/forge/wiki/User-Guide).

<img width="1282" height="752" alt="Sealed" src="https://github.com/user-attachments/assets/ae603dbd-4421-4753-a333-87cb0a28d772" />

---

## 💬 Support & Community

Need help? Join our vibrant Discord community! 
- 📜 Read the **#rules** and explore the **FAQ**.
- ❓ Ask your questions in the **#help** channel for assistance.

---

## 🤝 Contributing to Forge

We love community contributions! Interested in helping? Check out our [Contributing Guidelines](CONTRIBUTING.md) for details on how to get started.

---

## ℹ️ About Forge

Forge aims to deliver an immersive and customizable Magic: The Gathering experience for fans around the world. 

### 📊 Repository Statistics

| Metric         | Count                                                       |
|----------------|-------------------------------------------------------------|
| **⭐ Stars:**   | [![GitHub stars](https://img.shields.io/github/stars/Card-Forge/forge?style=flat-square)](https://github.com/Card-Forge/forge/stargazers) |
| **🍴 Forks:**   | [![GitHub forks](https://img.shields.io/github/forks/Card-Forge/forge?style=flat-square)](https://github.com/Card-Forge/forge/network) |
| **👥 Contributors:** | [![GitHub contributors](https://img.shields.io/github/contributors/Card-Forge/forge?style=flat-square)](https://github.com/Card-Forge/forge/graphs/contributors) |

---

**📄 License:** [GPL-3.0](LICENSE)
<div align="center" style="display: flex; align-items: center; justify-content: center;">
    <div style="margin-left: auto;">
        <a href="#top">
            <img src="https://img.shields.io/badge/Back%20to%20Top-000000?style=for-the-badge&logo=github&logoColor=white" alt="Back to Top">
        </a>
    </div>
</div>
