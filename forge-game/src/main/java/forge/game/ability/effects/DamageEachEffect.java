package forge.game.ability.effects;

import forge.game.Game;
import forge.game.GameEntity;
import forge.game.GameEntityCounterTable;
import forge.game.ability.AbilityUtils;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardDamageMap;
import forge.game.card.CardLists;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;
import forge.util.Lang;
import forge.util.collect.FCollectionView;

import java.util.List;

public class DamageEachEffect extends DamageBaseEffect {

    /* (non-Javadoc)
     * @see forge.card.abilityfactory.SpellEffect#getStackDescription(java.util.Map, forge.card.spellability.SpellAbility)
     */
    @Override
    protected String getStackDescription(SpellAbility sa) {
        final StringBuilder sb = new StringBuilder();
        final int iDmg = AbilityUtils.calculateAmount(sa.getHostCard(), sa.getParam("NumDmg"), sa);

        String desc = sa.getParamOrDefault("ValidCards", "");
        if (sa.hasParam("ValidDescription")) {
            desc = sa.getParam("ValidDescription");
        }

        String dmg = "";
        if (sa.hasParam("DamageDesc")) {
            dmg = sa.getParam("DamageDesc");
        } else {
            dmg += iDmg + " damage";
        }

        if (sa.hasParam("StackDescription")) {
            sb.append(sa.getParam("StackDescription"));
        } else {
            sb.append("Each ").append(desc).append(" deals ").append(dmg).append(" to ");
            sb.append(Lang.joinHomogenous(getTargetEntities(sa)));
        }
        sb.append(".");
        return sb.toString();
    }


    /* (non-Javadoc)
     * @see forge.card.abilityfactory.SpellEffect#resolve(java.util.Map, forge.card.spellability.SpellAbility)
     */
    @Override
    public void resolve(SpellAbility sa) {
        final Card card = sa.getHostCard();
        final Game game = card.getGame();
        final String num = sa.getParamOrDefault("NumDmg", "X");

        FCollectionView<Card> sources = game.getCardsIn(ZoneType.Battlefield);
        if (sa.hasParam("ValidCards")) {
            sources = CardLists.getValidCards(sources, sa.getParam("ValidCards"), sa.getActivatingPlayer(), card, sa);
        } else if (sa.hasParam("DefinedDamagers")) {
            List<Card> defined = AbilityUtils.getDefinedCards(card, sa.getParam("DefinedDamagers"), sa);
            CardCollection sources2 = new CardCollection();
            for (Card c : defined) {
                if (sources.contains(c)) {
                    sources2.add(c);
                }
            }
            sources = sources2;
        }

        boolean usedDamageMap = true;
        CardDamageMap damageMap = sa.getDamageMap();
        CardDamageMap preventMap = sa.getPreventMap();
        GameEntityCounterTable counterTable = sa.getCounterTable();

        if (damageMap == null) {
            // make a new damage map
            damageMap = new CardDamageMap();
            preventMap = new CardDamageMap();
            counterTable = new GameEntityCounterTable();
            usedDamageMap = false;
        }

        if (sa.hasParam("EachToItself")) {
            for (final Card source : sources) {
                final Card sourceLKI = game.getChangeZoneLKIInfo(source);

                final int dmg = AbilityUtils.calculateAmount(source, num, sa);
                damageMap.put(sourceLKI, source, dmg);
            }
        } else for (final GameEntity ge : getTargetEntities(sa)) {
            for (final Card source : sources) {
                final Card sourceLKI = game.getChangeZoneLKIInfo(source);

                final int dmg = AbilityUtils.calculateAmount(source, num, sa);

                if (ge instanceof Card) {
                    final Card c = (Card) ge;
                    if (c.isInPlay() && !c.isPhasedOut()) {
                        damageMap.put(sourceLKI, c, dmg);
                    }
                } else {
                    damageMap.put(sourceLKI, ge, dmg);
                }
            }
        }

        if (!usedDamageMap) {
            game.getAction().dealDamage(false, damageMap, preventMap, counterTable, sa);
        }

        replaceDying(sa);
    }
}
