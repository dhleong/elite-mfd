'use strict';
/* global angular, _ */

/*
 * Constants
 */
var WEBSOCKET_PORT = 9877;
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
        close: function(){}
    };

    ws._settings = _.extend(websocketSettings, s);
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
        ws['on' + eventName] = handler;
    };

    /** Attach a global packet handler */
    ws.registerGlobal = function(eventName, handler) {
        ws._settings.global_events[eventName] = handler;
    };

    /** NB: Completely overrides any previously-registered locals! */
    ws.registerLocal = function($scope, events) {
        ws._settings.$scope = $scope;
        ws._settings.current_events = events;
        $scope.$on('$destroy', function() {
            if (ws._settings.$scope === $scope) {
                ws._settings.$scope = null;
                ws._settings.current_events = {};
            }
        });
    };

    // TODO ?
    // $(window).unload(function(){ ws.close(); ws = null });
    return ws;
}


angular.module('emfd')
.provider('websocket', function() {
    console.log("create socket");

    this.socket = websocket("ws://localhost:" + WEBSOCKET_PORT, {
        open: function() {
            console.log("Connected");
        }
      , close: function() {
            console.log("Disconnected");
        }
    });

    var self = this;
    this.$get = function() {
        console.log("Get socket");
        return self.socket;
    };
});
