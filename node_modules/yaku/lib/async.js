var Promise = require("./_").Promise;

var tryErr = {};

function tryCatch (step, key) {
    try {
        return step(key);
    } catch (err) {
        tryErr.err = err;
        return tryErr;
    }
}

module.exports = function (generator) {
    return function () {
        var gen = generator.apply(this, arguments);

        function genNext (val) {
            return step("next", val);
        }

        function genThrow (val) {
            return step("throw", val);
        }

        function step (key, val) {
            var info = gen[key](val);

            if (info.done) {
                return Promise.resolve(info.value);
            } else {
                return Promise.resolve(info.value).then(genNext, genThrow);
            }
        }

        var ret = tryCatch(step, "next");

        if (ret === tryErr)
            return Promise.reject(ret.err);
        else
            return ret;
    };
};
