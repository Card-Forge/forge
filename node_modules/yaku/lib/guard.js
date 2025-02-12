var _ = require("./_");

_.Promise.prototype.guard = function (type, onRejected) {
    return this["catch"](function (reason) {
        if (reason instanceof type && onRejected)
            return onRejected(reason);
        else
            return _.Promise.reject(reason);
    });
};
