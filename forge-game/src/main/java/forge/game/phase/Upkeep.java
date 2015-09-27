/*
 * Forge: Play Magic: the Gathering.
 * Copyright (C) 2011  Forge Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that itinksidd will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package forge.game.phase;

import com.google.common.base.Predicate;

import forge.card.mana.ManaCost;
import forge.game.Game;
import forge.game.ability.AbilityFactory;
import forge.game.card.Card;
import forge.game.card.CardCollectionView;
import forge.game.card.CardFactoryUtil;
import forge.game.card.CardLists;
import forge.game.card.CounterType;
import forge.game.cost.Cost;
import forge.game.player.Player;
import forge.game.player.PlayerController.ManaPaymentPurpose;
import forge.game.spellability.Ability;
import forge.game.spellability.SpellAbility;
import forge.game.trigger.TriggerType;
import forge.game.zone.ZoneType;

import java.util.HashMap;
import org.apache.commons.lang3.StringUtils;

/**
 * <p>
 * The Upkeep class handles ending effects with "until your next upkeep" and
 * "until next upkeep".
 * 
 * It also handles hardcoded triggers "At the beginning of upkeep".
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
public class Upkeep extends Phase {
    private static final long serialVersionUID = 6906459482978819354L;

    protected final Game game;
    public Upkeep(final Game game) { 
        super(PhaseType.UPKEEP);
        this.game = game;
    }
    
    /**
     * <p>
     * Handles all the hardcoded events that happen at the beginning of each
     * Upkeep Phase.
     * 
     * This will freeze the Stack at the start, and unfreeze the Stack at the
     * end.
     * </p>
     */
    @Override
    public final void executeAt() {
        game.getStack().freezeStack();

        Upkeep.upkeepUpkeepCost(game); // sacrifice unless upkeep cost is paid
        Upkeep.upkeepEcho(game);

        game.getStack().unfreezeStack();
    }

    private static void upkeepEcho(final Game game) {
        CardCollectionView list = game.getPhaseHandler().getPlayerTurn().getCardsIn(ZoneType.Battlefield);
        list = CardLists.filter(list, new Predicate<Card>() {
            @Override
            public boolean apply(final Card c) {
                return c.hasStartOfKeyword("(Echo unpaid)");
            }
        });

        for (int i = 0; i < list.size(); i++) {
            final Card c = list.get(i);
            if (c.hasStartOfKeyword("(Echo unpaid)")) {
                final StringBuilder sb = new StringBuilder();
                sb.append("Echo for ").append(c).append("\n");
                String ref = "X".equals(c.getEchoCost()) ? " | References$ X" : "";
                String effect = "AB$ Sacrifice | Cost$ 0 | SacValid$ Self | "
                        + "Echo$ " + c.getEchoCost() + ref;

                SpellAbility sacAbility = AbilityFactory.getAbility(effect, c);
                sacAbility.setTrigger(true);
                sacAbility.setActivatingPlayer(c.getController());
                sacAbility.setStackDescription(sb.toString());
                sacAbility.setDescription(sb.toString());

                game.getStack().addSimultaneousStackEntry(sacAbility);

                c.removeAllExtrinsicKeyword("(Echo unpaid)");
            }
        }
    }

    private static void upkeepUpkeepCost(final Game game) {
        final CardCollectionView list = game.getPhaseHandler().getPlayerTurn().getCardsIn(ZoneType.Battlefield);

        for (int i = 0; i < list.size(); i++) {
            final Card c = list.get(i);
            final Player controller = c.getController();
            for (String ability : c.getKeywords()) {
                // sacrifice
                if (ability.startsWith("At the beginning of your upkeep, sacrifice")) {

                    final StringBuilder sb = new StringBuilder("Sacrifice upkeep for " + c);
                    final String[] k = ability.split(" pay ");
                    final String cost = k[1].replaceAll("[{]", "").replaceAll("[}]", " ");

                    String effect = "AB$ Sacrifice | Cost$ 0 | SacValid$ Self"
                            + "| UnlessPayer$ You | UnlessCost$ " + cost;

                    SpellAbility upkeepAbility = AbilityFactory.getAbility(effect, c);
                    upkeepAbility.setActivatingPlayer(controller);
                    upkeepAbility.setStackDescription(sb.toString());
                    upkeepAbility.setDescription(sb.toString());
                    upkeepAbility.setTrigger(true);

                    game.getStack().addSimultaneousStackEntry(upkeepAbility);
                }

                // Cumulative upkeep
                if (ability.startsWith("Cumulative upkeep")) {
                    final StringBuilder sb = new StringBuilder();
                    final String[] k = ability.split(":");
                    sb.append("Cumulative upkeep for " + c);

                    final Ability upkeepAbility = new Ability(c, ManaCost.ZERO) {
                        @Override
                        public void resolve() {
                            c.addCounter(CounterType.AGE, 1, true);
                            String cost = CardFactoryUtil.multiplyCost(k[1], c.getCounters(CounterType.AGE));
                            final Cost upkeepCost = new Cost(cost, true);
                            this.setCumulativeupkeep(true);
                            boolean isPaid = controller.getController().payManaOptional(c, upkeepCost, this, sb.toString(), ManaPaymentPurpose.CumulativeUpkeep);
                            final HashMap<String, Object> runParams = new HashMap<String, Object>();
                            runParams.put("CumulativeUpkeepPaid", Boolean.valueOf(isPaid));
                            runParams.put("Card", this.getHostCard());
                            runParams.put("PayingMana", StringUtils.join(this.getPayingMana(), ""));
                            game.getTriggerHandler().runTrigger(TriggerType.PayCumulativeUpkeep, runParams, false);
                            if (!isPaid) {
                                game.getAction().sacrifice(c, this);
                            }
                        }
                    };
                    sb.append("\n");
                    upkeepAbility.setActivatingPlayer(controller);
                    upkeepAbility.setStackDescription(sb.toString());
                    upkeepAbility.setDescription(sb.toString());

                    game.getStack().addSimultaneousStackEntry(upkeepAbility);
                }
            }
        }
    }
}
