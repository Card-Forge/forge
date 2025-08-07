### Disclaimer:

While multiplayer over a network *does* currently work, and complete games *have* been played, it is still very much a "*work in progress*". This means;

* You **will** find bugs. When they appear;
   * They will likely be mid-game.
   * They will likely make it impossible to complete the current game/match. 
   * You **will** need to restart both the client's and host's games.

**You've been warned!** But please report bugs and issues, this will help make network play more stable.

***

### Support:
**We have a discord channel dedicated for network play related matters: https://discord.gg/nsAhGwD**

I've created a Networking FAQ, check the sidebar for it. I'll update it as I get **Q's** in discord that are **FA**.


Note: This guide has been written to be as comprehensive as possible; from a quick start, to troubleshooting, to network configuration and concepts. It's not possible to identify all network configurations or issues, but the typical and most basic are represented in this guide. I personally want network play to be easy to use, set up, and for it to be stable. The more people playing over network the more its play tested, and more bugs can be found.

***

### Requirements:

* **At least two devices**, all devices must be running the same version of Forge.
   * Host: The device running forge as the "server."
   * Client: The device connecting to the Forge server.
* **A Network**; 
   * Local: Wi-Fi in the same home, "Wi-Fi Direct" between two devices, or Ethernet for PC
   * Remote: IPv4 Internet (Home Wi-Fi Router to the internet, Ethernet to Router to internet)
* **Firewall Exclusions and Port Forwards**;
   * NOTE: Don't DISABLE your firewall, use exclusions to allow forge app or the port specifically.
   * By default Forge (when running as a server) will listen on port **36743**. 
      * For clients to be able to connect to the server, this port will need to be allowed through the server host's local firewall (Windows Defender Firewall, Ubuntu/Linux UFW, etc.).  
      * Additionally, if the client is "remote" or outside your local network, the above port will need to be forwarded through the *host's router's firewall* (see below) **and** through the *host's local firewall*.

***

### Quick Start:

* Start Forge on both devices; please confirm versions numbers are the same before continuing.
   * Mobile players: Choose "Classic Mode"
* Go to 
   * Mobile: "Play Online"
   * Desktop: "Online Multiplayer" > "Lobby" > Click "Connect to Server"
* Decide who will "host" the game.
   * That person must **not** put anything in the "Connect to Server" popup box, and click OK.
   * Forge will attempt to show the IP Address of the Host's machine verify before providing to the other player: 
      * **For local play,** the IP address would probably start with 192.168... verify with your device's network settings. ```Forge may recommend to use the word "localhost", ignore this. I suspect this is a java issue with naming the network address.```
      * **For remote play,** the IP address should be verified with https://canyouseeme.org/
* The person **not** hosting is a "client" and MUST to put the IP Address determined from the above step into the "Connect to Server" popup.
* You can then start a network match;
   * The host can select the type of match, team configuration, number of matches per game, and other settings.
   * All Players can select their decks, their sleeves (if mobile), and avatar.
   * Toggle the ready switch to signal you're ready.
   * When all players are ready to start, the host can start the match.

***

### Troubleshooting:
Folks in discord are there to help you get your game going!
#### Current Network-based Multiplayer Known Issues

* Some points where the game is waiting on an opponent's decision/action do not properly indicate this. #158
* On mobile, if you are in the same room without Wi-Fi access, try "personal hotspot"/"WiFi-Direct" options first, you don't need a port forward that way.
  * If you are on the same Private Wi-Fi, you do not need to do this.
  * If you are on a Public Wi-Fi or Hotspot, you will probably not be able to communicate, using a "personal hotspot" would be a better option.
* Due to the lack of traffic optimizations, playing over network feels choppy/laggy. A single game can transfer hundreds of megabytes between all clients! _Slow networks will be slow._
* If your ISP uses IPv6 but uses something called Dual Stack lite, network play won't work. Network play **requires IPv4 addressing** or IPv6 using (full) Dual Stack! 

#### "Disconnected From Lobby"
* A common cause for this is that the client and server resource (**res**) folder content differs.  This can be verified by checking the game log and looking for an IOException referring to a "Card ... not found". #175

***

## Network Configuration - Setup and Testing  
_Please consider reading "Hosting a Server" section for the concepts behind the setup and testing, and terminology._

### Local Network:

#### **Host Based Firewall**

* Mobile Device: There shouldn't be much you need to do, start Forge, host a server. Anyone else on the same local network should be able to connect.
* Desktop Device: Depending on your OS, you may need to allow Forge through the host based firewall. Windows Defender Firewall, Ubuntu would have UFW. 
   * Either allow the app itself.
   * or the default port of 36743. 

#### **Validate**

* Android Device: You can test from your mobile device with PortDroid, to scan if the port is open:
   * Setup
      *  Install [**PortDroid**](https://play.google.com/store/apps/details?id=com.stealthcopter.portdroid)
      *  Open **PortDroid**
      *  From the menu in the upper left, select *Port Scanner*
      *  Select the three dot menu in the upper right.
      *  Select *Port Scanner Settings*
      *  At the bottom under *Port Lists*, select *Add New Scan*
      *  Enter *Forge* for the *Scan Title*
      *  Enter *36743* for the ports to scan.
      *  Select *ADD SCAN*
   * Test
      *  From the menu in the upper left, select *Port Scanner*
      *  In the first field on the left, enter the internal IP of the host system.
      *  From the middle drop down, select *Forge*
      *  Select *SCAN*
   * If all goes well, you'll receive an indication that 1 port is open.

* Windows Device: You can run telnet to test if the port is open.
   * Setup
      * Install Telnet: As telnet is no longer available in Windows by default, [please follow the instructions found here](https://social.technet.microsoft.com/wiki/contents/articles/38433.windows-10-enabling-telnet-client.aspx) before continuing.
   * Test
      * Open a **command prompt**
      * Type: *telnet INTERNALIP 36743* (where INTERNALIP is your forge host's internal IP address)
      * Press *Enter*
   * If all goes well, your command prompt title bar will change to *Telnet* followed by the host system's IP address. At this point you can close the window.
   * If it doesn't you'll see something like the following: ```Could not open connection to the host, on port 36743: Connect failed```

* Mac OSX Device: 
   * I'll quote Apple "Telnet is insecure and you should move to more secure communications methods, like HTTPS." 
   * You can't be trusted to use this on your own. 
   * Maybe check the app store for a "port scanner" follow their instructions for scanning for port "36743" on "localhost".

* Linux Device: You probably don't need our help. Install telnet from your package distro and run telnet similar to Windows.

If you've passed the validation, then your configuration should be good and you can provide your partner the IP address of your device. Please confirm your IP address with your devices' network configuration settings.

### Remote Network:

A Remote Network setup **MUST** pass the local network setup above.

#### **Port Forwarding or "NAT"**

_Each router is different, so instead of specific step by step instructions here, this is general theory._

* Setup
   * You'll need to find the admin interface for your router.  On most consumer models, this is a web page that can be accessed at `http://_._._.1` where the first three parts of the address are the same as your system's internal IP address (as reported by Forge).  For example, if your internal IP address is 192.168.0.54, the admin page would likely be at: `http://192.168.0.1/`.
   * Once inside the interface, you'll need to look for "port forwarding".  This is sometimes buried under different menu options, like "advanced".  Once located, you'll want to add a new port forward.  The interface will likely ask for several bits of information:
      * remote or external IP 
      * external port
      * internal host or IP
      * internal port
   * Both port values should be set to **36743**.
   * The remote or external IP should be left **blank** or set to **any**.
   * The internal host or IP would be the machine running Forge.

Help for your specific router may be here: https://portforward.com/router.htm 
*Don't download software you don't know what it does, that website's software is NOT recommended to be used, just use the guides they provide.*

* Testing
   * For the external testing use a site like: [CanYouSeeMe.org](http://canyouseeme.org)
   * Your external IP should already be detected by the site.  
   * Simply enter **36743** for the port to check and click *Check Port*.
   * If all goes well, you'll get a *Success* message back.

If you've passed both of the local and remote testing, then your configuration should be good and you can provide your partner the IP address of your network. Please confirm the IP address with [CanYouSeeMe.org](http://canyouseeme.org).

***

## Network Configuration - Hosting a Server

   Welcome to hosting a server, this is a more descriptive view as to what's going on when you host a server. These concepts can be applied to most server set ups, but we will focus on Forge in three different configurations:

* **Local Networks (Over your local network)**
   * Local Private Wi-Fi, "Wi-Fi Direct", or "Hotspot" from a mobile device, also wired networks for PC players.
* **Remote Networks (Over the Internet)**
   * Networks you don't control. (Cell to Cell, or Public Hotspots.)
   * Network you do control. (Home internet.)

#### Local Private Network:

A local network's IP addresses are typically self managed by your router, with what is called the DHCP server. These addresses are assigned to your devices automatically and are what you would use to connect to each other device. These devices will talk to each other over the Wi-Fi Access Point (which is typically your router.) In a wired environment they will talk across the wire through a switch (also typically your router.)

When you do a "Wi-Fi direct" in Android, this allows one device to be the Wi-Fi access point, other devices can connect to this network as a local private network.

#### Remote Network:

Remote networks are two networks separated by the internet, or by what is called VLANs. On public hotspots, two devices on that "same Wi-Fi" will be separated by these VLANs, and can not talk to each other directly. Similarly, two cell phones on the same provider will be separated by VLANs, or just by given an IPv6 address.

If you do not control your internet connection, like a public hotspot or a cell provided internet you'll need to do a software defined network (SDN), or a virtual private network (VPN) which is outside the scope of this wiki and any discord help. It is recommended to connect via "Wi-Fi Direct" if you are near each other instead.

If you control your internet connection's network, such as it's your home network and can access the router configuration, you can host a game by opening the ports on your network, and allowing the game through the router's firewall. This allows the remote player, on practically any other internet connected network to connect through your router to your server.

### Firewalls:

Firewalls are kind of archaic, but are still implemented, and are designed to block your connection intentionally. 

A quick history; Computer OS's used to respond with a port being "closed" if no service was available on it, and "open" if a connection was able to be opened. An OS should not report a port as "closed", just ignore the request. (Reporting a port as "closed" is like hearing a knock on the door, opening the door and slamming it shut.) This wasn't a security concern at the time, so attackers were able to identify if a computer was "alive" if it responded with a "closed" port, and would moved on if it didn't get a response at all. Any response from a computer triggered a deeper attack to find which ports were actually "open." Thus firewalls were born...

Firewalls were initially implemented to prevent open ports from talking to just any random device, limiting access to only approved connection requests and limiting attackers from gaining access, however ports were still reporting as "closed" in the early days. Firewalls then implemented a function to prevent ports without services on them from reporting "closed" at all and just ignored the traffic instead, making it appear as there was no computer at that address. 

In devices with firewalls you need to allow a port for a service through the Host Based Firewall. You'll also need to do this on your router for remote games as well for the Network Based Firewall. I believe, most Linux devices (including Android) no longer report "closed" for ports without services, and firewalls are typically not needed as the only ports that should be open are ones with services needing ports open. In Windows this is not the case, and is why Windows Defender Firewall is implemented and ports will need to be opened to allow Forge to accept a connection. You should however be able to connect to a mobile device without concerns of a firewall blocking you.

### Port Forwarding:

In routers, just after the firewall is a routing function called Network Address Translation. "NAT" allows a computer behind the router (from the internet perspective) to provide its service as if it is the router itself. In most routers, when you enable a NAT/Port Forward, it will also allow the Port through the Firewall.

```[Device](Internet) ---> 36743{Router} ---> 36743[Host]```

Once this initial communication is started, the two devices will talk over their designated and determined ports and have a TCP "conversation" (called a stream) until the connection is completed or closed. Once the TCP stream is closed, another device can connect.

If you've used PlayStation systems you might recognize the term "NAT"; level 1, 2, or 3. This naming is not part of the standard, and is bad and confusing; (I believe) "Level 1" meant no NAT'ing all traffic was redirected through PlayStation servers, "Level 2" meant Half Open or Static NAT (which is what we will do for Forge), and "Level 3" mean the use of UPnP/DLNA which was automatic NAT services and allows ALL software to control your firewall for you. (UPnP should be concerning from a security perspective.)

### Network Topology:

Here's how your typical network topology and network flow will look, when connecting and playing with Forge.

* Typical Wi-Fi Home Network with 2 devices
   * A Figure of Local Game Connection through Wi-Fi with PC Host.
![image](https://user-images.githubusercontent.com/1243145/186308954-930e2db5-e428-401f-8117-d223141a5037.png)
   * Mobile/Android would be similar, but without the firewall on the host. (Personally, I believe this may change because of how stupid android "security" is becoming, and you'll have a firewall you can't tune, or make exceptions.)

* Typical Remote Network Connection with 2 devices
   * A Figure of Remote Game Connection through Firewall and Port Forward on Router.
![image](https://user-images.githubusercontent.com/1243145/186335351-c2c670bd-599e-497a-a210-ab866d7ebd95.png)

