'use strict';
var Promise, _;

Promise = require('yaku');

module.exports = _ = {
    Promise: Promise,

    PromiseUtils: require('yaku/lib/utils'),

    id: function (v) { return v; },

    extend: function(to, from) {
        var k;
        for (k in from) {
            to[k] = from[k];
        }
        return to;
    },

    defaults: function(to, from) {
        var k;
        for (k in from) {
            if (to[k] === void 0) {
                to[k] = from[k];
            }
        }
        return to;
    },

    isString: function(val) {
        return typeof val === 'string';
    },

    isFunction: function(val) {
        return typeof val === 'function';
    },

    isObject: function(val) {
        return typeof val === 'object';
    },

    isRegExp: function(val) {
        return val instanceof RegExp;
    },

    keys: function(val) {
        return Object.keys(val);
    },

    all: function(arr, fn) {
        var el, i, j, len;
        for (i = j = 0, len = arr.length; j < len; i = ++j) {
            el = arr[i];
            if (fn(el, i) === false) {
                return false;
            }
        }
        return true;
    },

    any: function(arr, fn) {
        var el, i, j, len;
        for (i = j = 0, len = arr.length; j < len; i = ++j) {
            el = arr[i];
            if (fn(el, i) === true) {
                return true;
            }
        }
        return false;
    }
};
