var Yaku = require("./yaku");

try {
    global.Promise = Yaku;
    window.Promise = Yaku;
} catch (err) {
    null;
}