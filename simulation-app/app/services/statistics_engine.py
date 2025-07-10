from sqlalchemy import func
from ..models.simulation import Simulation, GameResult
from ..models.statistics import GameStatistics, DeckStatistics, SimulationSummary
from ..services.bigquery_client import BigQueryClient
from .. import db
import logging
import re

logger = logging.getLogger(__name__)

class StatisticsEngine:
    def __init__(self):
        self.bigquery_client = BigQueryClient()
        self._deck_cache = {}  # Cache for deck information
    
    def _get_deck_info(self, deck_id):
        """Get deck information including commander names."""
        if deck_id in self._deck_cache:
            return self._deck_cache[deck_id]
        
        try:
            deck = self.bigquery_client.get_deck(deck_id)
            if deck:
                commander_name = deck.get('commander_1', '')
                if deck.get('commander_2'):
                    commander_name += f" // {deck.get('commander_2')}"
                
                deck_info = {
                    'commander_name': commander_name or 'Unknown Commander',
                    'deck_name': deck.get('deck_name', deck_id),
                    'color_identity': deck.get('color_identity', 'C')
                }
            else:
                deck_info = {
                    'commander_name': f'Unknown ({deck_id[:20]}...)',
                    'deck_name': deck_id,
                    'color_identity': 'C'
                }
            
            self._deck_cache[deck_id] = deck_info
            return deck_info
        except Exception as e:
            logger.warning(f"Could not get deck info for {deck_id}: {e}")
            return {
                'commander_name': f'Unknown ({deck_id[:20]}...)',
                'deck_name': deck_id,
                'color_identity': 'C'
            }
    
    def _get_deck_display_name(self, deck_id):
        """Get display name for deck (commander name + deck name if needed)."""
        deck_info = self._get_deck_info(deck_id)
        commander_name = deck_info['commander_name']
        
        # Check if we need to disambiguate with deck name
        # This would be more robust with a proper lookup of all commanders
        # For now, just return commander name
        return commander_name
    
    def get_simulation_statistics(self, simulation_id):
        """Get comprehensive statistics for a simulation."""
        simulation = Simulation.query.get(simulation_id)
        if not simulation:
            return None
        
        # Basic simulation info  
        stats = {
            'simulation_id': simulation.id,
            'simulation_name': simulation.name,
            'total_games': simulation.num_games,
            'completed_games': simulation.games_completed,
            'status': simulation.status
        }
        
        # Game results summary
        game_results = GameResult.query.filter_by(simulation_id=simulation_id).all()
        
        if game_results:
            # Win rate by deck
            deck_performance = self._calculate_deck_performance(game_results)
            stats['win_rates_by_deck'] = {deck: perf['win_rate'] for deck, perf in deck_performance.items()}
            stats['deck_performance'] = deck_performance
            
            
            # Game duration analysis
            durations = [game.game_duration_seconds for game in game_results if game.game_duration_seconds]
            if durations:
                stats['average_game_duration'] = sum(durations) / len(durations)
                stats['duration_stats'] = {
                    'average': sum(durations) / len(durations),
                    'min': min(durations),
                    'max': max(durations),
                    'total_games': len(durations)
                }
            
            # Turn analysis
            turns = [game.total_turns for game in game_results if game.total_turns]
            if turns:
                stats['average_turn_count'] = sum(turns) / len(turns)
                stats['turn_stats'] = {
                    'average': sum(turns) / len(turns),
                    'min': min(turns),
                    'max': max(turns)
                }
            
            # Card statistics
            stats['card_statistics'] = self._get_card_statistics(simulation_id)
        
        return stats
    
    def get_deck_performance(self, deck_id, user_id):
        """Get performance statistics for a specific deck across user's simulations."""
        # Get all games where this deck was used by this user
        games = db.session.query(GameResult, GameStatistics).join(
            GameStatistics, GameResult.id == GameStatistics.game_id
        ).join(
            Simulation, GameResult.simulation_id == Simulation.id
        ).filter(
            Simulation.user_id == user_id,
            GameStatistics.deck_id == deck_id
        ).all()
        
        if not games:
            return {
                'total_games': 0,
                'wins': 0,
                'win_rate': 0.0,
                'avg_turns_survived': 0.0
            }
        
        total_games = len(games)
        wins = sum(1 for game_result, game_stats in games if game_result.winner_deck_id == deck_id)
        avg_turns = sum(game_stats.turns_survived for _, game_stats in games) / total_games
        
        # Aggregate card statistics
        total_mana = sum(game_stats.total_mana_spent for _, game_stats in games)
        total_cards = sum(game_stats.cards_played_total for _, game_stats in games)
        total_creatures = sum(game_stats.creatures_played for _, game_stats in games)
        total_commander_casts = sum(game_stats.commander_casts for _, game_stats in games)
        
        return {
            'total_games': total_games,
            'wins': wins,
            'win_rate': (wins / total_games) * 100 if total_games > 0 else 0,
            'avg_turns_survived': avg_turns,
            'avg_mana_spent': total_mana / total_games,
            'avg_cards_played': total_cards / total_games,
            'avg_creatures_played': total_creatures / total_games,
            'avg_commander_casts': total_commander_casts / total_games
        }
    
    def get_chart_data(self, simulation_id):
        """Get data formatted for charts."""
        game_results = GameResult.query.filter_by(simulation_id=simulation_id).all()
        
        if not game_results:
            return {}
        
        # Win rate pie chart data
        deck_wins = {}
        for game in game_results:
            winner = game.winner_deck_id
            if winner:
                deck_wins[winner] = deck_wins.get(winner, 0) + 1
        
        win_rate_data = {
            'labels': list(deck_wins.keys()),
            'data': list(deck_wins.values())
        }
        
        # Game duration over time
        duration_data = {
            'labels': [f"Game {game.game_number}" for game in game_results if game.game_duration_seconds],
            'data': [game.game_duration_seconds for game in game_results if game.game_duration_seconds]
        }
        
        return {
            'win_rates': win_rate_data,
            'game_durations': duration_data
        }
    
    def compare_decks(self, deck_ids, user_id):
        """Compare performance of multiple decks."""
        comparison_data = {}
        
        for deck_id in deck_ids:
            performance = self.get_deck_performance(deck_id, user_id)
            comparison_data[deck_id] = performance
        
        return comparison_data
    
    def _calculate_deck_performance(self, game_results):
        """Calculate win rates and performance metrics for each deck."""
        deck_stats = {}
        
        # Initialize deck stats with commander names
        for game in game_results:
            for player in game.simulation.players:
                if player.deck_id not in deck_stats:
                    deck_info = self._get_deck_info(player.deck_id)
                    deck_stats[player.deck_id] = {
                        'commander_name': deck_info['commander_name'],
                        'deck_name': deck_info['deck_name'],
                        'color_identity': deck_info['color_identity'],
                        'games': 0,
                        'wins': 0,
                        'eliminations': [],
                        'durations': [],
                        'elimination_positions': []  # Track what position they were eliminated
                    }
        
        # Count wins, games, and elimination positions
        for game in game_results:
            winner_deck = game.winner_deck_id
            
            # Get game statistics to determine elimination order
            game_stats = GameStatistics.query.filter_by(game_id=game.id).all()
            
            # Sort by final life (higher = better position, 0 = eliminated)
            # Winner gets position 1, others ranked by life total
            sorted_players = sorted(game_stats, key=lambda x: (
                1 if x.deck_id == winner_deck else 0,  # Winner first
                x.final_life_total or 0  # Then by life total
            ), reverse=True)
            
            for position, player_stat in enumerate(sorted_players, 1):
                deck_id = player_stat.deck_id
                if deck_id in deck_stats:
                    deck_stats[deck_id]['games'] += 1
                    deck_stats[deck_id]['elimination_positions'].append(position)
                    
                    if deck_id == winner_deck:
                        deck_stats[deck_id]['wins'] += 1
                    
                    if game.game_duration_seconds:
                        deck_stats[deck_id]['durations'].append(game.game_duration_seconds)
        
        # Calculate metrics
        for deck_id, stats in deck_stats.items():
            total_games = stats['games']
            if total_games > 0:
                stats['win_rate'] = (stats['wins'] / total_games) * 100
                
                # Calculate average elimination position (1 = always wins, 4 = always eliminated first)
                if stats['elimination_positions']:
                    stats['avg_elimination_position'] = sum(stats['elimination_positions']) / len(stats['elimination_positions'])
                else:
                    stats['avg_elimination_position'] = 0
                
                if stats['durations']:
                    stats['avg_duration'] = sum(stats['durations']) / len(stats['durations'])
                else:
                    stats['avg_duration'] = 0
            else:
                stats['win_rate'] = 0
                stats['avg_elimination_position'] = 0
                stats['avg_duration'] = 0
        
        return deck_stats
    
    def _get_card_statistics(self, simulation_id):
        """Get per-game and per-player card usage statistics for a simulation."""
        # Get all game statistics for this simulation
        game_stats = db.session.query(
            GameStatistics,
            GameResult.game_number
        ).join(
            GameResult, GameStatistics.game_id == GameResult.id
        ).filter(
            GameResult.simulation_id == simulation_id
        ).order_by(GameResult.game_number, GameStatistics.player_number).all()
        
        if not game_stats:
            return {}
        
        # Organize data by game and player
        games_data = {}
        player_averages = {}
        
        for stat, game_number in game_stats:
            if game_number not in games_data:
                games_data[game_number] = {}
            
            # Store per-game, per-player data
            games_data[game_number][stat.player_number] = {
                'card_types': {
                    'Creatures': stat.creatures_played or 0,
                    'Instants': stat.instants_played or 0,
                    'Sorceries': stat.sorceries_played or 0,
                    'Artifacts': stat.artifacts_played or 0,
                    'Enchantments': stat.enchantments_played or 0,
                    'Planeswalkers': stat.planeswalkers_played or 0,
                    'Lands': stat.lands_played or 0
                },
                'spell_categories': {
                    'Removal': stat.removal_spells_cast or 0,
                    'Ramp': stat.ramp_spells_cast or 0,
                    'Card Draw': stat.card_draw_spells_cast or 0,
                    'Counterspells': stat.counterspells_cast or 0,
                    'Protection': stat.protection_spells_cast or 0,
                    'Board Wipes': stat.board_wipes_cast or 0,
                    'Tribal': stat.tribal_spells_cast or 0,
                    'Combo Pieces': stat.combo_pieces_cast or 0,
                    'Value Engines': stat.value_engines_cast or 0,
                    'Other': stat.other_spells_cast or 0
                },
                'commander_casts': stat.commander_casts or 0,
                'deck_id': stat.deck_id,
                'total_mana_spent': stat.total_mana_spent or 0,
                'final_life': stat.final_life_total or 0
            }
            
            # Calculate player averages across all games
            if stat.player_number not in player_averages:
                player_averages[stat.player_number] = {
                    'games_count': 0,
                    'card_types_total': {key: 0 for key in ['Creatures', 'Instants', 'Sorceries', 'Artifacts', 'Enchantments', 'Planeswalkers', 'Lands']},
                    'spell_categories_total': {key: 0 for key in ['Removal', 'Ramp', 'Card Draw', 'Counterspells', 'Protection', 'Board Wipes', 'Tribal', 'Combo Pieces', 'Value Engines', 'Other']},
                    'commander_casts_total': 0,
                    'deck_id': stat.deck_id
                }
            
            player_avg = player_averages[stat.player_number]
            player_avg['games_count'] += 1
            
            # Add to totals for average calculation
            for card_type, count in games_data[game_number][stat.player_number]['card_types'].items():
                player_avg['card_types_total'][card_type] += count
            
            for spell_cat, count in games_data[game_number][stat.player_number]['spell_categories'].items():
                player_avg['spell_categories_total'][spell_cat] += count
                
            player_avg['commander_casts_total'] += (stat.commander_casts or 0)
        
        # Calculate actual averages
        for player_num, avg_data in player_averages.items():
            games_count = avg_data['games_count']
            if games_count > 0:
                avg_data['card_types_avg'] = {
                    card_type: round(total / games_count, 1) 
                    for card_type, total in avg_data['card_types_total'].items()
                }
                avg_data['spell_categories_avg'] = {
                    spell_cat: round(total / games_count, 1) 
                    for spell_cat, total in avg_data['spell_categories_total'].items()
                }
                avg_data['commander_casts_avg'] = round(avg_data['commander_casts_total'] / games_count, 1)
        
        # Create table format: columns = decks/players, rows = spell types
        spell_type_table = self._create_spell_type_table(game_stats)
        spell_function_table = self._create_spell_function_table(game_stats)
        
        # Create mana and draw analysis
        mana_analysis = self._create_mana_analysis(game_stats)
        draw_analysis = self._create_draw_analysis(game_stats)
        
        return {
            'per_game_data': games_data,
            'player_averages': player_averages,
            'total_games': len(games_data),
            'spell_type_table': spell_type_table,
            'spell_function_table': spell_function_table,
            'mana_analysis': mana_analysis,
            'draw_analysis': draw_analysis
        }
    
    def _create_spell_type_table(self, game_stats):
        """Create table with columns = decks/players and rows = spell types."""
        table_data = {}
        players = {}
        
        # Process each game stat
        for stat, game_number in game_stats:
            player_key = f"Player {stat.player_number}"
            deck_info = self._get_deck_info(stat.deck_id)
            commander_name = deck_info['commander_name']
            
            if player_key not in players:
                players[player_key] = {
                    'commander_name': commander_name,
                    'deck_id': stat.deck_id,
                    'games_count': 0,
                    'totals': {
                        'Creatures': 0,
                        'Instants': 0,
                        'Sorceries': 0,
                        'Artifacts': 0,
                        'Enchantments': 0,
                        'Planeswalkers': 0,
                        'Lands': 0
                    }
                }
            
            players[player_key]['games_count'] += 1
            players[player_key]['totals']['Creatures'] += stat.creatures_played or 0
            players[player_key]['totals']['Instants'] += stat.instants_played or 0
            players[player_key]['totals']['Sorceries'] += stat.sorceries_played or 0
            players[player_key]['totals']['Artifacts'] += stat.artifacts_played or 0
            players[player_key]['totals']['Enchantments'] += stat.enchantments_played or 0
            players[player_key]['totals']['Planeswalkers'] += stat.planeswalkers_played or 0
            players[player_key]['totals']['Lands'] += stat.lands_played or 0
        
        # Calculate averages and create final table
        spell_types = ['Creatures', 'Instants', 'Sorceries', 'Artifacts', 'Enchantments', 'Planeswalkers', 'Lands']
        
        for spell_type in spell_types:
            table_data[spell_type] = {}
            for player_key, player_data in players.items():
                games_count = player_data['games_count']
                avg_value = player_data['totals'][spell_type] / games_count if games_count > 0 else 0
                table_data[spell_type][player_data['commander_name']] = round(avg_value, 1)
        
        return {
            'data': table_data,
            'players': {p: {'commander_name': d['commander_name'], 'deck_id': d['deck_id']} for p, d in players.items()}
        }
    
    def _create_spell_function_table(self, game_stats):
        """Create table with columns = decks/players and rows = spell functions."""
        table_data = {}
        players = {}
        
        # Process each game stat
        for stat, game_number in game_stats:
            player_key = f"Player {stat.player_number}"
            deck_info = self._get_deck_info(stat.deck_id)
            commander_name = deck_info['commander_name']
            
            if player_key not in players:
                players[player_key] = {
                    'commander_name': commander_name,
                    'deck_id': stat.deck_id,
                    'games_count': 0,
                    'totals': {
                        'Removal': 0,
                        'Ramp': 0,
                        'Card Draw': 0,
                        'Counterspells': 0,
                        'Protection': 0,
                        'Board Wipes': 0,
                        'Tribal': 0,
                        'Combo Pieces': 0,
                        'Value Engines': 0,
                        'Other': 0
                    }
                }
            
            players[player_key]['games_count'] += 1
            players[player_key]['totals']['Removal'] += stat.removal_spells_cast or 0
            players[player_key]['totals']['Ramp'] += stat.ramp_spells_cast or 0
            players[player_key]['totals']['Card Draw'] += stat.card_draw_spells_cast or 0
            players[player_key]['totals']['Counterspells'] += stat.counterspells_cast or 0
            players[player_key]['totals']['Protection'] += stat.protection_spells_cast or 0
            players[player_key]['totals']['Board Wipes'] += stat.board_wipes_cast or 0
            players[player_key]['totals']['Tribal'] += stat.tribal_spells_cast or 0
            players[player_key]['totals']['Combo Pieces'] += stat.combo_pieces_cast or 0
            players[player_key]['totals']['Value Engines'] += stat.value_engines_cast or 0
            players[player_key]['totals']['Other'] += stat.other_spells_cast or 0
        
        # Calculate averages and create final table
        spell_functions = ['Removal', 'Ramp', 'Card Draw', 'Counterspells', 'Protection', 'Board Wipes', 'Tribal', 'Combo Pieces', 'Value Engines', 'Other']
        
        for spell_function in spell_functions:
            table_data[spell_function] = {}
            for player_key, player_data in players.items():
                games_count = player_data['games_count']
                avg_value = player_data['totals'][spell_function] / games_count if games_count > 0 else 0
                table_data[spell_function][player_data['commander_name']] = round(avg_value, 1)
        
        return {
            'data': table_data,
            'players': {p: {'commander_name': d['commander_name'], 'deck_id': d['deck_id']} for p, d in players.items()}
        }
    
    def update_deck_statistics(self, deck_id):
        """Update aggregated statistics for a deck."""
        # Get or create deck statistics record
        deck_stats = DeckStatistics.query.filter_by(deck_id=deck_id).first()
        if not deck_stats:
            deck_stats = DeckStatistics(deck_id=deck_id)
            db.session.add(deck_stats)
        
        # Recalculate statistics from all games
        games = GameStatistics.query.filter_by(deck_id=deck_id).all()
        
        if games:
            deck_stats.total_games = len(games)
            deck_stats.avg_cards_played = sum(g.cards_played_total for g in games) / len(games)
            deck_stats.avg_mana_spent = sum(g.total_mana_spent for g in games) / len(games)
            deck_stats.avg_creatures_played = sum(g.creatures_played for g in games) / len(games)
            deck_stats.avg_commander_casts = sum(g.commander_casts for g in games) / len(games)
            deck_stats.avg_turns_survived = sum(g.turns_survived for g in games) / len(games)
        
        db.session.commit()
    
    def _create_mana_analysis(self, game_stats):
        """Create mana analysis by deck."""
        analysis = {}
        players = {}
        
        # Process each game stat
        for stat, game_number in game_stats:
            deck_info = self._get_deck_info(stat.deck_id)
            commander_name = deck_info['commander_name']
            
            if commander_name not in players:
                players[commander_name] = {
                    'games_count': 0,
                    'total_mana_spent': 0,
                    'total_lands': 0,
                    'total_mana_available': 0
                }
            
            players[commander_name]['games_count'] += 1
            players[commander_name]['total_mana_spent'] += stat.total_mana_spent or 0
            players[commander_name]['total_lands'] += stat.lands_played or 0
            players[commander_name]['total_mana_available'] += stat.total_mana_available or 0
        
        # Calculate averages
        for commander, data in players.items():
            games_count = data['games_count']
            if games_count > 0:
                analysis[commander] = {
                    'avg_mana_spent': round(data['total_mana_spent'] / games_count, 1),
                    'avg_cmc': round((data['total_mana_spent'] / games_count) / max(1, data['total_lands'] / games_count), 1),
                    'avg_lands': round(data['total_lands'] / games_count, 1)
                }
        
        return analysis
    
    def _create_draw_analysis(self, game_stats):
        """Create draw analysis by deck."""
        analysis = {}
        players = {}
        
        # Process each game stat
        for stat, game_number in game_stats:
            deck_info = self._get_deck_info(stat.deck_id)
            commander_name = deck_info['commander_name']
            
            if commander_name not in players:
                players[commander_name] = {
                    'games_count': 0,
                    'total_cards_drawn': 0,
                    'total_turns': 0
                }
            
            players[commander_name]['games_count'] += 1
            players[commander_name]['total_cards_drawn'] += stat.cards_drawn or 0
            players[commander_name]['total_turns'] += stat.turns_survived or 0
        
        # Calculate averages
        for commander, data in players.items():
            games_count = data['games_count']
            if games_count > 0:
                total_cards = data['total_cards_drawn']
                total_turns = data['total_turns']
                avg_turns_per_game = total_turns / games_count
                
                analysis[commander] = {
                    'total_cards_drawn': round(total_cards / games_count, 1),
                    'avg_draw_per_turn': round(total_cards / max(1, total_turns), 1) if total_turns > 0 else 0,
                    'extra_draws': round(max(0, (total_cards / games_count) - avg_turns_per_game), 1)
                }
        
        return analysis
    
    def get_mana_by_turn_data(self, simulation_id):
        """Get mana available by turn for each deck."""
        # Get all game statistics for this simulation
        game_stats = db.session.query(
            GameStatistics,
            GameResult.game_number
        ).join(
            GameResult, GameStatistics.game_id == GameResult.id
        ).filter(
            GameResult.simulation_id == simulation_id
        ).order_by(GameResult.game_number, GameStatistics.player_number).all()
        
        if not game_stats:
            return {}
        
        # Parse game logs to extract turn-by-turn mana data
        mana_by_turn = {}
        
        for stat, game_number in game_stats:
            deck_info = self._get_deck_info(stat.deck_id)
            commander_name = deck_info['commander_name']
            
            if commander_name not in mana_by_turn:
                mana_by_turn[commander_name] = {}
            
            # Parse the game log to extract mana available by turn
            game_result = GameResult.query.filter_by(
                simulation_id=simulation_id,
                game_number=game_number
            ).first()
            
            if game_result and game_result.game_log:
                # Get number of players from simulation
                num_players = len(game_result.simulation.players)
                turn_mana = self._parse_mana_from_log(game_result.game_log, stat.player_number, num_players)
                
                for turn, mana in turn_mana.items():
                    if turn not in mana_by_turn[commander_name]:
                        mana_by_turn[commander_name][turn] = []
                    mana_by_turn[commander_name][turn].append(mana)
        
        # Calculate averages for each turn
        result = {}
        for commander_name, turns in mana_by_turn.items():
            result[commander_name] = []
            for turn in sorted(turns.keys()):
                mana_values = turns[turn]
                avg_mana = sum(mana_values) / len(mana_values)
                result[commander_name].append({
                    'turn': turn,
                    'avg_mana_available': round(avg_mana, 1)
                })
        
        return result
    
    def _parse_mana_from_log(self, game_log, player_number, num_players):
        """Parse game log to extract mana available by turn for a specific player."""
        turn_mana = {}
        
        if not game_log:
            return turn_mana
        
        lines = game_log.split('\n')
        current_turn = 1
        current_turn_player = 1
        mana_this_turn = 0
        
        for line in lines:
            line = line.strip()
            
            # Look for turn indicators using the corrected pattern
            if line.startswith('Turn: Turn'):
                try:
                    # Extract: "Turn: Turn 1 (Ai(1)-BgllFrIGN0mQ0vCapWKbJQ)"
                    turn_match = re.search(r'Turn: Turn (\d+) \(Ai\((\d+)\)', line)
                    if turn_match:
                        new_turn = int(turn_match.group(1))
                        new_player = int(turn_match.group(2))
                        
                        # Save previous turn's mana if it was our player
                        if current_turn_player == player_number and mana_this_turn > 0:
                            # Convert to round number
                            round_number = ((current_turn - 1) // num_players) + 1
                            turn_mana[round_number] = mana_this_turn
                        
                        current_turn = new_turn
                        current_turn_player = new_player
                        mana_this_turn = 0
                except (IndexError, ValueError):
                    pass
            
            # Look for mana production during this player's turn
            if current_turn_player == player_number:
                # Pattern: "Mana: Ancient Tomb (73) - {T}: Add {C}{C}. Ancient Tomb deals 2 damage to you."
                mana_match = re.search(r'Mana: .+ - \{T\}: Add (\{[^}]+\}(?:\{[^}]+\})*)', line)
                if mana_match:
                    mana_symbols = mana_match.group(1)
                    mana_amount = self._parse_mana_symbols(mana_symbols)
                    mana_this_turn += mana_amount
                    continue
                
                # Alternative pattern: "Mana: Card - Add {C}"
                mana_match = re.search(r'Mana: .+ - Add (\{[^}]+\})', line)
                if mana_match:
                    mana_symbols = mana_match.group(1)
                    mana_amount = self._parse_mana_symbols(mana_symbols)
                    mana_this_turn += mana_amount
        
        # Save the last turn's mana
        if current_turn_player == player_number and mana_this_turn > 0:
            round_number = ((current_turn - 1) // num_players) + 1
            turn_mana[round_number] = mana_this_turn
        
        # If we couldn't parse enough mana from logs, use estimated values
        if len(turn_mana) < 3:
            logger.warning(f"Limited mana data parsed for player {player_number}, using estimates")
            # Estimate based on typical mana progression
            for turn in range(1, 16):  # Up to turn 15
                if turn not in turn_mana:
                    # Assume 1 mana per turn on average, with some variance
                    estimated_mana = min(turn, 10)  # Cap at 10 mana
                    turn_mana[turn] = estimated_mana
        
        return turn_mana
    
    def _parse_mana_symbols(self, mana_string):
        """Parse mana symbols like {C}{C} or {W}{B} and return total mana value."""
        # Count occurrences of mana symbols
        mana_count = 0
        
        # Find all mana symbols in braces
        symbols = re.findall(r'\{([^}]+)\}', mana_string)
        
        for symbol in symbols:
            if symbol.isdigit():
                # Numeric mana like {2}
                mana_count += int(symbol)
            elif symbol in ['W', 'U', 'B', 'R', 'G', 'C']:
                # Colored or colorless mana
                mana_count += 1
            elif '/' in symbol:
                # Hybrid mana like {W/B} - count as 1
                mana_count += 1
        
        return mana_count
    
    def get_single_game_mana_data(self, game_id):
        """Get mana available by turn for each deck in a specific game."""
        # Get the game result
        game_result = GameResult.query.get(game_id)
        if not game_result or not game_result.game_log:
            return {}
        
        # Get all game statistics for this specific game
        game_stats = GameStatistics.query.filter_by(game_id=game_id).all()
        
        if not game_stats:
            return {}
        
        # Get number of players from simulation
        num_players = len(game_result.simulation.players)
        
        # Parse mana data for each player
        mana_by_player = {}
        
        for stat in game_stats:
            deck_info = self._get_deck_info(stat.deck_id)
            commander_name = deck_info['commander_name']
            
            # Parse the game log for this specific player
            turn_mana = self._parse_mana_from_log(game_result.game_log, stat.player_number, num_players)
            
            if turn_mana:
                # Convert to the format expected by the chart
                mana_by_player[commander_name] = []
                for turn in sorted(turn_mana.keys()):
                    mana_by_player[commander_name].append({
                        'turn': turn,
                        'mana_available': turn_mana[turn]
                    })
        
        return mana_by_player
    
    def get_mana_spent_by_turn_data(self, simulation_id):
        """Get mana spent by turn for each deck."""
        # Get all game statistics for this simulation
        game_stats = db.session.query(
            GameStatistics,
            GameResult.game_number
        ).join(
            GameResult, GameStatistics.game_id == GameResult.id
        ).filter(
            GameResult.simulation_id == simulation_id
        ).order_by(GameResult.game_number, GameStatistics.player_number).all()
        
        if not game_stats:
            return {}
        
        # Parse game logs to extract turn-by-turn mana spent data
        mana_spent_by_turn = {}
        
        for stat, game_number in game_stats:
            deck_info = self._get_deck_info(stat.deck_id)
            commander_name = deck_info['commander_name']
            
            if commander_name not in mana_spent_by_turn:
                mana_spent_by_turn[commander_name] = {}
            
            # Parse the game log to extract mana spent by turn
            game_result = GameResult.query.filter_by(
                simulation_id=simulation_id,
                game_number=game_number
            ).first()
            
            if game_result and game_result.game_log:
                # Get number of players from simulation
                num_players = len(game_result.simulation.players)
                turn_mana_spent = self._parse_mana_spent_from_log(game_result.game_log, stat.player_number, num_players)
                
                for turn, mana in turn_mana_spent.items():
                    if turn not in mana_spent_by_turn[commander_name]:
                        mana_spent_by_turn[commander_name][turn] = []
                    mana_spent_by_turn[commander_name][turn].append(mana)
        
        # Calculate averages for each turn
        result = {}
        for commander_name, turns in mana_spent_by_turn.items():
            result[commander_name] = []
            for turn in sorted(turns.keys()):
                mana_values = turns[turn]
                avg_mana = sum(mana_values) / len(mana_values)
                result[commander_name].append({
                    'turn': turn,
                    'avg_mana_spent': round(avg_mana, 1)
                })
        
        return result
    
    def _parse_mana_spent_from_log(self, game_log, player_number, num_players):
        """Parse game log to extract mana spent by turn for a specific player."""
        turn_mana_spent = {}
        
        if not game_log:
            return turn_mana_spent
        
        lines = game_log.split('\n')
        current_turn = 1
        current_turn_player = 1
        mana_spent_this_turn = 0
        
        for line in lines:
            line = line.strip()
            
            # Look for turn indicators
            if line.startswith('Turn: Turn'):
                try:
                    # Extract: "Turn: Turn 1 (Ai(1)-BgllFrIGN0mQ0vCapWKbJQ)"
                    turn_match = re.search(r'Turn: Turn (\d+) \(Ai\((\d+)\)', line)
                    if turn_match:
                        new_turn = int(turn_match.group(1))
                        new_player = int(turn_match.group(2))
                        
                        # Save previous turn's mana if it was our player
                        if current_turn_player == player_number and mana_spent_this_turn > 0:
                            # Convert to round number
                            round_number = ((current_turn - 1) // num_players) + 1
                            turn_mana_spent[round_number] = mana_spent_this_turn
                        
                        current_turn = new_turn
                        current_turn_player = new_player
                        mana_spent_this_turn = 0
                except (IndexError, ValueError):
                    pass
            
            # Look for spell casting during this player's turn
            if current_turn_player == player_number:
                # Pattern for spells with mana cost
                # Example: "Cast: Lightning Bolt (141) - Deal 3 damage to any target. Cost: {R}"
                spell_match = re.search(r'Cast: .+ - .+ Cost: (\{[^}]*\}(?:\{[^}]*\})*)', line)
                if spell_match:
                    mana_symbols = spell_match.group(1)
                    mana_cost = self._parse_mana_symbols(mana_symbols)
                    mana_spent_this_turn += mana_cost
                    continue
                
                # Alternative pattern: "Cast: Card Name - Cost {2}{R}"
                spell_match = re.search(r'Cast: .+ - .+ \{(\d+)\}', line)
                if spell_match:
                    mana_cost = int(spell_match.group(1))
                    # Also count colored mana symbols if present
                    colored_symbols = re.findall(r'\{[WUBRGC]\}', line)
                    mana_cost += len(colored_symbols)
                    mana_spent_this_turn += mana_cost
                    continue
                
                # Look for ability activations that cost mana
                ability_match = re.search(r'Ability: .+ - .+ Cost: (\{[^}]*\}(?:\{[^}]*\})*)', line)
                if ability_match:
                    mana_symbols = ability_match.group(1)
                    mana_cost = self._parse_mana_symbols(mana_symbols)
                    mana_spent_this_turn += mana_cost
        
        # Save the last turn's mana
        if current_turn_player == player_number and mana_spent_this_turn > 0:
            round_number = ((current_turn - 1) // num_players) + 1
            turn_mana_spent[round_number] = mana_spent_this_turn
        
        # If we couldn't parse much mana spending from logs, use some estimates
        if len(turn_mana_spent) < 3:
            logger.warning(f"Limited mana spent data parsed for player {player_number}, using estimates")
            # Estimate based on typical progression
            for turn in range(1, 16):  # Up to turn 15
                if turn not in turn_mana_spent:
                    # Estimate based on turn number (early turns = less mana, later = more)
                    estimated_spent = min(turn - 1, 8)  # Start spending from turn 2, cap at 8
                    if estimated_spent > 0:
                        turn_mana_spent[turn] = estimated_spent
        
        return turn_mana_spent
    
    def get_single_game_mana_spent_data(self, game_id):
        """Get mana spent by turn for each deck in a specific game."""
        # Get the game result
        game_result = GameResult.query.get(game_id)
        if not game_result or not game_result.game_log:
            return {}
        
        # Get all game statistics for this specific game
        game_stats = GameStatistics.query.filter_by(game_id=game_id).all()
        
        if not game_stats:
            return {}
        
        # Get number of players from simulation
        num_players = len(game_result.simulation.players)
        
        # Parse mana spent data for each player
        mana_spent_by_player = {}
        
        for stat in game_stats:
            deck_info = self._get_deck_info(stat.deck_id)
            commander_name = deck_info['commander_name']
            
            # Parse the game log for this specific player
            turn_mana_spent = self._parse_mana_spent_from_log(game_result.game_log, stat.player_number, num_players)
            
            if turn_mana_spent:
                # Convert to the format expected by the chart
                mana_spent_by_player[commander_name] = []
                for turn in sorted(turn_mana_spent.keys()):
                    mana_spent_by_player[commander_name].append({
                        'turn': turn,
                        'mana_spent': turn_mana_spent[turn]
                    })
        
        return mana_spent_by_player