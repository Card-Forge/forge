package forge.card.abilityfactory.effects;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import forge.Card;
import forge.Command;
import forge.Singletons;
import forge.card.abilityfactory.AbilityFactory;
import forge.card.abilityfactory.SpellEffect;
import forge.card.spellability.AbilitySub;
import forge.card.spellability.SpellAbility;
import forge.card.spellability.Target;
import forge.game.player.Player;
import forge.game.zone.ZoneType;

public class PumpAllEffect extends SpellEffect {
    /**
     * <p>
     * pumpAllStackDescription.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @return a {@link java.lang.String} object.
     */
    @Override
    protected String getStackDescription(java.util.Map<String,String> params, SpellAbility sa) {
        final StringBuilder sb = new StringBuilder();

        String desc = "";
        if (params.containsKey("SpellDescription")) {
            desc = params.get("SpellDescription");
        } else if (params.containsKey("PumpAllDescription")) {
            desc = params.get("PumpAllDescription");
        }

        if (!(sa instanceof AbilitySub)) {
            sb.append(sa.getSourceCard()).append(" - ");
        } else {
            sb.append(" ");
        }
        sb.append(desc);

        final AbilitySub abSub = sa.getSubAbility();
        if (abSub != null) {
            sb.append(abSub.getStackDescription());
        }

        return sb.toString();
    } // pumpAllStackDescription()

    /**
     * <p>
     * pumpAllResolve.
     * </p>
     * 
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     */
    @Override
    public void resolve(java.util.Map<String,String> params, SpellAbility sa) {
        List<Card> list;
        ArrayList<Player> tgtPlayers = null;
        final ArrayList<ZoneType> affectedZones = new ArrayList<ZoneType>();
    
        final Target tgt = sa.getTarget();
        if (tgt != null) {
            tgtPlayers = tgt.getTargetPlayers();
        } else if (params.containsKey("Defined")) {
            // use it
            tgtPlayers = AbilityFactory.getDefinedPlayers(sa.getSourceCard(), params.get("Defined"), sa);
        }
    
        if (params.containsKey("PumpZone")) {
            for (final String zone : params.get("PumpZone").split(",")) {
                affectedZones.add(ZoneType.valueOf(zone));
            }
        } else {
            affectedZones.add(ZoneType.Battlefield);
        }
    
        list = new ArrayList<Card>();
        if ((tgtPlayers == null) || tgtPlayers.isEmpty()) {
            for (final ZoneType zone : affectedZones) {
                list.addAll(Singletons.getModel().getGame().getCardsIn(zone));
            }
    
        } else {
            for (final ZoneType zone : affectedZones) {
                list.addAll(tgtPlayers.get(0).getCardsIn(zone));
            }
        }
    
        String valid = "";
        if (params.containsKey("ValidCards")) {
            valid = params.get("ValidCards");
        }
    
        list = AbilityFactory.filterListByType(list, valid, sa);
    
        final List<String> keywords = params.containsKey("KW") ? Arrays.asList(params.get("KW").split(" & ")) : new ArrayList<String>();
        final int a = AbilityFactory.calculateAmount(sa.getSourceCard(), params.get("NumAtt"), sa); 
        final int d = AbilityFactory.calculateAmount(sa.getSourceCard(), params.get("NumDef"), sa); 
    
        for (final Card c : list) {
            final Card tgtC = c;
    
            // only pump things in the affected zones.
            boolean found = false;
            for (final ZoneType z : affectedZones) {
                if (c.isInZone(z)) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                continue;
            }
    
            tgtC.addTempAttackBoost(a);
            tgtC.addTempDefenseBoost(d);
    
            for (int i = 0; i < keywords.size(); i++) {
                tgtC.addExtrinsicKeyword(keywords.get(i));
            }
    
            if (!params.containsKey("Permanent")) {
                // If not Permanent, remove Pumped at EOT
                final Command untilEOT = new Command() {
                    private static final long serialVersionUID = 5415795460189457660L;
    
                    @Override
                    public void execute() {
                        tgtC.addTempAttackBoost(-1 * a);
                        tgtC.addTempDefenseBoost(-1 * d);
    
                        if (keywords.size() > 0) {
                            for (int i = 0; i < keywords.size(); i++) {
                                tgtC.removeExtrinsicKeyword(keywords.get(i));
                            }
                        }
                    }
                };
                if (params.containsKey("UntilUntaps")) {
                    sa.getSourceCard().addUntapCommand(untilEOT);
                } else {
                    Singletons.getModel().getGame().getEndOfTurn().addUntil(untilEOT);
                }
            }
        }
    } // pumpAllResolve()

} // end class AbilityFactory_Pump