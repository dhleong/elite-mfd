'use strict';

/* global angular */
angular.module('emfd', [
    'ngRoute'
  , 'mobile-angular-ui'
  , 'emfd.views.home'
  , 'emfd.views.stations'
  , 'emfd.views.trading'
  , 'emfd.views.trading.search'
  , 'emfd.views.navigate'
])

.config(['$routeProvider', function($routeProvider) {
    $routeProvider.otherwise({redirectTo: '/home'});
}]);
