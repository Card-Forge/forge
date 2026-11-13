package forge.gamemodes.rogue;

import com.google.common.collect.ImmutableList;
import java.util.List;

/**
 * Defines tutorials shown to new players in Rogue Commander mode. Each tutorial is shown once and
 * then marked as seen in RogueMetaProgress.
 */
public enum RogueTutorial {

  MAP_NAVIGATION(
      "Map Navigation",
      "Rogue Commander is a roguelite deckbuilding format. Start with an open-ended start deck, then " +
          "embark on a Run, battling against the Planebounds to earn new cards, Gold and other rewards as " +
          "you go.",
      "Win matches to progress until the final Boss, but I have to warn you: lost life is not restored " +
          "after each battle!",
      "Before you lies the Trail I found for you. A network of so-called 'Omenpaths', letting you travel " +
          "between Planes. Yes, even without a Spark. Thank you, Phyrexian Invasion.\n" +
          "You can use the mouse wheel to zoom in on the Plane cards.\n" +
          "Each Run will be a different Trail, with different opponents, encounters and Planes.",
      "Choose your route through the Trail carefully; some might be harder than others. Each Trail is " +
          "divided into rows, alternating between ones where you will battle an opponent on a Plane-node, " +
          "and ones that offer healing, bazaars, and other encounters via Side-nodes.",
      "Whenever you complete a node, you unlock the next row of your path. If there is more than one " +
          "accessible node in that row, you can select your next destination.\n" +
          "Oh, and before I forget: The more Plane rows you complete, the more life your opponents will start " +
          "with."
  ),

  PRE_BATTLE(
      "Planebound Battles",
      "Battles on Plane-Nodes are played as a 1v1 match of 'Commander' (you against the Planebound) with " +
          "added 'Planechase' rules. Other than in a normal Planechase Match, you will stay on the current " +
          "Plane throughout the battle. 'Chaos' and 'When you Planeswalk...' effects will be resolved as " +
          "normal, but no effect will cause a plane change.",
      "I'm sure you won't disappoint me, Commander."
  ),

  MATCH_UI(
      "Your First Match",
      "Surprise, I'm still here. Don't worry, I'll leave you alone soon.\n" +
          "Take a moment to look around. In the default layout, your hand and battlefield are below your " +
          "opponent's area. Keep an eye on both life totals as the battle progresses. Double-click a card " +
          "to cast it when the rules allow and you can pay its costs.",
      "Use the zone icons in your player panel to open your Command Zone, Graveyard, or Exile. " +
          "Your Commander (which is you, sure, but that would make it sound slightly awkward) can be cast " +
          "from the Command Zone. " +
          "You can also use the Planar Die card there to roll the die when it's available.",
      "Use the icons by your opponent's avatar to inspect their zones. During their turn, " +
          "you'll find the Plane in their Command Zone.",
      "Follow the action prompts to make choices, select targets, and see when the game is waiting for you.",
      "The Stack tab shows spells and abilities about to resolve. Right-click a Stack entry " +
          "and choose 'Resolve entire stack' to pass automatically until the Stack clears. The Log tab " +
          "lets you review what has happened. In case it ever interests you, that is.",
      "The Card Detail tab shows the rules text of the card you're inspecting. Use the mouse wheel " +
          "over any card picture to zoom in.\n" +
          "I don't want to play the old wise professor here, but it is always good to read and understand " +
          "cards before casting them.\n" +
          "To restore the default layout, choose 'Layout > Reset to Default Match Layout'."
  ),

  MATCH_CARD_HIGHLIGHTING(
      "Card Highlighting",
      "Noticed the colored outlines of cards? When the game asks you to choose a card, look for a white outline around cards you can select. " +
      "A magenta outline can mark a card you've already chosen.",
      "If you'd like help spotting cards you can play or abilities you can activate, turn on " +
          "'Display > Highlight Actionable Cards'. It's off by default."
  ),

  MATCH_PHASES_AND_YIELDS(
      "Phase Stops and Yield Settings",
      "Before I finally leave you alone, let us sort out the game's yield controls. Sounds fun, doesn't " +
          "it?\n" +
          "Click the phase buttons to enable or disable the default priority stops during a turn. You are " +
          "choosing when the game pauses for your input, not removing phases from the game.\n" +
          "If you ask me, then leave them as they are for now.",
      "The 'End Turn' button passes automatically through the rest of the current " +
          "turn, subject to yield interruptions. It does not end the turn immediately. When your last action " +
          "can be undone, that button shows 'Undo' instead.",
      "Auto-Pass is off by default. To pass automatically when you have no available actions, check " +
          "'Game > Enable auto-pass'. The default shortcut is P. It stays enabled until you turn it off.",
      "Open 'Game > Yield Settings' to choose which events interrupt a yield and return control to you, " +
          "such as being targeted or having attackers declared against you. Set these controls to suit your " +
          "pace.\n" +
          "Oh, and you're now on your own, Commander. Good luck!"
  ),

  POST_BATTLE(
      "After a battle",
      "Saw that Gold you were awarded after the Battle? You can spend it at any Bazaar, but any unspent " +
          "Gold at the end of a Run will be lost.\n" +
          "The Echoes you earned are memories of the Aether before the Phyrexian invasion. They persist between " +
          "Runs and can be used to rebuild its lost Aetherworks. Don't worry, I'll show you when you're back " +
          "here.",
      "The cards you earned were added to your deck. You can view your deck at any time by clicking 'Edit " +
          "Rogue Deck'.\n" +
          "If you gained life above your Run's Max Life during the battle, it will reset back to your Max " +
          "Life after the battle.",
      "Your Run progress is saved automatically after every match. When you need a break, Commander, " +
          "you can return to your unfinished Run through 'Continue Run' in the main navigation."
  ),

  ELITE_PLANEBOUND(
      "Elite Planebound",
      "An Elite awaits in the next row on the Trail (marked with a star)! Elite Planebounds are tougher " +
          "than regular opponents but offer greater rewards:\n" +
          "double Echoes, double Gold, and an additional Mythic card reward. Risk-reward, Commander, and " +
          "of course your choice to make."
  ),

  CARD_SELECTION_ZOOM(
      "Zooming Cards",
      "Oh, I almost forgot, Commander: in Card Selection and the Codex, hold the middle mouse button " +
          "over a card to zoom in. Release it to close the zoom.\n" +
          "That leaves the mouse wheel free for scrolling through the cards."
  ),

  CARD_REWARDS(
      "You won!",
      "You made it! You survived your first battle! I knew I could count on you, Commander.",
      "After winning a battle, choose cards from your Reward Pool to add to your deck.\n" +
          "By default, 7 random cards are offered, of which 1 is guaranteed to be of mythic rarity.",
      "You can reroll for a new set of cards by spending Gold. The Gold cost increases by 2 for each " +
          "reroll.\n" +
          "Unchosen or rerolled cards leave the active Reward Pool and only return when there are not enough " +
          "cards left to offer.",
      "For each card added, you will also gain a 'Removal Credit' to remove an unwanted card from your " +
          "deck."
  ),

  BAZAAR(
      "The Bazaar",
      "Spend your earned Gold at the Bazaar to purchase new cards, price depending on Card rarity.",
      "You can reroll for a new selection by spending Gold, increasing by 2 for each reroll.\n" +
          "Unbought or rerolled cards leave the active Reward Pool and only return when there are not enough " +
          "cards left to offer.",
      "For each card added, you will also gain a 'Removal Credit' to remove an unwanted card from your " +
          "deck."
  ),

  EVENT(
      "An Event",
      "Events are random encounters that can have a variety of choices and outcomes for your Run.\n" +
          "Some events may offer powerful rewards, but often at a cost."
  ),

  CHEST(
      "A Chest",
      "Chests contain two random Loot rewards - gold, cards from your Reward Pool, or permanent positive " +
          "Traits that last for the rest of the Run.",
      "Choose which one to keep."
  ),

  SANCTUM(
      "The Sanctum",
      "The Sanctum offers various services: resting, cooking and reflecting.\n" +
          "Gain life (up until your Max. Life) and cure all wounds, craft a random Food item or gain 3 Removal " +
          "Credits.",
      "You may use only one of them."
  ),

  DECK_EDITOR(
      "Deck Editor",
      "Review and edit your current Rogue Deck at any time (seen in the center).\n" +
          "You can always add and remove as many basic lands as needed (from the Pool on the left), but other " +
          "cards can only be added as rewards during the Run, and can only be removed with Removal Credits " +
          "earned by adding cards from a Card Reward or Shop.",
      "Right-click or double-click a card to add/remove it. You can always revert your choices by clicking " +
          "'Undo'.",
      "To keep track of your Mana cost, Mana Production from Lands, average Lands in your starting hand " +
          "and other Stats, switch to the 'Statistic' tab at the top center.",
      "Click 'Back to Map' above your deck cards when you are ready to continue your Run.\n" +
          "Changes to your deck will be saved automatically and persist for the rest of the Run, but will " +
          "be reset at the end of the Run regardless of win or loss."
  ),

  RUN_COMPLETE(
      "Run Complete",
      "You completed your first Run! Win or lose, you've earned Echoes: persistent memories of the Aether " +
          "before the Phyrexian invasion. Use them to rebuild lost Aetherworks that strengthen future Runs.",
      "Open the 'Codex' to view and reset your overall game progress, stats, unlocked cards and tutorials.\n" +
          "View all your past Runs and Rogue Decks in the 'History'."
  ),

  AETHER(
      "Welcome to the Aether",
      "Welcome to my realm. Before the Phyrexian invasion, the Aether was filled with buildings and machines " +
          "that have since been lost. The Echoes you recover preserve memories of what once stood here. Spend " +
          "them to rebuild and upgrade these Aetherworks.",
      "There is one limitation, though. Each active Aetherwork uses one unit of Aether Energy, so you " +
          "can initially activate up to three at the same time. Activate or deactivate an Aetherwork by selecting " +
          "it.",
      "Find Sparks to restore more Aetherworks and increase the Aether's Energy capacity."
  ),

  DESCENSION_UNLOCKED(
      "Descension Mode Unlocked",
      "Descension Mode adds stacking difficulty modifiers to your Runs. Select a Commander you have already " +
          "won with to enable it.",
      "Winning at a Descension Level unlocks the next level for that Commander and earns you a Spark. " +
          "Sparks can restore more Aetherworks and expand the Aether's Energy capacity."
  ),

  DESCENSION_LEVEL_1_WIN(
      "Deeper Into Descension",
      "Descension Level 2 is now unlocked for the Commander who earned this victory.",
      "Remember: each Descension Level includes the modifiers of every previous level. Level 2 adds its " +
          "own challenge on top of Level 1, and that pattern continues as you descend.",
      "You also earned a Spark. Bring it to the Aether, where Sparks and Echoes can restore Aetherworks " +
          "and expand the energy that keeps them active. Prepare well. There is more for us to discover."
  ),

  ENDLESS_MODE(
      "Endless Mode",
      "An endless Run? Well, I suppose you assumed it but I know one or another thing about endless timeloops.\n"
          + "It looks this is still the same Run, but a new Trail: your deck, Gold, Traits and Carry Cards are still with you. " +
          "After each Trail victory, you can complete the Run or go on to the next Trail.",
      "Each Endless Level makes the next Trail harder. Opponents start with more life and cards, " +
          "cast spells for less Mana, and deal more damage or cause more life loss. Bazaar offers cost " +
          "more Gold, too. You can check the current modifiers in the map header and on the Endless Mode " +
          "card in the match.",
      "Defeat the final Planebound to complete a level. Then you may end the Run or continue. I suppose " +
          "we will see how far you can go."
  ),

  CARRY_CARDS(
      "Carry Cards",
      "You have acquired a carry card! Carry cards persist across matches and can be cast from your command " +
          "zone.",
      "There are three types:\n" +
          "- Items: Artifacts such as equipment and vehicles.\n" +
          "- Fellows: Creatures that fight alongside you.\n" +
          "- Scrolls: Instants and sorceries kept in your command zone until used.",
      "Items and fellows are lost permanently if they are neither in the command zone nor on the battlefield " +
          "after a match.\n" +
          "Scrolls are lost if they end a match outside the command zone."
  ),

  CODEX(
      "Codex",
      "The Codex tracks your Rogue Commander progress.\n" +
          "Global Stats shows run and match records, Rogue Commanders shows each commander's reward cards, " +
          "Planebounds shows encountered planes and their decks, and Traits shows discovered Run Trait cards.",
      "Entries have 3 states:\n" +
          "Unknown entries never appeared during a Run.\n" +
          "Seen entries were offered or revealed.\n" +
          "Acquired entries were added to your deck or gained during a Run."
  );

  private final String title;
  private final List<String> messageChunks;

  RogueTutorial(String title, String... messageChunks) {
    this.title = title;
    this.messageChunks = ImmutableList.copyOf(messageChunks);
  }

  public String getTitle() {
    return title;
  }

  public String getMessage() {
    return String.join("\n", messageChunks);
  }

  public List<String> getMessageChunks() {
    return messageChunks;
  }

  public String getId() {
    return name();
  }
}
