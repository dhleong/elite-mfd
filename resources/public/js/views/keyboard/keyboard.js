'use strict';
/* global angular, _ */
/* jshint indent: false */

angular.module('emfd')

.factory('keyboardSuggestions', [function() {

    var suggestionsMap = {
        initial: ["o7"]
    };

    return {
        setForScope: function($scope, suggestions) {
            var hadSuggestions = suggestionsMap[$scope];
            suggestionsMap[$scope] = suggestions;
            if (!hadSuggestions) {
                $scope.$on('$destroy', function() {
                    delete suggestionsMap[$scope];
                });
            }
        }
      , get suggestions() {
            return _.reduce(_.keys(suggestionsMap), function(list, scope) {
                return list.concat(suggestionsMap[scope]);
            }, []);
        }
    };
}])

.controller('KeyboardController', [
        '$scope', 'websocket', 'keyboardSuggestions',
        function($scope, $ws, keyboard) {

    $scope.keyboard = keyboard;
    $scope.input = '';

    $scope.sendKeys = function(input) {
        // NB we intentionally accept empty input,
        //  so our enter can mimic the real enter
        $scope.input = '';
        $ws.send({
            type: 'macro'
          , macro: ['"' + input + '"', 'ui-confirm']
        });
    }
}]);
