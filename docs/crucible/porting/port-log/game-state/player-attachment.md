# Port Log — Game State: Auras on players (Curses), CR 303.4h and CR 704.5m

- **Parent:** [`game-state.md`](../game-state.md) — Java counterpart, model, index, `Not ported yet`
- **Go:** [`internal/engine`](../../../../../crucible/internal/engine) — `card.go` (`attachedPlayer`,
  `AttachedToPlayer`), `player.go` (`attachments`, `Attachments`), `game.go` (`AttachToPlayer`, `Unattach`, `Clone`),
  `castspell.go` (`castPlayerAura`, `attachEffect`), `action.go` (`enchantPlayerSpec`, `playerEnchantLegal`,
  `cleanupDanglingAttachments`), `valid.go`, `defined.go`;
  [`internal/fixture`](../../../../../crucible/internal/fixture) (`EnchantingPlayer:`)

Java: `Card.getPlayerAttachedTo` / `getEntityAttachedTo` (one field, a `GameEntity`), `GameEntity.getAttachedCards`,
`GameEntity.cantBeEnchantedByMsg` (`GameEntity.java:292`), `GameAction.stateBasedAction704_attach`
(`GameAction.java:1754`). 50 corpus cards carry `K:Enchant:Player` (46) or `K:Enchant:Opponent` (4).

| Piece | Go | Java |
| --- | --- | --- |
| Which player | `Card.attachedPlayer`, never set together with `attachedTo` | `Card.entityAttachedTo` |
| Reverse | `Player.attachments` (ordered), cloned by `Game.Clone` | `GameEntity.attachedCards` |
| Writers | `Game.Attach`, `AttachToPlayer`, `Unattach` only | `Card.attachToEntity` |
| Cast | `castPlayerAura`: legal players, one `ChooseTargets` question | `AttachEffect` / `getAuraSpell` target |
| Legal players | `matchesPlayerSpec` on `Player`/`Opponent`, then `playerCantBeTargetedBy` | `isValid(Enchant type)` plus `canBeTargetedBy` |
| Resolve | `attachEffect` reads the player from `Ability.Targets` | `AttachEffect.resolve` |
| Stays legal | `playerEnchantLegal`: player in the game and still fits the type | `cantBeEnchantedByMsg` |
| Non-Aura on a player | unattached, stays on the battlefield (CR 704.5n) | `isCreature() \|\| !canBeAttached` in 704_attach |

The target is a player, so it travels in `Ability.Targets` like any player target and `Ability.Target` stays `NoCard`.
`targetsStillLegal` therefore runs the ordinary per-entity legality check on it (hexproof, shroud, protection, `Lost`)
and the spell fizzles when the player is no longer a legal target. A lone legal player is assigned without a question.

What the player attachment feeds:

| Corpus form | Lines | Resolved by |
| --- | --- | --- |
| `Player.EnchantedBy`, `Opponent.EnchantedBy` (trigger, replacement, static valid strings) | 36+ | `matchesPlayerProperty` `EnchantedBy` |
| `Defined$ EnchantedPlayer`, `Enchanted`, `EnchantedController` | 13 | `definedPlayers` |
| `Creature.EnchantedPlayerCtrl`, `Card.EnchantedPlayer` | 4 | `propertyMatches`: owner/controller is the enchanted player |
| `Curse.AttachedTo Player.EnchantedBy`, `Curse.AttachedTo You` | 4 | `propertyMatches` `AttachedTo <restriction>` |

`AttachedTo <restriction>` is `CardProperty.java:441` for every attached object, not only for players: a card host is
matched through `Matches`, a player host through `matchesPlayerSpec`. The Java fallback to the ability's own defined
cards (`Targeted`, `ParentTarget`) needs an ability the evaluator is not given and matches nothing, as before.

Fixture: `EnchantingPlayer:P<seat>` (`GameState.java:368`, `:1388`) loads and dumps in place of `AttachedTo:`; `HUMAN`
and `AI` load too.

Scenarios: `cast-a-curse-attaches-to-the-chosen-player`, `curse-of-bloodletting-doubles-damage-to-the-enchanted-player`,
`curse-of-thirst-counts-the-curses-on-the-enchanted-player`. Tests: `curse_test.go`.

Not ported: `Defined$ TriggeredAttackingPlayer.Opponent+controlsCreature.attacking Player.EnchantedBy` (Curse of
Vitality and 2 more) is the filtered `Triggered` player form, an error until the `Triggered...` key plus filter is
built; Protection's player-side `CantAttach`; `CantAttach` statics against a player host.
