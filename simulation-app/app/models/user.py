from flask_login import UserMixin
from .. import db
from datetime import datetime

class User(UserMixin, db.Model):
    __tablename__ = 'users'
    
    id = db.Column(db.Integer, primary_key=True)
    username = db.Column(db.String(80), unique=True, nullable=False)
    password_hash = db.Column(db.String(200), nullable=False)
    created_at = db.Column(db.DateTime, default=datetime.utcnow)
    
    # Relationships
    simulations = db.relationship('Simulation', backref='user', lazy=True)
    
    def __repr__(self):
        return f'<User {self.username}>'