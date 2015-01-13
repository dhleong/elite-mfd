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
      , 'jump-range': 10 // TODO memorize
    }
    $scope.results = null;
    $scope.loading = false;
    $scope.useTurnByTurn = false; // TODO memorize

    var narrate = function(text) {
        websocket.send({type:"narrate", text:text});
    };
    /** 
     * Tweak the string name of a system for better UX
     *  when narrated */
    narrate.system = function(systemName) {
        return systemName.replace(/-/g, ' dash ')
                         .replace(/(\d{3,})/g,
                            function(match, p1) {
                                // add a comma before it
                                //  so the first number doesn't
                                //  merge into the previous word
                                return ',' + p1.replace(/(\d)/g, ' $1 ');
                            });
    }

    $scope.lastSystem = $rootScope.currentSystem.system;

    websocket.registerLocal($scope, {
        on_system: function(packet) {
            console.log("System updated!", packet);
            if (packet.system == $scope.lastSystem) {
                // dup
                return;
            }

            if (!($scope.results && $scope.results.length)) {
                // no navigation; doesn't matter
                return;
            }

            // just loop through to see where we are
            var lastSystem = $scope.lastSystem;
            var currentIndex = -1;
            for (var i=0; i < $scope.results.length; i++) {
                // NB can't use inSystem because this might run BEFORE
                //  the CoreController's listener
                if ($rootScope.isSystem(packet.system, $scope.results[i].name)) {
                    currentIndex = i;
                    break;
                }
            }

            if (~currentIndex) {
                // found!
                $scope.lastSystem = packet.system;
            } else if ($scope.useTurnByTurn) {
                // nope :(
                // TODO dynamic re-route?
                narrate("Unexpected jump; Please return to " 
                    + narrate.system(lastSystem));
                return;
            }

            if ($scope.useTurnByTurn) {
                var nextIndex = currentIndex + 1;
                if ($scope.results.length == nextIndex) {
                    narrate("You have arrived");
                    $scope.useTurnByTurn = false;
                } else {
                    // cool. proceed
                    narrate("Arrived in " + narrate.system(packet.system));
                    narrate("Next jump: " 
                        + narrate.system($scope.results[nextIndex].name));
                }
            }
        }
      , navigate_result: function(packet) {
            console.log("Got navigation:", packet);
            $scope.loading = false;
            if (packet.result.error) {
                $scope.results = null;
                $scope.error = packet.result.errormsg;
            } else {
                $scope.results = packet.result || [];
            }

            if ($scope.useTurnByTurn && $scope.results.length > 2) {
                narrate((packet.result.length - 1)
                    + " jumps to reach "
                    + narrate.system($scope.form.end));
                narrate("First jump: " + narrate.system(packet.result[1].name));
            } else if ($scope.useTurnByTurn && $scope.results.length) {
                narrate("Jump directly to " + narrate.system(packet.result[1].name));
            }
        }
    });

    $scope.onSearch = function() {
        websocket.send($scope.form);
        $scope.results = null;
        $scope.error = null;
        $scope.loading = true;
    }
}])
