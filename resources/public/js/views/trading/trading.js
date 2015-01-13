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
        '$scope', '$routeParams', 'websocket', 'dataStore',
        function($scope, $routeParams, websocket, dataStore) {

    $scope.station = $routeParams.station;
    $scope.system = $routeParams.system;
    $scope.form = {
        type: 'calculate'
      , 'station-name': $scope.station + ' (' + $scope.system + ')'
      , 'station-name-end': $routeParams.to
      , cash: 1000 // TODO remember somehow?
      , cargo: 4
      , 'max-distance': 1000
      , 'min-profit': 500
      , 'pad-size': 'Small'
      , 'search-range': '15'
    };

    $scope.validSizes = ['Small', 'Medium', 'Large'];
    $scope.validRanges = ['15', '25', '50'];

    if (dataStore.trading 
            && dataStore.tradingForm['station-name'] == $scope.form['station-name']) {
        // restore last results
        $scope.results = dataStore.trading;
        $scope.form = dataStore.tradingForm;
    } else {
        // clear stale data
        dataStore.trading = null;
        dataStore.tradingForm = null;
    }

    websocket.registerLocal($scope, {
        calculate_result: function(result) {
            console.log("Results!", result);
            $scope.results = dataStore.trading = result.result;
        }
    });

    $scope.onCalculate = function() {
        console.log($scope.form);
        dataStore.tradingForm = $scope.form;
        websocket.send($scope.form);
    };
}]);


