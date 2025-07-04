from .. import db
from datetime import datetime
from sqlalchemy.dialects.postgresql import UUID
import uuid

class Simulation(db.Model):
    __tablename__ = 'simulations'
    
    id = db.Column(db.String(36), primary_key=True, default=lambda: str(uuid.uuid4()))
    name = db.Column(db.String(200), nullable=False)
    user_id = db.Column(db.Integer, db.ForeignKey('users.id'), nullable=False)
    num_games = db.Column(db.Integer, nullable=False)
    games_completed = db.Column(db.Integer, default=0)
    game_timeout = db.Column(db.Integer, default=600)  # 10 minutes in seconds
    status = db.Column(db.String(20), default='pending')  # pending, running, completed, failed, stopped
    created_at = db.Column(db.DateTime, default=datetime.utcnow)
    started_at = db.Column(db.DateTime)
    completed_at = db.Column(db.DateTime)
    error_message = db.Column(db.Text)
    
    # Relationships
    players = db.relationship('SimulationPlayer', backref='simulation', lazy=True, cascade='all, delete-orphan')
    game_results = db.relationship('GameResult', backref='simulation', lazy=True, cascade='all, delete-orphan')
    
    def __repr__(self):
        return f'<Simulation {self.name}>'
    
    @property
    def progress_percent(self):
        if self.num_games == 0:
            return 0
        return (self.games_completed / self.num_games) * 100

class SimulationPlayer(db.Model):
    __tablename__ = 'simulation_players'
    
    id = db.Column(db.Integer, primary_key=True)
    simulation_id = db.Column(db.String(36), db.ForeignKey('simulations.id'), nullable=False)
    player_number = db.Column(db.Integer, nullable=False)  # 1-4
    deck_id = db.Column(db.String(100), nullable=False)
    
    def __repr__(self):
        return f'<SimulationPlayer {self.player_number}: {self.deck_id}>'

class GameResult(db.Model):
    __tablename__ = 'game_results'
    
    id = db.Column(db.Integer, primary_key=True)
    simulation_id = db.Column(db.String(36), db.ForeignKey('simulations.id'), nullable=False)
    game_number = db.Column(db.Integer, nullable=False)
    winner_player_number = db.Column(db.Integer)
    winner_deck_id = db.Column(db.String(100))
    game_duration_seconds = db.Column(db.Integer)
    total_turns = db.Column(db.Integer)
    elimination_order = db.Column(db.JSON)  # List of player numbers in elimination order
    game_log = db.Column(db.Text)  # Full game log from Forge
    created_at = db.Column(db.DateTime, default=datetime.utcnow)
    
    # Relationships
    statistics = db.relationship('GameStatistics', backref='game', lazy=True, cascade='all, delete-orphan')
    
    def __repr__(self):
        return f'<GameResult {self.simulation_id}:{self.game_number}>'