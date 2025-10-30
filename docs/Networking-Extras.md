# Disclaimer

This page should be considered "experimental" and **at your own risk**.

Forge, it's developers, and supporters won't be able to help you with this. I'll try to provide some details about setting up the software and networks listed here, but please direct any support to the platform or provider's support and documentation. This should mostly be just a list of options of how to set up a remote game without access to port forwards on your network.

_Please don't use this guide to get around security of your work/school network on a work/school computer, only use it for accessible networks like hotspots or the like, with your personal devices._

_Attempting to subvert security measures may be considered a crime, in the least you'll get fired or expelled, I like you I don't want that on you._

# Networks

So you found out you don't have control over your network to open ports for a remote game connection, what can you do? **Well, hopefully this guide will get you on the right track.**

Some scenarios where this may be; 
+ you're on a cell data connection
+ you're on a public Wi-Fi Hotspot
+ you're at school or work
+ dad is kind of a tightwad and doesn't want games to have open ports on the router. 
  + sorry, my dad was actually cool about this, and let me manage the network router.

These connections don't allow you to forward ports to your device's IP address, and that's a problem with Forge for remote connections. You'll need to be able to communicate with a server or service outside of the network first to set up a line of communication, which then allows you to make direct connections to another device. The most common concept is a Virtual Private Network or VPN, allowing a virtual network to be instantiated, and connected to. Additionally newer concepts such as Software Defined Networks can provide similar functions, without the layers of security/encryption a VPN would.

## Virtual Private Network Software

VPNs were not designed to subvert security, but to provide a secure communication path on less secure networks. Allowing enterprise or work computers the ability to remotely connect to the work network.

Today, most VPN service providers provide a secure channel to the internet for "completely legitimate" reasons. However, going back to the original intent you can stand up a virtual private network that you and friends can access using VPN Software.

**Don't confuse a VPN Provider (PIA, Nord, Etc.) as a VPN Software or Service you run (OpenVPN).**

### Software:
If you know of other VPN software (NOT SERVICE PROVIDERS), please expand this list.

+ OpenVPN
  - VPN Software you can run on a router or a server.
+ IPSec Tunnel (?)
  - A part of VPN that you typically run on two routers?


### Notes:

VPN Software will still require control of **a network**, a place to install the VPN, and have remote access to it. So this is where complexity is introduced, you'll either need to set up the VPN on a home network you can control, or on a server that you can rent from a provider with a public IP address you can access. Once this is done, both players can install the VPN software on their devices, and connect to that server. 

Once you've connected to a VPN Server, you can return to the Network Play guide and test your connections as a "Local Private Network". 

## Virtual Private Network Service Providers

Some VPN Service Providers provide an actual VPN service, and not just a pipe for "completely legitimate internet access" purposes. Since these are rarer please only put these types of VPN providers here, **everyone can google** the others.

### Providers:

+ [RadminVPN](radmin-vpn.com)
  - A free VPN provider for actual Private Networking Access.
+ [Hamachi by LogMeIn](https://www.vpn.net/)
  - Free for up to 5 computers.

### Note: 
Some "completely legitimate internet access" VPN Service providers may allow you to have port forwards through their networks. Check with the provider before signing up, however both players might need the same provider but might not. You can search google for those VPN providers, **we don't need to list them here.**


## Software Defined Networks

A software defined network is similar to a VPN, but without the encryption and stuff that can slow and prevent a connection out. A Software Defined Network by a provider means you don't need to have your own server, they provide the remote access. Therefore you only need to configure the basic network settings like the IP Schema, and then allow access. 

Once you've configured the "virtual switch", users can connect with the provider's software, you can accept their access, then assign an IP address, and connect to each other.

### Providers:
Please expand this list if you know of others!

+ [ZeroTier One](https://www.zerotier.com/download/) - It's free for personal use.

### Notes:

Configuring a network can get complex, however a simple network should already be configured by the provider. You should be able to follow the instructions from the provider to stand up your first network, provide your friend the link to the software, the access code to the network, and accept their access request. 

Once you've done this, you can return to the Network Play guide and test your connections as a "Local Private Network".

## Security

As a general note, if you use a provider for network traffic including a VPN provider; They have access to **everything** you do on their network! They may not see the data if you use HTTPS for encryption, but they can see where you go, and can collect that meta data. 

Secondly, being on a private network (VPN or SDN) with anyone else does put your device at risk, especially if your device has security vulnerabilities that aren't patched. It is recommended to only provide access to people you TRUST on these private networks you stand up, and disconnect when you're not using them. **Don't create a network and provide access to hundreds of people and stay connected, you're ONLY asking for trouble.**