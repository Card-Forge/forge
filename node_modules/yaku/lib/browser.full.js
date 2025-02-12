// This file is intended for browser only.

var Yaku = require("./yaku");

var utils = require("./utils");

for (var key in utils) {
    Yaku[key] = utils[key];
}

module.exports = window.Yaku = Yaku;
