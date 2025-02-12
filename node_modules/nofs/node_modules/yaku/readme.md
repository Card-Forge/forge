<a href="http://promisesaplus.com/">
    <img src="http://promisesaplus.com/assets/logo-small.png" alt="Promises/A+ logo"
         title="Promises/A+ 1.1 compliant" align="right" />
</a>

# Overview

Yaku is full compatible with ES6's native [Promise][native], but much faster, and more error friendly.
If you want to learn how Promise works, read the minimum implementation [yaku.aplus][]. Without comments, it is only 80 lines of code (gzipped size is 0.5KB).
It only implements the `constructor` and `then`.

Yaku passed all the tests of [promises-aplus-tests][], [promises-es6-tests][], and even the [core-js tests][].

I am not an optimization freak, I try to keep the source code readable and maintainable.
I write this lib to research one of my data structure ideas: [docs/lazyTree.md][].

[![NPM version](https://badge.fury.io/js/yaku.svg)](http://badge.fury.io/js/yaku) [![Build Status](https://travis-ci.org/ysmood/yaku.svg)](https://travis-ci.org/ysmood/yaku) [![Deps Up to Date](https://david-dm.org/ysmood/yaku.svg?style=flat)](https://david-dm.org/ysmood/yaku) [![Coverage Status](https://coveralls.io/repos/ysmood/yaku/badge.svg?branch=master&service=github)](https://coveralls.io/github/ysmood/yaku?branch=master)



# Features

- The best for mobile, gzipped file is only 1.9KB
- Supports "uncaught rejection" and "long stack trace", [Comparison][docs/debugHelperComparison.md]
- Works on IE5+ and other major browsers
- 100% statement and branch test coverage
- Better CPU and memory performance than the native Promise
- Well commented source code with every Promises/A+ spec
- Highly modularized extra helpers, no pollution to its pure ES6 implements
- Supports ES7 `finally`



# Quick Start

## Node.js

```shell
npm install yaku
```

Then:

```js
var Promise = require('yaku');
```

Or if you don't want any extra debug helper, ES6 only version is here:

```js
var Promise = require('yaku/lib/yaku.core');
```

Or if you only want aplus support:

```js
var Promise = require('yaku/lib/yaku.aplus');
```

## Browser

Use something like [Browserify][] or [Webpack][], or download the `yaku.js` file from [release page][].
Raw usage without:

```html
<script type="text/javascript" src ="yaku.js"></script>
<script>
    // Yaku will be assigned to `window.Yaku`.
    var Promise = Yaku;
</script>
```



# Change Log

[docs/changelog.md](docs/changelog.md)



# Compare to Other Promise Libs

These comparisons only reflect some limited truth, no one is better than all others on all aspects.
There are tons of Promises/A+ implementations, you can see them [here](https://promisesaplus.com/implementations). Only some of the famous ones were tested.

```
Date: Sat Dec 17 2016 22:15:40 GMT+0800 (CST)
Node v7.2.1
OS   darwin
Arch x64
CPU  Intel(R) Core(TM) i7-4850HQ CPU @ 2.30GHz
```

| name | unit tests | coverage | 1ms async task | optional helpers | helpers | gzip |
| ---- | ---------- | -------- | -------------- | ---------------- | ------- | ---- |
| [yaku][]@0.17.4 | ✓ | 100% 100% | 221ms / 108MB | ✓ | 34 | 1.9KB |
| [yaku.core][]@0.17.4 | ✓ | 100% 100% | 217ms / 108MB | ✓ | 28 | 1.6KB |
| [yaku.aplus][]@0.17.4 | x (90 failed) | 100% 100% | 262ms / 116MB | ✓ | 7 | 0.5KB |
| [bluebird][]@3.4.6 | x (34 failed) | 99% 96% | 207ms / 81MB | partial | 102 | 15.9KB |
| [es6-promise][]@4.0.5 | x (52 failed) | ? ? | 432ms / 114MB | x | 12 | 2.4KB |
| [pinkie][]@2.0.4 | x (44 failed) | ? ? | 313ms / 135MB | ✓ | 10 | 1.2KB |
| [native][]@7.2.1 | ✓ | ? ? | 376ms / 134MB | x | 10 | 0KB |
| [core-js][]@2.4.1 | x (9 failed) | ? ? | 394ms / 142MB | x | 10 | 5KB |
| [es6-shim][]@0.35.2 | ✓ | ? ? | 390ms / 136MB | x | 10 | 15.5KB |
| [q][]@1.4.1 | x (42 failed) | ? ? | 1432ms / 370MB | x | 74 | 4.6KB |
| [my-promise][]@1.1.0 | x (10 failed) | ? ? | 786ms / 232MB | x | 10 | 3.9KB |

- **unit test**: [promises-aplus-tests][], [promises-es6-tests][], and even the [core-js tests][].

- **coverage**: statement coverage and branch coverage.

- **helpers**: extra methods that help with your promise programming, such as
  async flow control helpers, debug helpers. For more details: [docs/debugHelperComparison.md][].

- **1ms async task**: `npm run no -- benchmark`, the smaller the better (total time / memory rss).

- **promises-es6-tests**: If you want to test `bluebird` against promises-es6-tests,
  run `npm run no -- test-es6 --shim bluebird`.

- **optional helpers**: Whether the helpers can be imported separately or not,
  which means you can load the lib without helpers. Such as the `bluebird-core`, it will inevitably load
  some nonstandard helpers: `spread`, `promisify`, etc.


# FAQ

- `catch` on old browsers (IE7, IE8 etc)?

  > In ECMA-262 spec, `catch` cannot be used as method name. You have to alias the method name or use something like `Promise.resolve()['catch'](function() {})` or `Promise.resolve().then(null, function() {})`.

- When using with Babel and Regenerator, the unhandled rejection doesn't work.

  > Because Regenerator use global Promise directly and don't have an api to set the Promise lib.
  > You have to import Yaku globally to make it use Yaku: `require("yaku/lib/global");`.

- The name Yaku is weird?

  > The name `yaku` comes from the word `約束(yaku soku)` which means promise.


# Unhandled Rejection

Yaku will report any unhandled rejection via `console.error` by default, in case you forget to write `catch`.
You can catch them manually:

- Browser: `window.onunhandledrejection = ({ promise, reason }) => { /* Your Code */ };`
- Node: `process.on("unhandledRejection", (reason, promise) => { /* Your Code */ });`

For more spec read [Unhandled Rejection Tracking Browser Events](https://github.com/domenic/unhandled-rejections-browser-spec).


# API

- #### require('yaku')
  - [Yaku(executor)](#yakuexecutor)
  - [then(onFulfilled, onRejected)](#thenonfulfilled-onrejected)
  - [catch(onRejected)](#catchonrejected)
  - [finally(onFinally)](#finallyonfinally)
  - [Yaku.resolve(value)](#yakuresolvevalue)
  - [Yaku.reject(reason)](#yakurejectreason)
  - [Yaku.race(iterable)](#yakuraceiterable)
  - [Yaku.all(iterable)](#yakualliterable)
  - [Yaku.Symbol](#yakusymbol)
  - [Yaku.speciesConstructor(O, defaultConstructor)](#yakuspeciesconstructoro-defaultconstructor)
  - [Yaku.unhandledRejection(reason, p)](#yakuunhandledrejectionreason-p)
  - [Yaku.rejectionHandled(reason, p)](#yakurejectionhandledreason-p)
  - [Yaku.enableLongStackTrace](#yakuenablelongstacktrace)
  - [Yaku.nextTick](#yakunexttick)

- #### require('yaku/lib/utils')
  - [all(limit, list)](#alllimit-list)
  - [any(iterable)](#anyiterable)
  - [async(gen)](#asyncgen)
  - [callbackify(fn, self)](#callbackifyfn-self)
  - [Deferred](#deferred)
  - [flow(list)](#flowlist)
  - [guard(type, onRejected)](#guardtype-onrejected)
  - [if(cond, trueFn, falseFn)](#ifcond-truefn-falsefn)
  - [isPromise(obj)](#ispromiseobj)
  - [never()](#never)
  - [promisify(fn, self)](#promisifyfn-self)
  - [sleep(time, val)](#sleeptime-val)
  - [Observable](#observable)
  - [retry(countdown, span, fn, this)](#retrycountdown-span-fn-this)
  - [throw(err)](#throwerr)
  - [timeout(promise, time, reason)](#timeoutpromise-time-reason)

- #### require('yaku/lib/Observable')
  - [Observable(executor)](#observableexecutor)
  - [next(value)](#nextvalue)
  - [error(value)](#errorvalue)
  - [publisher](#publisher)
  - [subscribers](#subscribers)
  - [subscribe(onNext, onError)](#subscribeonnext-onerror)
  - [unsubscribe](#unsubscribe)
  - [Observable.merge(iterable)](#observablemergeiterable)

---------------------------------------


- ### **[Yaku(executor)](src/yaku.js?source#L49)**

    This class follows the [Promises/A+](https://promisesaplus.com) and
    [ES6](http://people.mozilla.org/~jorendorff/es6-draft.html#sec-promise-objects) spec
    with some extra helpers.

    - **<u>param</u>**: `executor` { _Function_ }

        Function object with two arguments resolve, reject.
        The first argument fulfills the promise, the second argument rejects it.
        We can call these functions, once our operation is completed.

- ### **[then(onFulfilled, onRejected)](src/yaku.js?source#L97)**

    Appends fulfillment and rejection handlers to the promise,
    and returns a new promise resolving to the return value of the called handler.

    - **<u>param</u>**: `onFulfilled` { _Function_ }

        Optional. Called when the Promise is resolved.

    - **<u>param</u>**: `onRejected` { _Function_ }

        Optional. Called when the Promise is rejected.

    - **<u>return</u>**: { _Yaku_ }

        It will return a new Yaku which will resolve or reject after

    - **<u>example</u>**:

        the current Promise.
        ```js
        var Promise = require('yaku');
        var p = Promise.resolve(10);

        p.then((v) => {
            console.log(v);
        });
        ```

- ### **[catch(onRejected)](src/yaku.js?source#L124)**

    The `catch()` method returns a Promise and deals with rejected cases only.
    It behaves the same as calling `Promise.prototype.then(undefined, onRejected)`.

    - **<u>param</u>**: `onRejected` { _Function_ }

        A Function called when the Promise is rejected.
        This function has one argument, the rejection reason.

    - **<u>return</u>**: { _Yaku_ }

        A Promise that deals with rejected cases only.

    - **<u>example</u>**:

        ```js
        var Promise = require('yaku');
        var p = Promise.reject(new Error("ERR"));

        p['catch']((v) => {
            console.log(v);
        });
        ```

- ### **[finally(onFinally)](src/yaku.js?source#L144)**

    Register a callback to be invoked when a promise is settled (either fulfilled or rejected).
    Similar with the try-catch-finally, it's often used for cleanup.

    - **<u>param</u>**: `onFinally` { _Function_ }

        A Function called when the Promise is settled.
        It will not receive any argument.

    - **<u>return</u>**: { _Yaku_ }

        A Promise that will reject if onFinally throws an error or returns a rejected promise.
        Else it will resolve previous promise's final state (either fulfilled or rejected).

    - **<u>example</u>**:

        ```js
        var Promise = require('yaku');
        var p = Math.random() > 0.5 ? Promise.resolve() : Promise.reject();
        p.finally(() => {
            console.log('finally');
        });
        ```

- ### **[Yaku.resolve(value)](src/yaku.js?source#L174)**

    The `Promise.resolve(value)` method returns a Promise object that is resolved with the given value.
    If the value is a thenable (i.e. has a then method), the returned promise will "follow" that thenable,
    adopting its eventual state; otherwise the returned promise will be fulfilled with the value.

    - **<u>param</u>**: `value` { _Any_ }

        Argument to be resolved by this Promise.
        Can also be a Promise or a thenable to resolve.

    - **<u>return</u>**: { _Yaku_ }

    - **<u>example</u>**:

        ```js
        var Promise = require('yaku');
        var p = Promise.resolve(10);
        ```

- ### **[Yaku.reject(reason)](src/yaku.js?source#L188)**

    The `Promise.reject(reason)` method returns a Promise object that is rejected with the given reason.

    - **<u>param</u>**: `reason` { _Any_ }

        Reason why this Promise rejected.

    - **<u>return</u>**: { _Yaku_ }

    - **<u>example</u>**:

        ```js
        var Promise = require('yaku');
        var p = Promise.reject(new Error("ERR"));
        ```

- ### **[Yaku.race(iterable)](src/yaku.js?source#L212)**

    The `Promise.race(iterable)` method returns a promise that resolves or rejects
    as soon as one of the promises in the iterable resolves or rejects,
    with the value or reason from that promise.

    - **<u>param</u>**: `iterable` { _iterable_ }

        An iterable object, such as an Array.

    - **<u>return</u>**: { _Yaku_ }

        The race function returns a Promise that is settled
        the same way as the first passed promise to settle.
        It resolves or rejects, whichever happens first.

    - **<u>example</u>**:

        ```js
        var Promise = require('yaku');
        Promise.race([
            123,
            Promise.resolve(0)
        ])
        .then((value) => {
            console.log(value); // => 123
        });
        ```

- ### **[Yaku.all(iterable)](src/yaku.js?source#L268)**

    The `Promise.all(iterable)` method returns a promise that resolves when
    all of the promises in the iterable argument have resolved.

    The result is passed as an array of values from all the promises.
    If something passed in the iterable array is not a promise,
    it's converted to one by Promise.resolve. If any of the passed in promises rejects,
    the all Promise immediately rejects with the value of the promise that rejected,
    discarding all the other promises whether or not they have resolved.

    - **<u>param</u>**: `iterable` { _iterable_ }

        An iterable object, such as an Array.

    - **<u>return</u>**: { _Yaku_ }

    - **<u>example</u>**:

        ```js
        var Promise = require('yaku');
        Promise.all([
            123,
            Promise.resolve(0)
        ])
        .then((values) => {
            console.log(values); // => [123, 0]
        });
        ```

    - **<u>example</u>**:

        Use with iterable.
        ```js
        var Promise = require('yaku');
        Promise.all((function * () {
            yield 10;
            yield new Promise(function (r) { setTimeout(r, 1000, "OK") });
        })())
        .then((values) => {
            console.log(values); // => [123, 0]
        });
        ```

- ### **[Yaku.Symbol](src/yaku.js?source#L304)**

    The ES6 Symbol object that Yaku should use, by default it will use the
    global one.

    - **<u>type</u>**: { _Object_ }

    - **<u>example</u>**:

        ```js
        var core = require("core-js/library");
        var Promise = require("yaku");
        Promise.Symbol = core.Symbol;
        ```

- ### **[Yaku.speciesConstructor(O, defaultConstructor)](src/yaku.js?source#L319)**

    Use this api to custom the species behavior.
    https://tc39.github.io/ecma262/#sec-speciesconstructor

    - **<u>param</u>**: `O` { _Any_ }

        The current this object.

    - **<u>param</u>**: `defaultConstructor` { _Function_ }

- ### **[Yaku.unhandledRejection(reason, p)](src/yaku.js?source#L345)**

    Catch all possibly unhandled rejections. If you want to use specific
    format to display the error stack, overwrite it.
    If it is set, auto `console.error` unhandled rejection will be disabled.

    - **<u>param</u>**: `reason` { _Any_ }

        The rejection reason.

    - **<u>param</u>**: `p` { _Yaku_ }

        The promise that was rejected.

    - **<u>example</u>**:

        ```js
        var Promise = require('yaku');
        Promise.unhandledRejection = (reason) => {
            console.error(reason);
        };

        // The console will log an unhandled rejection error message.
        Promise.reject('my reason');

        // The below won't log the unhandled rejection error message.
        Promise.reject('v').catch(() => {});
        ```

- ### **[Yaku.rejectionHandled(reason, p)](src/yaku.js?source#L360)**

    Emitted whenever a Promise was rejected and an error handler was
    attached to it (for example with `.catch()`) later than after an event loop turn.

    - **<u>param</u>**: `reason` { _Any_ }

        The rejection reason.

    - **<u>param</u>**: `p` { _Yaku_ }

        The promise that was rejected.

- ### **[Yaku.enableLongStackTrace](src/yaku.js?source#L378)**

    It is used to enable the long stack trace.
    Once it is enabled, it can't be reverted.
    While it is very helpful in development and testing environments,
    it is not recommended to use it in production. It will slow down
    application and eat up memory.
    It will add an extra property `longStack` to the Error object.

    - **<u>example</u>**:

        ```js
        var Promise = require('yaku');
        Promise.enableLongStackTrace();
        Promise.reject(new Error("err")).catch((err) => {
            console.log(err.longStack);
        });
        ```

- ### **[Yaku.nextTick](src/yaku.js?source#L401)**

    Only Node has `process.nextTick` function. For browser there are
    so many ways to polyfill it. Yaku won't do it for you, instead you
    can choose what you prefer. For example, this project
    [next-tick](https://github.com/medikoo/next-tick).
    By default, Yaku will use `process.nextTick` on Node, `setTimeout` on browser.

    - **<u>type</u>**: { _Function_ }

    - **<u>example</u>**:

        ```js
        var Promise = require('yaku');
        Promise.nextTick = require('next-tick');
        ```

    - **<u>example</u>**:

        You can even use sync resolution if you really know what you are doing.
        ```js
        var Promise = require('yaku');
        Promise.nextTick = fn => fn();
        ```





# Utils

It's a bundle of all the following functions. You can require them all with `var yutils = require("yaku/lib/utils")`,
or require them separately like `require("yaku/lib/flow")`. If you want to use it in the browser, you have to use `browserify` or `webpack`. You can even use another Promise lib, such as:

```js
require("yaku/lib/_").Promise = require("bluebird");
var source = require("yaku/lib/source");

// now "source" use bluebird instead of yaku.
```

- ### **[all(limit, list)](src/utils.js?source#L46)**

    A function that helps run functions under a concurrent limitation.
    To run functions sequentially, use `yaku/lib/flow`.

    - **<u>param</u>**: `limit` { _Int_ }

        The max task to run at a time. It's optional.
        Default is `Infinity`.

    - **<u>param</u>**: `list` { _Iterable_ }

        Any [iterable](https://developer.mozilla.org/en/docs/Web/JavaScript/Reference/Iteration_protocols) object. It should be a lazy iteralbe object,
        don't pass in a normal Array with promises.

    - **<u>return</u>**: { _Promise_ }

    - **<u>example</u>**:

        ```js
        var kit = require('nokit');
        var all = require('yaku/lib/all');

        var urls = [
            'http://a.com',
            'http://b.com',
            'http://c.com',
            'http://d.com'
        ];
        var tasks = function * () {
            var i = 0;
            yield kit.request(url[i++]);
            yield kit.request(url[i++]);
            yield kit.request(url[i++]);
            yield kit.request(url[i++]);
        }();

        all(tasks).then(() => kit.log('all done!'));

        all(2, tasks).then(() => kit.log('max concurrent limit is 2'));

        all(3, { next: () => {
            var url = urls.pop();
            return {
                 done: !url,
                 value: url && kit.request(url)
            };
        } })
        .then(() => kit.log('all done!'));
        ```

- ### **[any(iterable)](src/utils.js?source#L65)**

    Similar with the `Promise.race`, but only rejects when every entry rejects.

    - **<u>param</u>**: `iterable` { _iterable_ }

        An iterable object, such as an Array.

    - **<u>return</u>**: { _Yaku_ }

    - **<u>example</u>**:

        ```js
        var any = require('yaku/lib/any');
        any([
            123,
            Promise.resolve(0),
            Promise.reject(new Error("ERR"))
        ])
        .then((value) => {
            console.log(value); // => 123
        });
        ```

- ### **[async(gen)](src/utils.js?source#L85)**

    Generator based async/await wrapper.

    - **<u>param</u>**: `gen` { _Generator_ }

        A generator function

    - **<u>return</u>**: { _Yaku_ }

    - **<u>example</u>**:

        ```js
        var async = require('yaku/lib/async');
        var sleep = require('yaku/lib/sleep');

        var fn = async(function * () {
            return yield sleep(1000, 'ok');
        });

        fn().then(function (v) {
            console.log(v);
        });
        ```

- ### **[callbackify(fn, self)](src/utils.js?source#L94)**

    If a function returns promise, convert it to
    node callback style function.

    - **<u>param</u>**: `fn` { _Function_ }

    - **<u>param</u>**: `self` { _Any_ }

        The `this` to bind to the fn.

    - **<u>return</u>**: { _Function_ }

- ### **[Deferred](src/utils.js?source#L100)**

    **deprecate** Create a `jQuery.Deferred` like object.
    It will cause some buggy problems, please don't use it.

- ### **[flow(list)](src/utils.js?source#L158)**

    Creates a function that is the composition of the provided functions.
    See `yaku/lib/async`, if you need concurrent support.

    - **<u>param</u>**: `list` { _Iterable_ }

        Any [iterable](https://developer.mozilla.org/en/docs/Web/JavaScript/Reference/Iteration_protocols) object. It should be a lazy iteralbe object,
        don't pass in a normal Array with promises.

    - **<u>return</u>**: { _Function_ }

        `(val) -> Promise` A function that will return a promise.

    - **<u>example</u>**:

        It helps to decouple sequential pipeline code logic.
        ```js
        var kit = require('nokit');
        var flow = require('yaku/lib/flow');

        function createUrl (name) {
            return "http://test.com/" + name;
        }

        function curl (url) {
            return kit.request(url).then((body) => {
                kit.log('get');
                return body;
            });
        }

        function save (str) {
            kit.outputFile('a.txt', str).then(() => {
                kit.log('saved');
            });
        }

        var download = flow(createUrl, curl, save);
        // same as "download = flow([createUrl, curl, save])"

        download('home');
        ```

    - **<u>example</u>**:

        Walk through first link of each page.
        ```js
        var kit = require('nokit');
        var flow = require('yaku/lib/flow');

        var list = [];
        function iter (url) {
            return {
                done: !url,
                value: url && kit.request(url).then((body) => {
                    list.push(body);
                    var m = body.match(/href="(.+?)"/);
                    if (m) return m[0];
                });
            };
        }

        var walker = flow(iter);
        walker('test.com');
        ```

- ### **[guard(type, onRejected)](src/utils.js?source#L187)**

    Enable a helper to catch specific error type.
    It will be directly attach to the prototype of the promise.

    - **<u>param</u>**: `type` { _class_ }

    - **<u>param</u>**: `onRejected` { _Function_ }

    - **<u>return</u>**: { _Promise_ }

        ```js
        var Promise = require('yaku');
        require('yaku/lib/guard');

        class AnError extends Error {
        }

        Promise.reject(new AnError('hey'))
        .guard(AnError, (err) => {
             // only log AnError type
             console.log(err);
        })
        .then(() => {
             console.log('done');
        })
        .guard(Error, (err) => {
             // log all error type
             console.log(err)
        });
        ```

- ### **[if(cond, trueFn, falseFn)](src/utils.js?source#L207)**

    if-else helper

    - **<u>param</u>**: `cond` { _Promise_ }

    - **<u>param</u>**: `trueFn` { _Function_ }

    - **<u>param</u>**: `falseFn` { _Function_ }

    - **<u>return</u>**: { _Promise_ }

    - **<u>example</u>**:

        ```js
        var Promise = require('yaku');
        var yutils = require('yaku/lib/utils');

        yutils.if(Promise.resolve(false), () => {
            // true
        }, () => {
            // false
        })
        ```

- ### **[isPromise(obj)](src/utils.js?source#L215)**

    **deprecate** Check if an object is a promise-like object.
    Don't use it to coercive a value to Promise, instead use `Promise.resolve`.

    - **<u>param</u>**: `obj` { _Any_ }

    - **<u>return</u>**: { _Boolean_ }

- ### **[never()](src/utils.js?source#L221)**

    Create a promise that never ends.

    - **<u>return</u>**: { _Promise_ }

        A promise that will end the current pipeline.

- ### **[promisify(fn, self)](src/utils.js?source#L250)**

    Convert a node callback style function to a function that returns
    promise when the last callback is not supplied.

    - **<u>param</u>**: `fn` { _Function_ }

    - **<u>param</u>**: `self` { _Any_ }

        The `this` to bind to the fn.

    - **<u>return</u>**: { _Function_ }

    - **<u>example</u>**:

        ```js
        var promisify = require('yaku/lib/promisify');
        function foo (val, cb) {
            setTimeout(() => {
                cb(null, val + 1);
            });
        }

        var bar = promisify(foo);

        bar(0).then((val) => {
            console.log val // output => 1
        });

        // It also supports the callback style.
        bar(0, (err, val) => {
            console.log(val); // output => 1
        });
        ```

- ### **[sleep(time, val)](src/utils.js?source#L263)**

    Create a promise that will wait for a while before resolution.

    - **<u>param</u>**: `time` { _Integer_ }

        The unit is millisecond.

    - **<u>param</u>**: `val` { _Any_ }

        What the value this promise will resolve.

    - **<u>return</u>**: { _Promise_ }

    - **<u>example</u>**:

        ```js
        var sleep = require('yaku/lib/sleep');
        sleep(1000).then(() => console.log('after one second'));
        ```

- ### **[Observable](src/utils.js?source#L269)**

    Read the `Observable` section.

    - **<u>type</u>**: { _Function_ }

- ### **[retry(countdown, span, fn, this)](src/utils.js?source#L319)**

    Retry a function until it resolves before a mount of times, or reject with all
    the error states.

    - **<u>version_added</u>**:

        v0.7.10

    - **<u>param</u>**: `countdown` { _Number | Function_ }

        How many times to retry before rejection.

    - **<u>param</u>**: `span` { _Number_ }

        Optional. How long to wait before each retry in millisecond.
        When it's a function `(errs) => Boolean | Promise.resolve(Boolean)`,
        you can use it to create complex countdown logic,
        it can even return a promise to create async countdown logic.

    - **<u>param</u>**: `fn` { _Function_ }

        The function can return a promise or not.

    - **<u>param</u>**: `this` { _Any_ }

        Optional. The context to call the function.

    - **<u>return</u>**: { _Function_ }

        The wrapped function. The function will reject an array
        of reasons that throwed by each try.

    - **<u>example</u>**:

        Retry 3 times before rejection, wait 1 second before each retry.
        ```js
        var retry = require('yaku/lib/retry');
        var { request } = require('nokit');

        retry(3, 1000, request)('http://test.com').then(
           (body) => console.log(body),
           (errs) => console.error(errs)
        );
        ```

    - **<u>example</u>**:

        Here a more complex retry usage, it shows an random exponential backoff algorithm to
        wait and retry again, which means the 10th attempt may take 10 minutes to happen.
        ```js
        var retry = require('yaku/lib/retry');
        var sleep = require('yaku/lib/sleep');
        var { request } = require('nokit');

        function countdown (retries) {
           var attempt = 0;
           return async () => {
                var r = Math.random() * Math.pow(2, attempt) * 1000;
                var t = Math.min(r, 1000 * 60 * 10);
                await sleep(t);
                return attempt++ < retries;
           };
        }

        retry(countdown(10), request)('http://test.com').then(
           (body) => console.log(body),
           (errs) => console.error(errs)
        );
        ```

- ### **[throw(err)](src/utils.js?source#L333)**

    Throw an error to break the program.

    - **<u>param</u>**: `err` { _Any_ }

    - **<u>example</u>**:

        ```js
        var ythrow = require('yaku/lib/throw');
        Promise.resolve().then(() => {
            // This error won't be caught by promise.
            ythrow('break the program!');
        });
        ```

- ### **[timeout(promise, time, reason)](src/utils.js?source#L351)**

    Create a promise that will reject after a while if the passed in promise
    doesn't settle first.

    - **<u>param</u>**: `promise` { _Promise_ }

        The passed promise to wait.

    - **<u>param</u>**: `time` { _Integer_ }

        The unit is millisecond.

    - **<u>param</u>**: `reason` { _Any_ }

        After time out, it will be the reject reason.

    - **<u>return</u>**: { _Promise_ }

    - **<u>example</u>**:

        ```js
        var sleep = require('yaku/lib/sleep');
        var timeout = require('yaku/lib/timeout');
        timeout(sleep(500), 100).catch((err) => {
            console.error(err);
        });
        ```




# Observable

- ### **[Observable(executor)](src/Observable.js?source#L60)**

    Create a composable observable object.
    Promise can't resolve multiple times, this class makes it possible, so
    that you can easily map, filter and even back pressure events in a promise way.
    For live example: [Double Click Demo](https://jsbin.com/niwuti/edit?html,js,output).

    - **<u>version_added</u>**:

        v0.7.2

    - **<u>param</u>**: `executor` { _Function_ }

        `(next) ->` It's optional.

    - **<u>return</u>**: { _Observable_ }

    - **<u>example</u>**:

        ```js
        var Observable = require("yaku/lib/Observable");
        var linear = new Observable();

        var x = 0;
        setInterval(linear.next, 1000, x++);

        // Wait for 2 sec then emit the next value.
        var quad = linear.subscribe(async x => {
            await sleep(2000);
            return x * x;
        });

        var another = linear.subscribe(x => -x);

        quad.subscribe(
            value => { console.log(value); },
            reason => { console.error(reason); }
        );

        // Emit error
        linear.error(new Error("reason"));

        // Unsubscribe an observable.
        quad.unsubscribe();

        // Unsubscribe all subscribers.
        linear.subscribers = [];
        ```

    - **<u>example</u>**:

        Use it with DOM.
        ```js
        var filter = fn => v => fn(v) ? v : new Promise(() => {});

        var keyup = new Observable((next) => {
            document.querySelector('input').onkeyup = next;
        });

        var keyupText = keyup.subscribe(e => e.target.value);

        // Now we only get the input when the text length is greater than 3.
        var keyupTextGT3 = keyupText.subscribe(filter(text => text.length > 3));

        keyupTextGT3.subscribe(v => console.log(v));
        ```

- ### **[next(value)](src/Observable.js?source#L77)**

    Emit a value.

    - **<u>param</u>**: `value` { _Any_ }

        so that the event will go to `onError` callback.

- ### **[error(value)](src/Observable.js?source#L83)**

    Emit an error.

    - **<u>param</u>**: `value` { _Any_ }

- ### **[publisher](src/Observable.js?source#L89)**

    The publisher observable of this.

    - **<u>type</u>**: { _Observable_ }

- ### **[subscribers](src/Observable.js?source#L95)**

    All the subscribers subscribed this observable.

    - **<u>type</u>**: { _Array_ }

- ### **[subscribe(onNext, onError)](src/Observable.js?source#L103)**

    It will create a new Observable, like promise.

    - **<u>param</u>**: `onNext` { _Function_ }

    - **<u>param</u>**: `onError` { _Function_ }

    - **<u>return</u>**: { _Observable_ }

- ### **[unsubscribe](src/Observable.js?source#L118)**

    Unsubscribe this.

- ### **[Observable.merge(iterable)](src/Observable.js?source#L173)**

    Merge multiple observables into one.

    - **<u>version_added</u>**:

        0.9.6

    - **<u>param</u>**: `iterable` { _Iterable_ }

    - **<u>return</u>**: { _Observable_ }

    - **<u>example</u>**:

        ```js
        var Observable = require("yaku/lib/Observable");
        var sleep = require("yaku/lib/sleep");

        var src = new Observable(next => setInterval(next, 1000, 0));

        var a = src.subscribe(v => v + 1; });
        var b = src.subscribe((v) => sleep(10, v + 2));

        var out = Observable.merge([a, b]);

        out.subscribe((v) => {
            console.log(v);
        })
        ```





# Unit Test

This project use [promises-aplus-tests][] to test the compliance of Promises/A+ specification. There are about 900 test cases.

Use `npm run no -- test` to run the unit test against yaku.

## Test other libs

### basic test

To test `bluebird`: `npm run no -- test-basic --shim bluebird`

The `bluebird` can be replaced with other lib, see the `test/getPromise.js` for which libs are supported.

### aplus test

To test `bluebird`: `npm run no -- test-aplus --shim bluebird`

The `bluebird` can be replaced with other lib, see the `test/getPromise.js` for which libs are supported.

### es6 test

To test `bluebird`: `npm run no -- test-es6 --shim bluebird`

The `bluebird` can be replaced with other lib, see the `test/getPromise.js` for which libs are supported.


# Benchmark

Use `npm run no -- benchmark` to run the benchmark.

## async/await generator wrapper

```
Node v5.6.0
OS   darwin
Arch x64
CPU  Intel(R) Core(TM) i7-4770HQ CPU @ 2.20GHz

yaku: 117ms
co: 283ms
bluebird: 643ms
```

# Contribution

Make sure you have `npm` and `npm install` at the root of the project first.

Other than use `gulp`, all my projects use [nokit][] to deal with automation.
Run `npm run no -- -h` to print all the tasks that you can use.

## Update `readme.md`

Please don't alter the `readme.md` directly, it is compiled from the `docs/readme.jst.md`.
Edit the `docs/readme.jst.md` and execute `npm run no` to rebuild the project.

[docs/lazyTree.md]: docs/lazyTree.md
[docs/debugHelperComparison.md]: docs/debugHelperComparison.md
[Bluebird]: https://github.com/petkaantonov/bluebird
[ES6-promise]: https://github.com/jakearchibald/es6-promise
[pinkie]: https://github.com/floatdrop/pinkie
[core-js tests]: https://github.com/ysmood/core-js/tree/promise-yaku
[native]: http://people.mozilla.org/~jorendorff/es6-draft.html#sec-promise-objects
[q]: https://github.com/kriskowal/q
[my-promise]: https://github.com/hax/my-promise
[core-js]: https://github.com/zloirock/core-js
[yaku]: https://github.com/ysmood/yaku
[yaku.core]: src/yaku.core.js
[yaku.aplus]: src/yaku.aplus.js
[es6-shim]: https://github.com/paulmillr/es6-shim
[release page]: https://github.com/ysmood/yaku/releases
[docs/minPromiseAplus.js]: docs/minPromiseAplus.js
[promises-aplus-tests]: https://github.com/promises-aplus/promises-tests
[promises-es6-tests]: https://github.com/promises-es6/promises-es6
[longjohn]: https://github.com/mattinsler/longjohn
[crhome-lst]: http://www.html5rocks.com/en/tutorials/developertools/async-call-stack
[Browserify]: http://browserify.org
[Webpack]: http://webpack.github.io/
[nokit]: https://github.com/ysmood/nokit
[nofile.js]: nofile.js