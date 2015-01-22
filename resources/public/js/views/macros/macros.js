'use strict';
/* global angular, _ */
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
    $scope.macros = _.toArray(macros()); // copy

    // add a blank row for creating new macros
    var addMacroPlaceholder = function() {
        $scope.macros.push({name:"", value:[], isNew: true});
    }
    addMacroPlaceholder();

    /*
     * methods
     */

    $scope.addBinding = function(macro) {
        macro.value.push(Object.keys($scope.bindings)[0]);
    }

    $scope.checkMacroItem = function(macro, itemIndex) {
        if (!macro.value[itemIndex])
            macro.value.splice(itemIndex, 1);

        if (!macro.isNew) {
            // real macro; update it
            // NB: ensure we don't include the placeholder
            macros($scope.macros.slice(0, -1));
        }
    }

    $scope.saveBindings = function() {
        bindings($scope.bindings);
    }

    $scope.createMacro = function(macro) {
        // mark non-new
        delete macro.isNew;

        // save macros
        console.log($scope.macros);
        macros($scope.macros);

        // add new placeholder
        addMacroPlaceholder();
    }

    $scope.deleteMacro = function(macrosIndex) {
        // kill it
        $scope.macros.splice(macrosIndex, 1);

        // NB: ensure we don't include the placeholder
        macros($scope.macros.slice(0, -1));
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
