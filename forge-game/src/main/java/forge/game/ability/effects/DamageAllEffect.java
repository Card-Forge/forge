package forge.game.ability.effects;

import forge.game.Game;
import forge.game.GameEntity;
import forge.game.GameEntityCounterTable;
import forge.game.ability.AbilityUtils;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardCollectionView;
import forge.game.card.CardDamageMap;
import forge.game.card.CardLists;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

import java.util.List;

public class DamageAllEffect extends DamageBaseEffect {
    /* (non-Javadoc)
     * @see forge.game.ability.SpellAbilityEffect#getStackDescription(forge.game.spellability.SpellAbility)
     */
    @Override
    protected String getStackDescription(SpellAbility sa) {
        final StringBuilder sb = new StringBuilder();

        String desc = "";
        if (sa.hasParam("ValidDescription")) {
            desc = sa.getParam("ValidDescription");
        }

        final String damage = sa.getParam("NumDmg");
        final int dmg = AbilityUtils.calculateAmount(sa.getHostCard(), damage, sa);

        final String definedStr = sa.getParam("DamageSource");
        final List<Card> definedSources = AbilityUtils.getDefinedCards(sa.getHostCard(), definedStr, sa);

        if (!definedSources.isEmpty() && definedSources.get(0) != sa.getHostCard()) {
            sb.append(definedSources.get(0).toString()).append(" deals");
        } else if ("ParentTarget".equals(definedStr)){
            sb.append("Target creature deals");
        } else {
            sb.append("Deals");
        }

        sb.append(" ").append(dmg).append(" damage to ").append(desc);

        return sb.toString();
    }

    /* (non-Javadoc)
     * @see forge.game.ability.SpellAbilityEffect#resolve(forge.game.spellability.SpellAbility)
     */
    @Override
    public void resolve(SpellAbility sa) {
        final List<Card> definedSources = AbilityUtils.getDefinedCards(sa.getHostCard(), sa.getParam("DamageSource"), sa);
        final Card card = definedSources.get(0);
        final Card sourceLKI = card.getGame().getChangeZoneLKIInfo(card);
        final Card source = sa.getHostCard();
        final Game game = sa.getActivatingPlayer().getGame();

        final String damage = sa.getParam("NumDmg");
        final int dmg = AbilityUtils.calculateAmount(sa.getHostCard(), damage, sa);

        final boolean rememberCard = sa.hasParam("RememberDamaged") || sa.hasParam("RememberDamagedCreature");
        final boolean rememberPlayer = sa.hasParam("RememberDamaged") || sa.hasParam("RememberDamagedPlayer");

        Player targetPlayer = sa.getTargets().getFirstTargetedPlayer();

        String players = "";

        if (sa.hasParam("ValidPlayers")) {
            players = sa.getParam("ValidPlayers");
        }

        CardCollectionView list;
        if (sa.hasParam("ValidCards")) {
            list = game.getCardsIn(ZoneType.Battlefield);
        }
        else {
            list = CardCollection.EMPTY;
        }

        if (targetPlayer != null) {
            list = CardLists.filterControlledBy(list, targetPlayer);
        }

        list = AbilityUtils.filterListByType(list, sa.getParam("ValidCards"), sa);

        boolean usedDamageMap = true;
        CardDamageMap damageMap = sa.getDamageMap();
        CardDamageMap preventMap = sa.getPreventMap();
        GameEntityCounterTable counterTable = new GameEntityCounterTable();

        if (damageMap == null) {
            // make a new damage map
            damageMap = new CardDamageMap();
            preventMap = new CardDamageMap();
            usedDamageMap = false;
        }

        for (final Card c : list) {
            c.addDamage(dmg, sourceLKI, damageMap, preventMap, counterTable, sa);
        }

        if (!players.equals("")) {
            final List<Player> playerList = AbilityUtils.getDefinedPlayers(card, players, sa);
            for (final Player p : playerList) {
                p.addDamage(dmg, sourceLKI, damageMap, preventMap, counterTable, sa);
            }
        }

        // do Remember there
        if (rememberCard || rememberPlayer) {
            for (GameEntity e : damageMap.row(sourceLKI).keySet()) {
                if (e instanceof Card && rememberCard) {
                    source.addRemembered(e);
                } else if (e instanceof Player && rememberPlayer) {
                    source.addRemembered(e);
                }
            }
        }

        if (!usedDamageMap) {
            preventMap.triggerPreventDamage(false);
            damageMap.triggerDamageDoneOnce(false, game, sa);

            preventMap.clear();
            damageMap.clear();
        }

        counterTable.triggerCountersPutAll(game);

        replaceDying(sa);
    }
}
