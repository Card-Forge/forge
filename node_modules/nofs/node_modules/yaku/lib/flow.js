var _ = require("./_");
var genIterator = require("./genIterator");
var isPromise = require("./isPromise");

module.exports = function (iterable) {
    var iter = genIterator(iterable);

    return function (val) {
        function run (pre) {
            return pre.then(function (val) {
                var task = iter.next(val);

                if (task.done) {
                    return val;
                }
                var curr = task.value;
                return run(
                    isPromise(curr) ? curr :
                        _.isFunction(curr) ? _.Promise.resolve(curr(val)) :
                            _.Promise.resolve(curr)
                );
            });
        }

        return run(_.Promise.resolve(val));
    };
};
