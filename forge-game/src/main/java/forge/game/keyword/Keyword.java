package forge.game.keyword;

import forge.card.CardSplitType;
import forge.item.PaperCard;
import org.apache.commons.lang3.tuple.Pair;

import java.util.*;

public enum Keyword {
    UNDEFINED("", SimpleKeyword.class, false, ""),
    ABSORB("Absorb", KeywordWithAmount.class, false, "If a source would deal damage to this creature, prevent %d of that damage."),
    AFFINITY("Affinity", KeywordWithType.class, false, "This spell costs {1} less to cast for each %s you control."),
    AFFLICT("Afflict", KeywordWithAmount.class, false, "Whenever this creature becomes blocked, defending player loses %d life."),
    AFTERLIFE("Afterlife", KeywordWithAmount.class, false, "When this creature dies, create {%1$d:1/1 white and black Spirit creature token} with flying."),
    AFTERMATH("Aftermath", SimpleKeyword.class, false, "Cast this spell only from your graveyard. Then exile it."),
    AMPLIFY("Amplify", Amplify.class, false, "As this creature enters, put {%d:+1/+1 counter} on it for each %s card you reveal in your hand."),
    ANNIHILATOR("Annihilator", KeywordWithAmount.class, false, "Whenever this creature attacks, defending player sacrifices {%d:permanent}."),
    ASCEND("Ascend", SimpleKeyword.class, true, "If you control ten or more permanents, you get the city's blessing for the rest of the game."),
    ASSIST("Assist", SimpleKeyword.class, true, "Another player can pay up to %s of this spell's cost."),
    AURA_SWAP("Aura swap", KeywordWithCost.class, false, "%s: You may exchange this Aura with an Aura card in your hand."),
    AWAKEN("Awaken", KeywordWithCostAndAmount.class, false, "If you cast this spell for %s, also put {%d:+1/+1 counter} on target land you control and it becomes a 0/0 Elemental creature with haste. It's still a land."),
    BACKUP("Backup", KeywordWithAmount.class, false, "When this creature enters, put {%1$d:+1/+1 counter} on target creature. If that's another creature, it gains the following ability until end of turn."),
    BANDING("Banding", SimpleKeyword.class, true, "Any creatures with banding, and up to one without, can attack in a band. Bands are blocked as a group. If any creatures with banding you control are blocking or being blocked by a creature, you divide that creature's combat damage, not its controller, among any of the creatures it's being blocked by or is blocking."),
    BANDSWITH("Bands with other", KeywordWithType.class, false, "can attack in a band with another %s"),
    BARGAIN("Bargain", SimpleKeyword.class, false, "You may sacrifice an artifact, enchantment, or token as you cast this spell."),
    BATTLE_CRY("Battle cry", SimpleKeyword.class, false, "Whenever this creature attacks, each other attacking creature gets +1/+0 until end of turn."),
    BESTOW("Bestow", KeywordWithCost.class, false, "If you cast this card for its bestow cost, it's an Aura spell with enchant creature. It becomes a creature again if it's not attached to a creature."),
    BLITZ("Blitz", KeywordWithCost.class, false, "If you cast this spell for its blitz cost, it gains haste and \"When this creature dies, draw a card.\" Sacrifice it at the beginning of the next end step."),
    BLOODTHIRST("Bloodthirst", KeywordWithAmount.class, false, "If an opponent was dealt damage this turn, this creature enters with {%d:+1/+1 counter} on it."),
    BUSHIDO("Bushido", KeywordWithAmount.class, false, "Whenever this creature blocks or becomes blocked, it gets +%1$d/+%1$d until end of turn."),
    BUYBACK("Buyback", KeywordWithCost.class, false, "You may pay an additional %s as you cast this spell. If you do, put it into your hand instead of your graveyard as it resolves."),
    CASCADE("Cascade", SimpleKeyword.class, false, "When you cast this spell, exile cards from the top of your library until you exile a nonland card whose mana value is less than this spell's mana value. You may cast that spell without paying its mana cost if its mana value is less than this spell's mana value. Then put all cards exiled this way that weren't cast on the bottom of your library in a random order."),
    CASUALTY("Casualty", KeywordWithAmount.class, false, "As you cast this spell, you may sacrifice a creature with power %1$d or greater. When you do, copy this spell."),
    CHAMPION("Champion", KeywordWithType.class, false, "When this permanent enters, sacrifice it unless you exile another %s you control. When this permanent leaves the battlefield, return the exiled card to the battlefield under its owner's control."),
    CHANGELING("Changeling", SimpleKeyword.class, true, "This card is every creature type."),
    CHOOSE_A_BACKGROUND("Choose a Background", Partner.class, true, "You can have a Background as a second commander."),
    CIPHER("Cipher", SimpleKeyword.class, true, "Then you may exile this spell card encoded on a creature you control. Whenever that creature deals combat damage to a player, its controller may cast a copy of the encoded card without paying its mana cost."),
    COMPANION("Companion", Companion.class, true, "Reveal your companion from outside the game if your deck meets the companion restriction."),
    COMPLEATED("Compleated", Compleated.class, true, "This planeswalker enters with two fewer loyalty counters for each Phyrexian mana symbol life was paid for."),
    CONSPIRE("Conspire", SimpleKeyword.class, false, "As an additional cost to cast this spell, you may tap two untapped creatures you control that each share a color with it. If you do, copy it."),
    CONVOKE("Convoke", SimpleKeyword.class, true, "Your creatures can help cast this spell. Each creature you tap while playing this spell reduces its cost by {1} or by one mana of that creature's color."),
    CRAFT("Craft", Craft.class, false, "%s: Return this card transformed under its owner's control. Craft only as a sorcery."),
    CREW("Crew", KeywordWithAmount.class, false, "Tap any number of creatures you control with total power %1$d or more: This Vehicle becomes an artifact creature until end of turn."),
    CUMULATIVE_UPKEEP("Cumulative upkeep", KeywordWithCost.class, false, "At the beginning of your upkeep, put an age counter on this permanent, then sacrifice it unless you pay its upkeep cost for each age counter on it."),
    CYCLING("Cycling", KeywordWithCost.class, false, "%s, Discard this card: Draw a card."), //Typecycling reminder text handled by Cycling class
    DASH("Dash", KeywordWithCost.class, false, "You may cast this spell for its dash cost. If you do, it gains haste, and it's returned from the battlefield to its owner's hand at the beginning of the next end step."),
    DAYBOUND("Daybound", SimpleKeyword.class, true, "If a player casts no spells during their own turn, it becomes night next turn."),
    DEATHTOUCH("Deathtouch", SimpleKeyword.class, true, "Any amount of damage this deals to a creature is enough to destroy it."),
    DECAYED("Decayed", SimpleKeyword.class, true, "This creature can't block. When it attacks, sacrifice it at end of combat."),
    DEFENDER("Defender", SimpleKeyword.class, true, "This creature can't attack."),
    DELVE("Delve", SimpleKeyword.class, true, "As an additional cost to cast this spell, you may exile any number of cards from your graveyard. Each card exiled this way reduces the cost to cast this spell by {1}."),
    DEMONSTRATE("Demonstrate", SimpleKeyword.class, false, "When you cast this spell, you may copy it. If you do, choose an opponent to also copy it. Players may choose new targets for their copies."),
    DETHRONE("Dethrone", SimpleKeyword.class, false, "Whenever this creature attacks the player with the most life or tied for the most life, put a +1/+1 counter on it."),
    DEVOUR("Devour", Devour.class, false, "As this object enters, you may sacrifice any number of %2$s. This permanent enters with {%1$s:+1/+1 counter} on it for each permanent sacrificed this way."),
    DEVOID("Devoid", SimpleKeyword.class, true, "This card has no color."),
    DISGUISE("Disguise", KeywordWithCost.class, false, "You may cast this card face down for {3} as a 2/2 creature with ward {2}. Turn it face up any time for its disguise cost."),
    DISTURB("Disturb", KeywordWithCost.class, false, "You may cast this card from your graveyard transformed for its disturb cost."),
    DOCTORS_COMPANION("Doctor's companion", Partner.class, true, "You can have two commanders if the other is the Doctor."),
    DOUBLE_AGENDA("Double agenda", SimpleKeyword.class, false, "Start the game with this conspiracy face down in the command zone and secretly choose two different card names. You may turn this conspiracy face up any time and reveal those names."),
    DOUBLE_STRIKE("Double Strike", SimpleKeyword.class, true, "This creature deals both first-strike and regular combat damage."),
    DOUBLE_TEAM("Double team", SimpleKeyword.class, false, "When this creature attacks, if it's not a token, conjure a duplicate of it into your hand. Then both cards perpetually lose double team."),
    DREDGE("Dredge", KeywordWithAmount.class, false, "If you would draw a card, instead you may put exactly {%d:card} from the top of your library into your graveyard. If you do, return this card from your graveyard to your hand. Otherwise, draw a card."),
    ECHO("Echo", KeywordWithCost.class, false, "At the beginning of your upkeep, if this permanent came under your control since the beginning of your last upkeep, sacrifice it unless you pay its echo cost."),
    EMBALM("Embalm", KeywordWithCost.class, false, "%s, Exile this card from your graveyard: Create a token that's a copy of this card, except it's white, it has no mana cost, and it's a Zombie in addition to its other types. Embalm only as a sorcery."),
    EMERGE("Emerge", Emerge.class, false, "You may cast this spell by sacrificing {1:%2$s} and paying the emerge cost reduced by that %2$s's mana value."),
    ENCHANT("Enchant", KeywordWithType.class, false, "Target a %s as you cast this. This card enters attached to that %s."),
    ENCORE("Encore", KeywordWithCost.class, false, "%s, Exile this card from your graveyard: For each opponent, create a token copy that attacks that opponent this turn if able. They gain haste. Sacrifice them at the beginning of the next end step. Activate only as a sorcery."),
    ENLIST("Enlist", SimpleKeyword.class, false, "As this creature attacks, you may tap a nonattacking creature you control without summoning sickness. When you do, add its power to this creature’s until end of turn."),
    ENTWINE("Entwine", KeywordWithCost.class, true, "Choose both if you pay the entwine cost."),
    EPIC("Epic", SimpleKeyword.class, true, "For the rest of the game, you can't cast spells. At the beginning of each of your upkeeps for the rest of the game, copy this spell except for its epic ability. If the spell has any targets, you may choose new targets for the copy."),
    EQUIP("Equip", Equip.class, false, "%s: Attach to target %s you control. Equip only as a sorcery."),
    ESCAPE("Escape", KeywordWithCost.class, false, "You may cast this card from your graveyard for its escape cost."),
    ESCALATE("Escalate", KeywordWithCost.class, true, "Pay this cost for each mode chosen beyond the first."),
    ETERNALIZE("Eternalize", KeywordWithCost.class, false, "%s, Exile this card from your graveyard: Create a token that's a copy of this card, except it's black, it's 4/4, it has no mana cost, and it's a Zombie in addition to its other types. Eternalize only as a sorcery."),
    EVOKE("Evoke", KeywordWithCost.class, false, "You may cast this spell for its evoke cost. If you do, it's sacrificed when it enters."),
    EVOLVE("Evolve", SimpleKeyword.class, false, "Whenever a creature you control enters, if that creature has greater power or toughness than this creature, put a +1/+1 counter on this creature."),
    EXALTED("Exalted", SimpleKeyword.class, false, "Whenever a creature you control attacks alone, that creature gets +1/+1 until end of turn."),
    EXPLOIT("Exploit", SimpleKeyword.class, false, "When this creature enters, you may sacrifice a creature."),
    EXTORT("Extort", SimpleKeyword.class, false, "Whenever you cast a spell, you may pay {W/B}. If you do, each opponent loses 1 life and you gain that much life."),
    FABRICATE("Fabricate", KeywordWithAmount.class, false, "When this creature enters, put {%1$d:+1/+1 counter} on it, or create {%1$d:1/1 colorless Servo artifact creature token}."),
    FADING("Fading", KeywordWithAmount.class, false, "This permanent enters with {%d:fade counter} on it. At the beginning of your upkeep, remove a fade counter from it. If you can't, sacrifice it."),
    FEAR("Fear", SimpleKeyword.class, true, "This creature can't be blocked except by artifact creatures and/or black creatures."),
    FIREBENDING("Firebending", Firebending.class, false, "Whenever this creature attacks, add %s. This mana lasts until end of combat."),
    FIRST_STRIKE("First Strike", SimpleKeyword.class, true, "This creature deals combat damage before creatures without first strike."),
    FLANKING("Flanking", SimpleKeyword.class, false, "Whenever this creature becomes blocked by a creature without flanking, the blocking creature gets -1/-1 until end of turn."),
    FLASH("Flash", SimpleKeyword.class, true, "You may cast this spell any time you could cast an instant."),
    FLASHBACK("Flashback", KeywordWithCost.class, false, "You may cast this card from your graveyard for its flashback cost. Then exile it."),
    FLYING("Flying", SimpleKeyword.class, true, "This creature can't be blocked except by creatures with flying or reach."),
    FOR_MIRRODIN("For Mirrodin", SimpleKeyword.class, false, "When this Equipment enters, create a 2/2 red Rebel creature token, then attach this to it."),
    FORETELL("Foretell", KeywordWithCost.class, false, "During your turn, you may pay {2} and exile this card from your hand face down. Cast it on a later turn for its foretell cost."),
    FORTIFY("Fortify", KeywordWithCost.class, false, "%s: Attach to target land you control. Fortify only as a sorcery."),
    FREERUNNING("Freerunning", KeywordWithCost.class, false, "You may cast this spell for its freerunning cost if you dealt combat damage to a player this turn with an Assassin or commander."),
    FRENZY("Frenzy", KeywordWithAmount.class, false, "Whenever this creature attacks and isn't blocked, it gets +%d/+0 until end of turn."),
    FUSE("Fuse", SimpleKeyword.class, true, "You may cast one or both halves of this card from your hand."),
    GIFT("Gift", SimpleKeyword.class, true, "You may promise an opponent a gift as you cast this spell. If you do, when it enters, they %s."),
    GRAFT("Graft", KeywordWithAmount.class, false, "This permanent enters with {%d:+1/+1 counter} on it. Whenever another creature enters, you may move a +1/+1 counter from this permanent onto it."),
    GRAVESTORM("Gravestorm", SimpleKeyword.class, false, "When you cast this spell, copy it for each permanent that was put into a graveyard from the battlefield this turn. If the spell has any targets, you may choose new targets for any of the copies."),
    HARMONIZE("Harmonize", KeywordWithCost.class, false, "You may cast this card from your graveyard for its harmonize cost. You may tap a creature you control to reduce that cost by {X}, where X is its power. Then exile this spell."),
    HASTE("Haste", SimpleKeyword.class, true, "This creature can attack and {T} as soon as it comes under your control."),
    HAUNT("Haunt", SimpleKeyword.class, false, "When this is put into a graveyard from the battlefield, exile it haunting target creature."),
    HEXPROOF("Hexproof", Hexproof.class, true, "This can't be the target of %s spells or abilities your opponents control."),
    HIDEAWAY("Hideaway", KeywordWithAmount.class, false, "When this permanent enters, look at the top {%d:card} of your library, exile one face down, then put the rest on the bottom of your library."),
    HIDDEN_AGENDA("Hidden agenda", SimpleKeyword.class, false, "Start the game with this conspiracy face down in the command zone and secretly choose a card name. You may turn this conspiracy face up any time and reveal that name."),
    HORSEMANSHIP("Horsemanship", SimpleKeyword.class, true, "This creature can't be blocked except by creatures with horsemanship."),
    IMPENDING("Impending", KeywordWithCostAndAmount.class, false, "If you cast this spell for its impending cost, it enters with {%2$d:time counter} and isn't a creature until the last is removed. At the beginning of your end step, remove a time counter from it."),
    IMPROVISE("Improvise", SimpleKeyword.class, true, "Your artifacts can help cast this spell. Each artifact you tap after you're done activating mana abilities pays for {1}."),
    INDESTRUCTIBLE("Indestructible", SimpleKeyword.class, true, "Effects that say \"destroy\" don't destroy this."),
    INFECT("Infect", SimpleKeyword.class, true, "This creature deals damage to creatures in the form of -1/-1 counters and to players in the form of poison counters."),
    INGEST("Ingest", SimpleKeyword.class, false, "Whenever this creature deals combat damage to a player, that player exiles the top card of their library."),
    INTIMIDATE("Intimidate", SimpleKeyword.class, true, "This creature can't be blocked except by artifact creatures and/or creatures that share a color with it."),
    KICKER("Kicker", Kicker.class, false, "You may pay an additional %s as you cast this spell."),
    JOB_SELECT("Job select", SimpleKeyword.class, false, "When this Equipment enters, create a 1/1 colorless Hero creature token, then attach this to it."),
    JUMP_START("Jump-start", SimpleKeyword.class, false, "You may cast this card from your graveyard by discarding a card in addition to paying its other costs. Then exile this card."),
    LANDWALK("Landwalk", KeywordWithType.class, true, "This creature is unblockable as long as defending player controls {1:%s}."),
    LEVEL_UP("Level up", KeywordWithCost.class, false, "%s: Put a level counter on this. Level up only as a sorcery."),
    LIFELINK("Lifelink", SimpleKeyword.class, true, "Damage dealt by this creature also causes its controller to gain that much life."),
    LIVING_METAL("Living metal", SimpleKeyword.class, true, "During your turn, this Vehicle is also a creature."),
    LIVING_WEAPON("Living Weapon", SimpleKeyword.class, true, "When this Equipment enters, create a 0/0 black Phyrexian Germ creature token, then attach this to it."),
    MADNESS("Madness", KeywordWithCost.class, false, "If you discard this card, discard it into exile. When you do, cast it for its madness cost or put it into your graveyard."),
    MAYHEM("Mayhem", Mayhem.class, false, "You may cast this card from your graveyard for %s if you discarded it this turn. Timing rules still apply."),
    MELEE("Melee", SimpleKeyword.class, false, "Whenever this creature attacks, it gets +1/+1 until end of turn for each opponent you attacked this combat."),
    MENTOR("Mentor", SimpleKeyword.class, false, "Whenever this creature attacks, put a +1/+1 counter on target attacking creature with lesser power."),
    MENACE("Menace", SimpleKeyword.class, true, "This creature can't be blocked except by two or more creatures."),
    MEGAMORPH("Megamorph", KeywordWithCost.class, false, "You may cast this card face down as a 2/2 creature for {3}. Turn it face up any time for its megamorph cost and put a +1/+1 counter on it."),
    MIRACLE("Miracle", KeywordWithCost.class, false, "You may cast this card for its miracle cost when you draw it if it's the first card you drew this turn."),
    MOBILIZE("Mobilize", KeywordWithAmount.class, false, "When this creature attacks, create {%1$d:tapped and attacking 1/1 red Warrior creature token}. Sacrifice them at the beginning of the next end step."),
    // technically not a keyword but easier this way
    MONSTROSITY("Monstrosity", KeywordWithCostAndAmount.class, false, "If this creature isn't monstrous, put {%2$d:+1/+1 counter} on it and it becomes monstrous."),
    MODULAR("Modular", Modular.class, false, "This creature enters with {%d:+1/+1 counter} on it. When it dies, you may put its +1/+1 counters on target artifact creature."),
    MORE_THAN_MEETS_THE_EYE("More Than Meets the Eye", KeywordWithCost.class, false, "You may cast this card converted for %s."),
    MORPH("Morph", KeywordWithCost.class, false, "You may cast this card face down as a 2/2 creature for {3}. Turn it face up any time for its morph cost."),
    MULTIKICKER("Multikicker", KeywordWithCost.class, false, "You may pay an additional %s any number of times as you cast this spell."),
    MUTATE("Mutate", KeywordWithCost.class, true, "If you cast this spell for its mutate cost, put it over or under target non-Human creature you own. They mutate into the creature on top plus all abilities from under it."),
    MYRIAD("Myriad", SimpleKeyword.class, false, "Whenever this creature attacks, for each opponent other than defending player, you may create a token that's a copy of this creature that's tapped and attacking that player or a planeswalker they control. Exile the tokens at end of combat."),
    NIGHTBOUND("Nightbound", SimpleKeyword.class, true, "If a player casts at least two spells during their own turn, it becomes day next turn."),
    NINJUTSU("Ninjutsu", Ninjutsu.class, false, "%s, Return an unblocked attacker you control to hand: Put this card onto the battlefield from your %s tapped and attacking."),
    OUTLAST("Outlast", KeywordWithCost.class, false, "%s, {T}: Put a +1/+1 counter on this creature. Outlast only as a sorcery."),
    OFFERING("Offering", KeywordWithType.class, false, "You may cast this card any time you could cast an instant by sacrificing a %1$s and paying the difference in mana costs between this and the sacrificed %1$s. Mana cost includes color."),
    OFFSPRING("Offspring", KeywordWithCost.class, false, "You may pay an additional %s as you cast this spell. If you do, when this creature enters, create a 1/1 token copy of it."),
    OVERLOAD("Overload", KeywordWithCost.class, false, "You may cast this spell for its overload cost. If you do, change its text by replacing all instances of \"target\" with \"each.\""),
    PARTNER("Partner", Partner.class, true, "You can have two commanders if both have partner."),
    PARTNER_WITH("Partner with", KeywordWithType.class, false, "When this creature enters, target player may put %s into their hand from their library, then shuffle."),
    PERSIST("Persist", SimpleKeyword.class, false, "When this creature dies, if it had no -1/-1 counters on it, return it to the battlefield under its owner's control with a -1/-1 counter on it."),
    PHASING("Phasing", SimpleKeyword.class, true, "This phases in or out before you untap during each of your untap steps. While it's phased out, it's treated as though it doesn't exist."),
    PLOT("Plot", KeywordWithCost.class, false, "You may pay %s and exile this card from your hand. Cast it as a sorcery on a later turn without paying its mana cost. Plot only as a sorcery."),
    POISONOUS("Poisonous", KeywordWithAmount.class, false, "Whenever this creature deals combat damage to a player, that player gets {%d:poison counter}."),
    PROTECTION("Protection", Protection.class, true, "This creature can't be blocked, targeted, dealt damage, or equipped/enchanted by %s."),
    PROTOTYPE("Prototype", KeywordWithCost.class, false, "You may cast this spell with different mana cost, color, and size. It keeps its abilities and types."),
    PROVOKE("Provoke", SimpleKeyword.class, false, "Whenever this creature attacks, you may have target creature defending player controls untap and block it if able."),
    PROWESS("Prowess", SimpleKeyword.class, false, "Whenever you cast a noncreature spell, this creature gets +1/+1 until end of turn."),
    PROWL("Prowl", KeywordWithCost.class, false, "You may pay %s rather than pay this spell's mana cost if a player was dealt combat damage this turn by a source that, at the time it dealt that damage, was under your control and had any of this spell's creature types."),
    RAMPAGE("Rampage", KeywordWithAmount.class, false, "Whenever this creature becomes blocked, it gets +%1$d/+%1$d until end of turn for each creature blocking it beyond the first."),
    RAVENOUS("Ravenous", SimpleKeyword.class, false, "This creature enters with X +1/+1 counters on it. If X is 5 or more, draw a card when it enters."),
    REACH("Reach", SimpleKeyword.class, true, "This creature can block creatures with flying."),
    READ_AHEAD("Read ahead", SimpleKeyword.class, true, "Chapter abilities of this Saga can’t trigger the turn it entered the battlefield unless it has exactly the number of lore counters on it specified in the chapter symbol of that ability."),
    REBOUND("Rebound", SimpleKeyword.class, true, "If you cast this spell from your hand, exile it as it resolves. At the beginning of your next upkeep, you may cast this card from exile without paying its mana cost."),
    RECOVER("Recover", KeywordWithCost.class, false, "When a creature is put into your graveyard from the battlefield, you may pay %s. If you do, return this card from your graveyard to your hand. Otherwise, exile this card."),
    RECONFIGURE("Reconfigure", KeywordWithCost.class, false, "%s: Attach to target creature you control; or unattach from a creature. Reconfigure only as a sorcery. While attached, this isn't a creature."),
    REFLECT("Reflect", KeywordWithCost.class, false, "As this enters, each opponent may pay %s. When they do, they create a token copy of this except it lacks this ability."),
    REINFORCE("Reinforce", KeywordWithCostAndAmount.class, false, "%s, Discard this card: Put {%d:+1/+1 counter} on target creature."),
    RENOWN("Renown", KeywordWithAmount.class, false, "When this creature deals combat damage to a player, if it isn't renowned, put {%d:+1/+1 counter} on it and it becomes renowned."),
    REPLICATE("Replicate", KeywordWithCost.class, false, "As an additional cost to cast this spell, you may pay %s any number of times. If you do, copy it that many times. You may choose new targets for the copies."),
    RETRACE("Retrace", SimpleKeyword.class, false, "You may cast this card from your graveyard by discarding a land card in addition to paying its other costs."),
    RIOT("Riot", SimpleKeyword.class, false, "This creature enters with your choice of a +1/+1 counter or haste."),
    RIPPLE("Ripple", KeywordWithAmount.class, false, "When you cast this spell, you may reveal the top {%d:card} of your library. You may cast any of those cards with the same name as this spell without paying their mana costs. Put the rest on the bottom of your library in any order."),
    SADDLE("Saddle", KeywordWithAmount.class, false, "Tap any number of other creatures you control with total power %1$d or more: This Mount becomes saddled until end of turn. Saddle only as a sorcery."),
    SCAVENGE("Scavenge", KeywordWithCost.class, false, "%s, Exile this card from your graveyard: Put a number of +1/+1 counters equal to this card's power on target creature. Scavenge only as a sorcery."),
    SHADOW("Shadow", SimpleKeyword.class, true, "This creature can block or be blocked by only creatures with shadow."),
    SHROUD("Shroud", SimpleKeyword.class, true, "This can't be the target of spells or abilities."),
    SKULK("Skulk", SimpleKeyword.class, true, "This creature can't be blocked by creatures with greater power."),
    SNEAK("Sneak", KeywordWithCost.class, false, "You may cast this spell for %s if you also return an unblocked attacker you control to hand during the declare blockers step."),
    SOULBOND("Soulbond", SimpleKeyword.class, true, "You may pair this creature with another unpaired creature when either enters. They remain paired for as long as you control both of them."),
    SOULSHIFT("Soulshift", KeywordWithAmount.class, false, "When this creature dies, you may return target Spirit card with mana value %d or less from your graveyard to your hand."),
    SPACE_SCULPTOR("Space sculptor", SimpleKeyword.class, true, "CARDNAME divides the battlefield into alpha, beta, and gamma sectors. If a creature isn't assigned to a sector, its controller assigns it to one. Opponents assign first."),
    SPECIALIZE("Specialize", KeywordWithCost.class, false, "%s, Choose a color, discard a card of that color or associated basic land type: This card perpetually specializes into that color. Activate only as a sorcery."),
    SPECTACLE("Spectacle", KeywordWithCost.class, false, "You may cast this spell for its spectacle cost rather than its mana cost if an opponent lost life this turn."),
    SPLICE("Splice", KeywordWithCostAndType.class, false, "As you cast an %2$s spell, you may reveal this card from your hand and pay its splice cost. If you do, add this card's effects to that spell."),
    SPLIT_SECOND("Split second", SimpleKeyword.class, true, "As long as this spell is on the stack, players can't cast other spells or activate abilities that aren't mana abilities."),
    SPREE("Spree", SimpleKeyword.class, true, "Choose one or more additional costs."),
    SQUAD("Squad", KeywordWithCost.class, false, "As an additional cost to cast this spell, you may pay %s any number of times. When this creature enters, create that many tokens that are copies of it."),
    START_YOUR_ENGINES("Start your engines", SimpleKeyword.class, true, "If you have no speed, it starts at 1. It increases once on each of your turns when an opponent loses life. Max speed is 4."),
    STARTING_INTENSITY("Starting intensity", KeywordWithAmount.class, true, null),
    STATION("Station", KeywordWithAmount.class, false, "Tap another creature you control: Put charge counters equal to its power on this Spacecraft. Station only as a sorcery. It’s an artifact creature at %d+."),
    STORM("Storm", SimpleKeyword.class, false, "When you cast this spell, copy it for each other spell that was cast before it this turn. You may choose new targets for the copies."),
    STRIVE("Strive", KeywordWithCost.class, false, "CARDNAME costs %s more to cast for each target beyond the first."),
    SUNBURST("Sunburst", SimpleKeyword.class, false, "This enters with either a +1/+1 or charge counter on it for each color of mana spent to cast it based on whether it's a creature."),
    SURGE("Surge", KeywordWithCost.class, false, "You may cast this spell for its surge cost if you or a teammate has cast another spell this turn."),
    SUSPEND("Suspend", Suspend.class, false, "If you could begin to cast this card by putting it onto the stack from your hand, you may pay %s and exile it with {%d:time counter} on it. At the beginning of your upkeep, remove a time counter. When the last is removed, play it without paying its mana cost. If you cast a creature spell this way, it gains haste until you lose control of the spell or the permanent it becomes."),
    TIERED("Tiered", SimpleKeyword.class, true, "Choose one additional cost."),
    TOXIC("Toxic", KeywordWithAmount.class, false, "Players dealt combat damage by this creature also get {%d:poison counter}."),
    TRAINING("Training", SimpleKeyword.class, false, "Whenever this creature attacks with another creature with greater power, put a +1/+1 counter on this creature."),
    TRAMPLE("Trample", Trample.class, true, "This creature can deal excess combat damage to the player or planeswalker it's attacking."),
    TRANSFIGURE("Transfigure", KeywordWithCost.class, false, "%s, Sacrifice this creature: Search your library for a creature card with the same mana value as this creature and put that card onto the battlefield, then shuffle. Transfigure only as a sorcery."),
    TRANSMUTE("Transmute", KeywordWithCost.class, false, "%s, Discard this card: Search your library for a card with the same mana value as this card, reveal it, and put it into your hand, then shuffle. Transmute only as a sorcery."),
    TRIBUTE("Tribute", KeywordWithAmount.class, false, "As this creature enters, an opponent of your choice may put {%d:+1/+1 counter} on it."),
    TYPECYCLING("TypeCycling", KeywordWithCostAndType.class, false, "%s, Discard this card: Search your library for %s, reveal it, put it into your hand, then shuffle."),
    UMBRA_ARMOR("Umbra armor", SimpleKeyword.class, true, "If enchanted permanent would be destroyed, instead remove all damage marked on it and destroy this Aura."),
    UNDAUNTED("Undaunted", SimpleKeyword.class, false, "This spell costs {1} less to cast for each opponent."),
    UNDYING("Undying", SimpleKeyword.class, false, "When this creature dies, if it had no +1/+1 counters on it, return it to the battlefield under its owner's control with a +1/+1 counter on it."),
    UNEARTH("Unearth", KeywordWithCost.class, false, "%s: Return this card from your graveyard to the battlefield. It gains haste. Exile it at the beginning of the next end step or if it would leave the battlefield. Unearth only as a sorcery."),
    UNLEASH("Unleash", SimpleKeyword.class, false, "You may have this creature enter with a +1/+1 counter on it. It can't block as long as it has a +1/+1 counter on it."),
    VANISHING("Vanishing", Vanishing.class, false, "This permanent enters with {%d:time counter} on it. At the beginning of your upkeep, remove a time counter from it. When the last is removed, sacrifice it."),
    VIGILANCE("Vigilance", SimpleKeyword.class, true, "Attacking doesn't cause this creature to tap."),
    WARD("Ward", KeywordWithCost.class, false, "Whenever this permanent becomes the target of a spell or ability an opponent controls, counter it unless that player pays %s."),
    WARP("Warp", KeywordWithCost.class, false, "You may cast this card from your hand for its warp cost. Exile this creature at the beginning of the next end step, then you may cast it from exile on a later turn."),
    WEB_SLINGING("Web-slinging", KeywordWithCost.class, false, "You may cast this spell for %s if you also return a tapped creature you control to its owner’s hand."),
    WITHER("Wither", SimpleKeyword.class, true, "This deals damage to creatures in the form of -1/-1 counters."),

    // mayflash additional cast
    MAYFLASHCOST("MayFlashCost", KeywordWithCost.class, false, "You may cast CARDNAME as though it had flash if you pay %s more to cast it."),
    MAYFLASHSAC("MayFlashSac", SimpleKeyword.class, false, "You may cast CARDNAME as though it had flash. If you cast it any time a sorcery couldn't have been cast, the controller of the permanent it becomes sacrifices it at the beginning of the next cleanup step."),

    ;

    protected final Class<? extends KeywordInstance<?>> type;
    protected final boolean isMultipleRedundant;
    protected final String reminderText, displayName;

    Keyword(String displayName0, Class<? extends KeywordInstance<?>> type0, boolean isMultipleRedundant0, String reminderText0) {
        type = type0;
        isMultipleRedundant = isMultipleRedundant0;
        reminderText = reminderText0;
        displayName = displayName0;
    }

    private static Pair<Keyword, String> getKeywordDetails(String k) {
        Keyword keyword = Keyword.UNDEFINED;
        String details = k;
        // try to get real part
        if (k.contains(":")) {
            final String[] x = k.split(":", 2);
            keyword = smartValueOf(x[0]);
            details = x[1];
            // Flavor keyword titles should be last in the card script K: line
            if (details.contains(":Flavor ")) details = details.substring(0, details.indexOf(":Flavor "));
            // Simply remove flavor here so it doesn't goof up parsing details
        } else if (k.contains(" ")) {
            // First strike
            keyword = smartValueOf(k);
            details = "";

            // other keywords that contains other stuff like Enchant
            if (keyword == Keyword.UNDEFINED) {
                final String[] x = k.split(" ", 2);

                final Keyword k2 = smartValueOf(x[0]);
                // Keywords that needs to be undefined
                if (k2 != Keyword.UNDEFINED) {
                    keyword = k2;
                    details = x[1];
                }
            }
        } else {
            // Simple Keyword
            keyword = smartValueOf(k);
            details = "";
        }
        return Pair.of(keyword, details);
    }

    public static KeywordInterface getInstance(String k) {
        Pair<Keyword, String> p = getKeywordDetails(k);

        KeywordInstance<?> inst;
        try {
            inst = p.getKey().type.getConstructor().newInstance();
        } catch (Exception e) {
            inst = new SimpleKeyword();
        }
        inst.initialize(k, p.getKey(), p.getValue());
        return inst;
    }

    @Override
    public String toString() {
        return displayName;
    }

    public static List<Keyword> getAllKeywords() {
        Keyword[] values = values();
        List<Keyword> keywords = new ArrayList<>(Arrays.asList(values).subList(1, values.length)); //skip UNDEFINED
        return keywords;
    }

    private static Keyword get(String k) {
        if (k == null || k.isEmpty())
            return Keyword.UNDEFINED;

        return getKeywordDetails(k).getKey();
    }

    private static final Map<String, Set<Keyword>> cardKeywordSetLookup = new HashMap<>();

    public static Set<Keyword> getKeywordSet(PaperCard card) {
        String name = card.getName();
        Set<Keyword> keywordSet = cardKeywordSetLookup.get(name);
        if (keywordSet == null) {
            CardSplitType cardSplitType = card.getRules().getSplitType();
            keywordSet = EnumSet.noneOf(Keyword.class);
            if (cardSplitType != CardSplitType.None && cardSplitType != CardSplitType.Split) {
                if (card.getRules().getOtherPart() != null) {
                    if (card.getRules().getOtherPart().getKeywords() != null) {
                        for (String key : card.getRules().getOtherPart().getKeywords()) {
                            Keyword keyword = get(key);
                            if (!Keyword.UNDEFINED.equals(keyword))
                                keywordSet.add(keyword);
                        }
                    }
                }
            }
            if (card.getRules().getMainPart().getKeywords() != null) {
                for (String key : card.getRules().getMainPart().getKeywords()) {
                    Keyword keyword = get(key);
                    if (!Keyword.UNDEFINED.equals(keyword))
                        keywordSet.add(keyword);
                }
            }
            cardKeywordSetLookup.put(name, keywordSet);
        }
        return keywordSet;
    }

    public static Keyword smartValueOf(String value) {
        for (final Keyword v : Keyword.values()) {
            if (v.displayName.equalsIgnoreCase(value)) {
                return v;
            }
        }

        return UNDEFINED;
    }

    public static Set<Keyword> setValueOf(String value) {
        Set<Keyword> result = EnumSet.noneOf(Keyword.class);
        for (String s : value.split(" & ")) {
            Keyword k = smartValueOf(s);
            if (!UNDEFINED.equals(k)) {
                result.add(k);
            }
        }
        return result;
    }

    public String getReminderText() {
        return reminderText;
    }

    public boolean isMultipleRedundant() {
        return isMultipleRedundant;
    }
}
