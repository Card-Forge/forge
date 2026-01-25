package forge.game.card;

import java.util.List;

import forge.game.replacement.ReplacementEffect;
import forge.game.spellability.SpellAbility;
import forge.game.staticability.StaticAbility;
import forge.game.trigger.Trigger;

public interface ICardTraitChanges {
    default List<SpellAbility> applySpellAbility(List<SpellAbility> list) { return list;}
    default List<Trigger> applyTrigger(List<Trigger> list) { return list;}
    default List<ReplacementEffect> applyReplacementEffect(List<ReplacementEffect> list) { return list;}
    default List<StaticAbility> applyStaticAbility(List<StaticAbility> list) { return list;}
    
    default void changeText() {}
}
