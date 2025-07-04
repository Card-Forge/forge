from sqlalchemy import func
from ..models.simulation import Simulation, GameResult
from ..models.statistics import GameStatistics, DeckStatistics, SimulationSummary
from .. import db
import logging

logger = logging.getLogger(__name__)

class StatisticsEngine:
    def __init__(self):
        pass
    
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
            
            # Win rate by player number
            position_analysis = self._analyze_player_positions(game_results)
            stats['win_rates_by_player'] = {f"Player {pos}": data['win_rate'] for pos, data in position_analysis.items() if data['games'] > 0}
            
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
                stats['turn_distribution'] = self._get_turn_distribution(turns)
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
        
        # Turn count distribution
        turn_counts = {}
        for game in game_results:
            if game.total_turns:
                turns = game.total_turns
                turn_range = f"{(turns//5)*5}-{(turns//5)*5+4}"  # Group into ranges
                turn_counts[turn_range] = turn_counts.get(turn_range, 0) + 1
        
        turn_distribution_data = {
            'labels': sorted(turn_counts.keys()),
            'data': [turn_counts[label] for label in sorted(turn_counts.keys())]
        }
        
        return {
            'win_rates': win_rate_data,
            'game_durations': duration_data,
            'turn_distribution': turn_distribution_data
        }
    
    def compare_decks(self, deck_ids, user_id):
        """Compare performance of multiple decks."""
        comparison_data = {}
        
        for deck_id in deck_ids:
            performance = self.get_deck_performance(deck_id, user_id)
            comparison_data[deck_id] = performance
        
        return comparison_data
    
    def _get_turn_distribution(self, turns):
        """Calculate turn count distribution by ranges."""
        distribution = {}
        for turn_count in turns:
            turn_range = f"{(turn_count//5)*5}-{(turn_count//5)*5+4}"
            distribution[turn_range] = distribution.get(turn_range, 0) + 1
        return distribution
    
    def _calculate_deck_performance(self, game_results):
        """Calculate win rates and performance metrics for each deck."""
        deck_stats = {}
        
        # Initialize deck stats
        for game in game_results:
            for player in game.simulation.players:
                if player.deck_id not in deck_stats:
                    deck_stats[player.deck_id] = {
                        'games': 0,
                        'wins': 0,
                        'eliminations': [],
                        'durations': []
                    }
        
        # Count wins and games
        for game in game_results:
            winner_deck = game.winner_deck_id
            
            for player in game.simulation.players:
                deck_id = player.deck_id
                deck_stats[deck_id]['games'] += 1
                
                if deck_id == winner_deck:
                    deck_stats[deck_id]['wins'] += 1
                
                if game.game_duration_seconds:
                    deck_stats[deck_id]['durations'].append(game.game_duration_seconds)
        
        # Calculate rates
        for deck_id, stats in deck_stats.items():
            total_games = stats['games']
            if total_games > 0:
                stats['win_rate'] = (stats['wins'] / total_games) * 100
                if stats['durations']:
                    stats['avg_duration'] = sum(stats['durations']) / len(stats['durations'])
                else:
                    stats['avg_duration'] = 0
            else:
                stats['win_rate'] = 0
                stats['avg_duration'] = 0
        
        return deck_stats
    
    def _analyze_player_positions(self, game_results):
        """Analyze advantage of different player positions."""
        position_stats = {1: {'games': 0, 'wins': 0}, 2: {'games': 0, 'wins': 0}, 
                         3: {'games': 0, 'wins': 0}, 4: {'games': 0, 'wins': 0}}
        
        for game in game_results:
            winner_player = game.winner_player_number
            
            # Count games for each position
            for player in game.simulation.players:
                position = player.player_number
                if position in position_stats:
                    position_stats[position]['games'] += 1
                    
                    if position == winner_player:
                        position_stats[position]['wins'] += 1
        
        # Calculate win rates
        for position, stats in position_stats.items():
            if stats['games'] > 0:
                stats['win_rate'] = (stats['wins'] / stats['games']) * 100
            else:
                stats['win_rate'] = 0
        
        return position_stats
    
    def _get_card_statistics(self, simulation_id):
        """Get aggregated card usage statistics for a simulation."""
        stats = db.session.query(
            func.sum(GameStatistics.creatures_played).label('total_creatures'),
            func.sum(GameStatistics.instants_played).label('total_instants'),
            func.sum(GameStatistics.sorceries_played).label('total_sorceries'),
            func.sum(GameStatistics.artifacts_played).label('total_artifacts'),
            func.sum(GameStatistics.enchantments_played).label('total_enchantments'),
            func.sum(GameStatistics.planeswalkers_played).label('total_planeswalkers'),
            func.sum(GameStatistics.lands_played).label('total_lands'),
            func.sum(GameStatistics.removal_spells_cast).label('total_removal'),
            func.sum(GameStatistics.ramp_spells_cast).label('total_ramp'),
            func.sum(GameStatistics.card_draw_spells_cast).label('total_card_draw'),
            func.sum(GameStatistics.counterspells_cast).label('total_counterspells'),
            func.sum(GameStatistics.protection_spells_cast).label('total_protection'),
            func.sum(GameStatistics.board_wipes_cast).label('total_board_wipes'),
            func.sum(GameStatistics.tribal_spells_cast).label('total_tribal'),
            func.sum(GameStatistics.combo_pieces_cast).label('total_combo_pieces'),
            func.sum(GameStatistics.value_engines_cast).label('total_value_engines'),
            func.sum(GameStatistics.other_spells_cast).label('total_other'),
            func.avg(GameStatistics.commander_casts).label('avg_commander_casts')
        ).join(
            GameResult, GameStatistics.game_id == GameResult.id
        ).filter(
            GameResult.simulation_id == simulation_id
        ).first()
        
        if stats:
            return {
                'card_types': {
                    'Creatures': stats.total_creatures or 0,
                    'Instants': stats.total_instants or 0,
                    'Sorceries': stats.total_sorceries or 0,
                    'Artifacts': stats.total_artifacts or 0,
                    'Enchantments': stats.total_enchantments or 0,
                    'Planeswalkers': stats.total_planeswalkers or 0,
                    'Lands': stats.total_lands or 0
                },
                'spell_categories': {
                    'Removal': stats.total_removal or 0,
                    'Ramp': stats.total_ramp or 0,
                    'Card Draw': stats.total_card_draw or 0,
                    'Counterspells': stats.total_counterspells or 0,
                    'Protection': stats.total_protection or 0,
                    'Board Wipes': stats.total_board_wipes or 0,
                    'Tribal': stats.total_tribal or 0,
                    'Combo Pieces': stats.total_combo_pieces or 0,
                    'Value Engines': stats.total_value_engines or 0,
                    'Other': stats.total_other or 0
                },
                'commander_stats': {
                    'avg_casts_per_game': float(stats.avg_commander_casts or 0)
                }
            }
        
        return {}
    
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