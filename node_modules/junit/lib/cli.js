"use strict";

var cmder = require("commander");
var fs = require("nofs");
var fsPath = require("path");
var brush = require("./brush");
var junit = require("./");
var defaultReporter = require("./reporter");

var subArgIndex = process.argv.indexOf("--");
var br = brush();

/* istanbul ignore if */
if (subArgIndex > -1) {
    // This is hotfix of the Commander.js bug.
    process.argv.splice(subArgIndex, 1);
}

cmder
    .description("junit cli tool to run tests automatically")
    .usage("[options] [file | pattern...]")
    .option("-o --reporter <module>", "a reporter module [{ formatAssertErr, logPass, logFail, logFinal }]", null)
    .option("-r, --requires <str>", "pre-require modules", "")
    .option("-g, --grep <pattern>", "only run tests matching the pattern", "")
    .option("-t, --timeout <num>", "case timeout in milliseconds [5000]", parseInt)
    .option("-b, --bail <on|off>", "bail after first test failure [off]", "off")
    .option("-f, --failOnUnhandled <on|off>", "failed on unhandled exception [on]", "on")
    .option("-m, --mode <console|browser|none>", "the log mode [console]", "console")
    .option(
        "-p, --prompt <str>", "the prompt string ['junit cli >']",
        br.underline(br.grey("junit cli >"))
    )
    .on("--help",
        /* istanbul ignore next */
        function () {
            console.log(
                "  Examples:\n\n" +
                "    junit test/*.js\n" +
                "    junit test.es7.js\n" +
                "    junit -r coffee-script test.coffee\n" +
                "    noe -b junit -w '*.js' -- test.js\n"
            );
        }
    )
.parse(process.argv);

try {
    cmder.requires.split(",").map(require);
} catch (err) {
    /* istanbul ignore next */
    null;
}

var testReg = new RegExp(cmder.grep);
var reporter;

/* istanbul ignore next */
function loadModule (name) {
    try {
        return require.resolve(name);
    } catch (err) {
        var mod = require(fsPath.resolve(name));
        mod = typeof mod === "function" ? mod : mod.default;
        return mod;
    }
}

function run () {
    // reporter hook
    /* istanbul ignore if */
    if (cmder.reporter) {
        reporter = loadModule(cmder.reporter);
    } else {
        reporter = defaultReporter({ prompt: cmder.prompt, mode: cmder.mode });
    }

    var it = junit({
        filter: function (msg) { return testReg.test(msg); },
        reporter: reporter,
        isBail: cmder.bail === "on",
        isThrowOnFinal: false,
        isFailOnUnhandled: cmder.failOnUnhandled === "on",
        timeout: cmder.timeout || 5000
    });

    /* istanbul ignore next */
    return fs.glob(cmder.args, {
        iter: function (info) {
            var path = fsPath.resolve(info.path);

            var mod = require(path);
            mod = (typeof mod === "function" ? mod : mod.default);

            if (typeof mod !== "function")
                throw new Error('junit error no function is exported: ' + path);

            return mod(it);
        }
    }).then(it.run).then(function (info) {
        var failed = info.failed;

        if (failed) process.exit(1);
    }, function (err) {
        if (err && err.stack) console.error(err.stack);else console.error(err);

        process.exit(1);
    });
}

run();
