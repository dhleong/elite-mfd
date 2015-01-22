'use strict';
/* global angular, _ */
/* jshint indent: false */

var REQUEST_DOCKING = {
    name: "Request Docking", 
    value: ["navigation", "tab-right", "tab-right",
            "ui-select", "ui-down", "ui-select",
            "tab-left", "tab-left", "navigation"]
};

// would be nice to pull this from the server somehow...
var DEFAULT_BINDINGS = {
    "navigation": "1",
    "tab-left": "q",
    "tab-right": "e",
    "ui-down": "down",
    "ui-right": "right",
    "ui-select": "space"
}

angular.module('emfd.views.macros', ['ngRoute'])

.config(['$routeProvider', function($routeProvider) {
    $routeProvider.when('/macros', {
        templateUrl: 'js/views/macros/macros.html'
      , controller: 'MacrosController'
    });
}])

.controller('MacrosController', [
        '$scope', 'commander',
        function($scope, cmdr) {
    var bindings = cmdr.prop('bindings', {});
    if (!Object.keys(bindings()).length) {
        // passing DEFAULT_BINDINGS above
        //  doesn't seem to work....?
        bindings(DEFAULT_BINDINGS);
    }
    $scope.bindings = bindings();

    $scope.doSave = function() {
        bindings($scope.bindings);
    }
}])

.controller('MacrosBarController', [
        '$scope', 'websocket', 'commander',
        function($scope, websocket, commander) {

    $scope.macros = {
        get list() {
            var base = commander.prop('macros', [])();
            var actual = _.toArray(base);
            actual.unshift(REQUEST_DOCKING);
            return actual;
        }
    }

    $scope.sendMacro = function(item) {
        websocket.send({
            type: "macro"
          , macro: item.value
        });
    }
}]);
