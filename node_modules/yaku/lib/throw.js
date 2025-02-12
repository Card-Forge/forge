module.exports = function (err) {
    setTimeout(function () {
        throw err;
    });
};
