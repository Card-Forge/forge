"""Full game trace with all decisions logged."""
import grpc
from forge_rl.proto import forge_rl_pb2, forge_rl_pb2_grpc

channel = grpc.insecure_channel('localhost:50051')
stub = forge_rl_pb2_grpc.ForgeRlServiceStub(channel)

resp = stub.Reset(forge_rl_pb2.ResetRequest(
    deck_path_a='src/main/resources/decks/ramunap_red.dck',
    deck_path_b='src/main/resources/decks/bg_constrictor.dck',
    agent_player_index=0
))

phases = {
    0:'UNTAP', 1:'UPKEEP', 2:'DRAW', 3:'MAIN1', 4:'BEGIN_COMBAT',
    5:'DECLARE_ATTACKERS', 6:'DECLARE_BLOCKERS', 7:'FIRST_STRIKE_DAMAGE',
    8:'COMBAT_DAMAGE', 9:'END_COMBAT', 10:'MAIN2', 11:'END_OF_TURN', 12:'CLEANUP'
}
dtypes = {
    0:'CHOOSE_SPELL_ABILITY', 1:'DECLARE_ATTACKERS', 2:'DECLARE_BLOCKERS',
    3:'CHOOSE_TARGETS', 4:'MULLIGAN', 5:'CHOOSE_CARDS', 6:'CONFIRM_ACTION',
    7:'ORDER_BLOCKERS'
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
        f'Hand: {obs.agent_player.hand_size} | Mana: {mana_s}\n'
        f'  Agent BF: {agent_field}\n'
        f'  Opp BF:   {opp_field}'
    )

obs = resp.observation
dp = resp.decision_point

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

    if turn != last_turn:
        print(f'\n{"="*60}')
        print(f'  TURN {turn}')
        print(f'{"="*60}')
        print(board_str(obs))
        print(f'  Agent hand: {[c.name for c in obs.agent_player.hand]}')
        last_turn = turn

    # Show decision
    n_actions = len(dp.legal_actions)
    print(f'\nStep {step} | {phase} | {dt} | {n_actions} actions:')
    for a in dp.legal_actions:
        desc = a.description[:80]
        src = f' [{a.source_card_name}]' if a.source_card_name else ''
        print(f'  [{a.index}] {desc}{src}')

    # Always pick action 0
    action = 0
    chosen_desc = dp.legal_actions[0].description[:60] if dp.legal_actions else '?'
    print(f'  >>> CHOSE [{action}]: {chosen_desc}')

    try:
        sr = stub.Step(forge_rl_pb2.StepRequest(action_index=action))
        done = sr.terminated
        if not done:
            obs = sr.observation
            dp = sr.decision_point
    except Exception as e:
        print(f'ERROR: {e}')
        break

    if step > 300:
        print('\n... truncating at 300 steps')
        break

if sr:
    obs = sr.observation
    print(f'\n{"="*60}')
    print(f'  GAME OVER')
    print(f'{"="*60}')
    print(f'Steps: {step}, Reward: {sr.reward}')
    print(board_str(obs))
    print(f'Agent graveyard: {[c.name for c in obs.agent_player.graveyard]}')
    print(f'Opp graveyard: {[c.name for c in obs.opponent_player.graveyard]}')
    if sr.HasField('game_result'):
        gr = sr.game_result
        print(f'Winner: {gr.winner_index}, Turns: {gr.turns_played}, Condition: {gr.win_condition}')
