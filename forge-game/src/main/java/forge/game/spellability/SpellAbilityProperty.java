package forge.game.spellability;

import forge.card.mana.ManaAtom;
import forge.game.CardTraitBase;
import forge.game.Game;
import forge.game.GameObject;
import forge.game.ability.AbilityUtils;
import forge.game.ability.ApiType;
import forge.game.card.Card;
import forge.game.card.CounterEnumType;
import forge.game.cost.Cost;
import forge.game.keyword.Keyword;
import forge.game.mana.Mana;
import forge.game.mana.ManaCostBeingPaid;
import forge.game.player.Player;
import forge.game.staticability.StaticAbility;
import forge.game.staticability.StaticAbilityCastWithFlash;
import forge.game.zone.ZoneType;
import forge.util.Expressions;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public class SpellAbilityProperty {
    public static boolean hasProperty(SpellAbility sa, Player sourceController, Card source, String property, CardTraitBase spellAbility) {
        if (property.equals("ManaAbility")) {
            return sa.isManaAbility();
        } else if (property.equals("withoutXCost")) {
            return !sa.costHasManaX();
        } else if (property.startsWith("XCost")) {
            String comparator = property.substring(5, 7);
            int y = AbilityUtils.calculateAmount(sa.getHostCard(), property.substring(7), sa);
            return Expressions.compare(sa.getXManaCostPaid() == null ? 0 : sa.getXManaCostPaid(), comparator, y);
        } else if (property.equals("hasTapCost")) {
            Cost cost = sa.getPayCosts();
            return cost != null && cost.hasTapCost();
        } else if (property.equals("Bargain")) {
            return sa.isBargained();
        } else if (property.equals("Backup")) {
            return sa.isBackup();
        } else if (property.equals("Bestow")) {
            return sa.isBestow();
        } else if (property.equals("Blitz")) {
            return sa.isBlitz();
        } else if (property.equals("Buyback")) {
            return sa.isBuyback();
        } else if (property.equals("Craft")) {
            return sa.isCraft();
        } else if (property.equals("Crew")) {
            return sa.isCrew();
        } else if (property.equals("Saddle")) {
            return sa.isKeyword(Keyword.SADDLE);
        } else if (property.equals("Station")) {
            return sa.isKeyword(Keyword.STATION);
        } else if (property.equals("Cycling")) {
            return sa.isCycling();
        } else if (property.equals("Dash")) {
            return sa.isDash();
        } else if (property.equals("Disturb")) {
            return sa.isDisturb();
        } else if (property.equals("Embalm")) {
            return sa.isEmbalm();
        } else if (property.equals("Eternalize")) {
            return sa.isEternalize();
        } else if (property.equals("Flashback")) {
            return sa.isFlashback();
        } else if (property.equals("Harmonize")) {
            return sa.isHarmonize();
        } else if (property.equals("Jumpstart")) {
            return sa.isJumpstart();
        } else if (property.equals("Kicked")) {
            return sa.isKicked();
        } else if (property.equals("Loyalty")) {
            return sa.isPwAbility();
        } else if (property.equals("Aftermath")) {
            return sa.isAftermath();
        } else if (property.equals("PowerUp")) {
            return sa.isPowerUp();
        } else if (property.equals("MorphUp")) {
            return sa.isMorphUp();
        } else if (property.equals("ManifestUp")) {
            return sa.isManifestUp();
        } else if (property.equals("Teamwork")) {
            return sa.isTeamwork();
        } else if (property.equals("Unlock")) {
            return sa.isUnlock();
        } else if (property.equals("isTurnFaceUp")) {
            return sa.isTurnFaceUp();
        } else if (property.equals("isCastFaceDown")) {
            return sa.isCastFaceDown();
        } else if (property.equals("Unearth")) {
            return sa.isKeyword(Keyword.UNEARTH);
        } else if (property.equals("Modular")) {
            return sa.isKeyword(Keyword.MODULAR);
        } else if (property.equals("Equip")) {
            return sa.isEquip();
        } else if (property.equals("Boast")) {
            return sa.isBoast();
        } else if (property.equals("Monstrosity")) {
            return sa.isMonstrosity();
        } else if (property.equals("Exhaust")) {
            return sa.isExhaust();
        } else if (property.equals("Mayhem")) {
            return sa.isMayhem();
        } else if (property.equals("Mutate")) {
            return sa.isMutate();
        } else if (property.equals("Ninjutsu")) {
            return sa.isNinjutsu();
        } else if (property.equals("Sneak")) {
            return sa.isSneak();
        } else if (property.equals("Foretelling")) {
            return sa.isForetelling();
        } else if (property.equals("Foretold")) {
            return sa.isForetold();
        } else if (property.equals("Plotting")) {
            return sa.isPlotting();
        } else if (property.equals("Outlast")) {
            return sa.isOutlast();
        } else if (property.equals("Modal")) {
            return sa.getApi() == ApiType.Charm;
        } else if (property.equals("ClassLevelUp")) {
            return sa.getApi() == ApiType.ClassLevelUp;
        } else if (property.equals("Daybound")) {
            return sa.isKeyword(Keyword.DAYBOUND);
        } else if (property.equals("Nightbound")) {
            return sa.isKeyword(Keyword.NIGHTBOUND);
        } else if (property.equals("Warp")) {
            return sa.isWarp();
        } else if (property.equals("Ward")) {
            return sa.isKeyword(Keyword.WARD);
        } else if (property.equals("CumulativeUpkeep")) {
            return sa.isCumulativeUpkeep();
        } else if (property.equals("SameKeyword")) {
            if (sa.getKeyword() == null || spellAbility == null) {
                return false;
            }
            return Objects.equals(sa.getKeyword(), spellAbility.getKeyword());
        } else if (property.equals("ChapterNotLore")) {
            if (!sa.isChapter()) {
                return false;
            }
            if (sa.getChapter() == sa.getHostCard().getCounters(CounterEnumType.LORE)) {
                return false;
            }
        } else if (property.equals("EffectSourceAbility")) {
            if (!source.isImmutable()) {
                return false;
            }
            if (source.getEffectSourceAbility() == null) {
                return false;
            }
            if (!sa.equals(source.getEffectSourceAbility().getRootAbility().getOriginalAbility())) {
                return false;
            }
        } else if (property.equals("LastChapter")) {
            return sa.isLastChapter();
        } else if (property.equals("paidPhyrexianMana")) {
            return sa.getSpendPhyrexianMana() > 0;
        } else if (property.startsWith("ManaSpent")) {
            String[] k = property.split(" ", 2);
            String comparator = k[1].substring(0, 2);
            int y = AbilityUtils.calculateAmount(source, k[1].substring(2), spellAbility);
            return Expressions.compare(sa.getTotalManaSpent(), comparator, y);
        } else if (property.startsWith("ManaFrom")) {
            String fromWhat = property.substring(8);
            String[] parts = null;
            if (fromWhat.contains("_")) {
                parts = fromWhat.split("_");
                fromWhat = parts[0];
            }
            int toFind = parts != null ? AbilityUtils.calculateAmount(source, parts[1], spellAbility) : 1;
            int found = 0;
            for (Mana m : sa.getPayingMana()) {
                final Card manaSource = m.getSourceCard();
                if (manaSource != null) {
                    if (manaSource.isValid(fromWhat, sourceController, source, spellAbility)) {
                        found++;
                        if (found == toFind) {
                            break;
                        }
                    }
                }
            }
            return (found == toFind);
        } else if (property.equals("MayPlaySource")) {
            StaticAbility m = sa.getMayPlay();
            if (m == null) {
                return false;
            }
            return source.equals(m.getHostCard());
        } else if (property.startsWith("singleTarget")) {
            // this doesn't allow a second target, even if same object
            int num = 0;
            for (TargetChoices tc : sa.getAllTargetChoices()) {
                num += tc.size();
                if (num > 1) {
                    return false;
                }
            }
            if (num != 1) {
                return false;
            }
        } else if (property.startsWith("numTargets")) {
            Set<GameObject> targets = new HashSet<>();
            for (TargetChoices tc : sa.getAllTargetChoices()) {
                targets.addAll(tc);
            }
            String[] k = property.split(" ", 2);
            String comparator = k[1].substring(0, 2);
            int y = AbilityUtils.calculateAmount(sa.getHostCard(), k[1].substring(2), sa);
            return Expressions.compare(targets.size(), comparator, y);
        } else if (property.startsWith("IsTargeting")) {
            String[] k = property.split(" ", 2);
            String unescaped = k[1].replace("~", "+");
            boolean found = false;
            for (GameObject o : AbilityUtils.getDefinedObjects(source, unescaped, spellAbility)) {
                if (sa.getRootAbility().isTargeting(o)) {
                    found = true;
                    break;
                }
            }
            return found;
        } else if (property.equals("YouCtrl")) {
            return sa.getActivatingPlayer().equals(sourceController);
        } else if (property.equals("OppCtrl")) {
            return sa.getActivatingPlayer().isOpponentOf(sourceController);
        } else if (property.startsWith("cmc")) {
            int y;
            // spell was on the stack
            if (sa.getHostCard().isInZone(ZoneType.Stack)) {
                y = sa.getHostCard().getCMC();
            } else {
                y = sa.getPayCosts().getTotalMana().getCMC();
            }
            int x = AbilityUtils.calculateAmount(source, property.substring(5), spellAbility);
            if (!Expressions.compare(y, property, x)) {
                return false;
            }
        } else if (property.equals("ManaAbilityCantPaidFor")) {
            SpellAbility paidFor = sourceController.getPaidForSA();
            if (paidFor == null) {
                return false;
            }
            ManaCostBeingPaid manaCost = paidFor.getManaCostBeingPaid();
            // The following code is taken from InputPayMana.java, to determine if this mana ability can pay for SA currently being paid
            byte colorCanUse = 0;
            for (final byte color : ManaAtom.MANATYPES) {
                if (manaCost.isAnyPartPayableWith(color, sourceController.getManaPool())) {
                    colorCanUse |= color;
                }
            }
            if (manaCost.isAnyPartPayableWith((byte) ManaAtom.GENERIC, sourceController.getManaPool())) {
                colorCanUse |= ManaAtom.GENERIC;
            }
            if (sa.isManaAbilityFor(paidFor, colorCanUse)) {
                return false;
            }
        } else if (property.equals("NamedSpell")) {
            boolean found = false;
            for (String name : source.getNamedCards()) {
                if (sa.getCardState().getName().equals(name)) {
                    found = true;
                    break;
                }
            }
            return found;
        } else if (property.equals("otherAbility")) {
            if (sa.equals(spellAbility)) {
                return false;
            }
            if (spellAbility instanceof SpellAbility) {
                SpellAbility sourceSpell = (SpellAbility) spellAbility;
                if (sa.getRootAbility().equals(sourceSpell.getRootAbility())) {
                    return false;
                }
            }
        } else if (property.equals("CouldCastTiming")) {
            Card host = sa.getHostCard();
            Game game = host.getGame();
            if (game.getStack().isSplitSecondOnStack()) {
                return false;
            }
            // Adapted from SpellAbility.canCastTiming, to determine if the SA could be cast at the current timing (assuming the controller had priority).

            if (sourceController.canCastSorcery() || sa.getRestrictions().isInstantSpeed()) {
                return true;
            }
            if (sa.isSpell()) {
                return host.isInstant() || host.hasKeyword(Keyword.FLASH) || StaticAbilityCastWithFlash.anyWithFlash(sa, host, sourceController);
            }
            if (sa.isActivatedAbility()) {
                return !sa.isPwAbility() && !sa.getRestrictions().isSorcerySpeed();
            }
            return true;
        } else if (property.startsWith("NamedAbility")) {
            return sa.getName().equals(property.substring(12));
        } else if (sa.getHostCard() != null) {
            return sa.getHostCard().hasProperty(property, sourceController, source, spellAbility);
        }

        return true;
    }
}
