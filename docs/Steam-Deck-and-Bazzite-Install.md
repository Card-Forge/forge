# Steam Deck and Bazzite Support

_This instruction was written using Bazzite, however should be similar enough for SteamOS._

In order to support steamdeck "natively" for full desktop mode Forge would likely need to have a flatpack installer for best user isntall experience, currently Forge has no intent to have a flatpack. The current **best** and recommended way to have Forge on your Steamdeck is to install and run the Android APK version in Waydroid.

* You will need to have installed Waydroid first, this reddit post may work for you; https://www.reddit.com/r/SteamDeck/comments/1ay7ev8/how_to_install_waydroid_android_on_your_steam_deck/

## Install Forge Android in Waydroid (Recommended Method)
Once you've installed Waydroid, you can follow the same steps you would in any Android device.

1. Open browser to https://github.com/Card-Forge/forge

1. Open Snapshots from the Link in the ReadMe

1. Download the Snapshot APK

1. Open the APK from the download notification.

1. Allow the browser to run APK installs.

1. Once installed, Forge should request permissions.

1. Forge sends you to the Settings to provide permissions.

1. Run Forge from the Application Drawer (likely Forge is still running)

1. You can tap anywhere on the screen and Forge will restart.

1. Forge will restart and ask to update the Assets.

1. Congratulations you can play Forge on your SteamDeck.

### Running Forge Android in Steam GameMode

My understanding is that Waydroid calling specific Android apps doesn't work at the moment, so the best way to run Forge is to start Waydroid itself from Steam Gamemode and then start Forge. To do this you will need to add Waydroid to Steam as a non-Steam Game. Controller support should be natively supported this way.

## Forge Desktop (JAR or BZ2 File)

### Installing Forge Desktop Natively (Not Recommended)

Barring a flatpack (and packing Java with forge), the correct way to install Forge Desktop Natively (installer JAR or BZ2 archive) would be to install Java OpenJDK in the OS Globally. **This is against Steam and Bazzite Dev recommendations**, however is doable; 

* For Bazzite: simply use `rpm-ostree install java-21-openjdk`

> You can then install forge as you would in any Linux OS, following the [user guide](user-guide).
> Again, this is against Bazzite devs recommendations, and they will likely belittle you and tell you you can't ask for support, if you tell them you did this like a normal Linux user would.

* For SteamOS: I believe you need to unlock the OS, install java from the package repo, then lock the OS again. 

> This Wiki will not provide instructions for this, if you feel you can do this you can probably look up guides to help you.

### Installing Forge in a Container/VM (Boxes or Other)

Another option is to install Forge in a Container or VM, where you can download and install Java SDK into the Container, and download Forge into the container and run Forge. This is not recommended as it can be confusing to a novice Linux user, adds system overhead, likely UI problems, and would be difficult to perform on a handheld device.

> This Wiki will not provide instructions for this, if you feel you can do this you can probably look up guides to help you.


