// This file contains all the non-ES6-standard helpers based on promise.

module.exports = {

    /**
     * A function that helps run functions under a concurrent limitation.
     * To run functions sequentially, use `yaku/lib/flow`.
     * @param  {Int} limit The max task to run at a time. It's optional.
     * Default is `Infinity`.
     * @param  {Iterable} list Any [iterable](https://developer.mozilla.org/en/docs/Web/JavaScript/Reference/Iteration_protocols) object. It should be a lazy iteralbe object,
     * don't pass in a normal Array with promises.
     * @return {Promise}
     * @example
     * ```js
     * var kit = require('nokit');
     * var all = require('yaku/lib/all');
     *
     * var urls = [
     *     'http://a.com',
     *     'http://b.com',
     *     'http://c.com',
     *     'http://d.com'
     * ];
     * var tasks = function * () {
     *     var i = 0;
     *     yield kit.request(url[i++]);
     *     yield kit.request(url[i++]);
     *     yield kit.request(url[i++]);
     *     yield kit.request(url[i++]);
     * }();
     *
     * all(tasks).then(() => kit.log('all done!'));
     *
     * all(2, tasks).then(() => kit.log('max concurrent limit is 2'));
     *
     * all(3, { next: () => {
     *     var url = urls.pop();
     *     return {
     *          done: !url,
     *          value: url && kit.request(url)
     *     };
     * } })
     * .then(() => kit.log('all done!'));
     * ```
     */
    all: require("./all"),

    /**
     * Similar with the `Promise.race`, but only rejects when every entry rejects.
     * @param  {iterable} iterable An iterable object, such as an Array.
     * @return {Yaku}
     * @example
     * ```js
     * var any = require('yaku/lib/any');
     * any([
     *     123,
     *     Promise.resolve(0),
     *     Promise.reject(new Error("ERR"))
     * ])
     * .then((value) => {
     *     console.log(value); // => 123
     * });
     * ```
     */
    any: require("./any"),

    /**
     * Generator based async/await wrapper.
     * @param  {Generator} gen A generator function
     * @return {Yaku}
     * @example
     * ```js
     * var async = require('yaku/lib/async');
     * var sleep = require('yaku/lib/sleep');
     *
     * var fn = async(function * () {
     *     return yield sleep(1000, 'ok');
     * });
     *
     * fn().then(function (v) {
     *     console.log(v);
     * });
     * ```
     */
    async: require("./async"),

    /**
     * If a function returns promise, convert it to
     * node callback style function.
     * @param  {Function} fn
     * @param  {Any} self The `this` to bind to the fn.
     * @return {Function}
     */
    callbackify: require("./callbackify"),

    /**
     * **deprecate** Create a `jQuery.Deferred` like object.
     * It will cause some buggy problems, please don't use it.
     */
    Deferred: require("./Deferred"),

    /**
     * Creates a function that is the composition of the provided functions.
     * See `yaku/lib/async`, if you need concurrent support.
     * @param  {Iterable} list Any [iterable](https://developer.mozilla.org/en/docs/Web/JavaScript/Reference/Iteration_protocols) object. It should be a lazy iteralbe object,
     * don't pass in a normal Array with promises.
     * @return {Function} `(val) -> Promise` A function that will return a promise.
     * @example
     * It helps to decouple sequential pipeline code logic.
     * ```js
     * var kit = require('nokit');
     * var flow = require('yaku/lib/flow');
     *
     * function createUrl (name) {
     *     return "http://test.com/" + name;
     * }
     *
     * function curl (url) {
     *     return kit.request(url).then((body) => {
     *         kit.log('get');
     *         return body;
     *     });
     * }
     *
     * function save (str) {
     *     kit.outputFile('a.txt', str).then(() => {
     *         kit.log('saved');
     *     });
     * }
     *
     * var download = flow(createUrl, curl, save);
     * // same as "download = flow([createUrl, curl, save])"
     *
     * download('home');
     * ```
     * @example
     * Walk through first link of each page.
     * ```js
     * var kit = require('nokit');
     * var flow = require('yaku/lib/flow');
     *
     * var list = [];
     * function iter (url) {
     *     return {
     *         done: !url,
     *         value: url && kit.request(url).then((body) => {
     *             list.push(body);
     *             var m = body.match(/href="(.+?)"/);
     *             if (m) return m[0];
     *         });
     *     };
     * }
     *
     * var walker = flow(iter);
     * walker('test.com');
     * ```
     */
    flow: require("./flow"),

    /**
     * Enable a helper to catch specific error type.
     * It will be directly attach to the prototype of the promise.
     * @param  {class}    type
     * @param  {Function} onRejected
     * @return {Promise}
     * ```js
     * var Promise = require('yaku');
     * require('yaku/lib/guard');
     *
     * class AnError extends Error {
     * }
     *
     * Promise.reject(new AnError('hey'))
     * .guard(AnError, (err) => {
     *      // only log AnError type
     *      console.log(err);
     * })
     * .then(() => {
     *      console.log('done');
     * })
     * .guard(Error, (err) => {
     *      // log all error type
     *      console.log(err)
     * });
     * ```
     */
    guard: require("./guard"),

    /**
     * if-else helper
     * @param  {Promise} cond
     * @param  {Function} trueFn
     * @param  {Function} falseFn
     * @return {Promise}
     * @example
     * ```js
      * var Promise = require('yaku');
      * var yutils = require('yaku/lib/utils');
      *
      * yutils.if(Promise.resolve(false), () => {
      *     // true
      * }, () => {
      *     // false
      * })
     * ```
     */
    "if": require("./if"),

    /**
     * **deprecate** Check if an object is a promise-like object.
     * Don't use it to coercive a value to Promise, instead use `Promise.resolve`.
     * @param  {Any}  obj
     * @return {Boolean}
     */
    isPromise: require("./isPromise"),

    /**
     * Create a promise that never ends.
     * @return {Promise} A promise that will end the current pipeline.
     */
    never: require("./never"),

    /**
     * Convert a node callback style function to a function that returns
     * promise when the last callback is not supplied.
     * @param  {Function} fn
     * @param  {Any} self The `this` to bind to the fn.
     * @return {Function}
     * @example
     * ```js
     * var promisify = require('yaku/lib/promisify');
     * function foo (val, cb) {
     *     setTimeout(() => {
     *         cb(null, val + 1);
     *     });
     * }
     *
     * var bar = promisify(foo);
     *
     * bar(0).then((val) => {
     *     console.log val // output => 1
     * });
     *
     * // It also supports the callback style.
     * bar(0, (err, val) => {
     *     console.log(val); // output => 1
     * });
     * ```
     */
    promisify: require("./promisify"),

    /**
     * Create a promise that will wait for a while before resolution.
     * @param  {Integer} time The unit is millisecond.
     * @param  {Any} val What the value this promise will resolve.
     * @return {Promise}
     * @example
     * ```js
     * var sleep = require('yaku/lib/sleep');
     * sleep(1000).then(() => console.log('after one second'));
     * ```
     */
    sleep: require("./sleep"),

    /**
     * Read the `Observable` section.
     * @type {Function}
     */
    Observable: require("./Observable"),

    /**
     * Retry a function until it resolves before a mount of times, or reject with all
     * the error states.
     * @version_added v0.7.10
     * @param  {Number | Function} countdown How many times to retry before rejection.
     * @param  {Number} span Optional. How long to wait before each retry in millisecond.
     * When it's a function `(errs) => Boolean | Promise.resolve(Boolean)`,
     * you can use it to create complex countdown logic,
     * it can even return a promise to create async countdown logic.
     * @param  {Function} fn The function can return a promise or not.
     * @param  {Any} this Optional. The context to call the function.
     * @return {Function} The wrapped function. The function will reject an array
     * of reasons that throwed by each try.
     * @example
     * Retry 3 times before rejection, wait 1 second before each retry.
     * ```js
     * var retry = require('yaku/lib/retry');
     * var { request } = require('nokit');
     *
     * retry(3, 1000, request)('http://test.com').then(
     *    (body) => console.log(body),
     *    (errs) => console.error(errs)
     * );
     * ```
     * @example
     * Here a more complex retry usage, it shows an random exponential backoff algorithm to
     * wait and retry again, which means the 10th attempt may take 10 minutes to happen.
     * ```js
     * var retry = require('yaku/lib/retry');
     * var sleep = require('yaku/lib/sleep');
     * var { request } = require('nokit');
     *
     * function countdown (retries) {
     *    var attempt = 0;
     *    return async () => {
     *         var r = Math.random() * Math.pow(2, attempt) * 1000;
     *         var t = Math.min(r, 1000 * 60 * 10);
     *         await sleep(t);
     *         return attempt++ < retries;
     *    };
     * }
     *
     * retry(countdown(10), request)('http://test.com').then(
     *    (body) => console.log(body),
     *    (errs) => console.error(errs)
     * );
     * ```
     */
    retry: require("./retry"),

    /**
     * Throw an error to break the program.
     * @param  {Any} err
     * @example
     * ```js
     * var ythrow = require('yaku/lib/throw');
     * Promise.resolve().then(() => {
     *     // This error won't be caught by promise.
     *     ythrow('break the program!');
     * });
     * ```
     */
    "throw": require("./throw"),

    /**
     * Create a promise that will reject after a while if the passed in promise
     * doesn't settle first.
     * @param  {Promise} promise The passed promise to wait.
     * @param  {Integer} time The unit is millisecond.
     * @param  {Any} reason After time out, it will be the reject reason.
     * @return {Promise}
     * @example
     * ```js
     * var sleep = require('yaku/lib/sleep');
     * var timeout = require('yaku/lib/timeout');
     * timeout(sleep(500), 100)["catch"]((err) => {
     *     console.error(err);
     * });
     * ```
     */
    timeout: require("./timeout")
};
