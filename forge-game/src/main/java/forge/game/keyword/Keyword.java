package forge.game.keyword;

public enum Keyword {
    UNDEFINED(SimpleKeyword.class, false, ""),
    ABSORB(KeywordWithAmount.class, false, "If a source would deal damage to this creature, prevent %d of that damage."),
    AFFINITY(KeywordWithType.class, false, "This spell costs you {1} less to cast for each %s you control."),
    AMPLIFY(KeywordWithAmount.class, false, "As this card enters the battlefield, reveal any number of cards from your hand that share a creature type with it. This permanent enters the battlefield with %d +1/+1 counters on it for each card revealed this way. You can't reveal this card or any other cards that are entering the battlefield at the same time as this card."),
    ANNIHILATOR(KeywordWithAmount.class, false, "Whenever this creature attacks, defending player sacrifices %d permanents."),
    AURA_SWAP(KeywordWithCost.class, false, "%s: You may exchange this permanent with an Aura card in your hand."),
    BANDING(SimpleKeyword.class, true, "Any creatures with banding, and up to one without, can attack in a band. Bands are blocked as a group. If any creatures with banding you control are blocking or being blocked by a creature, you divide that creature's combat damage, not its controller, among any of the creatures it's being blocked by or is blocking."),
    BATTLE_CRY(KeywordWithAmount.class, false, "Whenever this creature attacks, each other attacking creature gets +1/+0 until end of turn."),
    BLOODTHIRST(KeywordWithAmount.class, false, "If an opponent was dealt damage this turn, this permanent enters the battlefield with %d +1/+1 counters on it."),
    BUSHIDO(KeywordWithAmount.class, false, "Whenever this creature blocks or becomes blocked, it gets +N/+N until end of turn."),
    BUYBACK(KeywordWithCost.class, false, "You may pay an additional %s as you cast this spell. If you do, put it into your hand instead of your graveyard as it resolves."),
    CASCADE(SimpleKeyword.class, false, "When you cast this spell, exile cards from the top of your library until you exile a nonland card whose converted mana cost is less than this spell's converted mana cost. You may cast that card without paying its mana cost. Then put all cards exiled this way that weren't cast on the bottom of your library in a random order."),
    CHAMPION(KeywordWithType.class, false, "When this permanent enters the battlefield, sacrifice it unless you exile another %s you control. When this permanent leaves the battlefield, return the exiled card to the battlefield under its owner's control."),
    CHANGELING(SimpleKeyword.class, true, "This card is every creature type at all times."),
    CONSPIRE(SimpleKeyword.class, false, "As an additional cost to cast this spell, you may tap two untapped creatures you control that each share a color with it. If you do, copy it."),
    CONVOKE(SimpleKeyword.class, true, "Each creature you tap while playing this spell reduces its cost by {1} or by one mana of that creature's color."),
    CUMULATIVE_UPKEEP(KeywordWithCost.class, false, "At the beginning of your upkeep, put an age counter on this permanent. Then you may pay %s for each age counter on it. If you don't, sacrifice it."),
    CYCLING(Cycling.class, false, "%s, Discard this card: Draw a card."), //Typecycling reminder text handled by Cycling class
    DEATHTOUCH(SimpleKeyword.class, true, "Any amount of damage this deals to a creature is enough to destroy it."),
    DEFENDER(SimpleKeyword.class, true, "This creature can't attack."),
    DELVE(SimpleKeyword.class, true, "As an additional cost to cast this spell, you may exile any number of cards from your graveyard. Each card exiled this way reduces the cost to cast this spell by {1}."),
    DEVOUR(KeywordWithAmount.class, true, "As this creature enters the battlefield, you may sacrifice any number of creatures. This creature enters the battlefield with %d +1/+1 counters on it for each creature sacrificed this way."),
    DOUBLE_STRIKE(SimpleKeyword.class, true, "This creature deals both first-strike and regular combat damage."),
    DREDGE(KeywordWithAmount.class, true, "If you would draw a card, you may instead put %d cards from the top of your library into your graveyard and return this card from your graveyard to your hand."),
    ECHO(KeywordWithCost.class, false, "At the beginning of your upkeep, if this permanent came under your control since the beginning of your last upkeep, sacrifice it unless you pay %s."),
    ENCHANT(KeywordWithType.class, false, "Target a %s as you play this. This card enters the battlefield attached to that %s."),
    ENTWINE(KeywordWithCost.class, true, "You may choose all modes of this spell instead of just one. If you do, you pay an additional %s."),
    EPIC(SimpleKeyword.class, true, "For the rest of the game, you can't cast spells. At the beginning of each of your upkeeps for the rest of the game, copy this spell except for its epic ability. If the spell has any targets, you may choose new targets for the copy."),
    EQUIP(KeywordWithCost.class, false, "%s: Attach this permanent to target creature you control. Activate this ability only any time you could cast a sorcery."),
    EVOKE(KeywordWithCost.class, false, "You may cast this card by paying %s rather than paying its mana cost. If you do, it's sacrificed when it enters the battlefield."),
    EXALTED(KeywordWithAmount.class, false, "Whenever a creature you control attacks alone, that creature gets +1/+1 until end of turn."),
    FADING(KeywordWithAmount.class, false, "This permanent enters the battlefield with %d fade counters on it. At the beginning of your upkeep, remove a fade counter from it. If you can't, sacrifice it."),
    FATESEAL(KeywordWithAmount.class, false, "Look at the top %d cards of an opponent's library, then put any number of them on the bottom of that player's library and the rest on top in any order."),
    FEAR(SimpleKeyword.class, true, "This creature can't be blocked except by artifact creatures and/or black creatures."),
    FIRST_STRIKE(SimpleKeyword.class, true, "This creature deals combat damage before creatures without first strike."),
    FLANKING(SimpleKeyword.class, false, "Whenever this creature becomes blocked by a creature without flanking, the blocking creature gets -1/-1 until end of turn."),
    FLASH(SimpleKeyword.class, true, "You may play this card any time you could cast an instant."),
    FLASHBACK(KeywordWithCost.class, false, "You may cast this card from your graveyard by paying %s rather than paying its mana cost. If you do, exile it as it resolves."),
    FLYING(SimpleKeyword.class, true, "This creature can't be blocked except by creatures with flying or reach."),
    FORECAST(KeywordWithCost.class, false, "Play this ability only during your upkeep and only once each turn."),
    FORTIFY(KeywordWithCost.class, false, "%s: Attach this permanent to target land you control. Activate this ability only any time you could cast a sorcery."),
    FRENZY(KeywordWithAmount.class, false, "Whenever this creature attacks and isn't blocked, it gets +%d/+0 until end of turn."),
    GRAFT(KeywordWithAmount.class, false, "This permanent enters the battlefield with %d +1/+1 counters on it. Whenever another creature enters the battlefield, you may move a +1/+1 counter from this permanent onto it."),
    GRAVESTORM(SimpleKeyword.class, false, "When you cast this spell, copy it for each permanent that was put into a graveyard from the battlefield this turn. You may choose new targets for the copies."),
    HASTE(SimpleKeyword.class, true, "This creature can attack and {T} as soon as it comes under your control."),
    HAUNT(SimpleKeyword.class, false, "When this is put into a graveyard, exile it haunting target creature."),
    HEXPROOF(SimpleKeyword.class, true, "This can't be the target of spells or abilities your opponents control."),
    HIDEAWAY(SimpleKeyword.class, false, "This permanent enters the battlefield tapped. When it does, look at the top four cards of your library, exile one of them face down, then put the rest on the bottom of your library in any order."),
    HORSEMANSHIP(SimpleKeyword.class, true, "This creature can't be blocked except by creatures with horsemanship."),
    INFECT(SimpleKeyword.class, true, " This creature deals damage to creatures in the form of -1/-1 counters and to players in the form of poison counters."),
    INTIMIDATE(SimpleKeyword.class, true, "This creature can't be blocked except by artifact creatures and/or creatures that share a color with it."),
    KICKER(KeywordWithCost.class, false, "You may pay an additional %s as you cast this spell."),
    LANDWALK(KeywordWithType.class, false, "This creature is unblockable as long as defending player controls a %s."),
    LEVELUP(KeywordWithCost.class, false, "%s: Put a level counter on this permanent. Activate this ability only any time you could cast a sorcery."),
    LIFELINK(SimpleKeyword.class, true, "Damage dealt by this creature also causes its controller to gain that much life."),
    LIVING_WEAPON(SimpleKeyword.class, true, "When this Equipment enters the battlefield, put a 0/0 black Germ creature token onto the battlefield, then attach this Equipment to it."),
    MADNESS(KeywordWithCost.class, true, "If you discard this card, you may cast it for %s instead of putting it into your graveyard."),
    MODULAR(KeywordWithAmount.class, false, "This creature enters the battlefield with %d +1/+1 counters on it. When it dies, you may put its +1/+1 counters on target artifact creature."),
    MORPH(KeywordWithCost.class, true, "You may cast this card face down as a 2/2 creature for {3}. You may pay %s at any time to turn it face up."),
    MULTIKICKER(KeywordWithCost.class, false, "You may pay an additional %s any number of times as you cast this spell."),
    NINJUTSU(KeywordWithCost.class, false, "%s, Reveal this card from your hand, Return an unblocked attacking creature you control to its owner's hand: Put this card onto the battlefield from your hand tapped and attacking."),
    OFFERING(KeywordWithType.class, false, "You may cast this card any time you could cast an instant by sacrificing a %s. If you do, the total cost to cast this card is reduced by the mana cost of the sacrificed %s."),
    PERSIST(SimpleKeyword.class, true, "When this permanent is put into a graveyard from the battlefield, if it had no -1/-1 counters on it, return it to the battlefield under its owner's control with a -1/-1 counter on it."),
    PHASING(SimpleKeyword.class, true, "This phases in or out before you untap during each of your untap steps. While it's phased out, it's treated as though it doesn't exist."),
    POISONOUS(KeywordWithAmount.class, false, "Whenever this creature deals combat damage to a player, that player gets %d poison counters."),
    PROTECTION(Protection.class, false, "This creature can't be blocked, targeted, dealt damage, or equipped/enchanted by %s."),
    PROVOKE(SimpleKeyword.class, false, "Whenever this creature attacks, you may choose to have target creature defending player controls block this creature this combat if able. If you do, untap that creature."),
    PROWL(KeywordWithCost.class, false, "You may cast this card by paying %s rather than paying its mana cost if you dealt combat damage to a player this turn with a creature that shares any of this spell's creature types."),
    RAMPAGE(KeywordWithAmount.class, false, "Whenever this creature becomes blocked, it gets +%d/+%d until end of turn for each creature blocking it beyond the first."),
    REACH(SimpleKeyword.class, true, "This creature can block creatures with flying."),
    REBOUND(SimpleKeyword.class, true, "If you cast this spell from your hand, exile it as it resolves. At the beginning of your next upkeep, you may cast this card from exile without paying its mana cost."),
    RECOVER(KeywordWithCost.class, false, "When a creature is put into your graveyard from the battlefield, you may pay %s. If you do, return this card from your graveyard to your hand. Otherwise, exile this card."),
    REINFORCE(KeywordWithCostAndAmount.class, false, "%s, Discard this card: Put %d +1/+1 counters on target creature."),
    REPLICATE(KeywordWithCost.class, false, "As an additional cost to cast this spell, you may pay %s any number of times. If you do, copy it that many times. You may choose new targets for the copies."),
    RETRACE(SimpleKeyword.class, true, "You may cast this card from your graveyard by discarding a land card in addition to paying its other costs."),
    RIPPLE(KeywordWithAmount.class, false, "When you cast this spell, you may reveal the top %d cards of your library. You may cast any of those cards with the same name as this spell without paying their mana costs. Put the rest on the bottom of your library in any order."),
    SHROUD(SimpleKeyword.class, true, "This can't be the target of spells or abilities."),
    SOULSHIFT(KeywordWithAmount.class, false, "When this permanent is put into a graveyard from the battlefield, you may return target Spirit card with converted mana cost %d or less from your graveyard to your hand."),
    SPLICE(KeywordWithCostAndType.class, false, "You may reveal this card from your hand as you cast a %s spell. If you do, copy this card's text box onto that spell and pay %s as an additional cost to cast that spell."),
    SPLIT_SECOND(SimpleKeyword.class, true, "As long as this spell is on the stack, players can't play other spells or abilities that aren't mana abilities."),
    STORM(SimpleKeyword.class, false, "When you cast this spell, copy it for each other spell that was cast before it this turn. You may choose new targets for the copies."),
    SUNBURST(SimpleKeyword.class, false, "This enters the battlefield with either a +1/+1 or charge counter on it for each color of mana spent to cast it based on whether it's a creature."),
    SUSPEND(KeywordWithCostAndAmount.class, false, "Rather than cast this card from your hand, you may pay %s and exile it with %d time counters on it. At the beginning of your upkeep, remove a time counter. When the last is removed, cast it without paying its mana cost."),
    TOTEM_ARMOR(SimpleKeyword.class, true, "If enchanted permanent would be destroyed, instead remove all damage marked on it and destroy this Aura."),
    TRAMPLE(SimpleKeyword.class, true, "If this creature would assign enough damage to its blockers to destroy them, you may have it assign the rest of its damage to defending player or planeswalker."),
    TRANSFIGURE(KeywordWithCost.class, false, "%s, Sacrifice this permanent: Search your library for a creature card with the same converted mana cost as this permanent and put it onto the battlefield. Then shuffle your library. Activate this ability only any time you could cast a sorcery."),
    TRANSMUTE(KeywordWithCost.class, false, "%s, Discard this card: Search your library for a card with the same converted mana cost as the discarded card, reveal that card, and put it into your hand. Then shuffle your library. Activate this ability only any time you could cast a sorcery."),
    UNEARTH(KeywordWithCost.class, false, "%s: Return this card from your graveyard to the battlefield. It gains haste. Exile it at the beginning of the next end step. If it would leave the battlefield, exile it instead of putting it anywhere else. Activate this ability only any time you could cast a sorcery."),
    VANISHING(KeywordWithAmount.class, false, "This permanent enters the battlefield with %d time counters on it. At the beginning of your upkeep, remove a time counter from it. When the last is removed, sacrifice it."),
    VIGILANCE(SimpleKeyword.class, true, "Attacking doesn't cause this creature to tap."),
    WITHER(SimpleKeyword.class, true, "This creature deals damage to creatures in the form of -1/-1 counters.");

    protected final Class<? extends KeywordInstance<?>> type;
    protected final boolean isMultipleRedundant;
    protected final String reminderText;

    private Keyword(Class<? extends KeywordInstance<?>> type0, boolean isMultipleRedundant0, String reminderText0) {
        type = type0;
        isMultipleRedundant = isMultipleRedundant0;
        reminderText = reminderText0;
    }

    public static KeywordInstance<?> getInstance(String k) {
        Keyword keyword = Keyword.UNDEFINED;
        String details = k;
        String enumName = k.replace(' ', '_').toUpperCase();
        for (Keyword kw : Keyword.values()) {
            if (enumName.startsWith(kw.name())) {
                keyword = kw;
                int idx = kw.name().length() + 1;
                if (idx < k.length()) {
                    details = k.substring(idx);
                }
                else {
                    details = "";
                }
                break;
            }
        }
        if (keyword == Keyword.UNDEFINED) {
            //check for special keywords that have a prefix before the keyword enum name
            int idx = k.indexOf(' ');
            String firstWord = idx == -1 ? enumName : enumName.substring(0, idx);
            if (firstWord.endsWith("CYCLING")) {
                //handle special case of Typecycling
                idx = firstWord.length() + 1;
                if (idx < k.length()) {
                    details = k.substring(idx);
                }
                else {
                    details = "";
                }
                return new Cycling(firstWord.substring(0, firstWord.length() - 7), details);
            }
            else if (firstWord.endsWith("WALK")) {
                keyword = Keyword.LANDWALK;
                details = firstWord.substring(0, firstWord.length() - 4);
            }
            else if (idx != -1) {
                idx = k.indexOf(' ', idx + 1);
                String secondWord = idx == -1 ? enumName.substring(firstWord.length() + 1) : enumName.substring(firstWord.length() + 1, idx);
                if (secondWord.equals("OFFERING")) {
                    keyword = Keyword.OFFERING;
                    details = firstWord;
                }
                else if (secondWord.equals("LANDWALK")) {
                    keyword = Keyword.LANDWALK;
                    details = firstWord;
                }
            }
        }
        KeywordInstance<?> inst;
        try {
            inst = keyword.type.newInstance();
        }
        catch (Exception e) {
            inst = new UndefinedKeyword();
        }
        inst.initialize(keyword, details);
        return inst;
    }
}
