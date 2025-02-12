var _ = require("./_");

module.exports = function (obj) {
    return obj && _.isFunction(obj.then);
};
