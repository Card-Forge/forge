"use strict";

var brush = require("./brush");
var util = require("util");
var utils = require("./utils");

var _util = util;
var inspect = _util.inspect;
var _utils = utils;
var isArray = _utils.isArray;


var regCleanStack = /^.+(\/node_modules\/|(node\.js:\d+:\d+)).+\n?/mg;
var regIndent = /^/mg;

function indent (str) {
    return (str + "").replace(regIndent, "  ");
}

function log (type, mode) {
    return function () {
        switch (mode) {

        /* istanbul ignore next */
        case "browser":
            var mainElem = window["junit-reporter"];
            if (mainElem) {
                var pre = document.createElement("pre");
                pre.style.fontFamily = "Monaco, \"Lucida Console\", Courier";
                for (var i = 0; i < arguments.length; i++) {
                    var span = document.createElement("span");
                    span.innerHTML = arguments[i] + " ";
                    pre.appendChild(span);
                }
                mainElem.appendChild(pre);
            } else {
                alert("JUnit: You must add a '<div id=\"junit-reporter\"></div>' element to the DOM");
            }
            break;

        default:
            console[type].apply(console, arguments);
            break;
        }
    };
}

function inspectObj (obj) {
    if (typeof obj === "string") return obj;

    /* istanbul ignore else */
    if (typeof window === "undefined")
        return inspect(obj, { depth: 7, colors: true });
    else
        return JSON.stringify(obj, 0, 4);
}

module.exports = function (opts) {
    opts = utils.extend({
        mode: "console"
    }, opts);

    var mode = opts.mode;

    var _brush = brush({ mode: mode });

    var red = _brush.red;
    var grey = _brush.grey;
    var cyan = _brush.cyan;
    var green = _brush.green;
    var underline = _brush.underline;

    var logPass = log("log", mode);
    var logFail = log("error", mode);
    var logFinal = log("info", mode);

    opts = utils.extend({
        prompt: underline(grey("junit >"))
    }, opts);
    var pt = opts.prompt;

    function formatMsg (msg) {
        if (isArray(msg)) return msg.join(" - ");else return msg;
    }

    return {
        formatAssertErr: function (actual, expected) {
            var _ref = new Error("Assertion");

            var stack = _ref.stack;

            stack = stack && stack.replace(regCleanStack, "");

            return indent(red("\n<<<<<<< actual") + "\n" +
                (inspectObj(actual) + "\n") + (red("=======") + "\n") +
                (inspectObj(expected) + "\n") +
                (red(">>>>>>> expected") + "\n\n") + grey(stack)
            );
        },

        logPass: function (msg, span) {
            logPass(pt, green("o"), formatMsg(msg), grey("(" + span + "ms)"));
        },

        logFail: function (msg, err, span) {
            err = err instanceof Error ? indent(err.stack ? err.stack : err.message) : inspectObj(err);

            logFail(pt + " " + red("x") + " " + formatMsg(msg) + " " +
                grey("(" + span + "ms)") + ("\n" + err + "\n")
            );
        },

        logFinal: function (total, tested, passed, failed) {
            logFinal(pt + " " + cyan("tested") + " " + tested + " / " +
                total + "\n" +
                (pt + " " + cyan("passed") + " " + green(passed) + "\n") +
                (pt + " " + cyan("failed") + " " + red(failed))
            );
        }
    };
};
