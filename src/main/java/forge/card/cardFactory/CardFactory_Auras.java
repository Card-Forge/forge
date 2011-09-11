package forge.card.cardFactory;


import com.esotericsoftware.minlog.Log;
import forge.AllZone;
import forge.AllZoneUtil;
import forge.Card;
import forge.CardList;
import forge.CardListFilter;
import forge.CardListUtil;
import forge.CardUtil;
import forge.Command;
import forge.Constant;
import forge.Player;
import forge.PlayerZone;
import forge.card.cost.Cost;
import forge.card.spellability.Ability;
import forge.card.spellability.Spell;
import forge.card.spellability.SpellAbility;
import forge.card.spellability.Spell_Permanent;
import forge.card.spellability.Target;
import forge.gui.GuiUtils;
import forge.gui.input.Input;

import java.util.ArrayList;
import java.util.HashMap;


/**
 * <p>CardFactory_Auras class.</p>
 *
 * @author Forge
 * @version $Id$
 */
class CardFactory_Auras {

    /**
     * <p>shouldCycle.</p>
     *
     * @param c a {@link forge.Card} object.
     * @return a int.
     */
    public static final int shouldCycle(final Card c) {
        ArrayList<String> a = c.getKeyword();
        for (int i = 0; i < a.size(); i++) {
            if (a.get(i).toString().startsWith("Cycling")) {
                return i;
            }
        }
        return -1;
    }

    /**
     * <p>getCard.</p>
     *
     * @param card a {@link forge.Card} object.
     * @param cardName a {@link java.lang.String} object.
     * @param owner a {@link forge.Player} object.
     * @return a {@link forge.Card} object.
     */
    public static Card getCard(final Card card, final String cardName, final Player owner) {

        //*************** START *********** START **************************
        if (cardName.equals("Convincing Mirage") || cardName.equals("Phantasmal Terrain")
                || cardName.equals("Spreading Seas")
                || cardName.equals("Lingering Mirage") || cardName.equals("Sea's Claim"))
        {

            final String[] newType = new String[1];
            final SpellAbility spell = new Spell(card) {

                private static final long serialVersionUID = 53941812202244498L;

                @Override
                public boolean canPlayAI() {

                    if (!super.canPlayAI()) {
                        return false;
                    }

                    if (card.getName().equals("Spreading Seas")
                            || card.getName().equals("Lingering Mirage")
                            || card.getName().equals("Sea's Claim")
                            || card.getName().equals("Phantasmal Terrain"))
                    {
                        newType[0] = "Island";
                    } else if (card.getName().equals("Convincing Mirage")
                            || card.getName().equals("Phantasmal Terrain"))
                    {
                        String[] landTypes = new String[]{"Plains", "Island", "Swamp", "Mountain", "Forest"};
                        HashMap<String, Integer> humanLandCount = new HashMap<String, Integer>();
                        CardList humanlands = AllZoneUtil.getPlayerLandsInPlay(AllZone.getHumanPlayer());

                        for (int i = 0; i < landTypes.length; i++) {
                            humanLandCount.put(landTypes[i], 0);
                        }

                        for (Card c : humanlands) {
                            for (String singleType : c.getType()) {
                                if (CardUtil.isABasicLandType(singleType)) {
                                    humanLandCount.put(singleType, humanLandCount.get(singleType) + 1);
                                }
                            }
                        }

                        int minAt = 0;
                        int minVal = Integer.MAX_VALUE;
                        for (int i = 0; i < landTypes.length; i++) {
                            if (getTargetCard().isType(landTypes[i])) {
                                continue;
                            }

                            if (humanLandCount.get(landTypes[i]) < minVal) {
                                minVal = humanLandCount.get(landTypes[i]);
                                minAt = i;
                            }
                        }

                        newType[0] = landTypes[minAt];
                    }
                    CardList list = AllZoneUtil.getPlayerLandsInPlay(AllZone.getHumanPlayer());
                    list = list.getNotType(newType[0]); // Don't enchant lands that already have the type
                    if (list.isEmpty()) {
                        return false;
                    }
                    setTargetCard(list.get(0));
                    return true;
                } //canPlayAI()

                @Override
                public void resolve() {
                    if (card.getName().equals("Spreading Seas")
                            || card.getName().equals("Lingering Mirage")
                            || card.getName().equals("Sea's Claim"))
                    {
                        newType[0] = "Island";
                    } else if (card.getName().equals("Convincing Mirage")
                            || card.getName().equals("Phantasmal Terrain"))
                    {
                        //Only query player, AI will have decided already.
                        if (card.getController().isHuman()) {
                            newType[0] = GuiUtils.getChoice("Select land type.",
                                    "Plains", "Island", "Swamp", "Mountain", "Forest");
                        }
                    }
                    AllZone.getGameAction().moveToPlay(card);

                    Card c = getTargetCard();

                    if (AllZoneUtil.isCardInPlay(c)
                            && CardFactoryUtil.canTarget(card, c))
                    {
                        card.enchantCard(c);
                    }

                } //resolve()
            }; //SpellAbility


            spell.setDescription("");
            card.addSpellAbility(spell);

            // Need to set the spell description for Lingering Mirage since it has cycling ability.
            if (card.getName().equals("Lingering Mirage")) {
                spell.setDescription("Enchanted land is an Island.");
            }

            Command onEnchant = new Command() {

                private static final long serialVersionUID = 3528675112863241126L;

                public void execute() {
                    if (card.isEnchanting()) {
                        Card crd = card.getEnchanting().get(0);
                        ArrayList<Card> seas = crd.getEnchantedBy();
                        int count = 0;
                        for (int i = 0; i < seas.size(); i++) {
                            if (seas.get(i).getName().equals(card.getName())) {
                                count = count + 1;
                            }
                        }
                        if (count == 1) {
                            crd.removeType("Swamp");
                            crd.removeType("Forest");
                            crd.removeType("Island");
                            crd.removeType("Plains");
                            crd.removeType("Mountain");
                            crd.removeType("Locus");
                            crd.removeType("Lair");

                            crd.addType(newType[0]);
                        } else {
                            Card otherSeas = null;
                            for (int i = 0; i < seas.size(); i++) {
                                if (seas.get(i) != card) {
                                    otherSeas = seas.get(i);
                                }
                            }
                            SpellAbility[] abilities = otherSeas.getSpellAbility();
                            for (int i = 0; i < abilities.length; i++) {
                                card.addSpellAbility(abilities[i]);
                            }
                        }
                    }
                } //execute()
            }; //Command

            Command onUnEnchant = new Command() {
                private static final long serialVersionUID = -202144631191180334L;

                public void execute() {
                    if (card.isEnchanting()) {
                        Card crd = card.getEnchanting().get(0);
                        ArrayList<Card> seas = crd.getEnchantedBy();
                        int count = 0;
                        for (int i = 0; i < seas.size(); i++) {
                            if (seas.get(i).getName().equals(card.getName())) {
                                count = count + 1;
                            }
                        }
                        if (count == 1) {
                            crd.removeType(newType[0]);
                            crd.removeType("Land");
                            crd.removeType("Basic");
                            crd.removeType("Snow");
                            crd.removeType("Legendary");
                            SpellAbility[] cardAbilities = crd.getSpellAbility();
                            for (int i = 0; i < cardAbilities.length; i++) {
                                if (cardAbilities[i].isIntrinsic()) {
                                    crd.removeSpellAbility(cardAbilities[i]);
                                }
                            }
                            Card c = AllZone.getCardFactory().copyCard(crd);
                            ArrayList<String> types = c.getType();
                            SpellAbility[] abilities = card.getSpellAbility();
                            for (int i = 0; i < types.size(); i++) {
                                crd.addType(types.get(i));
                            }
                            for (int i = 0; i < abilities.length; i++) {
                                crd.addSpellAbility(abilities[i]);
                            }
                        }
                    }
                } //execute()
            }; //Command

            Command onLeavesPlay = new Command() {

                private static final long serialVersionUID = -45433022112460839L;

                public void execute() {
                    if (card.isEnchanting()) {
                        Card crd = card.getEnchanting().get(0);
                        card.unEnchantCard(crd);
                    }
                }
            };

            card.addEnchantCommand(onEnchant);
            card.addUnEnchantCommand(onUnEnchant);
            card.addLeavesPlayCommand(onLeavesPlay);

            Input runtime = new Input() {

                private static final long serialVersionUID = -62372711146079880L;

                @Override
                public void showMessage() {
                    CardList land = AllZoneUtil.getLandsInPlay();
                    stopSetNext(CardFactoryUtil.input_targetSpecific(spell, land, "Select target land", true,
                            false));
                }
            };
            spell.setBeforePayMana(runtime);
        } //*************** END ************ END **************************


        //*************** START *********** START **************************
        else if (cardName.equals("Earthbind")) {
            Cost cost = new Cost(card.getManaCost(), cardName, false);
            Target tgt = new Target(card, "C");
            final SpellAbility spell = new Spell_Permanent(card, cost, tgt) {

                private static final long serialVersionUID = 142389375702113977L;

                @Override
                public boolean canPlayAI() {
                    CardList list = AllZoneUtil.getCreaturesInPlay(AllZone.getHumanPlayer());
                    list = list.getKeyword("Flying");
                    if (list.isEmpty()) {
                        return false;
                    }

                    CardListFilter f = new CardListFilter() {
                        public final boolean addCard(final Card c) {
                            return c.getNetDefense() - c.getDamage() <= 2;
                        }
                    };
                    if (!list.filter(f).isEmpty()) {
                        list = list.filter(f);
                    }
                    CardListUtil.sortAttack(list);

                    for (int i = 0; i < list.size(); i++) {
                        if (CardFactoryUtil.canTarget(card, list.get(i))) {
                            setTargetCard(list.get(i));
                            return super.canPlayAI();
                        }
                    }
                    return false;
                } //canPlayAI()

                @Override
                public void resolve() {
                    AllZone.getGameAction().moveToPlay(card);

                    Card c = getTargetCard();

                    if (AllZoneUtil.isCardInPlay(c)
                            && CardFactoryUtil.canTarget(card, c))
                    {
                        card.enchantCard(c);
                        Log.debug("Enchanted: " + getTargetCard());
                    }
                } //resolve()
            }; //SpellAbility

            card.addSpellAbility(spell);

            final boolean[] badTarget = {true};
            Command onEnchant = new Command() {

                private static final long serialVersionUID = -5302506578307993978L;

                public void execute() {
                    if (card.isEnchanting()) {
                        Card crd = card.getEnchanting().get(0);
                        if (crd.hasKeyword("Flying")) {
                            badTarget[0] = false;
                            crd.addDamage(2, card);
                            crd.removeIntrinsicKeyword("Flying");
                            crd.removeExtrinsicKeyword("Flying");
                        } else {
                            badTarget[0] = true;
                        }
                    }
                } //execute()
            }; //Command

            Command onUnEnchant = new Command() {

                private static final long serialVersionUID = -6908757692588823391L;

                public void execute() {
                    if (card.isEnchanting()
                            && !badTarget[0])
                    {
                        Card crd = card.getEnchanting().get(0);
                        crd.addIntrinsicKeyword("Flying");
                    }
                } //execute()
            }; //Command

            Command onLeavesPlay = new Command() {

                private static final long serialVersionUID = -7833240882415702940L;

                public void execute() {
                    if (card.isEnchanting()) {
                        Card crd = card.getEnchanting().get(0);
                        card.unEnchantCard(crd);
                    }
                }
            };

            card.addEnchantCommand(onEnchant);
            card.addUnEnchantCommand(onUnEnchant);
            card.addLeavesPlayCommand(onLeavesPlay);
        } //*************** END ************ END **************************


        //*************** START *********** START **************************
        else if (cardName.equals("Guilty Conscience")) {
            Cost cost = new Cost(card.getManaCost(), cardName, false);
            Target tgt = new Target(card, "C");
            final SpellAbility spell = new Spell_Permanent(card, cost, tgt) {

                private static final long serialVersionUID = 1169151960692309514L;

                @Override
                public boolean canPlayAI() {

                    CardList stuffy = AllZoneUtil.getPlayerCardsInPlay(AllZone.getComputerPlayer(), "Stuffy Doll");

                    if (stuffy.size() > 0) {
                        setTargetCard(stuffy.get(0));
                        return true;
                    } else {
                        CardList list = AllZoneUtil.getCreaturesInPlay(AllZone.getHumanPlayer());

                        if (list.isEmpty()) {
                            return false;
                        }

                        //else
                        CardListUtil.sortAttack(list);
                        CardListUtil.sortFlying(list);

                        for (int i = 0; i < list.size(); i++) {
                            if (CardFactoryUtil.canTarget(card, list.get(i))
                                    && (list.get(i).getNetAttack() >= list.get(i).getNetDefense())
                                    && list.get(i).getNetAttack() >= 3)
                            {
                                setTargetCard(list.get(i));
                                return super.canPlayAI();
                            }
                        }
                    }
                    return false;

                } //canPlayAI()

                @Override
                public void resolve() {
                    Card aura = AllZone.getGameAction().moveToPlay(card);

                    Card c = getTargetCard();

                    if (AllZoneUtil.isCardInPlay(c)
                            && CardFactoryUtil.canTarget(aura, c))
                    {
                        aura.enchantCard(c);
                    }
                } //resolve()
            }; //SpellAbility

            card.addSpellAbility(spell);
        } //*************** END ************ END **************************


        //*************** START *********** START **************************
        else if (cardName.equals("Animate Dead") || cardName.equals("Dance of the Dead")) {
            final Card[] targetC = new Card[1];
            // need to override what happens when this is cast.
            final Spell_Permanent animate = new Spell_Permanent(card) {
                private static final long serialVersionUID = 7126615291288065344L;

                public CardList getCreturesInGrave() {
                    // This includes creatures Animate Dead can't enchant once in play.
                    // The human may try to Animate them, the AI will not.
                    CardList cList = AllZoneUtil.getCardsInGraveyard();
                    cList = cList.getType("Creature");
                    return cList;
                }

                public boolean canPlay() {
                    return super.canPlay() && getCreturesInGrave().size() != 0;
                }

                @Override
                public boolean canPlayAI() {
                    CardList cList = getCreturesInGrave();
                    // AI will only target something that will stick in play.
                    cList = cList.filter(new CardListFilter() {
                        public final boolean addCard(final Card crd) {
                            return CardFactoryUtil.canTarget(card, crd)
                                        && !CardFactoryUtil.hasProtectionFrom(card, crd);
                        }
                    });
                    if (cList.size() == 0) {
                        return false;
                    }

                    Card c = CardFactoryUtil.AI_getBestCreature(cList);

                    setTargetCard(c);
                    boolean playable = 2 < c.getNetAttack() && 2 < c.getNetDefense() && super.canPlayAI();
                    return playable;
                } //canPlayAI

                @Override
                public void resolve() {
                    targetC[0] = getTargetCard();
                    super.resolve();
                }

            }; //addSpellAbility

            // Target AbCost and Restriction are set here to get this working as expected
            Target tgt = new Target(card, "Select a creature in a graveyard", "Creature".split(","));
            tgt.setZone(Constant.Zone.Graveyard);
            animate.setTarget(tgt);

            Cost cost = new Cost("1 B", cardName, false);
            animate.setPayCosts(cost);

            animate.getRestrictions().setZone(Constant.Zone.Hand);

            final Ability attach = new Ability(card, "0") {
                private static final long serialVersionUID = 222308932796127795L;

                @Override
                public void resolve() {
                    PlayerZone play = AllZone.getZone(Constant.Zone.Battlefield, card.getController());

                    // Animate Dead got destroyed before its ability resolved
                    if (!AllZoneUtil.isCardInZone(play, card)) {
                        return;
                    }

                    Card animated = targetC[0];
                    PlayerZone grave = AllZone.getZone(animated);

                    if (!grave.is(Constant.Zone.Graveyard)) {
                        // Animated Creature got removed before ability resolved
                        AllZone.getGameAction().sacrifice(card);
                        return;
                    }

                    // Bring creature onto the battlefield under your control (should trigger etb Abilities)
                    animated.addController(card.getController());
                    AllZone.getGameAction().moveToPlay(animated, card.getController());
                    if (cardName.equals("Dance of the Dead")) {
                        animated.tap();
                    }
                    card.enchantCard(animated);    // Attach before Targeting so detach Command will trigger

                    if (CardFactoryUtil.hasProtectionFrom(card, animated)) {
                        // Animated a creature with protection
                        AllZone.getGameAction().sacrifice(card);
                        return;
                    }

                    // Everything worked out perfectly.
                }
            }; //Ability

            final Command attachCmd = new Command() {
                private static final long serialVersionUID = 3595188622377350327L;

                public void execute() {
                    AllZone.getStack().addSimultaneousStackEntry(attach);

                }
            };

            final Ability detach = new Ability(card, "0") {

                @Override
                public void resolve() {
                    Card c = targetC[0];

                    PlayerZone play = AllZone.getZone(Constant.Zone.Battlefield, card.getController());

                    if (AllZoneUtil.isCardInZone(play, c)) {
                        AllZone.getGameAction().sacrifice(c);
                    }
                }
            }; //Detach

            final Command detachCmd = new Command() {
                private static final long serialVersionUID = 2425333033834543422L;

                public void execute() {
                    Card c = targetC[0];

                    PlayerZone play = AllZone.getZone(Constant.Zone.Battlefield, card.getController());

                    if (AllZoneUtil.isCardInZone(play, c)) {
                        AllZone.getStack().addSimultaneousStackEntry(detach);
                    }

                }
            };

            card.addSpellAbility(animate);

            attach.setStackDescription("Attaching " + cardName + " to creature in graveyard.");
            card.addComesIntoPlayCommand(attachCmd);
            detach.setStackDescription(cardName + " left play. Sacrificing creature if still around.");
            card.addLeavesPlayCommand(detachCmd);
            card.addUnEnchantCommand(detachCmd);
        } //*************** END ************ END **************************


        //*************** START *********** START **************************
        else if (CardFactoryUtil.hasKeyword(card, "enchant") != -1) {
            int n = CardFactoryUtil.hasKeyword(card, "enchant");
            if (n != -1) {
                String parse = card.getKeyword().get(n).toString();
                String[] k = parse.split(":");

                SpellAbility sa = card.getSpellAbility()[0];
                sa.setIsMultiKicker(true);
                sa.setMultiKickerManaCost(k[1]);
            }
        }


        return card;
    }

}
