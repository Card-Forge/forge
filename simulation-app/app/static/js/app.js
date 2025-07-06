// MTG Simulation App JavaScript

// Global app object
window.MTGSimApp = {
    // Configuration
    config: {
        apiBaseUrl: '/api',
        refreshInterval: 5000 // 5 seconds
    },
    
    // Utility functions
    utils: {
        formatDuration: function(seconds) {
            if (!seconds) return 'N/A';
            const hours = Math.floor(seconds / 3600);
            const minutes = Math.floor((seconds % 3600) / 60);
            const secs = seconds % 60;
            
            if (hours > 0) {
                return `${hours}h ${minutes}m ${secs}s`;
            } else if (minutes > 0) {
                return `${minutes}m ${secs}s`;
            } else {
                return `${secs}s`;
            }
        },
        
        formatNumber: function(num) {
            return new Intl.NumberFormat().format(num);
        },
        
        showError: function(message) {
            const alertDiv = document.createElement('div');
            alertDiv.className = 'alert alert-danger alert-dismissible fade show';
            alertDiv.innerHTML = `
                ${message}
                <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
            `;
            
            const container = document.querySelector('.container');
            container.insertBefore(alertDiv, container.firstChild);
            
            // Auto-dismiss after 5 seconds
            setTimeout(() => {
                const alert = bootstrap.Alert.getOrCreateInstance(alertDiv);
                alert.close();
            }, 5000);
        },
        
        showSuccess: function(message) {
            const alertDiv = document.createElement('div');
            alertDiv.className = 'alert alert-success alert-dismissible fade show';
            alertDiv.innerHTML = `
                ${message}
                <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
            `;
            
            const container = document.querySelector('.container');
            container.insertBefore(alertDiv, container.firstChild);
            
            // Auto-dismiss after 5 seconds
            setTimeout(() => {
                const alert = bootstrap.Alert.getOrCreateInstance(alertDiv);
                alert.close();
            }, 5000);
        }
    },
    
    // Monitoring functions
    monitoring: {
        intervals: {},
        
        startSimulationMonitoring: function(simulationId) {
            if (this.intervals[simulationId]) {
                clearInterval(this.intervals[simulationId]);
            }
            
            this.intervals[simulationId] = setInterval(() => {
                this.updateSimulationStatus(simulationId);
            }, MTGSimApp.config.refreshInterval);
            
            // Initial update
            this.updateSimulationStatus(simulationId);
        },
        
        stopSimulationMonitoring: function(simulationId) {
            if (this.intervals[simulationId]) {
                clearInterval(this.intervals[simulationId]);
                delete this.intervals[simulationId];
            }
        },
        
        updateSimulationStatus: function(simulationId) {
            fetch(`/monitoring/api/simulation/${simulationId}/status`)
                .then(response => response.json())
                .then(data => {
                    this.updateStatusDisplay(data);
                })
                .catch(error => {
                    console.error('Error updating simulation status:', error);
                });
        },
        
        updateStatusDisplay: function(data) {
            // Update progress bar
            const progressBar = document.getElementById('progress-bar');
            if (progressBar) {
                progressBar.style.width = `${data.progress_percent}%`;
                progressBar.textContent = `${data.progress_percent.toFixed(1)}%`;
                progressBar.setAttribute('aria-valuenow', data.progress_percent);
            }
            
            // Update status badge
            const statusBadge = document.getElementById('status-badge');
            if (statusBadge) {
                statusBadge.textContent = data.status.charAt(0).toUpperCase() + data.status.slice(1);
                statusBadge.className = `badge ${this.getStatusBadgeClass(data.status)}`;
            }
            
            // Update games completed
            const gamesCompleted = document.getElementById('games-completed');
            if (gamesCompleted) {
                gamesCompleted.textContent = `${data.games_completed} / ${data.total_games}`;
            }
            
            // Update estimated completion
            const estimatedCompletion = document.getElementById('estimated-completion');
            if (estimatedCompletion && data.estimated_completion) {
                estimatedCompletion.textContent = MTGSimApp.utils.formatDuration(data.estimated_completion);
            }
            
            // Update error display
            const errorContainer = document.getElementById('error-container');
            if (errorContainer && data.errors && data.errors.length > 0) {
                errorContainer.innerHTML = `
                    <div class="alert alert-warning">
                        <h6>Errors:</h6>
                        <ul class="mb-0">
                            ${data.errors.map(error => `<li>${error}</li>`).join('')}
                        </ul>
                    </div>
                `;
            }
            
            // Stop monitoring if simulation is complete
            if (data.status === 'completed' || data.status === 'failed' || data.status === 'stopped') {
                this.stopSimulationMonitoring(data.id);
                
                // Show completion message
                if (data.status === 'completed') {
                    MTGSimApp.utils.showSuccess('Simulation completed successfully!');
                }
            }
        },
        
        getStatusBadgeClass: function(status) {
            switch (status) {
                case 'running': return 'bg-primary';
                case 'completed': return 'bg-success';
                case 'failed': return 'bg-danger';
                case 'stopped': return 'bg-warning';
                default: return 'bg-secondary';
            }
        }
    },
    
    // Statistics functions
    statistics: {
        charts: {},
        
        initializeCharts: function(simulationId) {
            fetch(`/statistics/api/simulation/${simulationId}/charts`)
                .then(response => response.json())
                .then(data => {
                    this.createWinRateChart(data.win_rates);
                    this.createDurationChart(data.game_durations);
                })
                .catch(error => {
                    console.error('Error loading chart data:', error);
                });
        },
        
        createWinRateChart: function(data) {
            const ctx = document.getElementById('win-rate-chart');
            if (!ctx) return;
            
            this.charts.winRate = new Chart(ctx, {
                type: 'pie',
                data: {
                    labels: data.labels,
                    datasets: [{
                        data: data.data,
                        backgroundColor: [
                            '#FF6384',
                            '#36A2EB',
                            '#FFCE56',
                            '#4BC0C0'
                        ]
                    }]
                },
                options: {
                    responsive: true,
                    maintainAspectRatio: false,
                    plugins: {
                        title: {
                            display: true,
                            text: 'Win Rate by Deck'
                        },
                        legend: {
                            position: 'bottom'
                        }
                    }
                }
            });
        },
        
        createDurationChart: function(data) {
            const ctx = document.getElementById('duration-chart');
            if (!ctx) return;
            
            this.charts.duration = new Chart(ctx, {
                type: 'line',
                data: {
                    labels: data.labels,
                    datasets: [{
                        label: 'Game Duration (seconds)',
                        data: data.data,
                        borderColor: '#36A2EB',
                        backgroundColor: 'rgba(54, 162, 235, 0.1)',
                        fill: true
                    }]
                },
                options: {
                    responsive: true,
                    maintainAspectRatio: false,
                    plugins: {
                        title: {
                            display: true,
                            text: 'Game Duration Over Time'
                        }
                    },
                    scales: {
                        y: {
                            beginAtZero: true,
                            title: {
                                display: true,
                                text: 'Duration (seconds)'
                            }
                        }
                    }
                }
            });
        }
    },
    
    // Table sorting functionality
    tables: {
        initializeSortable: function() {
            const sortableTables = document.querySelectorAll('.sortable-table');
            sortableTables.forEach(table => {
                this.makeSortable(table);
            });
        },
        
        makeSortable: function(table) {
            const headers = table.querySelectorAll('th[data-sortable]');
            headers.forEach((header, index) => {
                header.style.cursor = 'pointer';
                header.addEventListener('click', () => {
                    this.sortTable(table, index, header.dataset.sortable);
                });
                
                // Add sort indicator
                header.innerHTML += ' <span class="sort-indicator"></span>';
            });
        },
        
        sortTable: function(table, columnIndex, dataType) {
            const tbody = table.querySelector('tbody');
            const rows = Array.from(tbody.querySelectorAll('tr'));
            const header = table.querySelectorAll('th')[columnIndex];
            const sortIndicator = header.querySelector('.sort-indicator');
            
            // Determine sort direction
            const currentDirection = header.dataset.sortDirection || 'asc';
            const newDirection = currentDirection === 'asc' ? 'desc' : 'asc';
            header.dataset.sortDirection = newDirection;
            
            // Clear all other sort indicators
            table.querySelectorAll('th .sort-indicator').forEach(indicator => {
                indicator.innerHTML = '';
            });
            
            // Set current sort indicator
            sortIndicator.innerHTML = newDirection === 'asc' ? '▲' : '▼';
            
            // Sort rows
            rows.sort((a, b) => {
                const aValue = this.getCellValue(a, columnIndex, dataType);
                const bValue = this.getCellValue(b, columnIndex, dataType);
                
                let comparison = 0;
                
                switch (dataType) {
                    case 'number':
                        comparison = parseFloat(aValue) - parseFloat(bValue);
                        break;
                    case 'date':
                        comparison = new Date(aValue) - new Date(bValue);
                        break;
                    case 'percentage':
                        const aPercent = parseFloat(aValue.replace('%', ''));
                        const bPercent = parseFloat(bValue.replace('%', ''));
                        comparison = aPercent - bPercent;
                        break;
                    case 'duration':
                        comparison = this.parseDuration(aValue) - this.parseDuration(bValue);
                        break;
                    default: // text
                        comparison = aValue.localeCompare(bValue);
                }
                
                return newDirection === 'asc' ? comparison : -comparison;
            });
            
            // Reorder rows in table
            rows.forEach(row => tbody.appendChild(row));
        },
        
        getCellValue: function(row, columnIndex, dataType) {
            const cell = row.cells[columnIndex];
            let value = cell.textContent.trim();
            
            // Handle special cases
            if (dataType === 'percentage') {
                // Extract percentage value from progress bars or text
                const progressBar = cell.querySelector('.progress-bar');
                if (progressBar) {
                    value = progressBar.style.width || '0%';
                }
            }
            
            return value;
        },
        
        parseDuration: function(duration) {
            // Parse duration strings like "1h 30m", "45m", "120s"
            let seconds = 0;
            const hoursMatch = duration.match(/(\d+)h/);
            const minutesMatch = duration.match(/(\d+)m/);
            const secondsMatch = duration.match(/(\d+)s/);
            
            if (hoursMatch) seconds += parseInt(hoursMatch[1]) * 3600;
            if (minutesMatch) seconds += parseInt(minutesMatch[1]) * 60;
            if (secondsMatch) seconds += parseInt(secondsMatch[1]);
            
            return seconds;
        }
    },

    // Deck browser functions
    decks: {
        searchTimeout: null,
        
        initializeSearch: function() {
            const searchInput = document.getElementById('deck-search');
            if (searchInput) {
                searchInput.addEventListener('input', (e) => {
                    clearTimeout(this.searchTimeout);
                    this.searchTimeout = setTimeout(() => {
                        this.performSearch(e.target.value);
                    }, 300);
                });
            }
        },
        
        performSearch: function(query) {
            if (query.length < 2) {
                this.displayMessage('Enter at least 2 characters to search');
                return;
            }
            
            this.showLoading();
            
            fetch(`/decks/api/search?q=${encodeURIComponent(query)}`)
                .then(response => response.json())
                .then(data => {
                    this.displayResults(data.decks);
                })
                .catch(error => {
                    this.displayMessage('Error searching decks');
                    console.error('Search error:', error);
                });
        },
        
        showLoading: function() {
            const container = document.getElementById('deck-results');
            if (container) {
                container.innerHTML = '<div class="text-center"><i class="fas fa-spinner fa-spin"></i> Searching...</div>';
            }
        },
        
        displayResults: function(decks) {
            const container = document.getElementById('deck-results');
            if (!container) return;
            
            if (decks.length === 0) {
                this.displayMessage('No decks found');
                return;
            }
            
            let html = '<div class="row">';
            decks.forEach(deck => {
                html += `
                    <div class="col-md-6 col-lg-4 mb-3">
                        <div class="card deck-card">
                            <div class="card-body">
                                <h6 class="card-title">${deck.deck_name}</h6>
                                <p class="card-text">
                                    <strong>Commander:</strong> ${deck.commander || 'Unknown'}<br>
                                    <strong>Colors:</strong> ${deck.color_identity || 'Unknown'}
                                </p>
                                <button class="btn btn-sm btn-primary" onclick="MTGSimApp.decks.selectDeck('${deck.deck_name}')">
                                    Select Deck
                                </button>
                            </div>
                        </div>
                    </div>
                `;
            });
            html += '</div>';
            
            container.innerHTML = html;
        },
        
        displayMessage: function(message) {
            const container = document.getElementById('deck-results');
            if (container) {
                container.innerHTML = `<div class="text-center text-muted">${message}</div>`;
            }
        },
        
        selectDeck: function(deckId) {
            // This would be customized based on the context where deck selection is used
            console.log('Deck selected:', deckId);
        }
    }
};

// Initialize app when DOM is loaded
document.addEventListener('DOMContentLoaded', function() {
    // Initialize deck search if on deck browser page
    MTGSimApp.decks.initializeSearch();
    
    // Initialize sortable tables
    MTGSimApp.tables.initializeSortable();
    
    // Auto-start monitoring if on simulation detail page
    const simulationId = document.querySelector('[data-simulation-id]')?.dataset.simulationId;
    if (simulationId) {
        MTGSimApp.monitoring.startSimulationMonitoring(simulationId);
    }
    
    // Auto-initialize charts if on statistics page
    if (simulationId && document.getElementById('win-rate-chart')) {
        MTGSimApp.statistics.initializeCharts(simulationId);
    }
});

// Clean up intervals when page unloads
window.addEventListener('beforeunload', function() {
    Object.keys(MTGSimApp.monitoring.intervals).forEach(simulationId => {
        MTGSimApp.monitoring.stopSimulationMonitoring(simulationId);
    });
});