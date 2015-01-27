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
        '$scope', '$routeParams', 'websocket', 'dataStore', 'commander',
        function($scope, $routeParams, websocket, dataStore, cmdr) {

    $scope.station = $routeParams.station;
    $scope.system = $routeParams.system;
    $scope.form = {
        type: 'calculate'
      , 'station-name': $scope.station + ' (' + $scope.system + ')'
      , 'station-name-end': $routeParams.to
      , cash: cmdr.cash
      , cargo: cmdr.cargo
      , 'max-distance': cmdr.prop('trading-max-distance', 1000)
      , 'min-profit': cmdr.prop('min-profit', 500)
      , 'pad-size': cmdr['pad-size']
      , 'search-range': cmdr.prop('trading-search-range', '15')
    };

    $scope.validSizes = cmdr.VALID_PAD_SIZES;
    $scope.validRanges = ['15', '25', '50', '75'];

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

    $scope.prepForm = function() {
        return cmdr.form($scope.form);
    }

    $scope.onCalculate = function() {
        var packet = $scope.prepForm();
        console.log('>>', packet);
        dataStore.tradingForm = $scope.form;
        websocket.send(packet);
    };
}]);


