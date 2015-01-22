'use strict';
/* global angular, _ */

/**
 * The Commander service is responsible
 *  for managing commander-global values,
 *  and providing a convenient way to
 *  bind to them. Simply add `{getterSetter: true}`
 *  to the ng-model-options of your form,
 *  and use `prop(name, defaultVal)` to get
 *  a bindable reference to a property
 *
 * The Commander has several shared properties
 *  that can be accessed directly on the commander
 *  as fields, so the default values can be set
 *  here and shared:
 *
 *      - cash
 *      - cargo
 *      - jump-range
 *      - pad-size
 *
 * Values that are specific to your view should
 *  use the `prop()` syntax, but those that are
 *  shared should be created as new fields. Note,
 *  however, that all names are global, so consider
 *  prefixing properties created via `prop()`.
 *
 */
angular.module('emfd')
.factory('commander', ['$rootScope', 'websocket', function($rootScope, $ws) {
    var _data = { };

    $ws.registerGlobal('commander_data', function(packet) {
        _data = _.extend(_data, packet.data);
        $rootScope.$broadcast('emfd.commander-data', packet);
    });

    var update = function(field, value) {
        _data[field] = value;

        // send updated value
        console.log("Update!", field, value);
        $ws.send({
            type: 'commander'
          , field: field
          , value: value
        });
    };

    var newField = function(fieldName, defaultValue) {
        return function(newValue) {
            var oldValue = _data[fieldName];
            if (angular.isDefined(newValue)) {
                update(fieldName, newValue);
            } else if (!angular.isDefined(oldValue)) {
                _data[fieldName] = defaultValue;
                return defaultValue;
            }

            return oldValue;
        }
    }

    var service = {
        /* 
         * Shared fields 
         */
        cash: newField('cash', 1000)
      , cargo: newField('cargo', 4)
      , 'jump-range': newField('jump-range', 10)
      , 'pad-size': newField('pad-size', 'Small')

        /*
         * Shared Constants
         */
      , VALID_PAD_SIZES: ['Small', 'Medium', 'Large']
    };

    /* Per-view fields */
    service.prop = function(name, defaultValue) {
        if (!service[name]) {
            service[name] = newField(name, defaultValue);
        }

        return service[name];
    }

    /* Given a form dict, replace any of our getters
     * with their current value */
    service.form = function(form) {
        return _.reduce(_.keys(form),
        function(result, key) {
            var val = form[key];
            result[key] = _.isFunction(val)
                ? val()
                : val;
            return result;
        }, {});
    }

    return service;
}]);
