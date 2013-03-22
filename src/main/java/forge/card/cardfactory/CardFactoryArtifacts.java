package forge.card.cardfactory;

import java.util.ArrayList;
import java.util.List;


import forge.Card;
import forge.card.cost.Cost;
import forge.card.spellability.AbilityActivated;
import forge.card.spellability.Target;
import forge.game.player.Player;
import forge.game.zone.ZoneType;

/** 
 * TODO: Write javadoc for this type.
 *
 */
class CardFactoryArtifacts {

    /**
     * TODO: Write javadoc for this method.
     * @param card
     * @param cardName
     * @return
     */
    public static void buildCard(final Card card, final String cardName) {

        // *************** START *********** START **************************
        if (cardName.equals("Grindstone")) {

            class AbilityGrindstone extends AbilityActivated {
                public AbilityGrindstone(final Card ca, final Cost co, final Target t) {
                    super(ca, co, t);
                }

                @Override
                public AbilityActivated getCopy() {
                    AbilityActivated res = new AbilityGrindstone(getSourceCard(),
                            getPayCosts(), getTarget() == null ? null : new Target(getTarget()));
                    CardFactory.copySpellAbility(this, res);
                    return res;
                }

                private static final long serialVersionUID = -6281219446216L;

                @Override
                public boolean canPlayAI() {
                    this.getTarget().resetTargets();
                    Player human = getActivatingPlayer().getOpponent();
                    final List<Card> libList = human.getCardsIn(ZoneType.Library);
                    this.getTarget().addTarget(human);
                    return !libList.isEmpty() && canTarget(human);
                }

                @Override
                public void resolve() {
                    final Player target = this.getTargetPlayer();
                    final List<Card> library = new ArrayList<Card>(this.getTargetPlayer().getCardsIn(ZoneType.Library));

                    boolean loop = true;
                    final List<Card> grinding = new ArrayList<Card>();
                    do {
                        grinding.clear();

                        for (int i = 0; i < 2; i++) {
                            // Move current grinding to a different list
                            if (library.size() > 0) {
                                final Card c = library.get(0);
                                grinding.add(c);
                                library.remove(c);
                            } else {
                                loop = false;
                                break;
                            }
                        }

                        // if current grinding dont share a color, stop grinding
                        if (loop) {
                            loop = grinding.get(0).sharesColorWith(grinding.get(1));
                        }
                        target.mill(grinding.size());
                    } while (loop);
                }
            }

            final Target target = new Target(card, "Select target player", new String[] { "Player" });
            final Cost abCost = new Cost(card, "3 T", true);
            final AbilityActivated ab1 = new AbilityGrindstone(card, abCost, target);

            final StringBuilder sb = new StringBuilder();
            sb.append(abCost);
            sb.append("Put the top two cards of target player's library into that player's graveyard. ");
            sb.append("If both cards share a color, repeat this process.");
            ab1.setDescription(sb.toString());
            ab1.setStackDescription(sb.toString());
            card.addSpellAbility(ab1);
        } // *************** END ************ END **************************
    }
}
