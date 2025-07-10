from flask import Blueprint, render_template
from flask_login import login_required
from ..models.simulation import Simulation
from ..models.statistics import GameStatistics

main_bp = Blueprint('main', __name__)

@main_bp.route('/')
@login_required
def dashboard():
    """Main dashboard showing overview of simulations and quick stats."""
    recent_simulations = Simulation.query.order_by(Simulation.created_at.desc()).limit(5).all()
    total_simulations = Simulation.query.count()
    total_games = GameStatistics.query.count()
    
    return render_template('dashboard.html', 
                         recent_simulations=recent_simulations,
                         total_simulations=total_simulations,
                         total_games=total_games)