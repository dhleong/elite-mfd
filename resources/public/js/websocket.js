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
var websocket_id = 0;

function websocket(url, s) {
    var ws = WebSocket ? new WebSocket( url ) : {
        send: function(/*m*/){ return false },
        close: function() {
        }
    };
    ws.id = websocket_id++;

    ws._settings = _.extend(websocketSettings, s);
    ws.onopen = function() {
        if (this._settings.open)
            this._settings.open.call(this);
    };
    ws.onclose = function() {
        if (this._settings.close)
            this._settings.close.call(this);
    };
    ws.onmessage = function(e){
        var self = this;
        var m = JSON.parse(e.data);
        // make pretty clojure keywords into
        //  pretty-ish javascript (so we don't have to quote)
        var t = m.type.replace('-', '_'); 
        var h = self._settings.current_events && self._settings.current_events[t];
        if (h) {
            self._settings.$scope.$apply(function() {
                h.call(self, m);
            });
            e.preventDefault();
        } 
        h = self._settings.global_events[t];
        if (h) {
            h.call(self, m);
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
        this._settings[eventName] = handler;
    };

    /** Attach a global packet handler */
    ws.registerGlobal = function(eventName, handler) {
        this._settings.global_events[eventName] = handler;
    };

    /**
     * NB: Completely overrides any previously-registered locals!
     * @param $scope The scope of the current view. Event
     *  callbacks will be fired within $scope.$apply() so
     *  updates to bound variables will be reflected as expected
     */
    ws.registerLocal = function($scope, events) {
        var settings = this._settings;
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

function makeReconnectable(provider, socket, url) {
    // save some original values
    socket._onclose = socket.onclose;
    var originalSettings = socket._settings;

    socket.onclose = function() {
        // call through, bound to newest socket
        this._onclose.call(provider.socket);

        console.log("Lost connection!");
        var delay = 500;
        var reconnector = function() {
            console.log("Reconnecting...", socket.id);

            websocket(url, {
                open: function() {
                    console.log("Reconnected!", this.id);
                    delay = 500; // reset

                    // out with the old, in with the new...
                    this._settings.open = null;
                    this._settings.close = null;
                    makeReconnectable(provider, this, url);

                    // forward send() from the old instance
                    //  to the new instance
                    provider.socket.send = _.bind(this.send, this);
                    provider.socket.close = _.bind(this.close, this);
                    provider.socket = this;

                    // copy over the old settings (since they have
                    //  scopes and stuff) but keep our close listener
                    //  (it was prepared by makeReconnectable)
                    var newClose = this._settings.close;
                    this._settings = originalSettings;
                    this._settings.close = newClose;
                },
                close: function() {
                    delay = Math.min(MAX_DELAY, delay * 2);
                    console.log("Failed, retrying after", delay, "on", this.id);
                    setTimeout(reconnector, delay);
                }
            });
        }

        // go!
        reconnector();
    }
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
    makeReconnectable(this, this.socket, url);

    this.$get = function() {
        // build a proxy for the socket whose method calls
        //  always refer to the current instance of the socket
        return _.reduce(['close', 'send', 'registerGlobal',
                         'registerStatus', 'registerLocal'],
        function(obj, funcName) {
            obj[funcName] = function() {
                self.socket[funcName].apply(self.socket, arguments);
            }
            return obj;
        }, {});
    };
});
