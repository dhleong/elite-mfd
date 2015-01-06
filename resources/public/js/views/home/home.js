'use strict';
/* jshint indent:false */
/* global angular */

angular.module('emfd.views.home', ['ngRoute'])

.config(['$routeProvider', function($routeProvider) {
    $routeProvider.when('/home', {
        templateUrl: 'js/views/home/home.html'
      , controller: 'HomeController'
    });
}])

.controller('HomeController', [
        '$scope', '$location', 'currentSystem', 
        function($scope, $location, info) {
    info.watch($scope, 'info');

    $scope.selectStation = function(stationName) {
        console.log("select", stationName);
        $location.path('/trading/' + stationName);
    };
}]);

