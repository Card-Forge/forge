/**
* Before reading this source file, open web page [Promises/A+](https://promisesaplus.com).
*/


(function (){
    /**
     * This class follows the [Promises/A+](https://promisesaplus.com) and
     * [ES6](http://people.mozilla.org/~jorendorff/es6-draft.html#sec-promise-objects) spec
     * with some extra helpers.
     * @param  {Function} executor Function object with two arguments resolve and reject.
     * The first argument fulfills the promise, the second argument rejects it.
     * We can call these functions, once our operation is completed.
     */
    var Yaku = module.exports = function (executor) {
        executor(genSettler(this, $resolved), genSettler(this, $rejected));
    };

    if (typeof window === "object") window["Yaku"] = Yaku;

    /**
     * Appends fulfillment and rejection handlers to the promise,
     * and returns a new promise resolving to the return value of the called handler.
     * @param  {Function} onFulfilled Optional. Called when the Promise is resolved.
     * @param  {Function} onRejected  Optional. Called when the Promise is rejected.
     * @return {Yaku} It will return a new Yaku which will resolve or reject after
     * the current Promise.
     */
    Yaku.prototype.then = function (onFulfilled, onRejected) {
        return addHandler(this, new Yaku(function () {}), onFulfilled, onRejected);
    };


    /*
    * All static variable name will begin with `$`. Such as `$rejected`.
    * @private
    */


    /**
     * These are some static symbolys.
     * @private
     */

    var $rejected = 0;

    var $resolved = 1;

    var $pending = 2;

    // Default state
    Yaku.prototype._s = $pending;


    /**
     * The number of current promises that attach to this Yaku instance.
     * @private
     */
    Yaku.prototype._c = 0;


    /**
     * It will produce a settlePromise function to user.
     * Such as the resolve and reject in this `new Yaku (resolve, reject) ->`.
     * @private
     * @param  {Yaku} self
     * @param  {Integer} state The value is one of `$pending`, `$resolved` or `$rejected`.
     * @return {Function} `(value) -> undefined` A resolve or reject function.
     */
    function genSettler (self, state) {
        return function (value) {
            return settlePromise(self, state, value);
        };
    }


    /**
     * Link the promise1 to the promise2.
     * @private
     * @param {Yaku} p1
     * @param {Yaku} p2
     * @param {Function} onFulfilled
     * @param {Function} onRejected
     */
    function addHandler (p1, p2, onFulfilled, onRejected) {
        // 2.2.1
        if (typeof onFulfilled === "function") {
            p2._onFulfilled = onFulfilled;
        }
        if (typeof onRejected === "function") {
            p2._onRejected = onRejected;
        }

        // 2.2.6
        if (p1._s === $pending) {
            p1[p1._c++] = p2;
        } else {
            scheduleHandler(p1, p2);
        }

        // # 2.2.7
        return p2;
    }


    /**
     * Resolve the value returned by onFulfilled or onRejected.
     * @private
     * @param {Yaku} p1
     * @param {Yaku} p2
     */
    function scheduleHandler (p1, p2) {
        return setTimeout(function () {
            var x, handler = p1._s ? p2._onFulfilled : p2._onRejected;

            // 2.2.7.3
            // 2.2.7.4
            if (handler === void 0) {
                settlePromise(p2, p1._s, p1._v);
                return;
            }

            try {
                // 2.2.5
                x = handler(p1._v);
            } catch (err) {
                // 2.2.7.2
                settlePromise(p2, $rejected, err);
                return;
            }

            // 2.2.7.1
            settleWithX(p2, x);
        });
    }


    /**
     * Resolve or reject a promise.
     * @param  {Yaku} p
     * @param  {Integer} state
     * @param  {Any} value
     */
    function settlePromise (p, state, value) {
        // 2.1.2
        // 2.1.3
        if (p._s !== $pending) return;

        // 2.1.1.1
        p._s = state;
        p._v = value;

        var i = 0,
            len = p._c;

        // 2.2.2
        // 2.2.3
        while (i < len) {
            // 2.2.4
            scheduleHandler(p, p[i++]);
        }

        return p;
    }


    /**
     * Resolve or reject primise with value x. The x can also be a thenable.
     * @private
     * @param {Yaku} p
     * @param {Any | Thenable} x A normal value or a thenable.
     */
    function settleWithX (p, x) {
        // 2.3.1
        if (x === p && x) {
            settlePromise(p, $rejected, new TypeError("promise_circular_chain"));
            return;
        }

        // 2.3.2
        // 2.3.3
        var xthen, type = typeof x;
        if (x !== null && (type === "function" || type === "object")) {
            try {
                // 2.3.2.1
                xthen = x.then;
            } catch (err) {
                // 2.3.3.2
                settlePromise(p, $rejected, err);
                return;
            }
            if (typeof xthen === "function") {
                settleXthen(p, x, xthen);
            } else {
                // 2.3.3.4
                settlePromise(p, $resolved, x);
            }
        } else {
            // 2.3.4
            settlePromise(p, $resolved, x);
        }
        return p;
    }


    /**
     * Resolve then with its promise.
     * @private
     * @param  {Yaku} p
     * @param  {Thenable} x
     * @param  {Function} xthen
     */

    function settleXthen (p, x, xthen) {
        try {
            // 2.3.3.3
            xthen.call(x, function (y) {
                // 2.3.3.3.3
                if (!x) return;
                x = null;

                // 2.3.3.3.1
                settleWithX(p, y);
            }, function (r) {
                // 2.3.3.3.3
                if (!x) return;
                x = null;

                // 2.3.3.3.2
                settlePromise(p, $rejected, r);
            });
        } catch (err) {
            // 2.3.3.3.4.1
            if (x) {
                // 2.3.3.3.4.2
                settlePromise(p, $rejected, err);
                x = null;
            }
        }
    }

})();
