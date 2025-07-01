from flask import Flask
from flask_sqlalchemy import SQLAlchemy
from flask_login import LoginManager
import os

db = SQLAlchemy()
login_manager = LoginManager()

def create_app():
    app = Flask(__name__)
    
    # Configuration
    app.config['SECRET_KEY'] = os.environ.get('SECRET_KEY', 'dev-secret-key-change-in-production')
    app.config['SQLALCHEMY_DATABASE_URI'] = os.environ.get('DATABASE_URL', 'sqlite:///simulation.db')
    app.config['SQLALCHEMY_TRACK_MODIFICATIONS'] = False
    
    # Initialize extensions
    db.init_app(app)
    login_manager.init_app(app)
    login_manager.login_view = 'auth.login'
    
    # Register blueprints
    from .routes.main import main_bp
    from .routes.simulation import simulation_bp
    from .routes.monitoring import monitoring_bp
    from .routes.statistics import statistics_bp
    from .routes.decks import decks_bp
    from .routes.auth import auth_bp
    from .routes.deck_import import deck_import_bp
    
    app.register_blueprint(main_bp)
    app.register_blueprint(simulation_bp, url_prefix='/simulation')
    app.register_blueprint(monitoring_bp, url_prefix='/monitoring')
    app.register_blueprint(statistics_bp, url_prefix='/statistics')
    app.register_blueprint(decks_bp, url_prefix='/decks')
    app.register_blueprint(auth_bp, url_prefix='/auth')
    app.register_blueprint(deck_import_bp, url_prefix='/deck_import')
    
    # Create tables
    with app.app_context():
        db.create_all()
    
    return app