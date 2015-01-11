'use strict';
/* jshint indent:false */
/* global angular */

angular.module('emfd.views.navigate', ['ngRoute'])

.config(['$routeProvider', function($routeProvider) {
    $routeProvider.when('/navigate/:start/:end', {
        templateUrl: 'js/views/navigate/navigate.html'
      , controller: 'NavigateController'
    });
}])

.controller('NavigateController', [
        '$scope', '$routeParams', '$rootScope', 'websocket',
        function($scope, $routeParams, $rootScope, websocket) {
    $scope.form = {
        type: 'navigate'
      , start: $routeParams.start
      , end: $routeParams.end
      , 'jump-distance': 10 // TODO memorize
    }
    $scope.results = null;
    $scope.loading = false;
    $scope.useTurnByTurn = false; // TODO memorize

    var narrate = function(text) {
        websocket.send({type:"narrate", text:text});
    };

    $scope.lastSystem = $rootScope.currentSystem.system;
    $scope.jumpIndex = 0;

    websocket.registerLocal($scope, {
        on_system: function(packet) {
            console.log("System updated!", packet);
            if (packet.system == $scope.lastSystem) {
                // dup
                return;
            }

            var lastSystem = $scope.lastSystem;
            $scope.lastSystem = packet.system;
            var nextIndex = $scope.jumpIndex + 1;
            if ($scope.results && $scope.results.length > nextIndex) {
                // have we gone to the right place?
                var nextName = $scope.results[nextIndex].name;
                if (nextName != packet.system) {
                    // TODO dynamic re-route?
                    narrate("Unexpected jump; Please go to " + 
                            nextName + " or return to " + lastSystem);
                } else if ($scope.results.length - 1 == nextIndex) {
                    narrate("You have arrived");
                } else {
                    // cool. proceed
                    $scope.jumpIndex = nextIndex;
                    narrate("Arrived in " + nextName);
                    narrate("Next jump: " + $scope.results[nextIndex+1].name);
                }
            }
        }
      , navigate_result: function(packet) {
            console.log("Got navigation:", packet);
            $scope.loading = false;
            $scope.results = packet.result || [];

            if ($scope.useTurnByTurn && $scope.results.length > 2) {
                narrate((packet.result.length - 1)
                    + " jumps to reach "
                    + $scope.form.end);
                narrate("First jump: " + packet.result[1].name);
            } else if ($scope.useTurnByTurn && $scope.results.length) {
                narrate("Jump directly to " + packet.result[1].name);
            }
        }
    });

    $scope.onSearch = function() {
        websocket.send($scope.form);
        $scope.results = null;
        $scope.loading = true;
    }
}])
