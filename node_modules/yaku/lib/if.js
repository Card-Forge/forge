var _ = require("./_");

module.exports = function (cond, trueFn, falseFn) {
    return _.Promise.resolve(cond).then(function (val) {
        return val ?
            trueFn() :
            (_.isFunction(falseFn) && falseFn());
    });
};
