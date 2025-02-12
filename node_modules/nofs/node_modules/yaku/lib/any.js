var _ = require("./_");
var genIterator = require("./genIterator");

module.exports = function (iterable) {
    var iter = genIterator(iterable);

    return new _.Promise(function (resolve, reject) {
        var countDown = 0
        , reasons = []
        , item;

        function onError (reason) {
            reasons.push(reason);
            if (!--countDown)
                reject(reasons);
        }

        while (!(item = iter.next()).done) {
            countDown++;
            _.Promise.resolve(item.value).then(resolve, onError);
        }
    });
};
