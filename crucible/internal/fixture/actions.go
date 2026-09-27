// A scenario's actions.log: the ordered, explicit decisions TEST-5 fixtures
// script against a loaded game.

package fixture

import (
	"bufio"
	"fmt"
	"io"
	"strconv"
	"strings"

	"github.com/jczastkiewicz/crucible/internal/engine"
	"github.com/jczastkiewicz/crucible/internal/mana"
)

// RunActions executes a TEST-5 actions.log against a loaded game.
//
// There is no Java counterpart: Plan Section 3.3's Layer 2 names
// setup.state/actions.log/expected as the scenario shape, but the line
// format inside actions.log is Crucible's own -- Forge's differential
// tooling reads setup.state and dumps expected/ independently, and never
// needs a scripted-action format of its own because it drives a real
// PlayerController from Java code, not from text.
//
// Because no AI is involved, this is what TEST-5 promises: any divergence
// between the resulting game and expect.state is a rules bug, never an AI
// one, the same reasoning Layer 3's replay parity applies to a full game
// (Plan Section 3.3).
//
// A blank line or one starting with # is skipped, matching setup.state's own
// convention. Each other line is one action:
//
//	startturn <player>            Game.StartTurn(player, controller)
//	advance [n]                   Game.AdvancePhase(controller), n times (default 1)
//	step [n]                      Game.Step(engine.NewRegistry(), controller), n times (default 1), ADR-0026
//	run <turns>                   Game.Run(engine.NewRegistry(), controller, turns), ADR-0026
//	dealopeninghands              DealOpeningHands(game, controller), starting player discarded
//	mulligan <firstplayer>        PerformMulligans(game, controller, firstplayer)
//	declareattackers              Game.DeclareCombatAttackers(controller)
//	declareblockers               Game.DeclareCombatBlockers(controller)
//	firststrikedamage             Game.DealFirstStrikeDamage(controller)
//	combatdamage                  Game.DealCombatDamage(controller)
//	queue keephand <bool>         ScriptedController.QueueKeepHand
//	queue tuck <id>[,<id>...]     ScriptedController.QueueTuck, ids from CardByFixtureID
//	queue startingplayer <p>      ScriptedController.QueueStartingPlayer
//	queue startinghand <n>        ScriptedController.QueueStartingHand
//	queue legendarykeep <id>      ScriptedController.QueueLegendaryToKeep, id from CardByFixtureID
//	queue enchanttarget <id>      ScriptedController.QueueEnchantTarget, id from CardByFixtureID
//	queue attackers [<id>,...]    ScriptedController.QueueAttackers, ids from CardByFixtureID (no ids declines)
//	queue exertattackers [<id>,...]  ScriptedController.QueueExertAttackers, ids from CardByFixtureID (no ids/"none" declines)
//	queue attacktarget <p>|<id>   ScriptedController.QueueAttackTarget, a player name or a planeswalker/battle's CardByFixtureID
//	queue blocks [<b>=<a>,...]    ScriptedController.QueueBlocks, blocker=attacker pairs from CardByFixtureID (no pairs declines)
//	queue damage <b>=<n>[,...]    ScriptedController.QueueDamageAssignment, blocker=amount pairs from CardByFixtureID
//	queue discard <id>[,...]      ScriptedController.QueueDiscard, ids from CardByFixtureID
//	queue cardchoice <id>[,...]   ScriptedController.QueueCardChoice, an effect's card pick, ids from CardByFixtureID
//	queue battleprotector <p>     ScriptedController.QueueBattleProtector, a seated player's name
//	paymanacost <player> <cost>          Game.PayManaCost(player, cost, controller) -- cost is mana.Parse's own text
//	tapformana <player> <id> <color>     Game.TapLandForMana(player, id, color), id from CardByFixtureID
//	playland <player> <id>               Game.PlayLand(player, id), id from CardByFixtureID
//	castspell <player> <id>              Game.CastSpell(player, id, controller), id from CardByFixtureID
//	resolvestack                         Game.ResolveStack(engine.NewRegistry(), controller)
//	passpriority                         Game.PassPriority(engine.NewRegistry(), controller), one CR 117 round (ADR-0019)
//	queue action <p> pass                ScriptedController.QueueAction, p passes once (an empty queue passes too)
//	queue action <p> cast <id>           ScriptedController.QueueAction, p casts id when next given priority
//	queue action <p> activate <id> <n>   ScriptedController.QueueAction, p activates id's n'th ability (0-based)
//	queue action <p> playland <id>       ScriptedController.QueueAction, p plays land id
//	queue action <p> tapformana <id> <color>  ScriptedController.QueueAction, p taps basic land id for color
//	queue action <p> manaability <id> <n>     ScriptedController.QueueAction, p activates id's n'th mana ability
//	queue paygeneric <shard>             ScriptedController.QueuePayGeneric, a bare shard symbol ("W", "C", ...)
//	queue payx <n>                       ScriptedController.QueuePayX, the value of X for a cost carrying one
//	queue paysnow <shard>                 ScriptedController.QueuePaySnow, a bare shard symbol naming the color
//	queue hybridmanacolor <color>        ScriptedController.QueueHybridManaColor, a bare color letter
//	queue paymonocoloredhybrid <bool>    ScriptedController.QueuePayMonocoloredHybrid
//	queue paycolorlesshybrid <bool>      ScriptedController.QueuePayColorlessHybrid
//	queue payphyrexian <bool>            ScriptedController.QueuePayPhyrexian
//	queue payhybridphyrexian <color|life> ScriptedController.QueuePayHybridPhyrexian, "life" for the zero mana.Colors answer
//	queue confirmeffect <bool>           ScriptedController.QueueConfirmEffect, an effect's yes/no prompt
//	queue confirmpaycost <bool>          ScriptedController.QueueConfirmPayCost, an UnlessCost$ pay-or-not prompt
//	queue optionaltrigger <bool>         ScriptedController.QueueConfirmOptionalTrigger, an OptionalDecider$ trigger's "you may"
//
// A scenario that needs a decision point no verb here reaches -- choosing
// modes, anything an instant or sorcery resolves into -- cannot be written
// yet, because nothing downstream of ScriptedController can answer it either
// (M5-M6, later). `castspell` now reaches two shapes: a non-Aura permanent,
// which resolves into nothing but "become a permanent," and an Aura, whose
// one target is chosen at cast time via `queue enchanttarget` (or assigned
// automatically when only one legal host exists, CastSpell's own "nothing
// meaningful to decide" reasoning) and attached at resolution
// (castspell.go's own doc comment). PayManaCost, TapLandForMana and
// CastSpell are each callable directly the same way
// Game.DeclareCombatAttackers is before a full turn glues combat together
// (manapay.go's own doc comment) -- PayManaCost is CR 106/601.2h's own
// self-contained payment step, TapLandForMana is CR 305.6/605.3's mana
// ability, which resolves immediately with no stack, and CastSpell/
// `resolvestack` are the payment step plus the one stack shape this port
// can push and resolve.
func RunActions(r io.Reader, l *Loaded, controller *engine.ScriptedController) error {
	sc := bufio.NewScanner(r)
	for line := 1; sc.Scan(); line++ {
		text := strings.TrimSpace(sc.Text())
		if text == "" || strings.HasPrefix(text, "#") {
			continue
		}
		if err := runAction(text, l, controller); err != nil {
			return fmt.Errorf("line %d: %w", line, err)
		}
		// Most verbs call a bool-returning entry point (tapformana,
		// castspell), so a static trigger's error waits on the Game until
		// someone takes it (engine.Game.TakePendingError, ADR-0020).
		if err := l.Game.TakePendingError(); err != nil {
			return fmt.Errorf("line %d: %w", line, err)
		}
	}
	return sc.Err()
}

func runAction(line string, l *Loaded, c *engine.ScriptedController) error {
	fields := strings.Fields(line)
	verb, args := fields[0], fields[1:]

	switch verb {
	case "startturn":
		pid, err := resolveActionPlayer(l, args, 1)
		if err != nil {
			return err
		}
		l.Game.StartTurn(pid, c)

	case "advance":
		n := 1
		if len(args) > 0 {
			v, err := strconv.Atoi(args[0])
			if err != nil {
				return fmt.Errorf("advance %q: %w", args[0], err)
			}
			n = v
		}
		for i := 0; i < n; i++ {
			l.Game.AdvancePhase(c)
		}

	case "step":
		n := 1
		if len(args) > 0 {
			v, err := strconv.Atoi(args[0])
			if err != nil {
				return fmt.Errorf("step %q: %w", args[0], err)
			}
			n = v
		}
		for i := 0; i < n; i++ {
			if err := l.Game.Step(engine.NewRegistry(), c); err != nil {
				return fmt.Errorf("step %d of %d: %w", i+1, n, err)
			}
		}

	case "run":
		if len(args) != 1 {
			return fmt.Errorf("run: want a turn cap, got %q", strings.Join(args, " "))
		}
		maxTurns, err := strconv.Atoi(args[0])
		if err != nil {
			return fmt.Errorf("run %q: %w", args[0], err)
		}
		if err := l.Game.Run(engine.NewRegistry(), c, maxTurns); err != nil {
			return fmt.Errorf("run: %w", err)
		}

	case "dealopeninghands":
		engine.DealOpeningHands(l.Game, c)

	case "mulligan":
		pid, err := resolveActionPlayer(l, args, 1)
		if err != nil {
			return err
		}
		engine.PerformMulligans(l.Game, c, pid)

	case "declareattackers":
		if _, err := l.Game.DeclareCombatAttackers(c); err != nil {
			return fmt.Errorf("declareattackers: %w", err)
		}

	case "declareblockers":
		if _, err := l.Game.DeclareCombatBlockers(c); err != nil {
			return fmt.Errorf("declareblockers: %w", err)
		}

	case "firststrikedamage":
		l.Game.DealFirstStrikeDamage(c)

	case "combatdamage":
		l.Game.DealCombatDamage(c)

	case "paymanacost":
		if len(args) < 2 {
			return fmt.Errorf("paymanacost: want a player and a cost, got %q", strings.Join(args, " "))
		}
		pid, err := resolveActionPlayer(l, args, 1)
		if err != nil {
			return err
		}
		costText := strings.Join(args[1:], " ")
		cost, err := mana.Parse(costText)
		if err != nil {
			return fmt.Errorf("paymanacost cost %q: %w", costText, err)
		}
		l.Game.PayManaCost(pid, cost, c)

	case "tapformana":
		if len(args) < 3 {
			return fmt.Errorf("tapformana: want a player, a card id, and a color, got %q", strings.Join(args, " "))
		}
		pid, err := resolveActionPlayer(l, args, 1)
		if err != nil {
			return err
		}
		ids, err := resolveCardIDs(l, args[1])
		if err != nil {
			return fmt.Errorf("tapformana: %w", err)
		}
		if len(ids) != 1 {
			return fmt.Errorf("tapformana: want exactly one card id, got %q", args[1])
		}
		color, err := resolveManaColor(args[2])
		if err != nil {
			return fmt.Errorf("tapformana color %q: %w", args[2], err)
		}
		l.Game.TapLandForMana(pid, ids[0], color, c)

	case "playland":
		if len(args) < 2 {
			return fmt.Errorf("playland: want a player and a card id, got %q", strings.Join(args, " "))
		}
		pid, err := resolveActionPlayer(l, args, 1)
		if err != nil {
			return err
		}
		ids, err := resolveCardIDs(l, args[1])
		if err != nil {
			return fmt.Errorf("playland: %w", err)
		}
		if len(ids) != 1 {
			return fmt.Errorf("playland: want exactly one card id, got %q", args[1])
		}
		l.Game.PlayLand(pid, ids[0], c)

	case "castspell":
		if len(args) < 2 {
			return fmt.Errorf("castspell: want a player and a card id, got %q", strings.Join(args, " "))
		}
		pid, err := resolveActionPlayer(l, args, 1)
		if err != nil {
			return err
		}
		ids, err := resolveCardIDs(l, args[1])
		if err != nil {
			return fmt.Errorf("castspell: %w", err)
		}
		if len(ids) != 1 {
			return fmt.Errorf("castspell: want exactly one card id, got %q", args[1])
		}
		l.Game.CastSpell(pid, ids[0], c)

	case "resolvestack":
		if err := l.Game.ResolveStack(engine.NewRegistry(), c); err != nil {
			return fmt.Errorf("resolvestack: %w", err)
		}

	case "passpriority":
		if err := l.Game.PassPriority(engine.NewRegistry(), c); err != nil {
			return fmt.Errorf("passpriority: %w", err)
		}

	case "queue":
		return runQueue(args, l, c)

	default:
		return fmt.Errorf("unknown action %q", verb)
	}
	return nil
}

func runQueue(args []string, l *Loaded, c *engine.ScriptedController) error {
	if len(args) < 2 {
		return fmt.Errorf("queue: want a kind and a value, got %q", strings.Join(args, " "))
	}
	kind, value := args[0], args[1]

	switch kind {
	case "keephand":
		v, err := strconv.ParseBool(value)
		if err != nil {
			return fmt.Errorf("queue keephand %q: %w", value, err)
		}
		c.QueueKeepHand(v)

	case "tuck":
		ids, err := resolveCardIDs(l, value)
		if err != nil {
			return fmt.Errorf("queue tuck: %w", err)
		}
		c.QueueTuck(ids)

	case "startingplayer":
		pid, err := resolveActionPlayer(l, args[1:], 1)
		if err != nil {
			return err
		}
		c.QueueStartingPlayer(pid)

	case "startinghand":
		n, err := strconv.Atoi(value)
		if err != nil {
			return fmt.Errorf("queue startinghand %q: %w", value, err)
		}
		c.QueueStartingHand(n)

	case "legendarykeep":
		ids, err := resolveCardIDs(l, value)
		if err != nil {
			return fmt.Errorf("queue legendarykeep: %w", err)
		}
		if len(ids) != 1 {
			return fmt.Errorf("queue legendarykeep: want exactly one id, got %q", value)
		}
		c.QueueLegendaryToKeep(ids[0])

	case "enchanttarget":
		ids, err := resolveCardIDs(l, value)
		if err != nil {
			return fmt.Errorf("queue enchanttarget: %w", err)
		}
		if len(ids) != 1 {
			return fmt.Errorf("queue enchanttarget: want exactly one id, got %q", value)
		}
		c.QueueEnchantTarget(ids[0])

	case "action":
		a, pid, err := resolveAction(l, args[1:])
		if err != nil {
			return fmt.Errorf("queue action: %w", err)
		}
		c.QueueAction(pid, a)

	case "targets":
		// A triggered ability's, cast spell's or activated ability's own
		// targets (resolveTargets' ChooseTargets) -- a card's own
		// CardByFixtureID or a seated player's name, per entry
		// (resolveAttackTarget's own either/or, reused per comma-separated
		// value here since ChooseTargets can return more than one entity,
		// unlike ChooseAttackTarget's own single answer).
		chosen, err := resolveTargetEntities(l, value)
		if err != nil {
			return fmt.Errorf("queue targets: %w", err)
		}
		c.QueueTargets(chosen)

	case "attackers":
		// "none" is written explicitly, not an empty value, because every
		// other queue kind requires a value too (the len(args) < 2 check
		// above) -- declining to attack with anything is still an answer
		// that has to be queued, not an absent one.
		if value == "none" {
			c.QueueAttackers(nil)
			break
		}
		ids, err := resolveCardIDs(l, value)
		if err != nil {
			return fmt.Errorf("queue attackers: %w", err)
		}
		c.QueueAttackers(ids)

	case "exertattackers":
		// "none" mirrors "attackers none" above: declining every offer is
		// still an answer that has to be queued.
		if value == "none" {
			c.QueueExertAttackers(nil)
			break
		}
		ids, err := resolveCardIDs(l, value)
		if err != nil {
			return fmt.Errorf("queue exertattackers: %w", err)
		}
		c.QueueExertAttackers(ids)

	case "attacktarget":
		target, err := resolveAttackTarget(l, value)
		if err != nil {
			return fmt.Errorf("queue attacktarget: %w", err)
		}
		c.QueueAttackTarget(target)

	case "blocks":
		// "none" mirrors "attackers none" above: declining to block is a
		// queued answer too, not an absent one.
		if value == "none" {
			c.QueueBlocks(nil)
			break
		}
		blocks, err := resolveBlocks(l, value)
		if err != nil {
			return fmt.Errorf("queue blocks: %w", err)
		}
		c.QueueBlocks(blocks)

	case "damage":
		assignment, err := resolveDamageAssignment(l, value)
		if err != nil {
			return fmt.Errorf("queue damage: %w", err)
		}
		c.QueueDamageAssignment(assignment)

	case "discard":
		ids, err := resolveCardIDs(l, value)
		if err != nil {
			return fmt.Errorf("queue discard: %w", err)
		}
		c.QueueDiscard(ids)

	case "cardchoice":
		ids, err := resolveCardIDs(l, value)
		if err != nil {
			return fmt.Errorf("queue cardchoice: %w", err)
		}
		c.QueueCardChoice(ids)

	case "battleprotector":
		pid, err := resolveActionPlayer(l, args[1:], 1)
		if err != nil {
			return err
		}
		c.QueueBattleProtector(pid)

	case "paygeneric":
		s, err := mana.ParseShard(value)
		if err != nil {
			return fmt.Errorf("queue paygeneric %q: %w", value, err)
		}
		c.QueuePayGeneric(s)

	case "payx":
		x, err := strconv.Atoi(value)
		if err != nil {
			return fmt.Errorf("queue payx %q: %w", value, err)
		}
		c.QueuePayX(x)

	case "paysnow":
		s, err := mana.ParseShard(value)
		if err != nil {
			return fmt.Errorf("queue paysnow %q: %w", value, err)
		}
		c.QueuePaySnow(s)

	case "hybridmanacolor":
		color, err := resolveManaColor(value)
		if err != nil {
			return fmt.Errorf("queue hybridmanacolor %q: %w", value, err)
		}
		c.QueueHybridManaColor(color)

	case "confirmpaycost":
		v, err := strconv.ParseBool(value)
		if err != nil {
			return fmt.Errorf("queue confirmpaycost %q: %w", value, err)
		}
		c.QueueConfirmPayCost(v)

	case "paymonocoloredhybrid":
		v, err := strconv.ParseBool(value)
		if err != nil {
			return fmt.Errorf("queue paymonocoloredhybrid %q: %w", value, err)
		}
		c.QueuePayMonocoloredHybrid(v)

	case "paycolorlesshybrid":
		v, err := strconv.ParseBool(value)
		if err != nil {
			return fmt.Errorf("queue paycolorlesshybrid %q: %w", value, err)
		}
		c.QueuePayColorlessHybrid(v)

	case "payphyrexian":
		v, err := strconv.ParseBool(value)
		if err != nil {
			return fmt.Errorf("queue payphyrexian %q: %w", value, err)
		}
		c.QueuePayPhyrexian(v)

	case "payhybridphyrexian":
		// "life" is written explicitly, not an empty value, the same reason
		// "attackers none"/"blocks none" spell out declining rather than
		// leaving the value blank -- the zero mana.Colors answer (pay with 2
		// life instead) is a real, legal answer that still has to be queued.
		if value == "life" {
			c.QueuePayHybridPhyrexian(0)
			break
		}
		color, err := resolveManaColor(value)
		if err != nil {
			return fmt.Errorf("queue payhybridphyrexian %q: %w", value, err)
		}
		c.QueuePayHybridPhyrexian(color)

	case "confirmeffect":
		// An effect's generic yes/no prompt (ConfirmEffect): Play's
		// single-option "cast it?", CopySpellAbility's Optional$ and
		// MayChooseTarget$, among others.
		v, err := strconv.ParseBool(value)
		if err != nil {
			return fmt.Errorf("queue confirmeffect %q: %w", value, err)
		}
		c.QueueConfirmEffect(v)

	case "optionaltrigger":
		// An OptionalDecider$ trigger's "you may" (ConfirmOptionalTrigger),
		// asked as the trigger goes on the stack: Swarm Intelligence's
		// "you may copy that spell."
		v, err := strconv.ParseBool(value)
		if err != nil {
			return fmt.Errorf("queue optionaltrigger %q: %w", value, err)
		}
		c.QueueConfirmOptionalTrigger(v)

	default:
		return fmt.Errorf("unknown queue kind %q", kind)
	}
	return nil
}

// resolveActionPlayer looks args[i] up as a seated player's name -- human,
// ai, p2..p9, the same vocabulary setup.state itself uses.
func resolveActionPlayer(l *Loaded, args []string, want int) (engine.PlayerID, error) {
	if len(args) < want {
		return engine.NoPlayer, fmt.Errorf("want a player name, got %q", strings.Join(args, " "))
	}
	name := strings.ToLower(args[want-1])
	for _, pid := range l.Game.Players() {
		if l.Game.Player(pid).Name == name {
			return pid, nil
		}
	}
	return engine.NoPlayer, fmt.Errorf("player %q is not seated in this game", name)
}

// resolveAction parses `queue action`'s own arguments -- a seated player,
// then "pass", "cast <id>", "activate <id> <index>", "playland <id>",
// "tapformana <id> <color>" or "manaability <id> <index>" -- into the Action
// that player's next TakeAction returns. An empty queue already answers
// pass (ScriptedController.TakeAction); an explicit "pass" is for a player
// who passes now and acts later in the same round, since the queue is
// consumed one ask at a time.
func resolveAction(l *Loaded, args []string) (engine.Action, engine.PlayerID, error) {
	if len(args) == 2 && args[1] == "pass" {
		pid, err := resolveActionPlayer(l, args, 1)
		return engine.Action{}, pid, err
	}
	if len(args) < 3 {
		return engine.Action{}, engine.NoPlayer, fmt.Errorf("want <player> <kind> [<id> ...], got %q", strings.Join(args, " "))
	}
	pid, err := resolveActionPlayer(l, args, 1)
	if err != nil {
		return engine.Action{}, engine.NoPlayer, err
	}
	ids, err := resolveCardIDs(l, args[2])
	if err != nil {
		return engine.Action{}, engine.NoPlayer, err
	}
	if len(ids) != 1 {
		return engine.Action{}, engine.NoPlayer, fmt.Errorf("want exactly one card id, got %q", args[2])
	}
	switch args[1] {
	case "cast", "playland":
		if len(args) != 3 {
			return engine.Action{}, engine.NoPlayer, fmt.Errorf("%s takes one card id, got %q", args[1], strings.Join(args[2:], " "))
		}
		kind := engine.ActionCast
		if args[1] == "playland" {
			kind = engine.ActionPlayLand
		}
		return engine.Action{Kind: kind, Card: ids[0]}, pid, nil
	case "activate", "manaability":
		if len(args) != 4 {
			return engine.Action{}, engine.NoPlayer, fmt.Errorf("%s takes a card id and an ability index, got %q", args[1], strings.Join(args[2:], " "))
		}
		n, err := strconv.Atoi(args[3])
		if err != nil {
			return engine.Action{}, engine.NoPlayer, fmt.Errorf("ability index %q: %w", args[3], err)
		}
		kind := engine.ActionActivate
		if args[1] == "manaability" {
			kind = engine.ActionManaAbility
		}
		return engine.Action{Kind: kind, Card: ids[0], AbilityIndex: n}, pid, nil
	case "tapformana":
		if len(args) != 4 {
			return engine.Action{}, engine.NoPlayer, fmt.Errorf("tapformana takes a card id and a color, got %q", strings.Join(args[2:], " "))
		}
		color, err := resolveManaColor(args[3])
		if err != nil {
			return engine.Action{}, engine.NoPlayer, fmt.Errorf("color %q: %w", args[3], err)
		}
		return engine.Action{Kind: engine.ActionTapForMana, Card: ids[0], Color: color}, pid, nil
	default:
		return engine.Action{}, engine.NoPlayer, fmt.Errorf("want pass, cast, activate, playland, tapformana or manaability, got %q", args[1])
	}
}

// resolveCardIDs turns a comma-separated list of setup.state Id: numbers
// into the CardIDs Load assigned them, in the order written.
func resolveCardIDs(l *Loaded, value string) ([]engine.CardID, error) {
	parts := strings.Split(value, ",")
	ids := make([]engine.CardID, len(parts))
	for i, p := range parts {
		n, err := strconv.Atoi(strings.TrimSpace(p))
		if err != nil {
			return nil, fmt.Errorf("id %q: %w", p, err)
		}
		id, ok := l.CardByFixtureID[n]
		if !ok {
			return nil, fmt.Errorf("id %d: no card in setup.state has that Id: entry", n)
		}
		ids[i] = id
	}
	return ids, nil
}

// resolveManaColor reads a bare color letter ("W", "U", ...) the way
// queue paygeneric already reads a bare shard: through mana.ParseShard,
// taking only its Colors() half, since a plain color letter parses to the
// pure shard of that color and pure Shard.Colors() is exactly one bit.
func resolveManaColor(value string) (mana.Colors, error) {
	s, err := mana.ParseShard(value)
	if err != nil {
		return 0, err
	}
	return s.Colors(), nil
}

// resolveAttackTarget turns a queue attacktarget value into the EntityID
// ChooseAttackTarget expects: a numeric value is a setup.state Id: naming a
// planeswalker or battle, anything else is a seated player's name (the same
// vocabulary resolveActionPlayer uses).
func resolveAttackTarget(l *Loaded, value string) (engine.EntityID, error) {
	if n, err := strconv.Atoi(strings.TrimSpace(value)); err == nil {
		id, ok := l.CardByFixtureID[n]
		if !ok {
			return engine.EntityID(0), fmt.Errorf("id %d: no card in setup.state has that Id: entry", n)
		}
		return engine.CardEntity(id), nil
	}
	pid, err := resolveActionPlayer(l, []string{value}, 1)
	if err != nil {
		return engine.EntityID(0), err
	}
	return engine.PlayerEntity(pid), nil
}

// resolveTargetEntities is resolveAttackTarget's own comma-separated-list
// sibling: each entry is a card's CardByFixtureID or a seated player's name,
// resolveAttackTarget's own either/or reused per entry.
func resolveTargetEntities(l *Loaded, value string) ([]engine.EntityID, error) {
	parts := strings.Split(value, ",")
	entities := make([]engine.EntityID, len(parts))
	for i, p := range parts {
		e, err := resolveAttackTarget(l, strings.TrimSpace(p))
		if err != nil {
			return nil, err
		}
		entities[i] = e
	}
	return entities, nil
}

// resolveBlocks turns a comma-separated list of blocker=attacker fixture-ID
// pairs into the Blocks Load's ids resolve to.
func resolveBlocks(l *Loaded, value string) ([]engine.Block, error) {
	pairs := strings.Split(value, ",")
	blocks := make([]engine.Block, len(pairs))
	for i, p := range pairs {
		halves := strings.SplitN(p, "=", 2)
		if len(halves) != 2 {
			return nil, fmt.Errorf("pair %q: want blocker=attacker", p)
		}
		ids, err := resolveCardIDs(l, halves[0]+","+halves[1])
		if err != nil {
			return nil, err
		}
		blocks[i] = engine.Block{Blocker: ids[0], Attacker: ids[1]}
	}
	return blocks, nil
}

// resolveDamageAssignment turns a comma-separated list of blocker=amount
// pairs into the []engine.DamageAssignment AssignCombatDamage expects, in
// the order written -- that order is the order the attacking player assigns
// in (CR 510.1c), so unlike resolveBlocks this cannot reorder its pairs.
func resolveDamageAssignment(l *Loaded, value string) ([]engine.DamageAssignment, error) {
	pairs := strings.Split(value, ",")
	assignment := make([]engine.DamageAssignment, len(pairs))
	for i, p := range pairs {
		halves := strings.SplitN(p, "=", 2)
		if len(halves) != 2 {
			return nil, fmt.Errorf("pair %q: want blocker=amount", p)
		}
		ids, err := resolveCardIDs(l, halves[0])
		if err != nil {
			return nil, err
		}
		amount, err := strconv.Atoi(strings.TrimSpace(halves[1]))
		if err != nil {
			return nil, fmt.Errorf("amount %q: %w", halves[1], err)
		}
		assignment[i] = engine.DamageAssignment{Blocker: ids[0], Amount: amount}
	}
	return assignment, nil
}
