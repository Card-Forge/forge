var _ = require("./_");

module.exports = function (time, val) {
    return new _.Promise(function (r) {
        setTimeout(r, time, val);
    });
};
