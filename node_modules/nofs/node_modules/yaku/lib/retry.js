var _ = require("./_");
var sleep = require("./sleep");
var $retryError = {};

module.exports = function (initRetries, span, fn, self) {
    return function () {
        var retries = initRetries;
        var errs = [], args = arguments;

        if (_.isFunction(span)) {
            self = fn;
            fn = span;
            span = 0;
        }

        var countdown = _.isFunction(retries) ?
            retries : function () { return sleep(span, --retries); };

        function tryFn (isContinue) {
            return isContinue ? fn.apply(self, args) : _.Promise.reject($retryError);
        }

        function onError (err) {
            if (err === $retryError) return _.Promise.reject(errs);

            errs.push(err);
            return attempt(countdown(errs));
        }

        function attempt (c) {
            return _.Promise.resolve(c).then(tryFn).catch(onError);
        }

        return attempt(true);
    };
};
