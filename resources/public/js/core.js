'use strict';
/* global angular */

angular.module('emfd')

.controller('CoreController', ['$rootScope', 'websocket',
        function($rootScope, websocket) {

    $rootScope.connected = false;
    $rootScope.everConnected = false;

    websocket.registerStatus('close', function() {
        $rootScope.$apply(function() {
            $rootScope.connected = false;
        });
    });

    websocket.registerGlobal('on_system', function(packet) {
        console.log("<<", packet);
        $rootScope.$apply(function() {
            // we set connected when we get the first
            //  on_system packet for better UX
            $rootScope.connected = true;
            $rootScope.everConnected = true;

            $rootScope.currentSystem = packet;
        });
    });

}]);
