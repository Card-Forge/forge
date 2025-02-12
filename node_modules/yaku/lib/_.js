var Promise = require("./yaku");

module.exports = {

    extendPrototype: function (src, target) {
        for (var k in target) {
            src.prototype[k] = target[k];
        }
        return src;
    },

    isFunction: function (obj) {
        return typeof obj === "function";
    },

    isNumber: function (obj) {
        return typeof obj === "number";
    },

    Promise: Promise,

    slice: [].slice

};
