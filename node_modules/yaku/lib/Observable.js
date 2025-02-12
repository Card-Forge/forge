var _ = require("./_");
var genIterator = require("./genIterator");
var Promise = _.Promise;

/**
 * Create a composable observable object.
 * Promise can't resolve multiple times, this class makes it possible, so
 * that you can easily map, filter and even back pressure events in a promise way.
 * For live example: [Double Click Demo](https://jsbin.com/niwuti/edit?html,js,output).
 * @version_added v0.7.2
 * @param {Function} executor `(next) ->` It's optional.
 * @return {Observable}
 * @example
 * ```js
 * var Observable = require("yaku/lib/Observable");
 * var linear = new Observable();
 *
 * var x = 0;
 * setInterval(linear.next, 1000, x++);
 *
 * // Wait for 2 sec then emit the next value.
 * var quad = linear.subscribe(async x => {
 *     await sleep(2000);
 *     return x * x;
 * });
 *
 * var another = linear.subscribe(x => -x);
 *
 * quad.subscribe(
 *     value => { console.log(value); },
 *     reason => { console.error(reason); }
 * );
 *
 * // Emit error
 * linear.error(new Error("reason"));
 *
 * // Unsubscribe an observable.
 * quad.unsubscribe();
 *
 * // Unsubscribe all subscribers.
 * linear.subscribers = [];
 * ```
 * @example
 * Use it with DOM.
 * ```js
 * var filter = fn => v => fn(v) ? v : new Promise(() => {});
 *
 * var keyup = new Observable((next) => {
 *     document.querySelector('input').onkeyup = next;
 * });
 *
 * var keyupText = keyup.subscribe(e => e.target.value);
 *
 * // Now we only get the input when the text length is greater than 3.
 * var keyupTextGT3 = keyupText.subscribe(filter(text => text.length > 3));
 *
 * keyupTextGT3.subscribe(v => console.log(v));
 * ```
 */
var Observable = module.exports = function Observable (executor) {
    var self = this;

    genHandler(self);

    self.subscribers = [];

    executor && executor(self.next, self.error);
};

_.extendPrototype(Observable, {

    /**
     * Emit a value.
     * @param  {Any} value
     * so that the event will go to `onError` callback.
     */
    next: null,

    /**
     * Emit an error.
     * @param  {Any} value
     */
    error: null,

    /**
     * The publisher observable of this.
     * @type {Observable}
     */
    publisher: null,

    /**
     * All the subscribers subscribed this observable.
     * @type {Array}
     */
    subscribers: null,

    /**
     * It will create a new Observable, like promise.
     * @param  {Function} onNext
     * @param  {Function} onError
     * @return {Observable}
     */
    subscribe: function (onNext, onError) {
        var self = this, subscriber = new Observable();
        subscriber._onNext = onNext;
        subscriber._onError = onError;
        subscriber._nextErr = genNextErr(subscriber.next);

        subscriber.publisher = self;
        self.subscribers.push(subscriber);

        return subscriber;
    },

    /**
     * Unsubscribe this.
     */
    unsubscribe: function () {
        var publisher = this.publisher;
        publisher && publisher.subscribers.splice(publisher.subscribers.indexOf(this), 1);
    }

});

function genHandler (self) {
    self.next = function (val) {
        var i = 0, len = self.subscribers.length, subscriber;
        while (i < len) {
            subscriber = self.subscribers[i++];
            Promise.resolve(val).then(
                subscriber._onNext,
                subscriber._onError
            ).then(
                subscriber.next,
                subscriber._nextErr
            );
        }
    };

    self.error = function (err) {
        self.next(Promise.reject(err));
    };
}

function genNextErr (next) {
    return function (reason) {
        next(Promise.reject(reason));
    };
}

/**
 * Merge multiple observables into one.
 * @version_added 0.9.6
 * @param  {Iterable} iterable
 * @return {Observable}
 * @example
 * ```js
 * var Observable = require("yaku/lib/Observable");
 * var sleep = require("yaku/lib/sleep");
 *
 * var src = new Observable(next => setInterval(next, 1000, 0));
 *
 * var a = src.subscribe(v => v + 1; });
 * var b = src.subscribe((v) => sleep(10, v + 2));
 *
 * var out = Observable.merge([a, b]);
 *
 * out.subscribe((v) => {
 *     console.log(v);
 * })
 * ```
 */
Observable.merge = function merge (iterable) {
    var iter = genIterator(iterable);
    return new Observable(function (next) {
        var item;

        function onError (e) {
            next(Promise.reject(e));
        }

        while (!(item = iter.next()).done) {
            item.value.subscribe(next, onError);
        }
    });
};

