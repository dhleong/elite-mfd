'use strict';
/* jshint indent:false */
/* global angular */

angular.module('emfd.views.trading', ['ngRoute'])

.config(['$routeProvider', function($routeProvider) {
    $routeProvider.when('/trading/:system/:station', {
        templateUrl: 'js/views/trading/trading.html'
      , controller: 'TradingController'
    });
}])

.controller('TradingController', [
        '$scope', '$routeParams', 'websocket',
        function($scope, $routeParams, websocket) {

    $scope.station = $routeParams.station;
    $scope.system = $routeParams.system;
    $scope.form = {
        type: 'calculate'
      , 'station-name': $scope.station + ' (' + $scope.system + ')'
      , 'station-name-end': null
      , cash: 1000 // TODO remember somehow?
      , cargo: 4
      , 'min-profit': 500
      , 'pad-size': 'Small'
      , 'search-range': '15'
    };

    $scope.validSizes = ['Small', 'Medium', 'Large'];
    $scope.validRanges = ['15', '25', '50'];

    $scope.results = null;

    websocket.registerLocal($scope, {
        calculate_result: function(result) {
            console.log("Results!", result);
            $scope.results = result.result;
        }
    });

    $scope.onCalculate = function() {
        console.log($scope.form);
        websocket.send($scope.form);
    };
}]);


