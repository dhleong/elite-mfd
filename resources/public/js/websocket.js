'use strict';
/* global angular, _ */

/*
 * Constants
 */
var WEBSOCKET_PORT = 9877;
var MAX_DELAY = 7000;
function Dummy() {}

/*
 * Websocket wrapper
 */

var websocketSettings = {
    message: Dummy,
    options: {},
    global_events: {},
    current_events: {},
};

function websocket(url, s) {
    var ws = WebSocket ? new WebSocket( url ) : {
        send: function(/*m*/){ return false },
        close: function() {
        }
    };

    ws._settings = _.extend(websocketSettings, s);
    ws.onopen = function() {
        if (ws._settings.open)
            ws._settings.open.call(this);
    };
    ws.onclose = function() {
        if (ws._settings.close)
            ws._settings.close.call(this);
    };
    ws.onmessage = function(e){
        var m = JSON.parse(e.data);
        // make pretty clojure keywords into
        //  pretty-ish javascript (so we don't have to quote)
        var t = m.type.replace('-', '_'); 
        var h = ws._settings.current_events && ws._settings.current_events[t];
        if (h) {
            ws._settings.$scope.$apply(function() {
                h.call(ws, m);
            });
            e.preventDefault();
        } 
        h = ws._settings.global_events[t];
        if (h) {
            h.call(ws, m);
            e.preventDefault();
        }
    };

    ws._send = ws.send;
    ws.send = function(packet) {
        // var m = (data || typeof(type) == 'string')
        // ? {type:type, data:data}
        // : type; // no "data"; just send the raw JSON
        // m = _.extend(true, m, _.extend(true, {}, ws._settings.options, m));
        // if (data) m.data = data;
        return this._send(JSON.stringify(packet));
    }

    /** 'open' or 'close' */
    ws.registerStatus = function(eventName, handler) {
        // I guess
        ws._settings[eventName] = handler;
    };

    /** Attach a global packet handler */
    ws.registerGlobal = function(eventName, handler) {
        ws._settings.global_events[eventName] = handler;
    };

    /**
     * NB: Completely overrides any previously-registered locals!
     * @param $scope The scope of the current view. Event
     *  callbacks will be fired within $scope.$apply() so
     *  updates to bound variables will be reflected as expected
     */
    ws.registerLocal = function($scope, events) {
        var settings = ws._settings;
        settings.$scope = $scope;
        settings.current_events = events;
        $scope.$on('$destroy', function() {
            if (settings.$scope === $scope) {
                settings.$scope = null;
                settings.current_events = {};
            }
        });
    };

    // TODO ?
    // $(window).unload(function(){ ws.close(); ws = null });
    return ws;
}


angular.module('emfd')
.provider('websocket', function() {

    var host = window.location.host;
    var portStart = host.lastIndexOf(':');
    if (~portStart) {
        host = host.substring(0, portStart);
    }
    var url = "ws://" + host + ":" + WEBSOCKET_PORT
    this.socket = websocket(url, {
        open: function() {
            console.log("Connected");
        }
    });

    // hax for auto-reconnect
    var self = this;
    var originalOnClose = this.socket.onclose;
    var originalSettings = this.socket._settings;
    this.socket.onclose = function() {
        // call through
        originalOnClose.call(self.socket);

        console.log("Lost connection!");
        var delay = 500;
        var reconnector = function() {
            console.log("Reconnecting...");

            websocket(url, {
                open: function() {
                    console.log("Reconnected!", this);
                    // forward send() from the old instance
                    //  to the new instance
                    self.socket.send = _.bind(this.send, this);
                    self.socket = this;
                    self._settings = originalSettings;
                },
                close: function() {
                    delay = Math.min(MAX_DELAY, delay * 2);
                    console.log("Failed, retrying after", delay);
                    setTimeout(reconnector, delay);
                }
            });
        }

        // go!
        reconnector();
    }

    this.$get = function() {
        return self.socket;
    };
});
