'use strict';
/* global angular, _ */

angular.module('emfd')

.provider('currentSystem', ['websocketProvider', function(wsProvider) {
    // NB not sure why we can't just request 'websocket'...
    var $websocket = wsProvider.$get();

    this._watchers = [];

    var self = this;
    var instance = {
        systemName: function() {
            return self._lastPacket && self._lastPacket.system;
        },
        get: function() {
            return self._lastPacket;
        }
    };

    var $trigger = function(parts) {
        var $scope = parts[0];
        var getter = parts[1];
        var varName = parts[2];
        $scope.$apply(function() {
            $scope[varName] = instance[getter]();
        });
    };

    $websocket.registerGlobal('on_system', function(packet) {
        console.log('currentSystem updated:', packet);
        self._lastPacket = packet;
        _.each(self._watchers, $trigger);
    });

    /**
     * Probably the best public interface?
     * @param getterName (optional) if not provided, returns
     *  the whole packet
     */
    instance.watch = function($scope, getterName, varName) {
        if (!varName) {
            varName = getterName;
            getterName = 'get';
        }

        var args = [$scope, getterName, varName];
        self._watchers.push(args);
        $scope[varName] = instance[getterName]();
        $scope.$on('$destroy', function() {
            self._watchers = _.filter(self._watchers, function(el) {
                return el !== args;
            });
        });
    };

    this.$get = function() {
        return instance;
    }
}]);
