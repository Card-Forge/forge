package forge.game.ability.effects;

import forge.game.Game;
import forge.game.ability.AbilityUtils;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardCollectionView;
import forge.game.card.CardLists;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

import java.util.List;

public class DamageAllEffect extends SpellAbilityEffect {
    @Override
    protected String getStackDescription(SpellAbility sa) {
        final StringBuilder sb = new StringBuilder();

        String desc = "";
        if (sa.hasParam("ValidDescription")) {
            desc = sa.getParam("ValidDescription");
        }

        final String damage = sa.getParam("NumDmg");
        final int dmg = AbilityUtils.calculateAmount(sa.getHostCard(), damage, sa);


        final List<Card> definedSources = AbilityUtils.getDefinedCards(sa.getHostCard(), sa.getParam("DamageSource"), sa);
        final Card source = definedSources.get(0);

        if (source != sa.getHostCard()) {
            sb.append(source.toString()).append(" deals");
        } else {
            sb.append("Deals");
        }

        sb.append(" ").append(dmg).append(" damage to ").append(desc);

            return sb.toString();
    }

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

        int damageSum = 0;
        for (final Card c : list) {
            int cardDamage = c.addDamage(dmg, sourceLKI);
            damageSum += cardDamage;
            if (cardDamage > 0 && rememberCard) {
                source.addRemembered(c);
            }
        }

        if (!players.equals("")) {
            final List<Player> playerList = AbilityUtils.getDefinedPlayers(card, players, sa);
            for (final Player p : playerList) {
                int playerDamage = p.addDamage(dmg, sourceLKI);
                damageSum += playerDamage;
                if (playerDamage > 0 && rememberPlayer) {
                    source.addRemembered(p);
                }
            }
        }

        if (damageSum > 0 && sourceLKI.hasKeyword("Lifelink")) {
            source.getController().gainLife(damageSum, sourceLKI);
        }
    }
}
