"use strict";

var utils = require("./utils");

var codes = {
    underline: ["\u001b[4m", "\u001b[24m", "<span class=\"underline\" style=\"text-decoration: underline;\">", "</span>"],
    red: ["\u001b[31m", "\u001b[39m", "<span class=\"red\" style=\"color: #E25757;\">", "</span>"],
    green: ["\u001b[32m", "\u001b[39m", "<span class=\"green\" style=\"color: #66B55E;\">", "</span>"],
    yellow: ["\u001b[33m", "\u001b[39m", "<span class=\"yellow\" style=\"color: #C7B414;\">", "</span>"],
    cyan: ["\u001b[36m", "\u001b[39m", "<span class=\"cyan\" style=\"color: #00B5B5;\">", "</span>"],
    grey: ["\u001b[90m", "\u001b[39m", "<span class=\"grey\" style=\"color: #A5A5A5;\">", "</span>"]
};

function genBrush (code, mode) {
    return function (str) {
        switch (mode) {
        case "console":
            return code[0] + str + code[1];
        /* istanbul ignore next */
        case "browser":
            return code[2] + str + code[3];
        default:
            return str;
        }
    };
}

module.exports = function (opts) {
    opts = utils.extend({
        mode: "console"
    }, opts);

    var brush = {};

    for (var k in codes) {
        brush[k] = genBrush(codes[k], opts.mode);
    }

    return brush;
};

