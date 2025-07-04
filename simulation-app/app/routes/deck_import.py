"""
Deck Import Routes
Handles web interface for importing decks from external websites.
"""

from flask import Blueprint, render_template, request, jsonify, flash, redirect, url_for
from flask_login import login_required, current_user
import logging
from ..services.deck_scraper import DeckScraper

logger = logging.getLogger(__name__)

deck_import_bp = Blueprint('deck_import', __name__)

@deck_import_bp.route('/import')
@login_required
def import_page():
    """Show the deck import page."""
    return render_template('deck_import/import.html')

@deck_import_bp.route('/api/import', methods=['POST'])
@login_required
def api_import_deck():
    """API endpoint to import a deck from a URL."""
    try:
        data = request.get_json()
        if not data or 'url' not in data:
            return jsonify({'error': 'URL is required'}), 400
        
        url = data['url'].strip()
        if not url:
            return jsonify({'error': 'URL cannot be empty'}), 400
        
        # Initialize scraper
        scraper = DeckScraper()
        
        # Check if URL is supported
        parsed = scraper.parse_url(url)
        if not parsed:
            supported_sites = list(set(scraper.SUPPORTED_SITES.values()))
            return jsonify({
                'error': f'Unsupported website. Supported sites: {", ".join(supported_sites)}'
            }), 400
        
        # Scrape and upload the deck
        result = scraper.scrape_and_upload(url)
        
        if result['success']:
            logger.info(f"User {current_user.id} successfully imported deck {result['deck_id']}")
            return jsonify({
                'success': True,
                'deck_id': result['deck_id'],
                'deck_name': result['deck_name'],
                'message': f"Successfully imported '{result['deck_name']}'"
            })
        else:
            logger.warning(f"Failed to import deck from {url}: {result['error']}")
            return jsonify({
                'success': False,
                'error': result['error'] or 'Unknown error occurred'
            }), 500
    
    except Exception as e:
        logger.error(f"Error importing deck: {e}")
        return jsonify({
            'success': False,
            'error': 'Internal server error'
        }), 500

@deck_import_bp.route('/api/preview', methods=['POST'])
@login_required
def api_preview_deck():
    """API endpoint to preview a deck without importing it."""
    try:
        data = request.get_json()
        if not data or 'url' not in data:
            return jsonify({'error': 'URL is required'}), 400
        
        url = data['url'].strip()
        if not url:
            return jsonify({'error': 'URL cannot be empty'}), 400
        
        # Initialize scraper
        scraper = DeckScraper()
        
        # Check if URL is supported
        parsed = scraper.parse_url(url)
        if not parsed:
            supported_sites = list(set(scraper.SUPPORTED_SITES.values()))
            return jsonify({
                'error': f'Unsupported website. Supported sites: {", ".join(supported_sites)}'
            }), 400
        
        # Scrape the deck (without uploading)
        deck_data = scraper.scrape_deck(url)
        forge_content = scraper.convert_to_forge_format(deck_data)
        
        # Return preview data
        preview_data = {
            'success': True,
            'deck_name': deck_data['deck_name'],
            'commander_1': deck_data.get('commander_1'),
            'commander_2': deck_data.get('commander_2'),
            'source': deck_data['source'],
            'card_count': len(deck_data.get('cards', [])),
            'color_identity': {
                'W': deck_data.get('is_W', False),
                'U': deck_data.get('is_U', False),
                'B': deck_data.get('is_B', False),
                'R': deck_data.get('is_R', False),
                'G': deck_data.get('is_G', False)
            },
            'forge_preview': forge_content[:500] + '...' if len(forge_content) > 500 else forge_content
        }
        
        return jsonify(preview_data)
    
    except Exception as e:
        logger.error(f"Error previewing deck: {e}")
        return jsonify({
            'success': False,
            'error': str(e)
        }), 500

@deck_import_bp.route('/api/check-url', methods=['POST'])
def api_check_url():
    """API endpoint to check if a URL is supported."""
    try:
        data = request.get_json()
        if not data or 'url' not in data:
            return jsonify({'error': 'URL is required'}), 400
        
        url = data['url'].strip()
        if not url:
            return jsonify({'error': 'URL cannot be empty'}), 400
        
        scraper = DeckScraper()
        parsed = scraper.parse_url(url)
        
        if parsed:
            site_type, deck_id = parsed
            return jsonify({
                'supported': True,
                'site': site_type,
                'deck_id': deck_id
            })
        else:
            supported_sites = list(set(scraper.SUPPORTED_SITES.values()))
            return jsonify({
                'supported': False,
                'supported_sites': supported_sites
            })
    
    except Exception as e:
        logger.error(f"Error checking URL: {e}")
        return jsonify({
            'supported': False,
            'error': str(e)
        }), 500

@deck_import_bp.route('/help')
def import_help():
    """Show help page for deck import."""
    scraper = DeckScraper()
    supported_sites = list(set(scraper.SUPPORTED_SITES.values()))
    
    example_urls = {
        'moxfield': 'https://www.moxfield.com/decks/abc123',
        'deckstats': 'https://deckstats.net/decks/12345/67890',
        'archidekt': 'https://archidekt.com/decks/123456'
    }
    
    return render_template('deck_import/help.html', 
                         supported_sites=supported_sites,
                         example_urls=example_urls)