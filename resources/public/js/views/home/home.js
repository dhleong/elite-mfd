'use strict';
/* jshint indent:false */
/* global angular */

angular.module('emfd.views.home', ['ngRoute'])

.config(['$routeProvider', function($routeProvider) {
    $routeProvider.when('/home', {
        templateUrl: 'js/views/home/home.html'
    });
}]);
