/*
 * Forge: Play Magic: the Gathering.
 * Copyright (C) 2011  Forge Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package forge.game.player;

import java.util.List;

import forge.Card;
import forge.FThreads;
import forge.card.ability.AbilityUtils;
import forge.card.ability.ApiType;
import forge.card.ability.effects.CharmEffect;
import forge.card.cost.Cost;
import forge.card.cost.CostPayment;
import forge.card.mana.ManaCostBeingPaid;
import forge.card.mana.ManaCostShard;
import forge.card.spellability.Ability;
import forge.card.spellability.HumanPlaySpellAbility;
import forge.card.spellability.SpellAbility;
import forge.card.spellability.Target;
import forge.control.input.InputPayManaSimple;
import forge.control.input.InputSelectCards;
import forge.control.input.InputSelectCardsFromList;
import forge.game.GameActionUtil;
import forge.game.GameState;
import forge.game.zone.ZoneType;

public class HumanPlayer extends Player {
    private PlayerControllerHuman controller;
    
    public HumanPlayer(final LobbyPlayer player, GameState game) {
        super(player, game);
        controller = new PlayerControllerHuman(game, this);
    }

    /** {@inheritDoc} */
    @Override
    public final void discardUnless(final int num, final String uType, final SpellAbility sa) {
        final List<Card> hand = getCardsIn(ZoneType.Hand);
        final InputSelectCards target = new InputSelectCardsFromList(num, num, hand) {
            private static final long serialVersionUID = -5774108410928795591L;

            @Override
            protected boolean hasAllTargets() {
                for(Card c : selected) {
                    if (c.isType(uType))
                        return true;
                }
                return super.hasAllTargets();
            }
        };
        target.setMessage("Select %d cards to discard, unless you discard a " + uType + ".");
        FThreads.setInputAndWait(target);
        for(Card c : target.getSelected())
            c.getController().discard(c, sa);
    } // input_discardNumUnless
    
    /**
     * TODO: Write javadoc for this method.
     * @param card
     * @param ab
     */
    public void playSpellAbility(Card c, SpellAbility ab) {
        if (ab == Ability.PLAY_LAND_SURROGATE)
            this.playLand(c);
        else {
            this.playSpellAbility(ab);
        }
        game.getPhaseHandler().setPriority(this);
    }

    @Override
    public PlayerType getType() {
        return PlayerType.HUMAN;
    }
    public PlayerController getController() {
        return controller;
    }
    

    /**
     * <p>
     * playSpellAbility.
     * </p>
     * 
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     */
    public final void playSpellAbility(SpellAbility sa) {
        FThreads.assertExecutedByEdt(false);
        sa.setActivatingPlayer(this);

        final Card source = sa.getSourceCard();
        
        source.setSplitStateToPlayAbility(sa);

        if (sa.getApi() == ApiType.Charm && !sa.isWrapper()) {
            CharmEffect.makeChoices(sa);
        }

        sa = chooseOptionalAdditionalCosts(sa);

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

        // System.out.println("Playing:" + sa.getDescription() + " of " + sa.getSourceCard() +  " new = " + newAbility);
        if (newAbility) {
            CostPayment payment = null;
            if (sa.getPayCosts() == null) {
                payment = new CostPayment(new Cost("0", sa.isAbility()), sa);
            } else {
                payment = new CostPayment(sa.getPayCosts(), sa);
            }

            final HumanPlaySpellAbility req = new HumanPlaySpellAbility(sa, payment);
            req.fillRequirements(false, false, false);
        } else {
            ManaCostBeingPaid manaCost = new ManaCostBeingPaid(sa.getPayCosts().getCostMana().getManaToPay());
            if (sa.getSourceCard().isCopiedSpell() && sa.isSpell()) {
                manaCost = new ManaCostBeingPaid("0");
            } else {
                manaCost.applySpellCostChange(sa);
            }

            if  (!manaCost.isPaid()) {
                FThreads.setInputAndWait(new InputPayManaSimple(game, sa, manaCost));
            }

            
            if (manaCost.isPaid()) {
                if (sa.isSpell() && !source.isCopiedSpell()) {
                    sa.setSourceCard(game.getAction().moveToStack(source));
                }

                game.getStack().add(sa);
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
    public final void playSpellAbilityNoStack( final SpellAbility sa) {
        playSpellAbilityNoStack(sa, false);
    }
    public final void playSpellAbilityNoStack(final SpellAbility sa, boolean useOldTargets) {
        sa.setActivatingPlayer(this);

        if (sa.getPayCosts() != null) {
            final HumanPlaySpellAbility req = new HumanPlaySpellAbility(sa, new CostPayment(sa.getPayCosts(), sa));
            
            req.fillRequirements(useOldTargets, false, true);
        } else {
            ManaCostBeingPaid manaCost = new ManaCostBeingPaid(sa.getPayCosts().getTotalMana());
            if (sa.getSourceCard().isCopiedSpell() && sa.isSpell()) {
                manaCost = new ManaCostBeingPaid("0");
            } else {
                manaCost.applySpellCostChange(sa);
            }

            if( !manaCost.isPaid() ) {
                FThreads.setInputAndWait(new InputPayManaSimple(game, sa, manaCost));
            }
            
            if (manaCost.isPaid()) {
                AbilityUtils.resolve(sa, false);
            }

        }
    }
    
    /**
     * choose optional additional costs. For HUMAN only
     * @param activator 
     * 
     * @param original
     *            the original sa
     * @return an ArrayList<SpellAbility>.
     */
    public SpellAbility chooseOptionalAdditionalCosts(final SpellAbility original) {
        //final HashMap<String, SpellAbility> map = new HashMap<String, SpellAbility>();
        final List<SpellAbility> abilities = GameActionUtil.getOptionalCosts(original);
        
        if (!original.isSpell()) {
            return original;
        }

        return getController().getAbilityToPlay(abilities);
    }


    public final void playCardWithoutManaCost(final Card c) {
        final List<SpellAbility> choices = c.getBasicSpells();
        // TODO add Buyback, Kicker, ... , spells here

        SpellAbility sa = controller.getAbilityToPlay(choices);

        if (sa == null) {
            return;
        }

        sa.setActivatingPlayer(this);
        this.playSpellAbilityWithoutPayingManaCost(sa);
    }
    
    /**
     * <p>
     * playSpellAbilityForFree.
     * </p>
     * 
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     */
    public final void playSpellAbilityWithoutPayingManaCost(final SpellAbility sa) {
        FThreads.assertExecutedByEdt(false);
        final Card source = sa.getSourceCard();
        
        source.setSplitStateToPlayAbility(sa);

        if (sa.getPayCosts() != null) {
            if (sa.getApi() == ApiType.Charm && !sa.isWrapper()) {
                CharmEffect.makeChoices(sa);
            }
            final CostPayment payment = new CostPayment(sa.getPayCosts(), sa);

            final HumanPlaySpellAbility req = new HumanPlaySpellAbility(sa, payment);
            req.fillRequirements(false, true, false);
        } else {
            if (sa.isSpell()) {
                final Card c = sa.getSourceCard();
                if (!c.isCopiedSpell()) {
                    sa.setSourceCard(game.getAction().moveToStack(c));
                }
            }
            boolean x = sa.getSourceCard().getManaCost().getShardCount(ManaCostShard.X) > 0;

            game.getStack().add(sa, x);
        }
    }
} // end HumanPlayer class
