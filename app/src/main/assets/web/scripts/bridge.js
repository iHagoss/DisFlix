/**
 * Stremio Core Web Bridge
 * Initializes the WASM module and provides JavaScript API for Android WebView
 */

(function() {
    'use strict';

    // Global Stremio Core instance
    window.StremioCoreWeb = null;
    window.StremioCoreLoaded = false;
    window.StremioCoreError = null;

    // Event emitter for core events
    const eventListeners = {
        'core:loaded': [],
        'core:error': [],
        'core:event': []
    };

    // Initialize WASM module
    async function initWASM() {
        try {
            console.log('[Bridge] Initializing Stremio Core WASM...');

            // Load the WASM module
            const wasmPath = './binaries/stremio_core_web_bg.wasm';

            // Import the JavaScript bindings
            const module = await import('./main.js');

            // Initialize the WASM
            await module.default(wasmPath);

            console.log('[Bridge] WASM module loaded successfully');

            // Store the module globally
            window.StremioCoreWeb = module;
            window.StremioCoreLoaded = true;

            // Emit loaded event
            emit('core:loaded', { module });

            return module;
        } catch (error) {
            console.error('[Bridge] Failed to initialize WASM:', error);
            window.StremioCoreError = error;
            emit('core:error', { error: error.message });
            throw error;
        }
    }

    // Event emitter functions
    function on(event, callback) {
        if (!eventListeners[event]) {
            eventListeners[event] = [];
        }
        eventListeners[event].push(callback);
    }

    function off(event, callback) {
        if (!eventListeners[event]) return;
        const index = eventListeners[event].indexOf(callback);
        if (index > -1) {
            eventListeners[event].splice(index, 1);
        }
    }

    function emit(event, data) {
        if (!eventListeners[event]) return;
        eventListeners[event].forEach(callback => {
            try {
                callback(data);
            } catch (error) {
                console.error('[Bridge] Event listener error:', error);
            }
        });
    }

    // API for Android WebView
    window.StremioBridge = {
        // Initialize the core
        // init: initWASM, // This is now exposed globally

        // Check if core is ready
        isReady: () => window.StremioCoreLoaded,

        // Get core instance
        getCore: () => window.StremioCoreWeb,

        // Event handling
        on: on,
        off: off,
        emit: emit,

        // Send action to core
        sendAction: function(action) {
            if (!window.StremioCoreLoaded) {
                console.error('[Bridge] Core not loaded yet');
                return null;
            }

            try {
                console.log('[Bridge] Sending action:', action);
                // This will be implemented based on the actual WASM API
                return window.StremioCoreWeb.dispatch(action);
            } catch (error) {
                console.error('[Bridge] Action dispatch error:', error);
                return null;
            }
        },

        // Get current state
        getState: function() {
            if (!window.StremioCoreLoaded) {
                console.error('[Bridge] Core not loaded yet');
                return null;
            }

            try {
                return window.StremioCoreWeb.getState();
            } catch (error) {
                console.error('[Bridge] Get state error:', error);
                return null;
            }
        },

        // Android interface for calling from Kotlin
        androidInterface: {
            // Called by Android when navigation happens
            onNavigate: function(path) {
                console.log('[Bridge] Android navigate:', path);
                emit('core:event', { type: 'navigate', path });
            },

            // Called by Android to get current route
            getCurrentRoute: function() {
                if (window.location) {
                    return window.location.hash || '/';
                }
                return '/';
            },

            // Called by Android to dispatch actions
            dispatch: function(actionJson) {
                try {
                    const action = JSON.parse(actionJson);
                    return JSON.stringify(window.StremioBridge.sendAction(action));
                } catch (error) {
                    console.error('[Bridge] Dispatch error:', error);
                    return JSON.stringify({ error: error.message });
                }
            }
        }
    };

    // Auto-initialize on DOM load
    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', function() {
            console.log('[Bridge] DOM loaded, waiting for init call...');
            // Don't auto-init, wait for Android to call it
        });
    } else {
        console.log('[Bridge] DOM already loaded, ready for init');
    }

    // Expose initialization function globally
    window.StremioBridge.init = initWASM;

    // Expose Android integration helpers
    window.StremioBridge.android = {
        isAvailable: function() {
            return typeof window.Android !== 'undefined';
        },

        openPlayer: function(streamUrl, metaId, videoId) {
            if (window.Android && window.Android.openPlayer) {
                window.Android.openPlayer(streamUrl, metaId || '', videoId || '');
                return true;
            }
            return false;
        },

        openExternalPlayer: function(streamUrl) {
            if (window.Android && window.Android.openExternalPlayer) {
                window.Android.openExternalPlayer(streamUrl);
                return true;
            }
            return false;
        },

        shareUrl: function(url) {
            if (window.Android && window.Android.shareUrl) {
                window.Android.shareUrl(url);
                return true;
            }
            return false;
        },

        copyToClipboard: function(text) {
            if (window.Android && window.Android.copyToClipboard) {
                window.Android.copyToClipboard(text);
                return true;
            }
            return false;
        },

        openUrl: function(url) {
            if (window.Android && window.Android.openUrl) {
                window.Android.openUrl(url);
                return true;
            }
            return false;
        },

        getDeviceInfo: function() {
            if (window.Android && window.Android.getDeviceInfo) {
                try {
                    return JSON.parse(window.Android.getDeviceInfo());
                } catch (e) {
                    console.error('[Bridge] Failed to parse device info:', e);
                }
            }
            return null;
        },

        setFullscreen: function(enabled) {
            if (window.Android && window.Android.setFullscreen) {
                window.Android.setFullscreen(enabled);
                return true;
            }
            return false;
        },

        vibrate: function(durationMs) {
            if (window.Android && window.Android.vibrate) {
                window.Android.vibrate(durationMs || 50);
                return true;
            }
            return false;
        }
    };

    // Notify Android that bridge is ready
    if (window.Android) {
        if (window.Android.log) {
            window.Android.log('[Bridge] Stremio Bridge initialized and ready');
        }
        // Notify MainActivity that the bridge is ready for communication
        if (window.Android.onBridgeReady) {
            window.Android.onBridgeReady();
        }
    }

    console.log('[Bridge] Stremio Bridge initialized');
    console.log('[Bridge] Android interface available:', window.StremioBridge.android.isAvailable());
})();