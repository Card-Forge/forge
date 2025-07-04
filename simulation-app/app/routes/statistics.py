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
    user_simulations = Simulation.query.filter_by(user_id=current_user.id).all()
    
    # Basic statistics
    total_games = GameStatistics.query.join(Simulation).filter(Simulation.user_id == current_user.id).count()
    avg_game_duration = db.session.query(func.avg(GameStatistics.game_duration_seconds)).join(Simulation).filter(Simulation.user_id == current_user.id).scalar() or 0
    
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