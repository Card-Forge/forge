"""Full game trace with a policy that actually plays spells."""
import grpc
import random
from collections import Counter
from forge_rl.proto import forge_rl_pb2, forge_rl_pb2_grpc

random.seed(7)

channel = grpc.insecure_channel('localhost:50051')
stub = forge_rl_pb2_grpc.ForgeRlServiceStub(channel)

resp = stub.Reset(forge_rl_pb2.ResetRequest(
    deck_path_a='src/main/resources/decks/ramunap_red.dck',
    deck_path_b='src/main/resources/decks/bg_constrictor.dck',
    agent_player_index=0
))

phases = {
    -1:'PRE_GAME', 0:'UNTAP', 1:'UPKEEP', 2:'DRAW', 3:'MAIN1', 4:'BEGIN_COMBAT',
    5:'DECLARE_ATTACKERS', 6:'DECLARE_BLOCKERS', 7:'FIRST_STRIKE_DAMAGE',
    8:'COMBAT_DAMAGE', 9:'END_COMBAT', 10:'MAIN2', 11:'END_OF_TURN', 12:'CLEANUP'
}
dtypes = {
    0:'CHOOSE_SPELL', 1:'DECLARE_ATTACKERS', 2:'DECLARE_BLOCKERS',
    3:'CHOOSE_TARGETS', 4:'MULLIGAN', 5:'CHOOSE_CARDS', 6:'CONFIRM_ACTION',
    7:'CHOOSE_ENTITY', 8:'CHOOSE_COLOR', 9:'CHOOSE_NUMBER', 10:'CHOOSE_TYPE',
    11:'CHOOSE_PILE', 12:'CHOOSE_MODE', 13:'ORDER_CARDS', 14:'AI_FALLBACK'
}

def phase_name(p):
    return phases.get(p, f'PHASE_{p}')
def dtype_name(d):
    return dtypes.get(d, f'TYPE_{d}')

def board_str(obs):
    agent_field = []
    for c in obs.agent_player.battlefield:
        s = c.name
        if c.tapped:
            s += '(T)'
        if c.summoning_sick:
            s += '(SS)'
        agent_field.append(s)
    opp_field = []
    for c in obs.opponent_player.battlefield:
        s = c.name
        if c.tapped:
            s += '(T)'
        opp_field.append(s)
    mana = obs.agent_player.mana_pool
    mana_str = []
    if mana.white: mana_str.append(f'{mana.white}W')
    if mana.blue: mana_str.append(f'{mana.blue}U')
    if mana.black: mana_str.append(f'{mana.black}B')
    if mana.red: mana_str.append(f'{mana.red}R')
    if mana.green: mana_str.append(f'{mana.green}G')
    if mana.colorless: mana_str.append(f'{mana.colorless}C')
    mana_s = ','.join(mana_str) if mana_str else 'empty'
    return (
        f'  Life: {obs.agent_player.life} vs {obs.opponent_player.life} | '
        f'Hand: {obs.agent_player.hand_size} | Lands: {obs.agent_player.lands_played}/{obs.agent_player.max_lands} | Mana: {mana_s}\n'
        f'  Agent BF: {agent_field}\n'
        f'  Opp BF:   {opp_field}'
    )

# Smart-ish policy:
# - Always keep (mulligan 0)
# - In MAIN1/MAIN2: play a land first, then play a spell (prioritize non-pass)
# - In combat: attack with everything
# - Otherwise: random from legal actions
def choose_action(dp, obs):
    actions = dp.legal_actions
    n = len(actions)
    if n <= 1:
        return 0

    dt = dp.type
    phase = obs.game_info.phase

    if dt == 4:  # MULLIGAN
        return 0  # keep

    if dt == 1:  # DECLARE_ATTACKERS
        # Attack with a random creature (not 0 = no attacks)
        return random.randint(1, n - 1) if n > 1 else 0

    if dt == 2:  # DECLARE_BLOCKERS
        # Block with a random creature
        return random.randint(1, n - 1) if n > 1 else 0

    if dt == 0:  # CHOOSE_SPELL_ABILITY
        # In main phases, try to play something
        if phase in (3, 10):  # MAIN1 or MAIN2
            # Look for "Play land" first
            for a in actions:
                if 'Play land' in a.description:
                    return a.index
            # Prefer creatures/spells over activated abilities
            spells = [a for a in actions if a.index > 0
                      and ('Creature' in a.description or 'Play land' in a.description
                           or 'Enchantment' in a.description or 'Sorcery' in a.description
                           or 'Instant' in a.description or 'Planeswalker' in a.description)]
            if spells:
                return random.choice(spells).index
            # Fall back to any non-pass, non-mana action
            non_pass = [a for a in actions if a.index > 0
                        and 'Add {' not in a.description]
            if non_pass and random.random() < 0.3:
                return random.choice(non_pass).index
        else:
            # In other phases, 50% chance to pass, 50% play something
            if random.random() < 0.5 and n > 1:
                non_pass = [a for a in actions if a.index > 0]
                if non_pass:
                    return random.choice(non_pass).index
        return 0  # pass

    # For all other decision types, pick randomly
    return random.randint(0, n - 1)


obs = resp.observation
dp = resp.decision_point
decision_type_counts = Counter()

print('=== GAME START ===')
print(f'Agent hand: {[c.name for c in obs.agent_player.hand]}')
print()

step = 0
done = False
sr = None
last_turn = -1

while not done:
    step += 1
    gi = obs.game_info
    turn = gi.turn
    phase = phase_name(gi.phase)
    dt = dtype_name(dp.type)
    decision_type_counts[dt] += 1

    if turn != last_turn:
        print(f'\n{"="*70}')
        print(f'  TURN {turn}')
        print(f'{"="*70}')
        print(board_str(obs))
        print(f'  Agent hand: {[c.name for c in obs.agent_player.hand]}')
        if obs.agent_player.graveyard:
            print(f'  Agent GY:   {[c.name for c in obs.agent_player.graveyard]}')
        last_turn = turn

    n_actions = len(dp.legal_actions)
    action = choose_action(dp, obs)
    chosen_desc = dp.legal_actions[action].description[:70] if action < len(dp.legal_actions) else '?'

    # Compact output for pass, detailed for interesting actions
    if action == 0 and dp.type == 0:
        # Only show pass for interesting phases
        if gi.phase in (3, 10, 5, 6):
            print(f'  Step {step:3d} | {phase:20s} | {dt:20s} | [{action}] Pass ({n_actions} options)')
    else:
        print(f'  Step {step:3d} | {phase:20s} | {dt:20s} | [{action}] {chosen_desc}')
        if n_actions > 2 or dp.type != 0:
            for a in dp.legal_actions:
                marker = ' >>>' if a.index == action else '    '
                src = f' [{a.source_card_name}]' if a.source_card_name else ''
                print(f'      {marker} [{a.index}] {a.description[:70]}{src}')

    try:
        sr = stub.Step(forge_rl_pb2.StepRequest(action_index=action))
        done = sr.terminated
        if not done:
            obs = sr.observation
            dp = sr.decision_point
    except Exception as e:
        print(f'ERROR: {e}')
        break

    if step > 500:
        print('\n... truncating at 500 steps')
        break

if sr:
    obs = sr.observation
    print(f'\n{"="*70}')
    print(f'  GAME OVER')
    print(f'{"="*70}')
    print(f'Steps: {step}, Reward: {sr.reward}')
    print(board_str(obs))
    print(f'Agent graveyard: {[c.name for c in obs.agent_player.graveyard]}')
    print(f'Opp graveyard: {[c.name for c in obs.opponent_player.graveyard]}')
    if sr.HasField('game_result'):
        gr = sr.game_result
        print(f'Winner: {gr.winner_index}, Turns: {gr.turns_played}, Condition: {gr.win_condition}')

print(f'\n=== DECISION TYPE COUNTS ===')
for dt, count in sorted(decision_type_counts.items(), key=lambda x: -x[1]):
    print(f'  {dt}: {count}')
