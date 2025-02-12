"use strict";

var Promise = require("yaku");
var yutils = require("yaku/lib/utils");
var utils = require("./utils");
var reporter = require("./reporter");

var extend = utils.extend;
var eq = utils.eq;

/**
 * A simple promise based module for unit tests.
 * @param  {Object} opts Defaults:
 * ```js
 * {
 *     filter: (msg) => true
 *
 *     // Stop test when error occurred.
 *     isBail: true,
 *
 *     isFailOnUnhandled: true,
 *
 *     // If any test failed, throw on final.
 *     isThrowOnFinal: true,
 *
 *     // Fail a test after timeout.
 *     timeout: 5000,
 *
 *     reporter: {
 *         // You can even use jsdiff here to generate more fancy error info.
 *         formatAssertErr: (actual, expected) => {},
 *
 *         logPass: (msg, span) => {},
 *         logFail: (msg, err, span) => {},
 *         logFinal: (total, tested, passed, failed) => {}
 *     }
 * }
 * ```
 * @return {Function} `(msg, fn) => Function` The `msg` can be anything.
 * The `fn`'s first param is a function `(after) =>`, you can pass a after hook
 * to it.
 * @example
 * ```js
 * import junit from "junit";
 * var it = junit();
 * (async () => {
 *     // Async tests.
 *     it("test 1", () =>
 *         // We use `it.eq` to assert on both simple type and complex object.
 *         it.eq("ok", "ok")
 *     );
 *
 *     it("test 2", async () => {
 *         // No more callback hell while testing async functions.
 *         await new junit.Promise(r => setTimeout(r, 1000));
 *
 *         return it.eq({ a: 1, b: 2 }, { a: 1, b: 2 });
 *     });
 *
 *     // Run sync tests within the main async flow.
 *     await it("test 3", (after) =>
 *         after(() => {
 *             // do some clean work after the test
 *         });
 *
 *         it.eq("ok", "ok")
 *     );
 *
 *     it.run();
 * })();
 * ```
 * @example
 * Filter the tests, only the message starts with "test" will be tested.
 * ```js
 * import junit from "junit";
 * var it = junit({
 *     filter: (msg) => msg.indexOf("test")
 * });
 *
 * (async () => {
 *     it("basic 1", () => it.eq(1, 1));
 *     it("test 1", () => it.eq(1, 1));
 *     it("test 2", () => it.eq(1, 1));
 *
 *     // Get the result of the test.
 *     var { total, tested, passed, failed } = await it.run();
 *
 *     console.log(total, tested, passed, failed);
 * })();
 * ```
 */
var junit = function (opts) {
    opts = extend({
        isBail: true,
        isFailOnUnhandled: true,
        isThrowOnFinal: true,
        timeout: 5000,
        filter: function () {
            return true;
        },
        reporter: reporter()
    }, opts || {});

    var _opts$reporter = opts.reporter;
    var formatAssertErr = _opts$reporter.formatAssertErr;
    var logPass = _opts$reporter.logPass;
    var logFail = _opts$reporter.logFail;
    var logFinal = _opts$reporter.logFinal;


    var passed = 0;
    var failed = 0;
    var total = 0;
    var tested = 0;
    var isEnd = false;
    var tests = [];

    if (opts.isFailOnUnhandled) {
        var onUnhandledRejection = Promise.onUnhandledRejection;
        /* istanbul ignore next */
        Promise.onUnhandledRejection = function (reason, p) {
            failed++;
            onUnhandledRejection(reason, p);
        };
    }

    function it (msg, fn) {
        if (typeof msg === 'function') {
            fn = msg;
            msg = '';
        }

        total++;

        if (isEnd) return;

        var ret;
        if (opts.filter(msg)) {
            tested++;
            var timeouter = null;
            var startTime = Date.now();
            var afterHook;

            ret = new Promise(function (resolve, reject) {
                resolve(fn(function (h) {
                    return afterHook = h;
                }));

                timeouter = setTimeout(reject, opts.timeout, new Error("test_timeout"));
            }).then(function () {
                clearTimeout(timeouter);
                if (isEnd) return;
                passed++;
                logPass(msg, Date.now() - startTime);
            }, function (err) {
                clearTimeout(timeouter);
                if (isEnd) return;
                failed++;
                if (opts.isBail) {
                    isEnd = true;
                }
                logFail(msg, err, Date.now() - startTime);
            }).then(function () {
                return afterHook && afterHook();
            }, function (err) {
                afterHook && afterHook();
                return Promise.reject(err);
            });
        } else {
            ret = Promise.resolve();
        }

        tests.push(ret);
        return ret;
    }

    function onFinal () {
        isEnd = true;
        logFinal(total, tested, passed, failed);

        /* istanbul ignore if */
        if (opts.isThrowOnFinal && failed) yutils['throw']('junit test failed with ' + failed);

        return { total: total, tested: tested, passed: passed, failed: failed };
    }

    var describe = function (msg, fn, notInit) {
        var subIt = function (subMsg, fn) {
            if (typeof subMsg === 'function') {
                fn = subMsg;
                subMsg = '';
            }

            return Promise.resolve(it(notInit ? msg.concat([subMsg]) : [msg, subMsg], fn));
        };

        extend(subIt, it);
        subIt.describe = function (subMsg, fn) {
            return Promise.resolve(describe(notInit ? msg.concat([subMsg]) : [msg, subMsg], fn, true));
        };

        return Promise.resolve(fn(subIt));
    };

    return extend(it, {

        /**
         * Start the tests.
         * @return {Promise} It will resolve `{ total, passed, failed }`
         */

        run: function () {
            return Promise.all(tests).then(onFinal);
        },


        /**
         * A smart strict deep equality assertion helper function.
         * If any of the arguments is promise, it will be auto-resolved before
         * comparision.
         * @param {Any} actual
         * @param {Any} expected
         * @param {Number = 7} maxDepth Optional. The max depth of the recursion check.
         * @return {Promise}
         */
        eq: eq(formatAssertErr),

        /**
         * Extend the msg of the test with a new test closure.
         * @param {Any} msg The msg object of the test.
         * @param {Function} fn `(it) => Promise` The new msg closure.
         * @return {Promise}
         * @example
         * ```js
         * import junit from "junit";
         *
         * var it = junit();
         * var { eq } = it;
         *
         * it.describe("level 01", it => {
         *     it("test 01", () => eq(1, 1));
         *
         *     it("test 02", () => eq(1, 1));
         *
         *     it.describe("level 02", it => {
         *         it("test 01", () => eq(1, 1));
         *
         *         it("test 02", () => eq(1, 1));
         *     });
         * });
         *
         * it.run();
         * ```
         */
        describe: describe
    });
};

/**
 * An example reporter for junit.
 * @param {Object} opts Defaults:
 * ```js
 * {
 *     prompt: String, // The prompt prefix
 *     mode: "console" // "console", "browser" or "none"
 * }
 * ```
 * @return {Function} `() => Object`.
 * @example
 * ```js
 * var it = junit({ reporter: junit.reporter({ prompt: 'my-prompt > ' }) });
 * ```
 */
junit.reporter = reporter;

/**
 * The promise class that junit uses: [Yaku](https://github.com/ysmood/yaku)
 * @type {Object}
 */
junit.Promise = Promise;

/**
 * The promise helpers: [Yaku Utils](https://github.com/ysmood/yaku#utils)
 * @type {Object}
 */
junit.yutils = yutils;

module.exports = junit;
