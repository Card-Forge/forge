package forge.card.cardfactory;

import java.util.ArrayList;
import java.util.HashMap;

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
import forge.Constant.Zone;
import forge.PlayerZone;
import forge.card.cost.Cost;
import forge.card.spellability.Ability;
import forge.card.spellability.Spell;
import forge.card.spellability.SpellAbility;
import forge.card.spellability.SpellPermanent;
import forge.card.spellability.Target;
import forge.gui.GuiUtils;
import forge.gui.input.Input;

/**
 * <p>
 * CardFactory_Auras class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
class CardFactoryAuras {

    /**
     * <p>
     * shouldCycle.
     * </p>
     * 
     * @param c
     *            a {@link forge.Card} object.
     * @return a int.
     */
    public static final int shouldCycle(final Card c) {
        final ArrayList<String> a = c.getKeyword();
        for (int i = 0; i < a.size(); i++) {
            if (a.get(i).toString().startsWith("Cycling")) {
                return i;
            }
        }
        return -1;
    }

    /**
     * <p>
     * getCard.
     * </p>
     * 
     * @param card
     *            a {@link forge.Card} object.
     * @param cardName
     *            a {@link java.lang.String} object.
     * @return a {@link forge.Card} object.
     */
    public static Card getCard(final Card card, final String cardName) {

        // *************** START *********** START **************************
        if (cardName.equals("Convincing Mirage") || cardName.equals("Phantasmal Terrain")) {

            final String[] newType = new String[1];
            final SpellAbility spell = new Spell(card) {

                private static final long serialVersionUID = 53941812202244498L;

                @Override
                public boolean canPlayAI() {

                    if (!super.canPlayAI()) {
                        return false;
                    }
                    final String[] landTypes = new String[] { "Plains", "Island", "Swamp", "Mountain", "Forest" };
                    final HashMap<String, Integer> humanLandCount = new HashMap<String, Integer>();
                    final CardList humanlands = AllZoneUtil.getPlayerLandsInPlay(AllZone.getHumanPlayer());

                    for (final String landType : landTypes) {
                        humanLandCount.put(landType, 0);
                    }

                    for (final Card c : humanlands) {
                        for (final String singleType : c.getType()) {
                            if (CardUtil.isABasicLandType(singleType)) {
                                humanLandCount.put(singleType, humanLandCount.get(singleType) + 1);
                            }
                        }
                    }

                    int minAt = 0;
                    int minVal = Integer.MAX_VALUE;
                    for (int i = 0; i < landTypes.length; i++) {
                        if (this.getTargetCard().isType(landTypes[i])) {
                            continue;
                        }

                        if (humanLandCount.get(landTypes[i]) < minVal) {
                            minVal = humanLandCount.get(landTypes[i]);
                            minAt = i;
                        }
                    }

                    newType[0] = landTypes[minAt];
                    CardList list = AllZoneUtil.getPlayerLandsInPlay(AllZone.getHumanPlayer());
                    list = list.getNotType(newType[0]); // Don't enchant lands
                                                        // that already have the
                                                        // type
                    if (list.isEmpty()) {
                        return false;
                    }
                    this.setTargetCard(list.get(0));
                    return true;
                } // canPlayAI()

                @Override
                public void resolve() {
                    // Only query player, AI will have decided already.
                    if (card.getController().isHuman()) {
                        newType[0] = GuiUtils.getChoice("Select land type.", "Plains", "Island", "Swamp", "Mountain",
                                "Forest");
                    }
                    AllZone.getGameAction().moveToPlay(card);

                    final Card c = this.getTargetCard();

                    if (AllZoneUtil.isCardInPlay(c) && CardFactoryUtil.canTarget(card, c)) {
                        card.enchantEntity(c);
                    }

                } // resolve()
            }; // SpellAbility

            spell.setDescription("");
            card.addSpellAbility(spell);

            // Need to set the spell description for Lingering Mirage since it
            // has cycling ability.
            if (card.getName().equals("Lingering Mirage")) {
                spell.setDescription("Enchanted land is an Island.");
            }

            final Command onEnchant = new Command() {

                private static final long serialVersionUID = 3528675112863241126L;

                @Override
                public void execute() {
                    if (card.isEnchanting()) {
                        final Card crd = card.getEnchantingCard();
                        final ArrayList<Card> seas = crd.getEnchantedBy();
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
                            final SpellAbility[] abilities = otherSeas.getSpellAbility();
                            for (final SpellAbility abilitie : abilities) {
                                card.addSpellAbility(abilitie);
                            }
                        }
                    }
                } // execute()
            }; // Command

            final Command onUnEnchant = new Command() {
                private static final long serialVersionUID = -202144631191180334L;

                @Override
                public void execute() {
                    if (card.isEnchanting()) {
                        final Card crd = card.getEnchantingCard();
                        final ArrayList<Card> seas = crd.getEnchantedBy();
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
                            final SpellAbility[] cardAbilities = crd.getSpellAbility();
                            for (final SpellAbility cardAbilitie : cardAbilities) {
                                if (cardAbilitie.isIntrinsic()) {
                                    crd.removeSpellAbility(cardAbilitie);
                                }
                            }
                            final Card c = AllZone.getCardFactory().copyCard(crd);
                            final ArrayList<String> types = c.getType();
                            final SpellAbility[] abilities = card.getSpellAbility();
                            for (int i = 0; i < types.size(); i++) {
                                crd.addType(types.get(i));
                            }
                            for (final SpellAbility abilitie : abilities) {
                                crd.addSpellAbility(abilitie);
                            }
                        }
                    }
                } // execute()
            }; // Command

            final Command onLeavesPlay = new Command() {

                private static final long serialVersionUID = -45433022112460839L;

                @Override
                public void execute() {
                    if (card.isEnchanting()) {
                        final Card crd = card.getEnchantingCard();
                        card.unEnchantEntity(crd);
                    }
                }
            };

            card.addEnchantCommand(onEnchant);
            card.addUnEnchantCommand(onUnEnchant);
            card.addLeavesPlayCommand(onLeavesPlay);

            final Input runtime = new Input() {

                private static final long serialVersionUID = -62372711146079880L;

                @Override
                public void showMessage() {
                    final CardList land = AllZoneUtil.getLandsInPlay();
                    this.stopSetNext(CardFactoryUtil.inputTargetSpecific(spell, land, "Select target land", true,
                            false));
                }
            };
            spell.setBeforePayMana(runtime);
        } // *************** END ************ END **************************

        // *************** START *********** START **************************
        else if (cardName.equals("Earthbind")) {
            final Cost cost = new Cost(card.getManaCost(), cardName, false);
            final Target tgt = new Target(card, "C");
            final SpellAbility spell = new SpellPermanent(card, cost, tgt) {

                private static final long serialVersionUID = 142389375702113977L;

                @Override
                public boolean canPlayAI() {
                    CardList list = AllZoneUtil.getCreaturesInPlay(AllZone.getHumanPlayer());
                    list = list.getKeyword("Flying");
                    if (list.isEmpty()) {
                        return false;
                    }

                    final CardListFilter f = new CardListFilter() {
                        @Override
                        public final boolean addCard(final Card c) {
                            return (c.getNetDefense() - c.getDamage()) <= 2;
                        }
                    };
                    if (!list.filter(f).isEmpty()) {
                        list = list.filter(f);
                    }
                    CardListUtil.sortAttack(list);

                    for (int i = 0; i < list.size(); i++) {
                        if (CardFactoryUtil.canTarget(card, list.get(i))) {
                            this.setTargetCard(list.get(i));
                            return super.canPlayAI();
                        }
                    }
                    return false;
                } // canPlayAI()

                @Override
                public void resolve() {
                    AllZone.getGameAction().moveToPlay(card);

                    final Card c = this.getTargetCard();

                    if (AllZoneUtil.isCardInPlay(c) && CardFactoryUtil.canTarget(card, c)) {
                        card.enchantEntity(c);
                        Log.debug("Enchanted: " + this.getTargetCard());
                    }
                } // resolve()
            }; // SpellAbility

            card.addSpellAbility(spell);

            final boolean[] badTarget = { true };
            final Command onEnchant = new Command() {

                private static final long serialVersionUID = -5302506578307993978L;

                @Override
                public void execute() {
                    if (card.isEnchanting()) {
                        final Card crd = card.getEnchantingCard();
                        if (crd.hasKeyword("Flying")) {
                            badTarget[0] = false;
                            crd.addDamage(2, card);
                            crd.removeIntrinsicKeyword("Flying");
                            crd.removeExtrinsicKeyword("Flying");
                        } else {
                            badTarget[0] = true;
                        }
                    }
                } // execute()
            }; // Command

            final Command onUnEnchant = new Command() {

                private static final long serialVersionUID = -6908757692588823391L;

                @Override
                public void execute() {
                    if (card.isEnchanting() && !badTarget[0]) {
                        final Card crd = card.getEnchantingCard();
                        crd.addIntrinsicKeyword("Flying");
                    }
                } // execute()
            }; // Command

            final Command onLeavesPlay = new Command() {

                private static final long serialVersionUID = -7833240882415702940L;

                @Override
                public void execute() {
                    if (card.isEnchanting()) {
                        final Card crd = card.getEnchantingCard();
                        card.unEnchantEntity(crd);
                    }
                }
            };

            card.addEnchantCommand(onEnchant);
            card.addUnEnchantCommand(onUnEnchant);
            card.addLeavesPlayCommand(onLeavesPlay);
        } // *************** END ************ END **************************

        // *************** START *********** START **************************
        else if (cardName.equals("Guilty Conscience")) {
            final Cost cost = new Cost(card.getManaCost(), cardName, false);
            final Target tgt = new Target(card, "C");
            final SpellAbility spell = new SpellPermanent(card, cost, tgt) {

                private static final long serialVersionUID = 1169151960692309514L;

                @Override
                public boolean canPlayAI() {

                    final CardList stuffy = AllZone.getComputerPlayer().getCardsIn(Zone.Battlefield, "Stuffy Doll");

                    if (stuffy.size() > 0) {
                        this.setTargetCard(stuffy.get(0));
                        return true;
                    } else {
                        final CardList list = AllZoneUtil.getCreaturesInPlay(AllZone.getHumanPlayer());

                        if (list.isEmpty()) {
                            return false;
                        }

                        // else
                        CardListUtil.sortAttack(list);
                        CardListUtil.sortFlying(list);

                        for (int i = 0; i < list.size(); i++) {
                            if (CardFactoryUtil.canTarget(card, list.get(i))
                                    && (list.get(i).getNetAttack() >= list.get(i).getNetDefense())
                                    && (list.get(i).getNetAttack() >= 3)) {
                                this.setTargetCard(list.get(i));
                                return super.canPlayAI();
                            }
                        }
                    }
                    return false;

                } // canPlayAI()

                @Override
                public void resolve() {
                    final Card aura = AllZone.getGameAction().moveToPlay(card);

                    final Card c = this.getTargetCard();

                    if (AllZoneUtil.isCardInPlay(c) && CardFactoryUtil.canTarget(aura, c)) {
                        aura.enchantEntity(c);
                    }
                } // resolve()
            }; // SpellAbility

            card.addSpellAbility(spell);
        } // *************** END ************ END **************************

        // *************** START *********** START **************************
        else if (cardName.equals("Animate Dead") || cardName.equals("Dance of the Dead")) {
            final Card[] targetC = new Card[1];
            // need to override what happens when this is cast.
            final SpellPermanent animate = new SpellPermanent(card) {
                private static final long serialVersionUID = 7126615291288065344L;

                public CardList getCreturesInGrave() {
                    // This includes creatures Animate Dead can't enchant once
                    // in play.
                    // The human may try to Animate them, the AI will not.
                    return AllZoneUtil.getCardsIn(Zone.Graveyard).filter(CardListFilter.CREATURES);
                }

                @Override
                public boolean canPlay() {
                    return super.canPlay() && (this.getCreturesInGrave().size() != 0);
                }

                @Override
                public boolean canPlayAI() {
                    CardList cList = this.getCreturesInGrave();
                    // AI will only target something that will stick in play.
                    cList = cList.filter(new CardListFilter() {
                        @Override
                        public final boolean addCard(final Card crd) {
                            return CardFactoryUtil.canTarget(card, crd)
                                    && !CardFactoryUtil.hasProtectionFrom(card, crd);
                        }
                    });
                    if (cList.size() == 0) {
                        return false;
                    }

                    final Card c = CardFactoryUtil.getBestCreatureAI(cList);

                    this.setTargetCard(c);
                    final boolean playable = (2 < c.getNetAttack()) && (2 < c.getNetDefense()) && super.canPlayAI();
                    return playable;
                } // canPlayAI

                @Override
                public void resolve() {
                    targetC[0] = this.getTargetCard();
                    super.resolve();
                }

            }; // addSpellAbility

            // Target AbCost and Restriction are set here to get this working as
            // expected
            final Target tgt = new Target(card, "Select a creature in a graveyard", "Creature".split(","));
            tgt.setZone(Constant.Zone.Graveyard);
            animate.setTarget(tgt);

            final Cost cost = new Cost("1 B", cardName, false);
            animate.setPayCosts(cost);

            animate.getRestrictions().setZone(Constant.Zone.Hand);

            final Ability attach = new Ability(card, "0") {
                @Override
                public void resolve() {
                    final PlayerZone play = card.getController().getZone(Constant.Zone.Battlefield);

                    // Animate Dead got destroyed before its ability resolved
                    if (!play.contains(card)) {
                        return;
                    }

                    final Card animated = targetC[0];
                    final PlayerZone grave = AllZone.getZoneOf(animated);

                    if (!grave.is(Constant.Zone.Graveyard)) {
                        // Animated Creature got removed before ability resolved
                        AllZone.getGameAction().sacrifice(card);
                        return;
                    }

                    // Bring creature onto the battlefield under your control
                    // (should trigger etb Abilities)
                    animated.addController(card.getController());
                    AllZone.getGameAction().moveToPlay(animated, card.getController());
                    if (cardName.equals("Dance of the Dead")) {
                        animated.tap();
                    }
                    card.enchantEntity(animated); // Attach before Targeting so
                                                  // detach Command will trigger

                    if (CardFactoryUtil.hasProtectionFrom(card, animated)) {
                        // Animated a creature with protection
                        AllZone.getGameAction().sacrifice(card);
                        return;
                    }

                    // Everything worked out perfectly.
                }
            }; // Ability

            final Command attachCmd = new Command() {
                private static final long serialVersionUID = 3595188622377350327L;

                @Override
                public void execute() {
                    AllZone.getStack().addSimultaneousStackEntry(attach);

                }
            };

            final Ability detach = new Ability(card, "0") {

                @Override
                public void resolve() {
                    final Card c = targetC[0];

                    final PlayerZone play = card.getController().getZone(Constant.Zone.Battlefield);

                    if (play.contains(c)) {
                        AllZone.getGameAction().sacrifice(c);
                    }
                }
            }; // Detach

            final Command detachCmd = new Command() {
                private static final long serialVersionUID = 2425333033834543422L;

                @Override
                public void execute() {
                    final Card c = targetC[0];

                    final PlayerZone play = card.getController().getZone(Constant.Zone.Battlefield);

                    if (play.contains(c)) {
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
        } // *************** END ************ END **************************

        // *************** START *********** START **************************
        else if (CardFactoryUtil.hasKeyword(card, "enchant") != -1) {
            final int n = CardFactoryUtil.hasKeyword(card, "enchant");
            if (n != -1) {
                final String parse = card.getKeyword().get(n).toString();
                final String[] k = parse.split(":");

                final SpellAbility sa = card.getSpellAbility()[0];
                sa.setIsMultiKicker(true);
                sa.setMultiKickerManaCost(k[1]);
            }
        }

        return card;
    }

}
