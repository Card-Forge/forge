package forge.game.keyword;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import forge.StaticData;
import forge.game.card.Card;
import forge.item.PaperCard;

public enum Keyword {
    UNDEFINED("", SimpleKeyword.class, false, ""),
    ABSORB("Absorb", KeywordWithAmount.class, false, "If a source would deal damage to this creature, prevent %d of that damage."),
    ADAPT("Adapt", KeywordWithCostAndAmount.class, false, "If this creature has no +1/+1 counters on it, put {%2$d:+1/+1 counter} on it."),
    AFFINITY("Affinity", KeywordWithType.class, false, "This spell costs {1} less to cast for each %s you control."),
    AFFLICT("Afflict", KeywordWithAmount.class, false, "Whenever this creature becomes blocked, defending player loses %d life."),
    AFTERLIFE("Afterlife", KeywordWithAmount.class, false, "When this creature dies, create {%1$d:1/1 white and black Spirit creature token} with flying."),
    AFTERMATH("Aftermath", SimpleKeyword.class, false, "Cast this spell only from your graveyard. Then exile it."),
    AMPLIFY("Amplify", KeywordWithAmountAndType.class, false, "As this creature enters the battlefield, put {%d:+1/+1 counter} on it for each %s card you reveal in your hand."),
    ANNIHILATOR("Annihilator", KeywordWithAmount.class, false, "Whenever this creature attacks, defending player sacrifices {%d:permanent}."),
    ASCEND("Ascend", SimpleKeyword.class, true, "If you control ten or more permanents, you get the city's blessing for the rest of the game."),
    ASSIST("Assist", SimpleKeyword.class, true, "Another player can pay up to %s of this spell's cost."),
    AURA_SWAP("Aura swap", KeywordWithCost.class, false, "%s: You may exchange this Aura with an Aura card in your hand."),
    AWAKEN("Awaken", KeywordWithCostAndAmount.class, false, "If you cast this spell for %s, also put {%d:+1/+1 counter} on target land you control and it becomes a 0/0 Elemental creature with haste. It's still a land."),
    BANDING("Banding", SimpleKeyword.class, true, "Any creatures with banding, and up to one without, can attack in a band. Bands are blocked as a group. If any creatures with banding you control are blocking or being blocked by a creature, you divide that creature's combat damage, not its controller, among any of the creatures it's being blocked by or is blocking."),
    BATTLE_CRY("Battle cry", SimpleKeyword.class, false, "Whenever this creature attacks, each other attacking creature gets +1/+0 until end of turn."),
    BESTOW("Bestow", KeywordWithCost.class, false, "If you cast this card for its bestow cost, it's an Aura spell with enchant creature. It becomes a creature again if it's not attached to a creature."),
    BLOODTHIRST("Bloodthirst", KeywordWithAmount.class, false, "If an opponent was dealt damage this turn, this creature enters the battlefield with {%d:+1/+1 counter} on it."),
    BUSHIDO("Bushido", KeywordWithAmount.class, false, "Whenever this creature blocks or becomes blocked, it gets +%1$d/+%1$d until end of turn."),
    BUYBACK("Buyback", KeywordWithCost.class, false, "You may pay an additional %s as you cast this spell. If you do, put it into your hand instead of your graveyard as it resolves."),
    CASCADE("Cascade", SimpleKeyword.class, false, "When you cast this spell, exile cards from the top of your library until you exile a nonland card whose mana value is less than this spell's mana value. You may cast that spell without paying its mana cost if its mana value is less than this spell's mana value. Then put all cards exiled this way that weren't cast on the bottom of your library in a random order."),
    CHAMPION("Champion", KeywordWithType.class, false, "When this enters the battlefield, sacrifice it unless you exile another %s you control. When this leaves the battlefield, that card returns to the battlefield."),
    CHANGELING("Changeling", SimpleKeyword.class, true, "This card is every creature type."),
    CIPHER("Cipher", SimpleKeyword.class, true, "Then you may exile this spell card encoded on a creature you control. Whenever that creature deals combat damage to a player, its controller may cast a copy of the encoded card without paying its mana cost."),
    COMPANION("Companion", Companion.class, true, "Reveal your companion from outside the game if your deck meets the companion restriction."),
    CONSPIRE("Conspire", SimpleKeyword.class, false, "As an additional cost to cast this spell, you may tap two untapped creatures you control that each share a color with it. If you do, copy it."),
    CONVOKE("Convoke", SimpleKeyword.class, true, "Your creatures can help cast this spell. Each creature you tap while playing this spell reduces its cost by {1} or by one mana of that creature's color."),
    CREW("Crew", KeywordWithAmount.class, false, "Tap any number of creatures you control with total power %1$d or more: This Vehicle becomes an artifact creature until end of turn."),
    CUMULATIVE_UPKEEP("Cumulative upkeep", KeywordWithCost.class, false, "At the beginning of your upkeep, put an age counter on this permanent, then sacrifice it unless you pay its upkeep cost for each age counter on it."),
    CYCLING("Cycling", KeywordWithCost.class, false, "%s, Discard this card: Draw a card."), //Typecycling reminder text handled by Cycling class
    DASH("Dash", KeywordWithCost.class, false, "You may cast this spell for its dash cost. If you do, it gains haste, and it's returned from the battlefield to its owner's hand at the beginning of the next end step."),
    DEATHTOUCH("Deathtouch", SimpleKeyword.class, true, "Any amount of damage this deals to a creature is enough to destroy it."),
    DEFENDER("Defender", SimpleKeyword.class, true, "This creature can't attack."),
    DELVE("Delve", SimpleKeyword.class, true, "As an additional cost to cast this spell, you may exile any number of cards from your graveyard. Each card exiled this way reduces the cost to cast this spell by {1}."),
    DEMONSTRATE("Demonstrate", SimpleKeyword.class, false, "When you cast this spell, you may copy it. If you do, choose an opponent to also copy it. Players may choose new targets for their copies."),
    DETHRONE("Dethrone", SimpleKeyword.class, false, "Whenever this creature attacks the player with the most life or tied for the most life, put a +1/+1 counter on it."),
    DEVOUR("Devour", KeywordWithAmount.class, false, "As this creature enters the battlefield, you may sacrifice any number of creatures. This creature enters the battlefield with {%d:+1/+1 counter} on it for each creature sacrificed this way."),
    DEVOID("Devoid", SimpleKeyword.class, true, "This card has no color."),
    DOUBLE_STRIKE("Double Strike", SimpleKeyword.class, true, "This creature deals both first-strike and regular combat damage."),
    DREDGE("Dredge", KeywordWithAmount.class, false, "If you would draw a card, instead you may put exactly {%d:card} from the top of your library into your graveyard. If you do, return this card from your graveyard to your hand. Otherwise, draw a card."),
    ECHO("Echo", KeywordWithCost.class, false, "At the beginning of your upkeep, if this permanent came under your control since the beginning of your last upkeep, sacrifice it unless you pay %s."),
    EMBALM("Embalm", KeywordWithCost.class, false, "%s, Exile this card from your graveyard: Create a token that's a copy of this card, except it's white, it has no mana cost, and it's a Zombie in addition to its other types. Embalm only as a sorcery."),
    EMERGE("Emerge", KeywordWithCost.class, false, "You may cast this spell by sacrificing a creature and paying the emerge cost reduced by that creature's mana value."),
    ENCHANT("Enchant", KeywordWithType.class, false, "Target a %s as you cast this. This card enters the battlefield attached to that %s."),
    ENCORE("Encore", KeywordWithCost.class, false, "%s, Exile this card from your graveyard: For each opponent, create a token copy that attacks that opponent this turn if able. They gain haste. Sacrifice them at the beginning of the next end step. Activate only as a sorcery."),
    ENTWINE("Entwine", KeywordWithCost.class, true, "You may choose all modes of this spell instead of just one. If you do, you pay an additional %s."),
    EPIC("Epic", SimpleKeyword.class, true, "For the rest of the game, you can't cast spells. At the beginning of each of your upkeeps for the rest of the game, copy this spell except for its epic ability. If the spell has any targets, you may choose new targets for the copy."),
    EQUIP("Equip", Equip.class, false, "%s: Attach to target %s you control. Equip only as a sorcery."),
    ESCAPE("Escape", KeywordWithCost.class, false, "You may cast this card from your graveyard for its escape cost."),
    ESCALATE("Escalate", KeywordWithCost.class, true, "Pay this cost for each mode chosen beyond the first."),
    ETERNALIZE("Eternalize", KeywordWithCost.class, false, "%s, Exile this card from your graveyard: Create a token that's a copy of this card, except it's black, it's 4/4, it has no mana cost, and it's a Zombie in addition to its other types. Eternalize only as a sorcery."),
    EVOKE("Evoke", KeywordWithCost.class, false, "You may cast this spell for its evoke cost. If you do, it's sacrificed when it enters the battlefield."),
    EVOLVE("Evolve", SimpleKeyword.class, false, "Whenever a creature enters the battlefield under your control, if that creature has greater power or toughness than this creature, put a +1/+1 counter on this creature."),
    EXALTED("Exalted", SimpleKeyword.class, false, "Whenever a creature you control attacks alone, that creature gets +1/+1 until end of turn."),
    EXERTED("Exerted", SimpleKeyword.class, true, "This creature won't untap during your next untap step."),
    EXPLOIT("Exploit", SimpleKeyword.class, false, "When this creature enters the battlefield, you may sacrifice a creature."),
    EXTORT("Extort", SimpleKeyword.class, false, "Whenever you cast a spell, you may pay {W/B}. If you do, each opponent loses 1 life and you gain that much life."),
    FABRICATE("Fabricate", KeywordWithAmount.class, false, "When this creature enters the battlefield, put {%1$d:+1/+1 counter} on it, or create {%1$d:1/1 colorless Servo artifact creature token}."),
    FADING("Fading", KeywordWithAmount.class, false, "This permanent enters the battlefield with {%d:fade counter} on it. At the beginning of your upkeep, remove a fade counter from it. If you can't, sacrifice it."),
    FEAR("Fear", SimpleKeyword.class, true, "This creature can't be blocked except by artifact creatures and/or black creatures."),
    FIRST_STRIKE("First Strike", SimpleKeyword.class, true, "This creature deals combat damage before creatures without first strike."),
    FLANKING("Flanking", SimpleKeyword.class, false, "Whenever this creature becomes blocked by a creature without flanking, the blocking creature gets -1/-1 until end of turn."),
    FLASH("Flash", SimpleKeyword.class, true, "You may cast this spell any time you could cast an instant."),
    FLASHBACK("Flashback", KeywordWithCost.class, false, "You may cast this card from your graveyard by paying %s rather than paying its mana cost. If you do, exile it as it resolves."),
    FLYING("Flying", SimpleKeyword.class, true, "This creature can't be blocked except by creatures with flying or reach."),
    FORETELL("Foretell", KeywordWithCost.class, false, "During your turn, you may pay {2} and exile this card from your hand face down. Cast it on a later turn for its foretell cost."),
    FORTIFY("Fortify", KeywordWithCost.class, false, "%s: Attach to target land you control. Fortify only as a sorcery."),
    FRENZY("Frenzy", KeywordWithAmount.class, false, "Whenever this creature attacks and isn't blocked, it gets +%d/+0 until end of turn."),
    FUSE("Fuse", SimpleKeyword.class, true, "You may cast one or both halves of this card from your hand."),
    GRAFT("Graft", KeywordWithAmount.class, false, "This permanent enters the battlefield with {%d:+1/+1 counter} on it. Whenever another creature enters the battlefield, you may move a +1/+1 counter from this permanent onto it."),
    GRAVESTORM("Gravestorm", SimpleKeyword.class, false, "When you cast this spell, copy it for each permanent that was put into a graveyard from the battlefield this turn. If the spell has any targets, you may choose new targets for any of the copies."),
    HASTE("Haste", SimpleKeyword.class, true, "This creature can attack and {T} as soon as it comes under your control."),
    HAUNT("Haunt", SimpleKeyword.class, false, "When this is put into a graveyard, exile it haunting target creature."),
    HEXPROOF("Hexproof", Hexproof.class, false, "This can't be the target of %s spells or abilities your opponents control."),
    HIDEAWAY("Hideaway", SimpleKeyword.class, false, "This land enters the battlefield tapped. When it does, look at the top four cards of your library, exile one face down, then put the rest on the bottom of your library."),
    HORSEMANSHIP("Horsemanship", SimpleKeyword.class, true, "This creature can't be blocked except by creatures with horsemanship."),
    IMPROVISE("Improvise", SimpleKeyword.class, true, "Your artifacts can help cast this spell. Each artifact you tap after you're done activating mana abilities pays for {1}."),
    INDESTRUCTIBLE("Indestructible", SimpleKeyword.class, true, "Effects that say \"destroy\" don’t destroy this."),
    INFECT("Infect", SimpleKeyword.class, true, "This creature deals damage to creatures in the form of -1/-1 counters and to players in the form of poison counters."),
    INGEST("Ingest", SimpleKeyword.class, false, "Whenever this creature deals combat damage to a player, that player exiles the top card of their library."),
    INTIMIDATE("Intimidate", SimpleKeyword.class, true, "This creature can't be blocked except by artifact creatures and/or creatures that share a color with it."),
    KICKER("Kicker", Kicker.class, false, "You may pay an additional %s as you cast this spell."),
    JUMP_START("Jump-start", SimpleKeyword.class, false, "You may cast this card from your graveyard by discarding a card in addition to paying its other costs. Then exile this card."),
    LANDWALK("Landwalk", KeywordWithType.class, false, "This creature is unblockable as long as defending player controls a %s."),
    LEVEL_UP("Level up", KeywordWithCost.class, false, "%s: Put a level counter on this. Level up only as a sorcery."),
    LIFELINK("Lifelink", SimpleKeyword.class, true, "Damage dealt by this creature also causes its controller to gain that much life."),
    LIVING_WEAPON("Living weapon", SimpleKeyword.class, true, "When this Equipment enters the battlefield, create a 0/0 black Germ creature token, then attach this to it."),
    MADNESS("Madness", KeywordWithCost.class, false, "If you discard this card, discard it into exile. When you do, cast it for %s  or put it into your graveyard."),
    MELEE("Melee", SimpleKeyword.class, false, "Whenever this creature attacks, it gets +1/+1 until end of turn for each opponent you attacked this combat."),
    MENTOR("Mentor", SimpleKeyword.class, false, "Whenever this creature attacks, put a +1/+1 counter on target attacking creature with lesser power."),
    MENACE("Menace", SimpleKeyword.class, true, "This creature can't be blocked except by two or more creatures."),
    MEGAMORPH("Megamorph", KeywordWithCost.class, false, "You may cast this card face down as a 2/2 creature for {3}. Turn it face up any time for its megamorph cost and put a +1/+1 counter on it."),
    MIRACLE("Miracle", KeywordWithCost.class, false, "You may cast this card for its miracle cost when you draw it if it's the first card you drew this turn."),
    // technically not a keyword but easier this way
    MONSTROSITY("Monstrosity", KeywordWithCostAndAmount.class, false, "If this creature isn't monstrous, put {%2$d:+1/+1 counter} on it and it becomes monstrous."),
    MODULAR("Modular", Modular.class, false, "This creature enters the battlefield with {%d:+1/+1 counter} on it. When it dies, you may put its +1/+1 counters on target artifact creature."),
    MORPH("Morph", KeywordWithCost.class, false, "You may cast this card face down as a 2/2 creature for {3}. Turn it face up any time for its morph cost."),
    MULTIKICKER("Multikicker", KeywordWithCost.class, false, "You may pay an additional %s any number of times as you cast this spell."),
    MUTATE("Mutate", KeywordWithCost.class, true, "If you cast this spell for its mutate cost, put it over or under target non-Human creature you own. They mutate into the creature on top plus all abilities from under it."),
    MYRIAD("Myriad", SimpleKeyword.class, false, "Whenever this creature attacks, for each opponent other than defending player, you may create a token that's a copy of this creature that's tapped and attacking that player or a planeswalker they control. Exile the tokens at end of combat."),
    NINJUTSU("Ninjutsu", Ninjutsu.class, false, "%s, Return an unblocked attacker you control to hand: Put this card onto the battlefield from your %s tapped and attacking."),
    OUTLAST("Outlast", KeywordWithCost.class, false, "%s, {T}: Put a +1/+1 counter on this creature. Outlast only as a sorcery."),
    OFFERING("Offering", KeywordWithType.class, false, "You may cast this card any time you could cast an instant by sacrificing a %1$s and paying the difference in mana costs between this and the sacrificed %1$s. Mana cost includes color."),
    PARTNER("Partner", Partner.class, true, "You can have two commanders if both have partner."),
    PERSIST("Persist", SimpleKeyword.class, false, "When this creature dies, if it had no -1/-1 counters on it, return it to the battlefield under its owner's control with a -1/-1 counter on it."),
    PHASING("Phasing", SimpleKeyword.class, true, "This phases in or out before you untap during each of your untap steps. While it's phased out, it's treated as though it doesn't exist."),
    POISONOUS("Poisonous", KeywordWithAmount.class, false, "Whenever this creature deals combat damage to a player, that player gets {%d:poison counter}."),
    PRESENCE("Presence", KeywordWithType.class, false, "As an additional cost to cast this spell, you may reveal a %s card from your hand."),
    PROTECTION("Protection", Protection.class, false, "This creature can't be blocked, targeted, dealt damage, or equipped/enchanted by %s."),
    PROVOKE("Provoke", SimpleKeyword.class, false, "Whenever this creature attacks, you may have target creature defending player controls untap and block it if able."),
    PROWESS("Prowess", SimpleKeyword.class, false, "Whenever you cast a noncreature spell, this creature gets +1/+1 until end of turn."),
    PROWL("Prowl", KeywordWithCost.class, false, "You may pay %s rather than pay this spell’s mana cost if a player was dealt combat damage this turn by a source that, at the time it dealt that damage, was under your control and had any of this spell’s creature types."),
    RAMPAGE("Rampage", KeywordWithAmount.class, false, "Whenever this creature becomes blocked, it gets +%1$d/+%1$d until end of turn for each creature blocking it beyond the first."),
    REACH("Reach", SimpleKeyword.class, true, "This creature can block creatures with flying."),
    REBOUND("Rebound", SimpleKeyword.class, true, "If you cast this spell from your hand, exile it as it resolves. At the beginning of your next upkeep, you may cast this card from exile without paying its mana cost."),
    RECOVER("Recover", KeywordWithCost.class, false, "When a creature is put into your graveyard from the battlefield, you may pay %s. If you do, return this card from your graveyard to your hand. Otherwise, exile this card."),
    REFLECT("Reflect", KeywordWithCost.class, false, "As this enters the battlefield, each opponent may pay %s. When they do, they create a token copy of this except it lacks this ability."),
    REINFORCE("Reinforce", KeywordWithCostAndAmount.class, false, "%s, Discard this card: Put {%d:+1/+1 counter} on target creature."),
    RENOWN("Renown", KeywordWithAmount.class, false, "When this creature deals combat damage to a player, if it isn't renowned, put {%d:+1/+1 counter} on it and it becomes renowned."),
    REPLICATE("Replicate", KeywordWithCost.class, false, "As an additional cost to cast this spell, you may pay %s any number of times. If you do, copy it that many times. You may choose new targets for the copies."),
    RETRACE("Retrace", SimpleKeyword.class, false, "You may cast this card from your graveyard by discarding a land card in addition to paying its other costs."),
    RIOT("Riot", SimpleKeyword.class, false, "This creature enters the battlefield with your choice of a +1/+1 counter or haste."),
    RIPPLE("Ripple", KeywordWithAmount.class, false, "When you cast this spell, you may reveal the top {%d:card} of your library. You may cast any of those cards with the same name as this spell without paying their mana costs. Put the rest on the bottom of your library in any order."),
    SHADOW("Shadow", SimpleKeyword.class, true, "This creature can block or be blocked by only creatures with shadow."),
    SHROUD("Shroud", SimpleKeyword.class, true, "This can't be the target of spells or abilities."),
    SKULK("Skulk", SimpleKeyword.class, true, "This creature can't be blocked by creatures with greater power."),
    SCAVENGE("Scavenge", KeywordWithCost.class, false, "%s, Exile this card from your graveyard: Put a number of +1/+1 counters equal to this card's power on target creature. Scavenge only as a sorcery."),
    SOULBOND("Soulbond", SimpleKeyword.class, true, "You may pair this creature with another unpaired creature when either enters the battlefield. They remain paired for as long as you control both of them."),
    SOULSHIFT("Soulshift", KeywordWithAmount.class, false, "When this creature dies, you may return target Spirit card with mana value %d or less from your graveyard to your hand."),
    SPECTACLE("Spectacle", KeywordWithCost.class, false, "You may cast this spell for its spectacle cost rather than its mana cost if an opponent lost life this turn."),
    SPLICE("Splice", KeywordWithCostAndType.class, false, "As you cast an %2$s spell, you may reveal this card from your hand and pay its splice cost. If you do, add this card's effects to that spell."),
    SPLIT_SECOND("Split second", SimpleKeyword.class, true, "As long as this spell is on the stack, players can't cast other spells or activate abilities that aren't mana abilities."),
    STORM("Storm", SimpleKeyword.class, false, "When you cast this spell, copy it for each other spell that was cast before it this turn. You may choose new targets for the copies."),
    STRIVE("Strive", KeywordWithCost.class, false, "CARDNAME costs %s more to cast for each target beyond the first."),
    SUNBURST("Sunburst", SimpleKeyword.class, false, "This enters the battlefield with either a +1/+1 or charge counter on it for each color of mana spent to cast it based on whether it's a creature."),
    SURGE("Surge", KeywordWithCost.class, false, "You may cast this spell for its surge cost if you or a teammate has cast another spell this turn."),
    SUSPEND("Suspend", Suspend.class, false, "Rather than cast this card from your hand, you may pay %s and exile it with {%d:time counter} on it. At the beginning of your upkeep, remove a time counter. When the last is removed, cast it without paying its mana cost."),
    TOTEM_ARMOR("Totem armor", SimpleKeyword.class, true, "If enchanted permanent would be destroyed, instead remove all damage marked on it and destroy this Aura."),
    TRAMPLE("Trample", SimpleKeyword.class, true, "This creature can deal excess combat damage to the player or planeswalker it's attacking."),
    TRANSFIGURE("Transfigure", KeywordWithCost.class, false, "%s, Sacrifice this creature: Search your library for a creature card with the same mana value as this creature and put that card onto the battlefield, then shuffle. Transfigure only as a sorcery."),
    TRANSMUTE("Transmute", KeywordWithCost.class, false, "%s, Discard this card: Search your library for a card with the same mana value as this card, reveal it, and put it into your hand, then shuffle. Transmute only as a sorcery."),
    TRIBUTE("Tribute", KeywordWithAmount.class, false, "As this creature enters the battlefield, an opponent of your choice may put {%d:+1/+1 counter} on it."),
    TYPECYCLING("TypeCycling", KeywordWithCostAndType.class, false, "%s, Discard this card: Search your library for a %s card, reveal it, put it into your hand, then shuffle."),
    UNDAUNTED("Undaunted", SimpleKeyword.class, false, "This spell costs {1} less to cast for each opponent."),
    UNDYING("Undying", SimpleKeyword.class, false, "When this creature dies, if it had no +1/+1 counters on it, return it to the battlefield under its owner's control with a +1/+1 counter on it."),
    UNEARTH("Unearth", KeywordWithCost.class, false, "%s: Return this card from your graveyard to the battlefield. It gains haste. Exile it at the beginning of the next end step or if it would leave the battlefield. Unearth only as a sorcery."),
    UNLEASH("Unleash", SimpleKeyword.class, false, "You may have this creature enter the battlefield with a +1/+1 counter on it. It can't block as long as it has a +1/+1 counter on it."),
    VANISHING("Vanishing", KeywordWithAmount.class, false, "This permanent enters the battlefield with {%d:time counter} on it. At the beginning of your upkeep, remove a time counter from it. When the last is removed, sacrifice it."),
    VIGILANCE("Vigilance", SimpleKeyword.class, true, "Attacking doesn't cause this creature to tap."),
    WARD("Ward", KeywordWithCost.class, false, "Whenever this permanent becomes the target of a spell or ability an opponent controls, counter it unless that player pays %s."),
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

    public static KeywordInterface getInstance(String k) {
        Keyword keyword = Keyword.UNDEFINED;
        String details = k;
        // try to get real part
        if (k.contains(":")) {
            final String[] x = k.split(":", 2);
            keyword = smartValueOf(x[0]);
            details = x[1];
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

        if (keyword == Keyword.UNDEFINED) {
            //check for special keywords that have a prefix before the keyword enum name
            int idx = k.indexOf(' ');
            String enumName = k.replace(" ", "_").toUpperCase(Locale.ROOT);
            String firstWord = idx == -1 ? enumName : enumName.substring(0, idx);
            if (firstWord.endsWith("WALK")) {
                keyword = Keyword.LANDWALK;
                details = firstWord.substring(0, firstWord.length() - 4);
            }
            else if (idx != -1) {
                idx = k.indexOf(' ', idx + 1);
                String secondWord = idx == -1 ? enumName.substring(firstWord.length() + 1) : enumName.substring(firstWord.length() + 1, idx);
                if (secondWord.equalsIgnoreCase("OFFERING")) {
                    keyword = Keyword.OFFERING;
                    details = firstWord;
                }
                else if (secondWord.equalsIgnoreCase("LANDWALK")) {
                    keyword = Keyword.LANDWALK;
                    details = firstWord;
                }
            }
        }
        KeywordInstance<?> inst;
        try {
            inst = keyword.type.getConstructor().newInstance();
        }
        catch (Exception e) {
            inst = new UndefinedKeyword();
        }
        inst.initialize(k, keyword, details);
        return inst;
    }

    @Override
    public String toString() {
        return displayName;
    }

    public static List<Keyword> getAllKeywords() {
        Keyword[] values = values();
        List<Keyword> keywords = new ArrayList<>();
        for (int i = 1; i < values.length; i++) { //skip UNDEFINED
            keywords.add(values[i]);
        }
        return keywords;
    }

    private static final Map<String, Set<Keyword>> cardKeywordSetLookup = new HashMap<>();

    public static Set<Keyword> getKeywordSet(PaperCard card) {
        String key = card.getName();
        Set<Keyword> keywordSet = cardKeywordSetLookup.get(key);
        if (keywordSet == null) {
            keywordSet = new HashSet<>();
            for (KeywordInterface inst : Card.getCardForUi(card).getKeywords()) {
                final Keyword keyword = inst.getKeyword();
                if (keyword != Keyword.UNDEFINED) {
                    keywordSet.add(keyword);
                }
            }
            cardKeywordSetLookup.put(card.getName(), keywordSet);
        }
        return keywordSet;
    }

    public static Runnable getPreloadTask() {
        if (cardKeywordSetLookup.size() < 10000) { //allow preloading even if some but not all cards loaded
            return new Runnable() {
                @Override
                public void run() {
                    final Collection<PaperCard> cards = StaticData.instance().getCommonCards().getUniqueCards();
                    for (PaperCard card : cards) {
                        getKeywordSet(card);
                    }
                }
            };
        }
        return null;
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
}
