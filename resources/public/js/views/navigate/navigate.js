'use strict';
/* jshint indent:false */
/* global angular, _ */

var LONG_JOURNEY_SIZE = 5;

angular.module('emfd.views.navigate', ['ngRoute'])

.config(['$routeProvider', function($routeProvider) {
    $routeProvider.when('/navigate/:start/:end?', {
        templateUrl: 'js/views/navigate/navigate.html'
      , controller: 'NavigateController'
    });
}])

.controller('NavigateController', [
        '$scope', '$routeParams', '$rootScope', 
        'websocket', 'commander', 'keyboardSuggestions',
        function($scope, $routeParams, $rootScope, 
            websocket, cmdr, keyboardSuggestions) {
    $scope.form = {
        type: 'navigate'
      , start: $routeParams.start
      , end: $routeParams.end
      , 'jump-range': cmdr['jump-range']
    }
    $scope.endProvided = !!$routeParams.end;
    $scope.results = null;
    $scope.loading = false;
    $scope.useTurnByTurn = false; 

    var narrate = function(text) {
        websocket.send({type:"narrate", text:text});
    };
    /** 
     * Tweak the string name of a system for better UX
     *  when narrated
     */
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
    /** Describe a jump; includes distance and cleaned name */
    narrate.jump = function(jumpInfo) {
        return parseInt(jumpInfo.distance) 
            + " light years to " 
            + narrate.system(jumpInfo.name);
    }

    $scope.lastSystem = $rootScope.currentSystem.system;

    var updateSuggestions = function(currentIndex) {
        keyboardSuggestions.setForScope($scope, 
            _.drop(_.pluck($scope.results, 'name'), currentIndex + 1));
    }

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

            updateSuggestions(currentIndex);

            if ($scope.useTurnByTurn) {
                var nextIndex = currentIndex + 1;
                if ($scope.results.length == nextIndex) {
                    narrate("You have arrived");
                    $scope.useTurnByTurn = false;
                } else {
                    // cool. proceed
                    narrate("Arrived in " + narrate.system(packet.system));
                    narrate("Next jump: " 
                        + narrate.jump($scope.results[nextIndex]));

                    if ($scope.results.length >= LONG_JOURNEY_SIZE) {
                        // NB there are len-1 jumps, because [0] is the start
                        if (nextIndex == $scope.results.length - 1) {
                            narrate("Last jump");
                        } else {
                            narrate(($scope.results.length - nextIndex) + " jumps remaining");
                        }
                    }
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
                updateSuggestions(0);
            }

            if ($scope.useTurnByTurn && $scope.results.length > 2) {
                narrate((packet.result.length - 1)
                    + " jumps to reach "
                    + narrate.system($scope.form.end));
                narrate("First jump: " + narrate.jump(packet.result[1]));
            } else if ($scope.useTurnByTurn && $scope.results.length) {
                narrate("Jump directly to " + narrate.system(packet.result[1].name));
            }
        }
    });

    $scope.onSearch = function() {
        var packet = cmdr.form($scope.form);
        console.log('>>', packet);
        websocket.send(packet);
        $scope.results = null;
        $scope.error = null;
        $scope.loading = true;
    }

    $scope.canPerformNavMacro = function() {
        var bindings = cmdr.get('bindings');
        console.log(bindings);
        return bindings && bindings['galaxy-map'];
    }
    $scope.performNavMacro = function(systemName) {
        websocket.send({
            type: 'macro'
          , macro: ['galaxy-map', 'macro-wait', 
                    'tab-right', 'ui-select', // select the field
                    'backspace', // if `space` is ui-select, one is added
                    '"' + systemName + '"', // finally type
                    'press-enter']
        });
    }
}])
