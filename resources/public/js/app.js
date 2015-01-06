'use strict';

/* global angular */
angular.module('emfd', [
    'ngRoute'
  , 'emfd.views.home'
  , 'emfd.views.trading'
])

.config(['$routeProvider', function($routeProvider) {
    $routeProvider.otherwise({redirectTo: '/home'});
}]);
