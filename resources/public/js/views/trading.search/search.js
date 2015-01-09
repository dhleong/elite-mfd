'use strict';
/* jshint indent:false */
/* global angular */

angular.module('emfd.views.trading.search', ['ngRoute'])

.config(['$routeProvider', function($routeProvider) {
    $routeProvider.when('/search/:system', {
        templateUrl: 'js/views/trading.search/search.html'
      , controller: 'SearchController'
    });
}])

.controller('SearchController', [
        '$scope', '$routeParams', 'websocket',
        function($scope, $routeParams, websocket) {
    $scope.system = $routeParams.system;
    $scope.form = {
        type: 'search'
      , system: $scope.system
        // TODO etc.
      , 'pad-size': 'Small'
      , 'search-range': '15'
    };

    $scope.validSizes = ['Small', 'Medium', 'Large'];
    $scope.validRanges = ['15', '25', '50'];

    $scope.results = null;

    websocket.registerLocal($scope, {
        search_result: function(result) {
            console.log("Results!", result);
            $scope.results = result.result;
        }
    });

    $scope.onSearch = function() {
        console.log($scope.form);
        websocket.send($scope.form);
    };
}]);
