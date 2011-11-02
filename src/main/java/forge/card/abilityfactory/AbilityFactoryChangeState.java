package forge.card.abilityfactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import forge.AllZoneUtil;
import forge.Card;
import forge.CardList;
import forge.Constant.Zone;
import forge.Player;
import forge.card.cardfactory.CardFactoryUtil;
import forge.card.spellability.AbilityActivated;
import forge.card.spellability.AbilitySub;
import forge.card.spellability.Spell;
import forge.card.spellability.SpellAbility;
import forge.card.spellability.Target;

/**
 * AbilityFactory for abilities that cause cards to change states.
 * 
 */
public class AbilityFactoryChangeState {

    /**
     * Gets the change state ability.
     * 
     * @param abilityFactory
     *            the aF
     * @return the change state ability
     */
    public static SpellAbility getChangeStateAbility(final AbilityFactory abilityFactory) {
        final SpellAbility ret = new AbilityActivated(abilityFactory.getHostCard(), abilityFactory.getAbCost(),
                abilityFactory.getAbTgt()) {
            private static final long serialVersionUID = -1083427558368639457L;

            @Override
            public String getStackDescription() {
                return AbilityFactoryChangeState.changeStateStackDescription(abilityFactory, this);
            }

            @Override
            public void resolve() {
                AbilityFactoryChangeState.changeStateResolve(abilityFactory, this);
            }
        };

        return ret;
    }

    /**
     * Gets the change state spell.
     * 
     * @param abilityFactory
     *            the aF
     * @return the change state spell
     */
    public static SpellAbility getChangeStateSpell(final AbilityFactory abilityFactory) {
        final SpellAbility ret = new Spell(abilityFactory.getHostCard()) {
            private static final long serialVersionUID = -7506856902233086859L;

            @Override
            public String getStackDescription() {
                return AbilityFactoryChangeState.changeStateStackDescription(abilityFactory, this);
            }

            @Override
            public void resolve() {
                AbilityFactoryChangeState.changeStateResolve(abilityFactory, this);
            }
        };

        return ret;
    }

    /**
     * Gets the change state drawback.
     * 
     * @param abilityFactory
     *            the aF
     * @return the change state drawback
     */
    public static SpellAbility getChangeStateDrawback(final AbilityFactory abilityFactory) {
        final AbilitySub ret = new AbilitySub(abilityFactory.getHostCard(), abilityFactory.getAbTgt()) {

            private static final long serialVersionUID = -3793247725721587468L;

            @Override
            public String getStackDescription() {
                return AbilityFactoryChangeState.changeStateStackDescription(abilityFactory, this);
            }

            @Override
            public boolean chkAIDrawback() {

                // Gross generalization, but this always considers alternate
                // states more powerful
                if (abilityFactory.getHostCard().isInAlternateState()) {
                    return false;
                }

                return true;
            }

            @Override
            public boolean doTrigger(final boolean mandatory) {
                if (!mandatory && abilityFactory.getHostCard().isInAlternateState()) {
                    return false;
                }

                return true;
            }

            @Override
            public void resolve() {
                AbilityFactoryChangeState.changeStateResolve(abilityFactory, this);
            }

        };

        return ret;
    }

    private static String changeStateStackDescription(final AbilityFactory abilityFactory, final SpellAbility sa) {
        final Map<String, String> params = abilityFactory.getMapParams();

        final StringBuilder sb = new StringBuilder();
        final Card host = abilityFactory.getHostCard();

        final String conditionDesc = params.get("ConditionDescription");
        if (conditionDesc != null) {
            sb.append(conditionDesc).append(" ");
        }

        ArrayList<Card> tgtCards;

        final Target tgt = abilityFactory.getAbTgt();
        if (tgt != null) {
            tgtCards = tgt.getTargetCards();
        } else {
            tgtCards = AbilityFactory.getDefinedCards(sa.getSourceCard(), params.get("Defined"), sa);
        }

        if (sa instanceof AbilitySub) {
            sb.append(" ");
        } else {
            sb.append(host).append(" - ");
        }

        if (params.containsKey("Flip")) {
            sb.append("Flip");
        } else {
            sb.append("Transform ");
        }

        final Iterator<Card> it = tgtCards.iterator();
        while (it.hasNext()) {
            final Card tgtC = it.next();
            if (tgtC.isFaceDown()) {
                sb.append("Morph ").append("(").append(tgtC.getUniqueNumber()).append(")");
            } else {
                sb.append(tgtC);
            }

            if (it.hasNext()) {
                sb.append(", ");
            }
        }
        sb.append(".");

        final AbilitySub abSub = sa.getSubAbility();
        if (abSub != null) {
            sb.append(abSub.getStackDescription());
        }

        return sb.toString();
    }

    private static void changeStateResolve(final AbilityFactory abilityFactory, final SpellAbility sa) {

        ArrayList<Card> tgtCards;

        if (abilityFactory.getAbTgt() != null) {
            tgtCards = abilityFactory.getAbTgt().getTargetCards();
        } else {
            tgtCards = AbilityFactory.getDefinedCards(abilityFactory.getHostCard(),
                    abilityFactory.getMapParams().get("Defined"), sa);
        }

        for (final Card tgt : tgtCards) {
            if (abilityFactory.getAbTgt() != null) {
                if (!CardFactoryUtil.canTarget(abilityFactory.getHostCard(), tgt)) {
                    continue;
                }
            }
            tgt.changeState();
        }

    }

    // //////////////////////////////////////////////
    // changeStateAll //
    // //////////////////////////////////////////////

    /**
     * Gets the change state all ability.
     * 
     * @param abilityFactory
     *            the aF
     * @return the change state all ability
     */
    public static SpellAbility getChangeStateAllAbility(final AbilityFactory abilityFactory) {
        final SpellAbility ret = new AbilityActivated(abilityFactory.getHostCard(), abilityFactory.getAbCost(),
                abilityFactory.getAbTgt()) {

            private static final long serialVersionUID = 7841029107610111992L;

            @Override
            public String getStackDescription() {
                return AbilityFactoryChangeState.changeStateAllStackDescription(abilityFactory, this);
            }

            @Override
            public void resolve() {
                AbilityFactoryChangeState.changeStateAllResolve(abilityFactory, this);
            }

        };

        return ret;
    }

    /**
     * Gets the change state all spell.
     * 
     * @param abilityFactory
     *            the aF
     * @return the change state all spell
     */
    public static SpellAbility getChangeStateAllSpell(final AbilityFactory abilityFactory) {
        final SpellAbility ret = new Spell(abilityFactory.getHostCard()) {

            private static final long serialVersionUID = 4217632586060204603L;

            @Override
            public String getStackDescription() {
                return AbilityFactoryChangeState.changeStateAllStackDescription(abilityFactory, this);
            }

            @Override
            public void resolve() {
                AbilityFactoryChangeState.changeStateAllResolve(abilityFactory, this);
            }
        };

        return ret;
    }

    /**
     * Gets the change state all drawback.
     * 
     * @param abilityFactory
     *            the aF
     * @return the change state all drawback
     */
    public static SpellAbility getChangeStateAllDrawback(final AbilityFactory abilityFactory) {
        final AbilitySub ret = new AbilitySub(abilityFactory.getHostCard(), abilityFactory.getAbTgt()) {

            private static final long serialVersionUID = 4047514893482113436L;

            @Override
            public String getStackDescription() {
                return AbilityFactoryChangeState.changeStateAllStackDescription(abilityFactory, this);
            }

            @Override
            public boolean chkAIDrawback() {

                // Gross generalization, but this always considers alternate
                // states more powerful
                if (abilityFactory.getHostCard().isInAlternateState()) {
                    return false;
                }

                return true;
            }

            @Override
            public boolean doTrigger(final boolean mandatory) {
                return true;
            }

            @Override
            public void resolve() {
                AbilityFactoryChangeState.changeStateAllResolve(abilityFactory, this);
            }

        };

        return ret;
    }

    private static void changeStateAllResolve(final AbilityFactory abilityFactory, final SpellAbility sa) {
        final HashMap<String, String> params = abilityFactory.getMapParams();

        final Card card = sa.getSourceCard();

        final Target tgt = abilityFactory.getAbTgt();
        Player targetPlayer = null;
        if (tgt != null) {
            targetPlayer = tgt.getTargetPlayers().get(0);
        }

        String valid = "";

        if (params.containsKey("ValidCards")) {
            valid = params.get("ValidCards");
        }

        // Ugh. If calculateAmount needs to be called with DestroyAll it _needs_
        // to use the X variable
        // We really need a better solution to this
        if (valid.contains("X")) {
            valid = valid.replace("X", Integer.toString(AbilityFactory.calculateAmount(card, "X", sa)));
        }

        CardList list = AllZoneUtil.getCardsIn(Zone.Battlefield);

        if (targetPlayer != null) {
            list = list.getController(targetPlayer);
        }

        list = AbilityFactory.filterListByType(list, valid, sa);

        final boolean remChanged = params.containsKey("RememberChanged");
        if (remChanged) {
            card.clearRemembered();
        }

        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).changeState()) {
                card.addRemembered(list.get(i));
            }
        }
    }

    private static String changeStateAllStackDescription(final AbilityFactory abilityFactory, final SpellAbility sa) {

        final Card host = abilityFactory.getHostCard();
        final Map<String, String> params = abilityFactory.getMapParams();
        final StringBuilder sb = new StringBuilder();

        if (sa instanceof AbilitySub) {
            sb.append(" ");
        } else {
            sb.append(host).append(" - ");
        }

        if (params.containsKey("Flip")) {
            sb.append("Flip");
        } else {
            sb.append("Transform ");
        }

        sb.append(" permanents.");

        final AbilitySub abSub = sa.getSubAbility();
        if (abSub != null) {
            sb.append(abSub.getStackDescription());
        }

        return sb.toString();
    }
}
