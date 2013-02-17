package forge.game;

import java.util.ArrayList;
import java.util.List;

import com.google.common.collect.Lists;

import forge.Card;
import forge.CardLists;
import forge.CardPredicates;
import forge.CardUtil;
import forge.card.MagicColor;
import forge.card.SpellManaCost;
import forge.card.ability.AbilityUtils;
import forge.card.ability.ApiType;
import forge.card.ability.effects.CharmEffect;
import forge.card.cardfactory.CardFactoryUtil;
import forge.card.cost.Cost;
import forge.card.cost.CostPayment;
import forge.card.mana.ManaCostBeingPaid;
import forge.card.mana.ManaCostShard;
import forge.card.spellability.SpellAbility;
import forge.card.spellability.SpellAbilityRequirements;
import forge.card.spellability.Target;
import forge.card.spellability.TargetSelection;
import forge.card.staticability.StaticAbility;
import forge.control.input.InputControl;
import forge.control.input.InputPayManaSimple;
import forge.game.player.Player;
import forge.game.zone.ZoneType;
import forge.gui.GuiChoose;

/** 
 * TODO: Write javadoc for this type.
 *
 */
public class GameActionPlay {
    
    private final GameState game;
    private InputControl matchInput;
    

    public GameActionPlay(final GameState game0) {
        game = game0;
    }
    
    void setMatchInput(InputControl input) {
        this.matchInput = input; // TODO: Add 0 to parameter's name.
    }
    
    public final void playCardWithoutManaCost(final Card c, Player player) {
        final List<SpellAbility> choices = c.getBasicSpells();
        // TODO add Buyback, Kicker, ... , spells here

        SpellAbility sa = player.getController().getAbilityToPlay(choices);

        if (sa == null) {
            return;
        }

        sa.setActivatingPlayer(player);
        this.playSpellAbilityForFree(sa);
    }

    /**
     * <p>
     * playSpellAbilityForFree.
     * </p>
     * 
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     */
    public final void playSpellAbilityForFree(final SpellAbility sa) {
        if (sa.getPayCosts() != null) {
            if (sa.getApi() == ApiType.Charm && !sa.isWrapper()) {
                CharmEffect.makeChoices(sa);
            }
            final TargetSelection ts = new TargetSelection(sa.getTarget(), sa);
            final CostPayment payment = new CostPayment(sa.getPayCosts(), sa, game);

            final SpellAbilityRequirements req = new SpellAbilityRequirements(sa, ts, payment);
            req.setFree(true);
            req.fillRequirements();
        } else if (sa.getBeforePayMana() == null) {
            if (sa.isSpell()) {
                final Card c = sa.getSourceCard();
                if (!c.isCopiedSpell()) {
                    sa.setSourceCard(game.getAction().moveToStack(c));
                }
            }
            boolean x = sa.getSourceCard().getManaCost().getShardCount(ManaCostShard.X) > 0;

            game.getStack().add(sa, x);
        } else {
            sa.setManaCost(SpellManaCost.ZERO); // Beached As
            matchInput.setInput(sa.getBeforePayMana());
        }
    }

    /**
     * <p>
     * getSpellCostChange.
     * </p>
     * 
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @param originalCost
     *            a {@link forge.card.mana.ManaCostBeingPaid} object.
     * @return a {@link forge.card.mana.ManaCostBeingPaid} object.
     */
    public final ManaCostBeingPaid getSpellCostChange(final SpellAbility sa, final ManaCostBeingPaid originalCost) {
        // Beached
        final Card originalCard = sa.getSourceCard();
        final SpellAbility spell = sa;
        String mana = originalCost.toString();
        ManaCostBeingPaid manaCost = new ManaCostBeingPaid(mana);
        if (sa.isXCost() && !originalCard.isCopiedSpell()) {
            originalCard.setXManaCostPaid(0);
        }

        if (game == null || sa.isTrigger()) {
            return manaCost;
        }

        if (spell.isSpell()) {
            if (spell.isDelve()) {
                manaCost = getCostAfterDelve(originalCost, originalCard);
            } else if (spell.getSourceCard().hasKeyword("Convoke")) {
                ManaCostBeingPaid convokeCost = getCostAfterConvoke(sa, originalCost, spell);
                if ( null != convokeCost ) 
                    manaCost = convokeCost; 
            }
        } // isSpell

        List<Card> cardsOnBattlefield = Lists.newArrayList(game.getCardsIn(ZoneType.Battlefield));
        cardsOnBattlefield.addAll(game.getCardsIn(ZoneType.Stack));
        cardsOnBattlefield.addAll(game.getCardsIn(ZoneType.Command));
        if (!cardsOnBattlefield.contains(originalCard)) {
            cardsOnBattlefield.add(originalCard);
        }
        final ArrayList<StaticAbility> raiseAbilities = new ArrayList<StaticAbility>();
        final ArrayList<StaticAbility> reduceAbilities = new ArrayList<StaticAbility>();
        final ArrayList<StaticAbility> setAbilities = new ArrayList<StaticAbility>();

        // Sort abilities to apply them in proper order
        for (Card c : cardsOnBattlefield) {
            final ArrayList<StaticAbility> staticAbilities = c.getStaticAbilities();
            for (final StaticAbility stAb : staticAbilities) {
                if (stAb.getMapParams().get("Mode").equals("RaiseCost")) {
                    raiseAbilities.add(stAb);
                } else if (stAb.getMapParams().get("Mode").equals("ReduceCost")) {
                    reduceAbilities.add(stAb);
                } else if (stAb.getMapParams().get("Mode").equals("SetCost")) {
                    setAbilities.add(stAb);
                }
            }
        }
        // Raise cost
        for (final StaticAbility stAb : raiseAbilities) {
            manaCost = stAb.applyAbility("RaiseCost", spell, manaCost);
        }

        // Reduce cost
        for (final StaticAbility stAb : reduceAbilities) {
            manaCost = stAb.applyAbility("ReduceCost", spell, manaCost);
        }

        // Set cost (only used by Trinisphere) is applied last
        for (final StaticAbility stAb : setAbilities) {
            manaCost = stAb.applyAbility("SetCost", spell, manaCost);
        }

        return manaCost;
    } // GetSpellCostChange

    private ManaCostBeingPaid getCostAfterConvoke(final SpellAbility sa, final ManaCostBeingPaid originalCost, final SpellAbility spell) {
        
        List<Card> untappedCreats = CardLists.filter(spell.getActivatingPlayer().getCardsIn(ZoneType.Battlefield), CardPredicates.Presets.CREATURES);
        untappedCreats = CardLists.filter(untappedCreats, CardPredicates.Presets.UNTAPPED);

        if (untappedCreats.size() != 0) {
            final List<Card> choices = new ArrayList<Card>();
            for (final Card c : untappedCreats) {
                choices.add(c);
            }
            ArrayList<String> usableColors = new ArrayList<String>();
            ManaCostBeingPaid newCost = new ManaCostBeingPaid(originalCost.toString());
            Card tapForConvoke = null;
            if (sa.getActivatingPlayer().isHuman()) {
                tapForConvoke = GuiChoose.oneOrNone("Tap for Convoke? " + newCost.toString(),
                        choices);
            } else {
                // TODO: AI to choose a creature to tap would go here
                // Probably along with deciding how many creatures to
                // tap
            }
            while (tapForConvoke != null && untappedCreats.size() != 0) {
                final Card workingCard = (Card) tapForConvoke;
                usableColors = CardUtil.getConvokableColors(workingCard, newCost);

                if (usableColors.size() != 0) {
                    String chosenColor = usableColors.get(0);
                    if (usableColors.size() > 1) {
                        if (sa.getActivatingPlayer().isHuman()) {
                            chosenColor = GuiChoose.one("Convoke for which color?", usableColors);
                        } else {
                            // TODO: AI for choosing which color to
                            // convoke goes here.
                        }
                    }

                    if (chosenColor.equals("colorless")) {
                        newCost.decreaseColorlessMana(1);
                    } else {
                        String newCostStr = newCost.toString();
                        newCostStr = newCostStr.replaceFirst(
                                MagicColor.toShortString(chosenColor), "").replaceFirst("  ", " ");
                        newCost = new ManaCostBeingPaid(newCostStr.trim());
                    }

                    sa.addTappedForConvoke(workingCard);
                    choices.remove(workingCard);
                    untappedCreats.remove(workingCard);
                    if ((choices.size() < 2) || (newCost.getConvertedManaCost() == 0)) {
                        break;
                    }
                } else {
                    untappedCreats.remove(workingCard);
                }

                if (sa.getActivatingPlayer().isHuman()) {
                    tapForConvoke = GuiChoose.oneOrNone("Tap for Convoke? " + newCost.toString(), choices);
                } else {
                    // TODO: AI to choose a creature to tap would go
                    // here
                }
            }

            // will only be null if user cancelled.
            if (tapForConvoke != null) {
                // Convoked creats are tapped here with triggers
                // suppressed,
                // Then again when payment is done(In
                // InputPayManaCost.done()) with suppression cleared.
                // This is to make sure that triggers go off at the
                // right time
                // AND that you can't use mana tapabilities of convoked
                // creatures
                // to pay the convoked cost.
                for (final Card c : sa.getTappedForConvoke()) {
                    c.setTapped(true);
                }

                return newCost;
            }
        }
        return null;
    }

    private ManaCostBeingPaid getCostAfterDelve(final ManaCostBeingPaid originalCost, final Card originalCard) {
        ManaCostBeingPaid manaCost;
        final int cardsInGrave = originalCard.getController().getCardsIn(ZoneType.Graveyard).size();

        final Player pc = originalCard.getController();
        if (pc.isHuman()) {
            final Integer[] cntChoice = new Integer[cardsInGrave + 1];
            for (int i = 0; i <= cardsInGrave; i++) {
                cntChoice[i] = Integer.valueOf(i);
            }

            final Integer chosenAmount = GuiChoose.one("Exile how many cards?", cntChoice);
            System.out.println("Delve for " + chosenAmount);
            final List<Card> choices = new ArrayList<Card>(pc.getCardsIn(ZoneType.Graveyard));
            final List<Card> chosen = new ArrayList<Card>();
            for (int i = 0; i < chosenAmount; i++) {
                final Card nowChosen = GuiChoose.oneOrNone("Exile which card?", choices);

                if (nowChosen == null) {
                    // User canceled,abort delving.
                    chosen.clear();
                    break;
                }

                choices.remove(nowChosen);
                chosen.add(nowChosen);
            }

            for (final Card c : chosen) {
                game.getAction().exile(c);
            }

            manaCost = new ManaCostBeingPaid(originalCost.toString());
            manaCost.decreaseColorlessMana(chosenAmount);
        } else {
            // AI
            int numToExile = 0;
            final int colorlessCost = originalCost.getColorlessManaAmount();

            if (cardsInGrave <= colorlessCost) {
                numToExile = cardsInGrave;
            } else {
                numToExile = colorlessCost;
            }

            for (int i = 0; i < numToExile; i++) {
                final List<Card> grave = pc.getZone(ZoneType.Graveyard).getCards();
                Card chosen = null;
                for (final Card c : grave) { // Exile noncreatures first
                                             // in
                    // case we can revive. Might
                    // wanna do some additional
                    // checking here for Flashback
                    // and the like.
                    if (!c.isCreature()) {
                        chosen = c;
                        break;
                    }
                }
                if (chosen == null) {
                    chosen = CardFactoryUtil.getWorstCreatureAI(grave);
                }

                if (chosen == null) {
                    // Should never get here but... You know how it is.
                    chosen = grave.get(0);
                }

                game.getAction().exile(chosen);
            }
            manaCost = new ManaCostBeingPaid(originalCost.toString());
            manaCost.decreaseColorlessMana(numToExile);
        }
        return manaCost;
    }

    /**
     * choose optional additional costs. For HUMAN only
     * @param activator 
     * 
     * @param original
     *            the original sa
     * @return an ArrayList<SpellAbility>.
     */
    public static SpellAbility chooseOptionalAdditionalCosts(Player activator, final SpellAbility original) {
        //final HashMap<String, SpellAbility> map = new HashMap<String, SpellAbility>();
        final ArrayList<SpellAbility> abilities = GameActionUtil.getOptionalAdditionalCosts(original);
        
        if (!original.isSpell()) {
            return original;
        }

        return activator.getController().getAbilityToPlay(abilities);
    }

    /**
     * <p>
     * playSpellAbility.
     * </p>
     * 
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     */
    public final void playSpellAbility(SpellAbility sa, Player activator) {
        sa.setActivatingPlayer(activator);

        if (sa.getApi() == ApiType.Charm && !sa.isWrapper()) {
            CharmEffect.makeChoices(sa);
        }

        sa = chooseOptionalAdditionalCosts(activator, sa);

        if (sa == null) {
            return;
        }

        // Need to check PayCosts, and Ability + All SubAbilities for Target
        boolean newAbility = sa.getPayCosts() != null;
        SpellAbility ability = sa;
        while ((ability != null) && !newAbility) {
            final Target tgt = ability.getTarget();

            newAbility |= tgt != null;
            ability = ability.getSubAbility();
        }

        if (newAbility) {
            final TargetSelection ts = new TargetSelection(sa.getTarget(), sa);
            CostPayment payment = null;
            if (sa.getPayCosts() == null) {
                payment = new CostPayment(new Cost(sa.getSourceCard(), "0", sa.isAbility()), sa, game);
            } else {
                payment = new CostPayment(sa.getPayCosts(), sa, game);
            }

            final SpellAbilityRequirements req = new SpellAbilityRequirements(sa, ts, payment);
            req.fillRequirements();
        } else {
            ManaCostBeingPaid manaCost = new ManaCostBeingPaid(sa.getManaCost());
            if (sa.getSourceCard().isCopiedSpell() && sa.isSpell()) {
                manaCost = new ManaCostBeingPaid("0");
            } else {
                manaCost = this.getSpellCostChange(sa, new ManaCostBeingPaid(sa.getManaCost()));
            }
            if (manaCost.isPaid() && (sa.getBeforePayMana() == null)) {
                final Card source = sa.getSourceCard();
                if (sa.isSpell() && !source.isCopiedSpell()) {
                    sa.setSourceCard(game.getAction().moveToStack(source));
                }

                game.getStack().add(sa);
                return;
            } else if (sa.getBeforePayMana() == null) {
                matchInput.setInput(new InputPayManaSimple(game, sa, manaCost));
            } else {
                matchInput.setInput(sa.getBeforePayMana());
            }
        }
    }

    /**
     * <p>
     * playSpellAbility_NoStack.
     * </p>
     * 
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @param skipTargeting
     *            a boolean.
     */
    public final void playSpellAbilityNoStack(final Player human, final SpellAbility sa, final boolean skipTargeting) {
        sa.setActivatingPlayer(human);

        if (sa.getPayCosts() != null) {
            final TargetSelection ts = new TargetSelection(sa.getTarget(), sa);
            final CostPayment payment = new CostPayment(sa.getPayCosts(), sa, game);

            if (!sa.isTrigger()) {
                payment.changeCost();
            }

            final SpellAbilityRequirements req = new SpellAbilityRequirements(sa, ts, payment);
            req.setSkipStack(true);
            req.fillRequirements(skipTargeting);
        } else {
            ManaCostBeingPaid manaCost = new ManaCostBeingPaid(sa.getManaCost());
            if (sa.getSourceCard().isCopiedSpell() && sa.isSpell()) {
                manaCost = new ManaCostBeingPaid("0");
            } else {
                manaCost = this.getSpellCostChange(sa, new ManaCostBeingPaid(sa.getManaCost()));
            }
            if (manaCost.isPaid() && (sa.getBeforePayMana() == null)) {
                AbilityUtils.resolve(sa, false);
                return;
            } else if (sa.getBeforePayMana() == null) {
                matchInput.setInput(new InputPayManaSimple(game, sa, true));
            } else {
                matchInput.setInput(sa.getBeforePayMana());
            }
        }
    }


    /** The Cost cutting_ get multi kicker mana cost paid. */
    private int costCuttingGetMultiKickerManaCostPaid = 0;
    /** The Cost cutting_ get multi kicker mana cost paid_ colored. */
    private String costCuttingGetMultiKickerManaCostPaidColored = "";

    /**
     * Gets the cost cutting get multi kicker mana cost paid.
     * 
     * @return the costCuttingGetMultiKickerManaCostPaid
     */
    public int getCostCuttingGetMultiKickerManaCostPaid() {
        return this.costCuttingGetMultiKickerManaCostPaid;
    }

    /**
     * Sets the cost cutting get multi kicker mana cost paid.
     * 
     * @param costCuttingGetMultiKickerManaCostPaid0
     *            the costCuttingGetMultiKickerManaCostPaid to set
     */
    public void setCostCuttingGetMultiKickerManaCostPaid(final int costCuttingGetMultiKickerManaCostPaid0) {
        this.costCuttingGetMultiKickerManaCostPaid = costCuttingGetMultiKickerManaCostPaid0;
    }

    /**
     * Gets the cost cutting get multi kicker mana cost paid colored.
     * 
     * @return the costCuttingGetMultiKickerManaCostPaidColored
     */
    public String getCostCuttingGetMultiKickerManaCostPaidColored() {
        return this.costCuttingGetMultiKickerManaCostPaidColored;
    }

    /**
     * Sets the cost cutting get multi kicker mana cost paid colored.
     * 
     * @param costCuttingGetMultiKickerManaCostPaidColored0
     *            the costCuttingGetMultiKickerManaCostPaidColored to set
     */
    public void setCostCuttingGetMultiKickerManaCostPaidColored(
            final String costCuttingGetMultiKickerManaCostPaidColored0) {
        this.costCuttingGetMultiKickerManaCostPaidColored = costCuttingGetMultiKickerManaCostPaidColored0;
    }
}
