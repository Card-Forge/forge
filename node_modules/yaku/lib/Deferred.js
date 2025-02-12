var _ = require("./_");

module.exports = function () {
    var defer;
    defer = {};
    defer.promise = new _.Promise(function (resolve, reject) {
        defer.resolve = resolve;
        return defer.reject = reject;
    });
    return defer;
};
