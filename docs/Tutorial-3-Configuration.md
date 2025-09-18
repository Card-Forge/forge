## Tutorial 3 Configuration, (Configuring your Plane)

Alright, our third tutorial will be a simpler one again. Customizing your plane. A common desire when making a new plane, is to change things such as which sets are available to play, or which cards an be bought or gained as rewards. Maybe you want to be able to more easily build super powerful decks from the get go, or just have entirely new decks. All these settings, and more, can be adjusted from the 'config.json' file in your plane's directory. For this tutorial, to get a basic idea of what things can be changed, we are going to make some minor adjustments to the plane. Further details on other changes and what they do will be covered in other sections and tutorials.

For now, in IntelliJ, open up the 'config.json' file found in `...\YourIntelliJProjectFolder\forge-gui\res\adventure\YourPlane`, as a reminder for our tutorials, I am using a directory called Test. Inside the config file, there are a large number of entries, but we are going to start with the `"restrictedCards"` array. After-all, we are here to have fun, so let's just bring in almost all the craziness. We are going to delete every entry in the array, _except_ for "Black Lotus"... Why not Black Lotus you ask? Because it's too rare, clearly. (Or, maybe just because it is the first entry and I'm being lazy, and keeping one entry is a reminder how this array is formatted.) So, when you are done, the array should look like this.
```
"restrictedCards": [
    "Black Lotus"
  ],
```

To better explain what that array does, is any card in the array will not appear in any generic rewards or shops. This is the closest thing to banning individual cards that one can. However, it should be noted that if those cards are ever specifically added as a reward or a shop's content. Doing so overrides this setting. Essentially allowing you to award cards for very special cases, or hide a fancy shop somewhere in your game.

Now, restricting one card at a time is a very laborious process, especially if there are a large number of cards you want removed. In many cases, you will want entire sets removed from your game, which brings us to the `"restrictedEditions"` array. Any card whose set code appears in this array will be restricted from the game in the exact same way as `"restrictedCards"`. There's already several entries in here, but since I'm making a really wacky unbalanced plane here, we're going to remove every entry from this array, except for "UNF". Why not UNF you ask? Because it's a traitor and made some of it's cards legal and others not, clearly. (Or, maybe again it's because I'm lazy, and at the time of this writing, it's the last entry in the list. As well as once again keeping one entry as a reminder how this array is formatted.) So if you follow my flawless example, it will look like this.

```
"restrictedEditions": [
    "UNF"
  ],
```

Now that we've opened the floodgates for our awards, we could go have fun in the wacky world. However, our Inn's drafts and jumpstarts won't be as open as the rest of the game still. Why? Because the array `"restrictedEvents"` tells our Inn what "events" (aka sealed and draft editions) it can't spawn. Just like `"restrictedEditions"` any set in this array won't appear for draft events (but if it is not in the `"restrictedEditions"` it can still appear as rewards.) As such, let's go ahead and open those flood gates further. In my infinite wisdom, I have decided, for no specific reason at all, that we will remove every set from this list except for "CMM". (Yes, you got the picture, I'm lazy but trying to be helpful still.) If you followed me so far, the entries between `"colorIDNames"` and `"difficulties"` should appear as follows.

```
"restrictedCards": [
    "Black Lotus"
  ],
  "restrictedEditions": [
    "UNF"
  ],
  "restrictedEvents": [
    "CMM"
  ],
```

Now if you load up the game at this timeframe, and go hopping through towns, you'll notice the occasional card you hadn't seen before. This is good, but man there are a lot of cards in Magic, and it can take a while to find ones to show you've made a change. For a tutorial, that's bad. Also, if you want to make a plane like "Shandalar Old Border" or "Innistrad", you'd have to go through and add every unwanted set to the `"restrictedEditions"` array. including updating this array every time Wizards of the Coast releases another set, and that's **worse** than bad... Luckily, we have a solution for this. We are going to add two new arrays to our 'config.json' file. Where the previous arrays act as a black-list for the entries, the following arrays are a overwrite. If they are present, the plane will only spawn cards and events found in them. The first one is `"allowedEditions"`. For this tutorial, I am going to go completely unhinged and risk making some of our games unstable, but what mod never comes unglued? If you didn't pick up the hints, my array is going to add `"UNG"`, `"UNH"`, and `"UST"`. So my array looks as follows.

```
 "allowedEditions": [
    "UNG",
    "UNH",
    "UST"
  ],
```

The second array I'm going to add is the `"allowedJumpstart"` array. This changes which Jumpstart sets are available, and will allow us to still have them even when the previous arrays have blacklisted them in any way. For this tutorial, I know we all want to get a jump on things, so I'm just going to add `"Jumpstart"`.

```
"allowedJumpstart": [
    "Jumpstart"
  ],
```

You might have noticed that unlike the previous arrays, this one uses the set name, not code. This is a current limitation in Forge, and one we will have to live with. Luckily, there are far fewer jumpstart sets than than other sets. (**NOTE: Adding a set to this list that does not have any official Jumpstart packs will NOT enable them for jumpstart events.** Some of you who have tooled around will have noticed there are Jumpstarts in "Innistrad" that don't exist in normal jumpstarts. Well, that is for a much later tutorial. My apologies.)

As a final note, while this changes the rewards from enemies and shops, as well as what can appear in the Inns. It has no bearing on your starting decks, or enemy decks, just the rewards and shop options. So yes, if you followed along so far, you will playing with 'normal' cards, against normal cards, to earn Silver-Bordered cards.. yeah, it's a bit crazy, but that works... For those who want to customize their plane even further, the next tutorial will focus on modifying both starting decks, and enemy decks. See you there. For now, enjoy, and again. if you have any problems, please reach out me "Shenshinoman" on the discord and I will be happy to help.