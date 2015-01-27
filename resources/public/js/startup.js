'use strict';
/* global angular */
/* jshint indent: false */

angular.module('emfd')

.controller('StartupController', [
        '$scope', 'websocket',
        function($scope, $ws) {
    $scope.isClientRunning = false;

    $ws.registerGlobal("startup_data", function(packet) {
        $scope.$apply(function() {
            $scope.isClientRunning = packet['client-running'];
        });
    });

    /*
     * methods
     */

    $scope.sendMenu = function() {
        $ws.send({type: 'open-menu'});
    }
    $scope.sendStartup = function() {
        $scope.isClientRunning = true;
        $ws.send({type: 'startup'});
    }
    $scope.sendShutdown = function() {
        if (window.confirm("Are you sure you want to quit?")) {
            $scope.isClientRunning = false;
            $ws.send({type: 'shutdown'});
        }
    }
}]);
