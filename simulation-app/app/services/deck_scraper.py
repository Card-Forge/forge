"""
Deck Scraping Service
Handles downloading deck lists from various MTG deck websites and uploading to BigQuery.
Based on the patterns from mtg/bigquery.
"""

import requests
import re
import json
import logging
import subprocess
import time
import random
import os
from urllib.parse import urlparse
from datetime import datetime
from typing import Dict, List, Optional, Tuple

logger = logging.getLogger(__name__)


class DeckScraper:
    """Service for scraping deck lists from various MTG websites."""
    
    SUPPORTED_SITES = {
        'moxfield.com': 'moxfield',
        'www.moxfield.com': 'moxfield',
        'deckstats.net': 'deckstats',
        'www.deckstats.net': 'deckstats',
        'archidekt.com': 'archidekt',
        'www.archidekt.com': 'archidekt'
    }
    
    def __init__(self):
        # Set up request headers following mtg patterns
        self.headers = {
            'User-Agent': 'Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36',
            'Accept': 'text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8',
            'Accept-Language': 'en-US,en;q=0.9',
            'DNT': '1',
            'Sec-Fetch-Dest': 'document',
            'Sec-Fetch-Mode': 'navigate',
            'Sec-Fetch-Site': 'none',
            'Sec-Fetch-User': '?1'
        }
    
    def parse_url(self, url: str) -> Optional[Tuple[str, str]]:
        """Parse a deck URL to identify the site and extract deck ID."""
        try:
            parsed = urlparse(url)
            domain = parsed.netloc.lower()
            
            if domain not in self.SUPPORTED_SITES:
                return None
            
            site_type = self.SUPPORTED_SITES[domain]
            
            if site_type == 'moxfield':
                # Moxfield URLs: https://www.moxfield.com/decks/[deck_id]
                match = re.search(r'/decks/([a-zA-Z0-9_-]+)', parsed.path)
                if match:
                    return 'moxfield', match.group(1)
            
            elif site_type == 'deckstats':
                # Deckstats URLs: https://deckstats.net/decks/[user_id]/[deck_id]
                match = re.search(r'/decks/(\d+)/(\d+)', parsed.path)
                if match:
                    return 'deckstats', f"{match.group(1)}/{match.group(2)}"
            
            elif site_type == 'archidekt':
                # Archidekt URLs: https://archidekt.com/decks/[deck_id]
                match = re.search(r'/decks/(\d+)', parsed.path)
                if match:
                    return 'archidekt', match.group(1)
            
            return None
            
        except Exception as e:
            logger.error(f"Error parsing URL {url}: {e}")
            return None
    
    def scrape_deck(self, url: str) -> Optional[Dict]:
        """Scrape a deck from a supported website."""
        parsed = self.parse_url(url)
        if not parsed:
            raise ValueError(f"Unsupported URL or invalid format: {url}")
        
        site_type, deck_id = parsed
        
        try:
            if site_type == 'moxfield':
                return self._scrape_moxfield(deck_id, url)
            elif site_type == 'deckstats':
                return self._scrape_deckstats(deck_id, url)
            elif site_type == 'archidekt':
                return self._scrape_archidekt(deck_id, url)
            else:
                raise ValueError(f"Scraper not implemented for {site_type}")
                
        except Exception as e:
            logger.error(f"Error scraping deck from {url}: {e}")
            raise
    
    def _moxfield_api_request(self, url: str) -> Optional[Dict]:
        """Make a request to the Moxfield API using curl (following mtg pattern)."""
        # Use curl directly as in mtg/scraping/scraping_utils.py
        cmd_list = [
            "curl",
            url,
            "-H",
            "accept: text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7",
            "-H",
            "accept-language: en-US,en;q=0.9",
            "-H",
            "dnt: 1",
            "-H",
            "priority: u=0, i",
            "-H",
            'sec-ch-ua: "Google Chrome";v="131", "Chromium";v="131", "Not_A Brand";v="24"',
            "-H",
            "sec-ch-ua-mobile: ?0",
            "-H",
            'sec-ch-ua-platform: "macOS"',
            "-H",
            "sec-fetch-dest: document",
            "-H",
            "sec-fetch-mode: navigate",
            "-H",
            "sec-fetch-site: none",
            "-H",
            "sec-fetch-user: ?1",
            "-H",
            "upgrade-insecure-requests: 1",
            "-H",
            "user-agent: Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36",
        ]
        
        try:
            result = subprocess.check_output(cmd_list)
            return json.loads(result)
        except Exception as e:
            logger.error(f"Curl request failed: {e}")
            return None
    
    def _scrape_moxfield(self, deck_id: str, original_url: str) -> Dict:
        """Scrape a deck from Moxfield using the API pattern from mtg."""
        api_url = f"https://api2.moxfield.com/v3/decks/all/{deck_id}"
        
        data = self._moxfield_api_request(api_url)
        if not data:
            raise Exception(f"Failed to fetch Moxfield deck {deck_id}")
        
        # Extract deck information following mtg patterns
        deck_info = {
            'deck_id': deck_id,  # Use original ID, not prefixed
            'deck_name': data.get('name', 'Unknown Deck'),
            'deck_url': original_url,
            'commander_1': None,
            'commander_2': None,
            'source': 'moxfield',
            'format': data.get('format', 'commander').lower(),
            'cards': [],
            'raw_data': data  # Store raw data for debugging
        }
        
        # Extract commanders
        commanders = data['boards']['commanders']['cards']
        commander_names = []
        for card_data in commanders.values():
            card_name = card_data.get('card', {}).get('name')
            if card_name:
                commander_names.append(card_name)
        
        if commander_names:
            deck_info['commander_1'] = commander_names[0]
            if len(commander_names) > 1:
                deck_info['commander_2'] = commander_names[1]
        
        # Extract color identity
        colors = data.get('colorIdentity', [])
        deck_info.update({
            'is_W': 'W' in colors,
            'is_U': 'U' in colors,
            'is_B': 'B' in colors,
            'is_R': 'R' in colors,
            'is_G': 'G' in colors
        })
        
        # Extract mainboard cards
        mainboard = data['boards']['mainboard']['cards']
        for card_data in mainboard.values():
            card = card_data.get('card', {})
            card_name = card.get('name')
            if card_name:
                deck_info['cards'].append({
                    'name': card_name,
                    'quantity': card_data.get('quantity', 1),
                    'type': 'main'
                })
        
        return deck_info
    
    def _scrape_deckstats(self, deck_path: str, original_url: str) -> Dict:
        """Scrape a deck from Deckstats."""
        try:
            response = requests.get(original_url, headers=self.headers, timeout=30)
            response.raise_for_status()
            
            html_content = response.text
            
            # Extract deck information using regex patterns
            deck_info = {
                'deck_id': deck_path.replace('/', '-'),
                'deck_name': self._extract_deckstats_name(html_content),
                'deck_url': original_url,
                'commander_1': None,
                'commander_2': None,
                'source': 'deckstats',
                'format': 'commander',
                'cards': []
            }
            
            # Extract cards from the HTML
            cards = self._extract_deckstats_cards(html_content)
            deck_info['cards'] = cards
            
            # Try to identify commander(s) from section headers or card data
            commanders = self._extract_deckstats_commanders(html_content, cards)
            if commanders:
                deck_info['commander_1'] = commanders[0]
                if len(commanders) > 1:
                    deck_info['commander_2'] = commanders[1]
            
            # Estimate color identity (basic implementation)
            colors = self._estimate_color_identity(cards)
            deck_info.update(colors)
            
            return deck_info
            
        except Exception as e:
            logger.error(f"Error scraping Deckstats deck: {e}")
            raise
    
    def _scrape_archidekt(self, deck_id: str, original_url: str) -> Dict:
        """Scrape a deck from Archidekt following mtg patterns."""
        api_url = f"https://archidekt.com/api/decks/{deck_id}/"
        
        try:
            response = requests.get(api_url, headers=self.headers, timeout=30)
            response.raise_for_status()
            data = response.json()
            
            deck_info = {
                'deck_id': deck_id,  # Use original ID
                'deck_name': data.get('name', 'Unknown Deck'),
                'deck_url': original_url,
                'commander_1': None,
                'commander_2': None,
                'source': 'archidekt',
                'format': 'commander',
                'cards': [],
                'raw_data': data
            }
            
            # Extract cards and commanders
            cards = data.get('cards', [])
            commanders = []
            
            for card_data in cards:
                # Get card name from nested structure
                oracle_card = card_data.get('card', {}).get('oracleCard', {})
                card_name = oracle_card.get('name')
                
                if not card_name:
                    continue
                
                card_info = {
                    'name': card_name,
                    'quantity': card_data.get('quantity', 1),
                    'type': 'main'
                }
                
                # Check if it's a commander
                categories = card_data.get('categories', [])
                if any('commander' in str(cat).lower() for cat in categories):
                    commanders.append(card_name)
                    card_info['type'] = 'commander'
                
                deck_info['cards'].append(card_info)
            
            # Set commanders
            if commanders:
                deck_info['commander_1'] = commanders[0]
                if len(commanders) > 1:
                    deck_info['commander_2'] = commanders[1]
            
            # Extract color identity from the deck data
            colors = data.get('colorIdentity', [])
            deck_info.update({
                'is_W': 'W' in colors,
                'is_U': 'U' in colors,
                'is_B': 'B' in colors,
                'is_R': 'R' in colors,
                'is_G': 'G' in colors
            })
            
            return deck_info
            
        except Exception as e:
            logger.error(f"Error scraping Archidekt deck {deck_id}: {e}")
            raise
    
    def _extract_deckstats_name(self, html: str) -> str:
        """Extract deck name from Deckstats HTML."""
        # Look for the deck title in the HTML
        patterns = [
            r'<title>([^<]+) - Deckstats</title>',
            r'<h1[^>]*>([^<]+)</h1>',
            r'<meta property="og:title" content="([^"]+)"'
        ]
        
        for pattern in patterns:
            match = re.search(pattern, html, re.IGNORECASE)
            if match:
                return match.group(1).strip()
        
        return "Unknown Deck"
    
    def _extract_deckstats_cards(self, html: str) -> List[Dict]:
        """Extract card list from Deckstats HTML."""
        cards = []
        
        # Multiple patterns to catch different HTML structures
        patterns = [
            r'data-card-name="([^"]+)"[^>]*>.*?(\d+)x?\s*',
            r'<span[^>]*class="[^"]*card[^"]*"[^>]*>([^<]+)</span>[^0-9]*(\d+)',
            r'(\d+)\s*x?\s*([^<\n]+)(?=\n|\r|<|$)'
        ]
        
        for pattern in patterns:
            matches = re.findall(pattern, html, re.DOTALL | re.IGNORECASE)
            for match in matches:
                if len(match) == 2:
                    # Handle different ordering
                    if match[0].isdigit():
                        quantity, card_name = match
                    else:
                        card_name, quantity = match
                    
                    card_name = card_name.strip()
                    if card_name and len(card_name) > 2:  # Filter out noise
                        cards.append({
                            'name': card_name,
                            'quantity': int(quantity) if quantity.isdigit() else 1,
                            'type': 'main'
                        })
        
        return cards
    
    def _extract_deckstats_commanders(self, html: str, cards: List[Dict]) -> List[str]:
        """Extract commanders from Deckstats HTML."""
        commanders = []
        
        # Look for commander section in HTML
        commander_patterns = [
            r'<h3[^>]*>.*?commander.*?</h3>[^<]*<[^>]*>([^<]+)',
            r'<div[^>]*class="[^"]*commander[^"]*"[^>]*>([^<]+)',
            r'Commander:?\s*([^<\n]+)'
        ]
        
        for pattern in commander_patterns:
            matches = re.findall(pattern, html, re.IGNORECASE | re.DOTALL)
            for match in matches:
                commander_name = match.strip()
                if commander_name and len(commander_name) > 2:
                    commanders.append(commander_name)
        
        # If no commanders found in HTML, try to guess from legendary creatures
        if not commanders:
            # This is a simplified heuristic - in practice you'd need card database
            for card in cards[:5]:  # Check first few cards
                card_name = card['name']
                if any(word in card_name.lower() for word in ['legendary', 'general']):
                    commanders.append(card_name)
        
        return commanders
    
    def _estimate_color_identity(self, cards: List[Dict]) -> Dict[str, bool]:
        """Estimate color identity based on card names (basic implementation)."""
        # This is a simplified version - in practice, you'd need a card database
        # For now, return all colors as possible
        return {
            'is_W': False,
            'is_U': False,
            'is_B': False,
            'is_R': False,
            'is_G': False
        }
    
    def convert_to_forge_format(self, deck_data: Dict) -> str:
        """Convert scraped deck data to Forge .dck format."""
        lines = []
        
        # Metadata
        lines.append("[metadata]")
        lines.append(f"Name={deck_data['deck_name']}")
        lines.append("")
        
        # Commander section
        commanders = []
        if deck_data.get('commander_1'):
            commanders.append(f"1 {deck_data['commander_1']}")
        if deck_data.get('commander_2'):
            commanders.append(f"1 {deck_data['commander_2']}")
        
        if commanders:
            lines.append("[Commander]")
            lines.extend(commanders)
            lines.append("")
        
        # Main deck - only include non-commander cards
        main_cards = [card for card in deck_data.get('cards', []) 
                     if card.get('type') == 'main']
        
        if main_cards:
            lines.append("[Main]")
            for card in main_cards:
                # Ensure we have valid quantity and name
                quantity = card.get('quantity', 1)
                name = card.get('name', '').strip()
                if name:
                    lines.append(f"{quantity} {name}")
            lines.append("")
        
        return "\n".join(lines)
    
    def upload_to_bigquery(self, deck_data: Dict) -> bool:
        """Upload deck data to BigQuery using pandas_gbq with proper authentication."""
        try:
            import pandas as pd
            import pandas_gbq
            from google.oauth2 import service_account
            import google.auth
            
            # Get project ID
            project_id = os.environ.get('GOOGLE_CLOUD_PROJECT', 'elated-liberty-100303')
            credentials = None
            
            # Try service account first
            credentials_path = os.environ.get('BIGQUERY_CREDENTIALS_PATH')
            if credentials_path and os.path.exists(credentials_path):
                try:
                    logger.info(f"Using service account credentials from: {credentials_path}")
                    credentials = service_account.Credentials.from_service_account_file(
                        credentials_path,
                        scopes=['https://www.googleapis.com/auth/bigquery']
                    )
                except Exception as e:
                    logger.warning(f"Failed to load service account credentials: {e}")
            
            # Fall back to ADC
            if credentials is None:
                try:
                    logger.info("Using Application Default Credentials for BigQuery upload")
                    credentials, _ = google.auth.default(
                        scopes=['https://www.googleapis.com/auth/bigquery']
                    )
                except Exception as e:
                    logger.error(f"Failed to get ADC credentials: {e}")
                    raise Exception("No valid BigQuery credentials found")
            
            prefixed_deck_id = f"{deck_data['source']}-{deck_data['deck_id']}"
            
            # Format deck data for BigQuery following existing mtg.decks schema
            deck_record = {
                'deck_id': prefixed_deck_id,
                'deck_name': deck_data['deck_name'],
                'deck_url': deck_data['deck_url'],
                'commander_1': deck_data.get('commander_1'),
                'commander_2': deck_data.get('commander_2'),
                'is_W': deck_data.get('is_W', False),
                'is_U': deck_data.get('is_U', False),
                'is_B': deck_data.get('is_B', False),
                'is_R': deck_data.get('is_R', False),
                'is_G': deck_data.get('is_G', False),
                'source': deck_data['source']
            }
            
            # Create DataFrame for deck metadata
            deck_df = pd.DataFrame([deck_record])
            
            # Upload deck metadata to mtg.decks
            pandas_gbq.to_gbq(
                deck_df,
                "mtg.decks",
                project_id=project_id,
                credentials=credentials,
                if_exists='append',
                table_schema=None
            )
            
            logger.info(f"Successfully uploaded deck {prefixed_deck_id} to mtg.decks")
            
            # Format and upload card data to mtg.deck_cards
            cards = deck_data.get('cards', [])
            if cards:
                card_records = []
                for card in cards:
                    card_record = {
                        'deck_id': prefixed_deck_id,
                        'name': card.get('name'),
                        'amount': card.get('quantity', 1),
                        'commander': False
                    }
                    card_records.append(card_record)
                
                # Create DataFrame for cards
                if cmd1 := deck_data.get('commander_1'):
                    card_records.append({
                        'deck_id': prefixed_deck_id,
                        'name': cmd1,
                        'amount': 1,
                        'commander': True
                    })
                if cmd2 := deck_data.get('commander_2'):
                    card_records.append({
                        'deck_id': prefixed_deck_id,
                        'name': cmd2,
                        'amount': 1,
                        'commander': True
                    })

                cards_df = pd.DataFrame(card_records)
                
                # Upload cards to mtg.deck_cards
                pandas_gbq.to_gbq(
                    cards_df,
                    "mtg.deck_cards",
                    project_id=project_id,
                    credentials=credentials,
                    if_exists='append',
                    table_schema=None
                )
                
                logger.info(f"Successfully uploaded {len(card_records)} cards for deck {prefixed_deck_id} to mtg.deck_cards")
            
            return True
            
        except Exception as e:
            logger.error(f"Error uploading deck to BigQuery: {e}")
            import traceback
            traceback.print_exc()
            return False
    
    def scrape_and_upload(self, url: str) -> Dict:
        """Scrape a deck and upload it to BigQuery."""
        result = {
            'success': False,
            'deck_id': None,
            'deck_name': None,
            'error': None,
            'forge_content': None
        }
        
        try:
            # Scrape the deck
            logger.info(f"Scraping deck from {url}")
            deck_data = self.scrape_deck(url)
            
            # Add a small delay to avoid rate limiting
            time.sleep(random.uniform(0.5, 2.0))
            
            # Convert to Forge format
            forge_content = self.convert_to_forge_format(deck_data)
            
            # Upload to BigQuery
            upload_success = self.upload_to_bigquery(deck_data)
            
            # Use the prefixed deck_id for result
            final_deck_id = f"{deck_data['source']}-{deck_data['deck_id']}"
            
            result.update({
                'success': upload_success,
                'deck_id': final_deck_id,
                'deck_name': deck_data['deck_name'],
                'forge_content': forge_content
            })
            
            if not upload_success:
                result['error'] = 'Failed to upload to BigQuery'
            
        except Exception as e:
            result['error'] = str(e)
            logger.error(f"Error in scrape_and_upload: {e}")
            import traceback
            traceback.print_exc()
        
        return result
    
    def check_deck_exists(self, deck_id: str, source: str) -> bool:
        """Check if a deck already exists in BigQuery."""
        try:
            from .bigquery_client import BigQueryClient
            
            bq_client = BigQueryClient()
            prefixed_id = f"{source}-{deck_id}"
            
            return bq_client.deck_exists(prefixed_id)
            
        except Exception as e:
            logger.warning(f"Could not check if deck exists: {e}")
            return False