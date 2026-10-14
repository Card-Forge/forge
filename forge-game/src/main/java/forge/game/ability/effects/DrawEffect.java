package forge.game.ability.effects;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.google.common.collect.Sets;

import forge.game.ability.AbilityKey;
import forge.game.ability.AbilityUtils;
import forge.game.ability.SpellAbilityEffect;
import forge.game.GameAction;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardCollectionView;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.staticability.StaticAbilityCantDraw;
import forge.game.zone.ZoneType;
import forge.util.Lang;
import forge.util.Localizer;

public class DrawEffect extends SpellAbilityEffect {

    @Override
    public boolean movesCardToOrFromLibrary(final SpellAbility sa) {
        return true;
    }

    @Override
    protected String getStackDescription(SpellAbility sa) {
        final StringBuilder sb = new StringBuilder();

        if (sa.hasParam("IfDesc")) {
            if (sa.getParam("IfDesc").equals("True") && sa.hasParam("SpellDescription")) {
                String ifDesc = sa.getParam("SpellDescription");
                sb.append(ifDesc, 0, ifDesc.indexOf(",") + 1);
            } else {
                sb.append(sa.getParam("IfDesc"));
            }
            sb.append(" ");
        }

        final List<Player> tgtPlayers = getDefinedPlayersOrTargeted(sa);

        if (!tgtPlayers.isEmpty()) {
            int numCards = sa.hasParam("NumCards") ? AbilityUtils.calculateAmount(sa.getHostCard(), sa.getParam("NumCards"), sa) : 1;

            sb.append(Lang.joinHomogenous(tgtPlayers));

            if (tgtPlayers.size() > 1) {
                sb.append(" each");
            }
            sb.append(Lang.joinVerb(tgtPlayers, " draw")).append(" ");
            //if NumCards calculation could change between getStackDescription and resolve, use NumCardsDesc to avoid
            //a "wrong" stack description
            sb.append(sa.hasParam("NumCardsDesc") ? sa.getParam("NumCardsDesc") : numCards == 1 ? "a card" :
                    (Lang.getNumeral(numCards) + " cards"));
            sb.append(".");
        }

        return sb.toString();
    }

    @Override
    public void resolve(SpellAbility sa) {
        final Card source = sa.getHostCard();
        final int numCards = sa.hasParam("NumCards") ? AbilityUtils.calculateAmount(sa.getHostCard(), sa.getParam("NumCards"), sa) : 1;
        final boolean upto = sa.hasParam("Upto");
        final boolean optional = sa.hasParam("OptionalDecider") || upto;
        Map<AbilityKey, Object> moveParams = AbilityKey.newMap();
        moveParams.put(AbilityKey.LastStateBattlefield, sa.getLastStateBattlefield());
        moveParams.put(AbilityKey.LastStateGraveyard, sa.getLastStateGraveyard());

        final List<Player> tgts = getTargetPlayersWithDuplicates(true, "Defined", sa);

        if (sa.getHostCard().getGame().getRules().isDanDan()) {
            resolveDanDanDrawInstruction(sa, source, tgts, numCards, optional, upto, moveParams);
            return;
        }

        for (final Player p : Sets.newLinkedHashSet(tgts)) {
            if (!p.isInGame()) {
                continue;
            }

            int actualNum = numCards * Collections.frequency(tgts, p);

            // it is optional, not upto and player can't choose to draw that many cards
            if (optional && !upto && !p.canDraw(actualNum)) {
                continue;
            }

            if (optional && !p.getController().confirmAction(sa, null, Localizer.getInstance().getMessage("lblDoYouWantDrawCards", Lang.nounWithAmount(actualNum, " card")), null)) {
                continue;
            }

            if (upto) { // if it is upto, player can only choose how many cards they can draw
                actualNum = StaticAbilityCantDraw.canDrawAmount(p, actualNum);
            }
            if (actualNum <= 0) {
                continue;
            }
            if (upto) {
                actualNum = p.getController().chooseNumber(sa, Localizer.getInstance().getMessage("lblHowManyCardDoYouWantDraw"), 0, actualNum);
            }

            final CardCollectionView drawn = p.drawCards(actualNum, sa, moveParams);
            if (sa.hasParam("Reveal")) {
                p.getGame().getAction().reveal(drawn, p, !sa.getParam("Reveal").equals("All"));
            }
            if (sa.hasParam("RememberDrawn")) {
                source.addRemembered(drawn);
            }
        }
    }

    private void resolveDanDanDrawInstruction(SpellAbility sa, Card source, List<Player> targets, int numCards,
            boolean optional, boolean upto, Map<AbilityKey, Object> moveParams) {
        final GameAction action = source.getGame().getAction();
        GameAction.DrawInstruction instruction = source.getGame().getReplacementHandler()
                .getCapturedDrawInstruction(sa);
        final boolean captured = instruction != null;
        if (!captured) {
            instruction = action.newDanDanDrawInstruction();
        }

        for (final Player player : Sets.newLinkedHashSet(targets)) {
            if (!player.isInGame()) {
                continue;
            }

            int actualNum = numCards * Collections.frequency(targets, player);
            if (optional && !upto && !player.canDraw(actualNum)) {
                continue;
            }
            if (optional && !player.getController().confirmAction(sa, null,
                    Localizer.getInstance().getMessage("lblDoYouWantDrawCards", Lang.nounWithAmount(actualNum, " card")), null)) {
                continue;
            }

            if (upto) {
                actualNum = StaticAbilityCantDraw.canDrawAmount(player, actualNum);
            }
            if (actualNum <= 0) {
                continue;
            }
            if (upto) {
                actualNum = player.getController().chooseNumber(sa,
                        Localizer.getInstance().getMessage("lblHowManyCardDoYouWantDraw"), 0, actualNum);
            }
            if (actualNum <= 0) {
                continue;
            }

            Player.DrawBatch batch = player.prepareDanDanDrawBatch(actualNum, sa, moveParams,
                    player.getZone(ZoneType.Hand), instruction);
            if (batch == null) {
                applyDrawResult(sa, source, player, new CardCollection());
            } else {
                batch.setCompletionAction(() -> applyDrawResult(sa, source, player, batch.getDrawnCards()));
            }
        }

        if (!captured) {
            action.resolveDanDanDrawInstruction(instruction);
        }
    }

    private void applyDrawResult(SpellAbility sa, Card source, Player player, CardCollectionView drawn) {
        if (sa.hasParam("Reveal")) {
            player.getGame().getAction().reveal(drawn, player, !sa.getParam("Reveal").equals("All"));
        }
        if (sa.hasParam("RememberDrawn")) {
            source.addRemembered(drawn);
        }
    }
}
