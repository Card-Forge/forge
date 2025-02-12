var _ = require("./_");

module.exports = function (promise, time, error) {
    if (error === void 0)
        error = new Error("time out");

    return new _.Promise(function (resolve, reject) {
        setTimeout(reject, time, error);
        _.Promise.resolve(promise).then(resolve, reject);
    });
};
