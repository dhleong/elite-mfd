'use strict';
/* global angular, _ */

var autoDirectiveFactory = function(requestType, responseType, formatter) {
    return ['$compile', '$parse', 'SharedState', 'websocket', 
        function($compile, $parse, SharedState, $websocket) {
            return {
                restrict: 'A',
                link: function($scope, $el, $attrs) {
                    $scope._stationsDeployed = false;
                    $scope.autoStationsData = {
                        input: $($el).val() // prefill with current
                      , results: null
                    };
                    var modelValue = $parse($attrs.ngModel);

                    // if we wanted to be fancier about registerLocal to not
                    //  totally override then we'd have less boilerplate here,
                    //  but so far this is the only case where we'd need that....
                    $websocket.registerGlobal(responseType, function(packet) {
                        if (packet.q != $scope.autoStationsData.input) 
                            return;
                        $scope.$apply(function() {
                            $scope.autoStationsData.results = _.map(packet.result, formatter);
                            console.log($scope.autoStationsData.results);
                        });
                    });
                    $scope.$on('$destroy', function() {
                        // unregister
                        $websocket.registerGlobal(responseType, null);
                    });

                    $scope.$watch('autoStationsData.input', function(newValue) {
                        if (newValue.length > 1) {
                            $websocket.send({
                                type: requestType
                              , q: newValue
                            });
                        } else if (!newValue.length) {
                            $scope.autoStationsData.results = null;
                        }
                    });

                    $scope.onModalStationClosed = function() {
                        // update the element with the input value
                        // $el.val($scope.autoStationsData.input.trim());
                        modelValue.assign($scope, $scope.autoStationsData.input.trim());

                        // gross dom hacks for better performance
                        //  on re-open. see below
                        $('stations-modal').remove();
                        $('#modals').find('.modal-stations').remove();
                        $('html').removeClass("has-modal has-modal-overlay");
                    }

                    $scope.clearInput = function() {
                        $scope.autoStationsData.input = '';
                        $scope.autoStationsData.result = null;

                        // re-focus
                        $('.modal-stations input').focus();
                    }

                    $scope.selectResult = function(row) {
                        $scope.autoStationsData.input = row;
                        $scope.onModalStationClosed(); // have to call manually
                        SharedState.set('modalStation', false);
                    }

                    $($el).focus(function() {
                        // DON'T focus; that would show the keyboard
                        $($el).blur();

                        // show the modal for selection
                        if (!$scope._stationsDeployed) {
                            SharedState.initialize($scope, 'modalStation', {defaultValue: true});

                        } else {
                            SharedState.set('modalStation', true);
                        }

                        // NB for whatever reason, keeping the same element and just doing
                        //  a SharedState.set() introduces a significant delay when
                        //  re-opening. So, we'll use some gross hacks (see above) to
                        //  always re-create the element (without dumping bizarre error
                        //  messages to the console)
                        $scope._stationsDeployed = true;
                        var modal = $compile("<stations-modal></stations-modal>")($scope);

                        $(document.body).append(modal);
                    });
                }
            };
        }
    ];
}

angular.module('emfd')

.controller('CoreController', ['$rootScope', 'SharedState', 'commander', 'websocket',
        function($rootScope, Ui, commander, websocket) {

    // NB we don't actually need the commander service here,
    //  but we'll inject it so it gets initialized in time
    //  to register the `commander-data` packet listener

    $rootScope.connected = false;
    $rootScope.everConnected = false;
    $rootScope.currentSystem = null;

    $rootScope.isSystem = function(candidate, systemName) {
        return candidate.trim().toLowerCase() == systemName.trim().toLowerCase();
    }
    $rootScope.inSystem = function(systemName) {
        // see issue #2
        var current = $rootScope.currentSystem;
        return current && systemName
            && $rootScope.isSystem(current.system, systemName);
    }

    window.ws = websocket;
    websocket.registerStatus('close', function() {
        $rootScope.$apply(function() {
            $rootScope.connected = false;

            Ui.turnOff('uiSidebarLeft');
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

    websocket.registerGlobal('error', function(packet) {
        console.error("<<", packet);
    });

}])

.directive('stationsModal', function() {
    return {
        restrict: 'E',
        templateUrl: 'js/modals/stations.html'
    }
})

.directive('autoStations', autoDirectiveFactory('stations', 'stations_result',
        function formatter(row) {
    return row.Station + ' (' + row.System + ')'
}))

.directive('autoSystems', autoDirectiveFactory('systems', 'systems_result',
        function formatter(row) { return row; }))

.directive('autofocus', ['$timeout', function($timeout) {
    return {
        restrict: 'A',
        link: function(scope, el) {
            $timeout(function() {
                $(el).focus();
            });
        }
    }
}])

/** for when you don't know where "back" should go */
.directive('backButton', ['$window', function($window) {
    return {
        restrict: 'A',
        link: function (scope, elem) {
            elem.bind('click', function () {
                $window.history.back();
                console.log("back", $window.history);
            });
        }
    };
}])

.factory('dataStore', function() {
    // dumb service for storing temporary data
    return {
    };
})

.filter('bigNumber', function() {
    /**
     * If you have a unit after the number, for example,
     *  but want a space between the number an the unit,
     *  you can use as `bigNumber:' '` and we'll put the
     *  space before the 'M' or 'K'
     */
    return function(input, after) {
        after = after || '';
        if (input > 10e6) {
            return Math.round(input / 1e6) + after + 'M';
        } else if (input > 10e3) {
            return Math.round(input / 1e3) + after + 'K';
        } else {
            return input + after;
        }
    }
});
