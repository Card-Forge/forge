package forge;

import forge.card.spellability.SpellAbility;

public interface ITargetable {
    boolean canBeTargetedBy(final SpellAbility sa);
}