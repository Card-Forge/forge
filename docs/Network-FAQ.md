# Frequently Asked Questions About Networking Games

### Can I play you?

* No. Thanks for the offer, but right now I don't have time. And the truth is I'm just not that good at magic really, the AI is a better opponent than me.

### Can Mac Players play against Windows?

* Yes! Forge is completely cross platform, as it's built in java the same software is running on every system, this allows cross platform compatibility; Android to Windows to MacOS/OSX... potentially anything that can run Java. 

Note: Forge is not built for iOS, as iOS doesn't support or run java.

### Can I play random players?

* No. Network play is designed, currently, to play against someone you know. If you want to play against random people, you can make a request in the discord. 

### Why can't my friend connect?

* There's three major reasons

1. You're not on the same network and you're trying to use the local IP address. Please read [this part of the guide](network-play#remote-network).
1. You're on the same network and your local firewall isn't configured correctly. Please read [this part of the guide](network-play#host-based-firewall).
1. You're not on the same network and your router ports aren't forwarded. Please read [this part of the guide](network-play#port-forwarding-or-nat).
    * IF you're not on the same network and you can't access port forwards, you'll need this [Network Extra](Networking-Extras).

### Is it possible to play with people not on the same Wi-Fi?

* Yes. There's two primary ways to handle this:

1. On a home internet: Open and forward ports on your home router to your computer hosting the game. 
    * Additional information is here: [Remote Networking](network-play#remote-network)
2. On public Wi-Fi hotspots or cell networks: Use a VPN to create a private network. 
    * Additional information is here: [Network Extra](Networking-Extras)
    * This may be more specific to getting you going: [VPN Providers](Networking-Extras#virtual-private-network-service-providers)