from flask import Blueprint, render_template, request, jsonify
from flask_login import login_required, current_user
from ..models.simulation import Simulation
from ..models.statistics import GameStatistics, DeckStatistics
from ..services.statistics_engine import StatisticsEngine
from sqlalchemy import func
from .. import db

statistics_bp = Blueprint('statistics', __name__)

@statistics_bp.route('/')
@login_required
def statistics_dashboard():
    """Main statistics dashboard."""
    from ..models.simulation import GameResult
    
    user_simulations = Simulation.query.filter_by(user_id=current_user.id).all()
    
    # Basic statistics - need to join through GameResult
    total_games = GameStatistics.query.join(
        GameResult, GameStatistics.game_id == GameResult.id
    ).join(
        Simulation, GameResult.simulation_id == Simulation.id
    ).filter(Simulation.user_id == current_user.id).count()
    
    # For average game duration, we should use GameResult.game_duration_seconds
    avg_game_duration = db.session.query(func.avg(GameResult.game_duration_seconds)).join(
        Simulation, GameResult.simulation_id == Simulation.id
    ).filter(Simulation.user_id == current_user.id).scalar() or 0
    
    return render_template('statistics/dashboard.html',
                         simulations=user_simulations,
                         total_games=total_games,
                         avg_game_duration=int(avg_game_duration))

@statistics_bp.route('/simulation/<simulation_id>')
@login_required
def simulation_statistics(simulation_id):
    """Detailed statistics for a specific simulation."""
    simulation = Simulation.query.get_or_404(simulation_id)
    
    if simulation.user_id != current_user.id:
        return "Unauthorized", 403
    
    # Get detailed statistics
    stats_engine = StatisticsEngine()
    detailed_stats = stats_engine.get_simulation_statistics(simulation_id)
    
    return render_template('statistics/simulation_detail.html',
                         simulation=simulation,
                         stats=detailed_stats)

@statistics_bp.route('/deck/<deck_id>')
@login_required
def deck_statistics(deck_id):
    """Statistics for a specific deck across all simulations."""
    deck_stats = DeckStatistics.query.filter_by(deck_id=deck_id).first()
    
    if not deck_stats:
        return "Deck not found", 404
    
    # Get deck performance across user's simulations
    stats_engine = StatisticsEngine()
    performance_stats = stats_engine.get_deck_performance(deck_id, current_user.id)
    
    return render_template('statistics/deck_detail.html',
                         deck_id=deck_id,
                         deck_stats=deck_stats,
                         performance=performance_stats)

@statistics_bp.route('/api/simulation/<simulation_id>/charts')
@login_required
def api_simulation_charts(simulation_id):
    """API endpoint for chart data."""
    simulation = Simulation.query.get_or_404(simulation_id)
    
    if simulation.user_id != current_user.id:
        return jsonify({'error': 'Unauthorized'}), 403
    
    stats_engine = StatisticsEngine()
    chart_data = stats_engine.get_chart_data(simulation_id)
    
    return jsonify(chart_data)

@statistics_bp.route('/api/comparison')
@login_required
def api_deck_comparison():
    """API endpoint for deck comparison data."""
    deck_ids = request.args.getlist('deck_ids')
    
    if not deck_ids:
        return jsonify({'error': 'No deck IDs provided'}), 400
    
    stats_engine = StatisticsEngine()
    comparison_data = stats_engine.compare_decks(deck_ids, current_user.id)
    
    return jsonify(comparison_data)

@statistics_bp.route('/game/<int:game_id>/log')
@login_required
def game_log(game_id):
    """View detailed log for a specific game."""
    from ..models.simulation import GameResult
    
    game_result = GameResult.query.get_or_404(game_id)
    simulation = game_result.simulation
    
    if simulation.user_id != current_user.id:
        return "Unauthorized", 403
    
    return render_template('statistics/game_log.html', 
                         game_result=game_result, 
                         simulation=simulation)

@statistics_bp.route('/api/game/<int:game_id>/details')
@login_required
def api_game_details(game_id):
    """API endpoint for detailed game information."""
    from ..models.simulation import GameResult
    from ..models.statistics import CardPlay
    
    game_result = GameResult.query.get_or_404(game_id)
    simulation = game_result.simulation
    
    if simulation.user_id != current_user.id:
        return jsonify({'error': 'Unauthorized'}), 403
    
    # Get game statistics
    game_stats = GameStatistics.query.filter_by(game_id=game_id).all()
    
    # Get card plays for spell categories
    card_plays = CardPlay.query.filter_by(game_id=game_id).all()
    
    # Aggregate spell categories
    spell_categories = {}
    for card_play in card_plays:
        if card_play.functional_category:
            category = card_play.functional_category.replace('_', ' ').title()
            spell_categories[category] = spell_categories.get(category, 0) + 1
    
    # Aggregate player stats
    player_stats = []
    for stat in game_stats:
        cards_played = (stat.creatures_played + stat.artifacts_played + 
                       stat.enchantments_played + stat.instants_played + 
                       stat.sorceries_played + stat.planeswalkers_played)
        
        player_stats.append({
            'player_number': stat.player_number,
            'cards_played': cards_played,
            'mana_spent': stat.total_mana_spent,
            'final_life': stat.final_life_total
        })
    
    # Format duration
    duration = None
    if game_result.game_duration_seconds:
        if game_result.game_duration_seconds > 60:
            duration = f"{game_result.game_duration_seconds / 60:.1f}m"
        else:
            duration = f"{game_result.game_duration_seconds}s"
    
    return jsonify({
        'success': True,
        'game': {
            'id': game_id,
            'game_number': game_result.game_number,
            'winner': game_result.winner_player_number,
            'duration': duration,
            'turns': game_result.total_turns,
            'cards_played': len(card_plays),
            'player_stats': player_stats,
            'spell_categories': spell_categories
        }
    })

@statistics_bp.route('/api/game/<int:game_id>/turns')
@login_required
def api_game_turns(game_id):
    """API endpoint for turn-by-turn card play data."""
    from ..models.simulation import GameResult
    from ..models.statistics import CardPlay
    
    game_result = GameResult.query.get_or_404(game_id)
    simulation = game_result.simulation
    
    if simulation.user_id != current_user.id:
        return jsonify({'error': 'Unauthorized'}), 403
    
    # Get card plays ordered by turn and play order
    card_plays = CardPlay.query.filter_by(game_id=game_id).order_by(
        CardPlay.turn_number, CardPlay.play_order
    ).all()
    
    turns_data = []
    for card_play in card_plays:
        # Format functional category for display
        functional_category = card_play.functional_category
        if functional_category:
            functional_category = functional_category.replace('_', ' ').title()
        
        turns_data.append({
            'turn_number': card_play.turn_number,
            'player_number': card_play.player_number,
            'card_name': card_play.card_name,
            'card_type': card_play.card_type,
            'functional_category': functional_category,
            'mana_cost': card_play.mana_cost,
            'play_order': card_play.play_order
        })
    
    return jsonify({
        'success': True,
        'turns': turns_data
    })

@statistics_bp.route('/api/simulation/<simulation_id>/mana-by-turn')
@login_required
def api_simulation_mana_by_turn(simulation_id):
    """API endpoint for mana available by turn data."""
    simulation = Simulation.query.get_or_404(simulation_id)
    
    if simulation.user_id != current_user.id:
        return jsonify({'error': 'Unauthorized'}), 403
    
    stats_engine = StatisticsEngine()
    mana_data = stats_engine.get_mana_by_turn_data(simulation_id)
    
    return jsonify({
        'success': True,
        'mana_data': mana_data
    })

@statistics_bp.route('/api/game/<int:game_id>/mana-by-turn')
@login_required
def api_game_mana_by_turn(game_id):
    """API endpoint for mana available by turn data for a specific game."""
    from ..models.simulation import GameResult
    
    game_result = GameResult.query.get_or_404(game_id)
    simulation = game_result.simulation
    
    if simulation.user_id != current_user.id:
        return jsonify({'error': 'Unauthorized'}), 403
    
    stats_engine = StatisticsEngine()
    mana_data = stats_engine.get_single_game_mana_data(game_id)
    
    return jsonify({
        'success': True,
        'mana_data': mana_data
    })

