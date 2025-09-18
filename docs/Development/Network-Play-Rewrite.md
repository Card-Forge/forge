# Why Rewrite Network Play?

The current implementation of **Network Play** relies on [Java serialization/deserialization](https://www.geeksforgeeks.org/serialization-in-java/) via [Netty](https://netty.io/).  While this does work, it is inefficient, transferring large amounts of unnecessary (duplicate) data.  The transferring of duplicate data has two negatives:

1. increased latency
2. increased bandwidth

The increased latency is very noticeable throughout a network game.
The increased bandwidth is a potential concern for mobile players, not everyone has an unlimited data plan.

Testing of the existing **Network Play** implementation has shown an individual **Game** transferring over 300MB of data.

# The Rewrite

The rewrite will utilize [protobuf](https://developers.google.com/protocol-buffers) and be approached in phases:

1. Lobby
2. Match
3. Game

## Lobby

The **Lobby** portion will handle:

* Handshake
* Player
  * Name
  * Avatar
* Game Rules Selection
* Deck Submission

The **Handshake** portion of the **Lobby** will be responsible for ensuring that it is a **Forge** client that is connecting **and** that the client is running a compatible **Network Play** implementation.

## Match

Number of **Games** that comprise a **Match**, normally first player to win 2 **Games**.  This is important, because it is not *technically* best of 3.  For example if either of the first two games of a **Match** are a *draw*, it is entirely possible to play a fourth **Game** in a **Match**. (Need a judge ruling reference on this)

## Game

This will be broken down in more detail, but the important bits to hand first are:

* Phases
* Passing of **priority**
* Notification upon receipt of **priority**