'use strict';
/* global angular */
/* jshint indent: false */

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
    var macros = cmdr.prop('macros', []);
    $scope.bindings = bindings();
    $scope.macros = macros();
    $scope.newMacro = {};
    console.log($scope.macros);

    $scope.saveBindings = function() {
        bindings($scope.bindings);
    }

    $scope.saveMacro = function() {
        console.log($scope.macros);
        console.log("NEW", $scope.newMacro);
    }
}])

.controller('MacrosBarController', [
        '$scope', 'websocket', 'commander',
        function($scope, websocket, cmdr) {

    // it's kinda BS that we can't use the getterSetter here...
    $scope.macros = cmdr.prop('macros', [])();

    $scope.sendMacro = function(item) {
        websocket.send({
            type: "macro"
          , macro: item.value
        });
    }
}]);
