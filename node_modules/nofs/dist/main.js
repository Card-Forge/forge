'use strict';
var Promise, _, child_process, fs, fs_exists, nofs, npath;

_ = require('./utils');

npath = require('./path');

child_process = require('child_process');


/**
 * Here I use [Yaku](https://github.com/ysmood/yaku) only as an ES6 shim for Promise.
 * No APIs other than ES6 spec will be used. In the
 * future it will be removed.
 */

Promise = _.Promise;

fs = _.extend({}, require('fs'));

fs_exists = fs.exists;

fs.exists = function(path, fn) {
    return fs_exists(path, function(exists) {
        return fn(null, exists);
    });
};

(function() {
    var k, name, results;
    results = [];
    for (k in fs) {
        if (k.slice(-4) === 'Sync') {
            name = k.slice(0, -4);
            results.push(fs[name] = _.PromiseUtils.promisify(fs[name]));
        } else {
            results.push(void 0);
        }
    }
    return results;
})();

nofs = _.extend({}, {

    /**
     * Copy an empty directory.
     * @param  {String} src
     * @param  {String} dest
     * @param  {Object} opts
     * ```js
     * {
     *     isForce: false,
     *     mode: auto
     * }
     * ```
     * @return {Promise}
     */
    copyDir: function(src, dest, opts) {
        var copy;
        _.defaults(opts, {
            isForce: false
        });
        copy = function() {
            return (opts.isForce ? fs.mkdir(dest, opts.mode)["catch"](function(err) {
                if (err.code !== 'EEXIST') {
                    return Promise.reject(err);
                }
            }) : fs.mkdir(dest, opts.mode))["catch"](function(err) {
                if (err.code === 'ENOENT') {
                    return nofs.mkdirs(dest);
                } else {
                    return Promise.reject(err);
                }
            });
        };
        if (opts.mode) {
            return copy();
        } else {
            return fs.stat(src).then(function(arg) {
                var mode;
                mode = arg.mode;
                opts.mode = mode;
                return copy();
            });
        }
    },
    copyDirSync: function(src, dest, opts) {
        var copy, mode;
        _.defaults(opts, {
            isForce: false
        });
        copy = function() {
            var err, error, error1;
            try {
                if (opts.isForce) {
                    try {
                        return fs.mkdirSync(dest, opts.mode);
                    } catch (error) {
                        err = error;
                        if (err.code !== 'EEXIST') {
                            throw err;
                        }
                    }
                } else {
                    return fs.mkdirSync(dest, opts.mode);
                }
            } catch (error1) {
                err = error1;
                if (err.code === 'ENOENT') {
                    return nofs.mkdirsSync(dest);
                } else {
                    throw err;
                }
            }
        };
        if (opts.mode) {
            return copy();
        } else {
            mode = fs.statSync(src).mode;
            opts.mode = mode;
            return copy();
        }
    },

    /**
     * Copy a single file.
     * @param  {String} src
     * @param  {String} dest
     * @param  {Object} opts
     * ```js
     * {
     *     isForce: false,
     *     mode: auto
     * }
     * ```
     * @return {Promise}
     */
    copyFile: function(src, dest, opts) {
        var copy, copyFile;
        _.defaults(opts, {
            isForce: false
        });
        copyFile = function() {
            return new Promise(function(resolve, reject) {
                var err, error, sDest, sSrc;
                try {
                    sDest = fs.createWriteStream(dest, opts);
                    sSrc = fs.createReadStream(src);
                } catch (error) {
                    err = error;
                    reject(err);
                }
                sSrc.on('error', reject);
                sDest.on('error', reject);
                sDest.on('close', resolve);
                return sSrc.pipe(sDest);
            });
        };
        copy = function() {
            return (opts.isForce ? fs.unlink(dest)["catch"](function(err) {
                if (err.code !== 'ENOENT') {
                    return Promise.reject(err);
                }
            }).then(function() {
                return copyFile();
            }) : copyFile())["catch"](function(err) {
                if (err.code === 'ENOENT') {
                    return nofs.mkdirs(npath.dirname(dest)).then(copyFile);
                } else {
                    return Promise.reject(err);
                }
            });
        };
        if (opts.mode) {
            return copy();
        } else {
            return fs.stat(src).then(function(arg) {
                var mode;
                mode = arg.mode;
                opts.mode = mode;
                return copy();
            });
        }
    },
    copyFileSync: function(src, dest, opts) {
        var buf, bufLen, copy, copyFile, mode;
        _.defaults(opts, {
            isForce: false
        });
        bufLen = 64 * 1024;
        buf = new Buffer(bufLen);
        copyFile = function() {
            var bytesRead, fdr, fdw, pos;
            fdr = fs.openSync(src, 'r');
            fdw = fs.openSync(dest, 'w', opts.mode);
            bytesRead = 1;
            pos = 0;
            while (bytesRead > 0) {
                bytesRead = fs.readSync(fdr, buf, 0, bufLen, pos);
                fs.writeSync(fdw, buf, 0, bytesRead);
                pos += bytesRead;
            }
            fs.closeSync(fdr);
            return fs.closeSync(fdw);
        };
        copy = function() {
            var err, error, error1;
            try {
                if (opts.isForce) {
                    try {
                        fs.unlinkSync(dest);
                    } catch (error) {
                        err = error;
                        if (err.code !== 'ENOENT') {
                            throw err;
                        }
                    }
                    return copyFile();
                } else {
                    return copyFile();
                }
            } catch (error1) {
                err = error1;
                if (err.code === 'ENOENT') {
                    nofs.mkdirsSync(npath.dirname(dest));
                    return copyFile();
                } else {
                    throw err;
                }
            }
        };
        if (opts.mode) {
            return copy();
        } else {
            mode = fs.statSync(src).mode;
            opts.mode = mode;
            return copy();
        }
    },

    /**
     * Like `cp -r`.
     * @param  {String} from Source path.
     * @param  {String} to Destination path.
     * @param  {Object} opts Extends the options of [eachDir](#eachDir-opts).
     * Defaults:
     * ```js
     * {
     *     // Overwrite file if exists.
     *     isForce: false,
     *     isIterFileOnly: false
     *
     *     filter: (fileInfo) => true
     * }
     * ```
     * @return {Promise}
     * @example
     * Copy the contents of the directory rather than copy the directory itself.
     * ```js
     * nofs.copy('dir/path/**', 'dest/path');
     *
     * nofs.copy('dir/path', 'dest/path', {
     *     filter: (fileInfo) => {
     *         return /\d+/.test(fileInfo.path);
     *     }
     * });
     * ```
     */
    copy: function(from, to, opts) {
        var flags, pm, filter;
        if (opts == null) {
            opts = {};
        }
        _.defaults(opts, {
            isForce: false,
            isIterFileOnly: false,
            filter: function () { return true; }
        });

        flags = opts.isForce ? 'w' : 'wx';

        filter = opts.filter;

        opts.iter = function(src, dest, arg) {
            if (_.isFunction(filter) && !filter(arg)) return;

            var isDir, stats;
            isDir = arg.isDir, stats = arg.stats;

            if (isDir) {
                return nofs.copyDir(src, dest, {
                    isForce: true,
                    mode: opts.mode
                });
            } else {
                return nofs.copyFile(src, dest, {
                    isForce: opts.isForce,
                    mode: opts.mode
                });
            }
        };
        if (pm = nofs.pmatch.isPmatch(from)) {
            from = nofs.pmatch.getPlainPath(pm);
            pm = npath.relative(from, pm.pattern);
            opts.filter = pm;
        }
        return nofs.dirExists(to).then(function(exists) {
            if (exists) {
                if (!pm) {
                    return to = npath.join(to, npath.basename(from));
                }
            } else {
                return nofs.mkdirs(npath.dirname(to));
            }
        }).then(function() {
            return fs.stat(from);
        }).then(function(stats) {
            var isDir;
            isDir = stats.isDirectory();
            if (isDir) {
                return nofs.mapDir(from, to, opts);
            } else {
                return opts.iter(from, to, {
                    isDir: isDir,
                    stats: stats
                });
            }
        });
    },
    copySync: function(from, to, opts) {
        var flags, isDir, pm, stats, filter;
        if (opts == null) {
            opts = {};
        }
        _.defaults(opts, {
            isForce: false,
            isIterFileOnly: false,
            filter: function () { return true; }
        });

        flags = opts.isForce ? 'w' : 'wx';

        filter = opts.filter;

        opts.iter = function(src, dest, arg) {
            if (_.isFunction(filter) && !filter(arg)) return;

            var isDir, stats;
            isDir = arg.isDir, stats = arg.stats;

            if (isDir) {
                return nofs.copyDirSync(src, dest, {
                    isForce: true,
                    mode: opts.mode
                });
            } else {
                return nofs.copyFileSync(src, dest, {
                    isForce: opts.isForce,
                    mode: opts.mode
                });
            }
        };
        if (pm = nofs.pmatch.isPmatch(from)) {
            from = nofs.pmatch.getPlainPath(pm);
            pm = npath.relative(from, pm.pattern);
            opts.filter = pm;
        }
        if (nofs.dirExistsSync(to)) {
            if (!pm) {
                to = npath.join(to, npath.basename(from));
            }
        } else {
            nofs.mkdirsSync(npath.dirname(to));
        }
        stats = fs.statSync(from);
        isDir = stats.isDirectory();
        if (isDir) {
            return nofs.mapDirSync(from, to, opts);
        } else {
            return opts.iter(from, to, {
                isDir: isDir,
                stats: stats
            });
        }
    },

    /**
     * Check if a path exists, and if it is a directory.
     * @param  {String}  path
     * @return {Promise} Resolves a boolean value.
     */
    dirExists: function(path) {
        return fs.stat(path).then(function(stats) {
            return stats.isDirectory();
        })["catch"](function() {
            return false;
        });
    },
    dirExistsSync: function(path) {
        if (fs.existsSync(path)) {
            return fs.statSync(path).isDirectory();
        } else {
            return false;
        }
    },

    /**
     * <a name='eachDir'></a>
     * Concurrently walks through a path recursively with a callback.
     * The callback can return a Promise to continue the sequence.
     * The resolving order is also recursive, a directory path resolves
     * after all its children are resolved.
     * @param  {String} spath The path may point to a directory or a file.
     * @param  {Object} opts Optional. <a id='eachDir-opts'></a> Defaults:
     * ```js
     * {
     *     // Callback on each path iteration.
     *     iter: (fileInfo) => Promise | Any,
     *
     *     // Auto check if the spath is a minimatch pattern.
     *     isAutoPmatch: true,
     *
     *     // Include entries whose names begin with a dot (.), the posix hidden files.
     *     all: true,
     *
     *     // To filter paths. It can also be a RegExp or a glob pattern string.
     *     // When it's a string, it extends the Minimatch's options.
     *     filter: (fileInfo) => true,
     *
     *     // The current working directory to search.
     *     cwd: '',
     *
     *     // Call iter only when it is a file.
     *     isIterFileOnly: false,
     *
     *     // Whether to include the root directory or not.
     *     isIncludeRoot: true,
     *
     *     // Whehter to follow symbol links or not.
     *     isFollowLink: true,
     *
     *     // Iterate children first, then parent folder.
     *     isReverse: false,
     *
     *     // When isReverse is false, it will be the previous iter resolve value.
     *     val: any,
     *
     *     // If it return false, sub-entries won't be searched.
     *     // When the `filter` option returns false, its children will
     *     // still be itered. But when `searchFilter` returns false, children
     *     // won't be itered by the iter.
     *     searchFilter: (fileInfo) => true,
     *
     *     // If you want sort the names of each level, you can hack here.
     *     // Such as `(names) => names.sort()`.
     *     handleNames: (names) => names
     * }
     * ```
     * The argument of `opts.iter`, `fileInfo` object has these properties:
     * ```js
     * {
     *     path: String,
     *     name: String,
     *     baseDir: String,
     *     isDir: Boolean,
     *     children: [fileInfo],
     *     stats: fs.Stats,
     *     val: Any
     * }
     * ```
     * Assume we call the function: `nofs.eachDir('dir', { iter: (f) => f })`,
     * the resolved directory object array may look like:
     * ```js
     * {
     *     path: 'some/dir/path',
     *     name: 'path',
     *     baseDir: 'some/dir',
     *     isDir: true,
     *     val: 'test',
     *     children: [
     *         {
     *             path: 'some/dir/path/a.txt', name: 'a.txt',
     *             baseDir: 'dir', isDir: false, stats: { ... }
     *         },
     *         { path: 'some/dir/path/b.txt', name: 'b.txt', ... }
     *     ],
     *     stats: {
     *         size: 527,
     *         atime: 'Mon, 10 Oct 2011 23:24:11 GMT',
     *         mtime: 'Mon, 10 Oct 2011 23:24:11 GMT',
     *         ctime: 'Mon, 10 Oct 2011 23:24:11 GMT'
     *         ...
     *     }
     * }
     * ```
     * The `stats` is a native `fs.Stats` object.
     * @return {Promise} Resolves a directory tree object.
     * @example
     * ```js
     * // Print all file and directory names, and the modification time.
     * nofs.eachDir('dir/path', {
     *     iter: (obj, stats) =>
     *         console.log(obj.path, stats.mtime)
     * });
     *
     * // Print path name list.
     * nofs.eachDir('dir/path', { iter: (curr) => curr })
     * .then((tree) =>
     *     console.log(tree)
     * );
     *
     * // Find all js files.
     * nofs.eachDir('dir/path', {
     *     filter: '**\/*.js',
     *     iter: ({ path }) =>
     *         console.log(paths)
     * });
     *
     * // Find all js files.
     * nofs.eachDir('dir/path', {
     *     filter: /\.js$/,
     *     iter: ({ path }) =>
     *         console.log(paths)
     * });
     *
     * // Custom filter.
     * nofs.eachDir('dir/path', {
     *     filter: ({ path, stats }) =>
     *         path.slice(-1) != '/' && stats.size > 1000
     *     iter: (path) =>
     *         console.log(path)
     * });
     * ```
     */
    eachDir: function(spath, opts) {
        var decideNext, execFn, handleFilter, handleSpath, raceResolver, readdir, resolve, stat;
        if (opts == null) {
            opts = {};
        }
        _.defaults(opts, {
            isAutoPmatch: true,
            all: true,
            filter: function() {
                return true;
            },
            searchFilter: function() {
                return true;
            },
            handleNames: function(names) {
                return names;
            },
            cwd: '',
            isIterFileOnly: false,
            isIncludeRoot: true,
            isFollowLink: true,
            isReverse: false
        });
        stat = opts.isFollowLink ? fs.stat : fs.lstat;
        handleSpath = function() {
            var pm;
            spath = npath.normalize(spath);
            if (opts.isAutoPmatch && (pm = nofs.pmatch.isPmatch(spath))) {
                if (nofs.pmatch.isNotPlain(pm)) {
                    // keep the user defined filter
                    opts._filter = opts.filter;
                    opts.filter = pm;
                }
                return spath = nofs.pmatch.getPlainPath(pm);
            }
        };

        handleFilter = function() {
            var pm, reg;
            if (_.isRegExp(opts.filter)) {
                reg = opts.filter;
                opts.filter = function(fileInfo) {
                    return reg.test(fileInfo.path);
                };
                return;
            }
            pm = null;
            if (_.isString(opts.filter)) {
                pm = new nofs.pmatch.Minimatch(opts.filter);
            }
            if (opts.filter instanceof nofs.pmatch.Minimatch) {
                pm = opts.filter;
            }
            if (pm) {
                opts.filter = function(fileInfo) {
                    // Hot fix for minimatch, it should match '**' to '.'.
                    if (fileInfo.path === '.') {
                        return pm.match('');
                    }
                    return pm.match(fileInfo.path) && (_.isFunction(opts._filter) ? opts._filter(fileInfo) : true);
                };
                return opts.searchFilter = function(fileInfo) {
                    // Hot fix for minimatch, it should match '**' to '.'.
                    if (fileInfo.path === '.') {
                        return true;
                    }
                    return pm.match(fileInfo.path, true) && (_.isFunction(opts._searchFilter) ? opts._searchFilter(fileInfo) : true);
                };
            }
        };

        resolve = function(path) {
            return npath.join(opts.cwd, path);
        };

        execFn = function(fileInfo) {
            if (!opts.all && fileInfo.name[0] === '.') {
                return;
            }
            if (opts.isIterFileOnly && fileInfo.isDir) {
                return;
            }
            if ((opts.iter != null) && opts.filter(fileInfo)) {
                return opts.iter(fileInfo);
            }
        };

        // TODO: Race Condition
        // It's possible that the file has already gone.
        // Here we silently ignore it, since you normally don't
        // want to iterate a non-exists path.
        raceResolver = function(err) {
            if (err.code !== 'ENOENT') {
                return Promise.reject(err);
            }
        };

        decideNext = function(dir, name) {
            var path;
            path = npath.join(dir, name);
            return stat(resolve(path))["catch"](raceResolver).then(function(stats) {
                var fileInfo, isDir, p;
                if (!stats) {
                    return;
                }
                isDir = stats.isDirectory();
                if (opts.baseDir === void 0) {
                    opts.baseDir = isDir ? spath : npath.dirname(spath);
                }
                fileInfo = {
                    path: path,
                    name: name,
                    baseDir: opts.baseDir,
                    isDir: isDir,
                    stats: stats
                };
                if (isDir) {
                    if (!opts.searchFilter(fileInfo)) {
                        return;
                    }
                    if (opts.isReverse) {
                        return readdir(path).then(function(children) {
                            fileInfo.children = children;
                            return execFn(fileInfo);
                        });
                    } else {
                        p = execFn(fileInfo);
                        if (!p || !p.then) {
                            p = Promise.resolve(p);
                        }
                        return p.then(function(val) {
                            return readdir(path).then(function(children) {
                                fileInfo.children = children;
                                fileInfo.val = val;
                                return fileInfo;
                            });
                        });
                    }
                } else {
                    return execFn(fileInfo);
                }
            });
        };
        readdir = function(dir) {
            return fs.readdir(resolve(dir))["catch"](raceResolver).then(function(names) {
                if (!names) {
                    return;
                }
                return Promise.all(opts.handleNames(names).map(function(name) {
                    return decideNext(dir, name);
                }));
            });
        };
        handleSpath();
        handleFilter();
        if (opts.isIncludeRoot) {
            return decideNext(npath.dirname(spath), npath.basename(spath));
        } else {
            return readdir(spath);
        }
    },
    eachDirSync: function(spath, opts) {
        var decideNext, execFn, handleFilter, handleSpath, raceResolver, readdir, resolve, stat;
        if (opts == null) {
            opts = {};
        }
        _.defaults(opts, {
            isAutoPmatch: true,
            all: true,
            filter: function() {
                return true;
            },
            searchFilter: function() {
                return true;
            },
            handleNames: function(names) {
                return names;
            },
            cwd: '',
            isIterFileOnly: false,
            isIncludeRoot: true,
            isFollowLink: true,
            isReverse: false
        });
        stat = opts.isFollowLink ? fs.statSync : fs.lstatSync;
        handleSpath = function() {
            var pm;
            spath = npath.normalize(spath);
            if (opts.isAutoPmatch && (pm = nofs.pmatch.isPmatch(spath))) {
                if (nofs.pmatch.isNotPlain(pm)) {
                    opts._filter = opts.filter;
                    opts.filter = pm;
                }
                return spath = nofs.pmatch.getPlainPath(pm);
            }
        };
        handleFilter = function() {
            var pm, reg;
            if (_.isRegExp(opts.filter)) {
                reg = opts.filter;
                opts.filter = function(fileInfo) {
                    return reg.test(fileInfo.path);
                };
                return;
            }
            pm = null;
            if (_.isString(opts.filter)) {
                pm = new nofs.pmatch.Minimatch(opts.filter);
            }
            if (opts.filter instanceof nofs.pmatch.Minimatch) {
                pm = opts.filter;
            }
            if (pm) {
                opts.filter = function(fileInfo) {
                    if (fileInfo.path === '.') {
                        return pm.match('');
                    }
                    return pm.match(fileInfo.path) && (_.isFunction(opts._filter) ? opts._filter(fileInfo) : true);
                };
                return opts.searchFilter = function(fileInfo) {
                    if (fileInfo.path === '.') {
                        return true;
                    }
                    return pm.match(fileInfo.path, true) && (_.isFunction(opts._searchFilter) ? opts._searchFilter(fileInfo) : true);
                };
            }
        };
        resolve = function(path) {
            return npath.join(opts.cwd, path);
        };
        execFn = function(fileInfo) {
            if (!opts.all && fileInfo.name[0] === '.') {
                return;
            }
            if (opts.isIterFileOnly && fileInfo.isDir) {
                return;
            }
            if ((opts.iter != null) && opts.filter(fileInfo)) {
                return opts.iter(fileInfo);
            }
        };
        raceResolver = function(err) {
            if (err.code !== 'ENOENT') {
                throw err;
            }
        };
        decideNext = function(dir, name) {
            var children, err, error, fileInfo, isDir, path, stats, val;
            path = npath.join(dir, name);
            try {
                stats = stat(resolve(path));
                isDir = stats.isDirectory();
            } catch (error) {
                err = error;
                raceResolver(err);
                return;
            }
            if (opts.baseDir === void 0) {
                opts.baseDir = isDir ? spath : npath.dirname(spath);
            }
            fileInfo = {
                path: path,
                name: name,
                baseDir: opts.baseDir,
                isDir: isDir,
                stats: stats
            };
            if (isDir) {
                if (!opts.searchFilter(fileInfo)) {
                    return;
                }
                if (opts.isReverse) {
                    children = readdir(path);
                    fileInfo.children = children;
                    return execFn(fileInfo);
                } else {
                    val = execFn(fileInfo);
                    children = readdir(path);
                    fileInfo.children = children;
                    fileInfo.val = val;
                    return fileInfo;
                }
            } else {
                return execFn(fileInfo);
            }
        };
        readdir = function(dir) {
            var err, error, names;
            try {
                names = fs.readdirSync(resolve(dir));
            } catch (error) {
                err = error;
                raceResolver(err);
                return;
            }
            return opts.handleNames(names).map(function(name) {
                return decideNext(dir, name);
            });
        };
        handleSpath();
        handleFilter();
        if (opts.isIncludeRoot) {
            return decideNext(npath.dirname(spath), npath.basename(spath));
        } else {
            return readdir(spath);
        }
    },

    /**
     * Ensures that the file exists.
     * Change file access and modification times.
     * If the file does not exist, it is created.
     * If the file exists, it is NOT MODIFIED.
     * @param  {String} path
     * @param  {Object} opts
     * @return {Promise}
     */
    ensureFile: function(path, opts) {
        if (opts == null) {
            opts = {};
        }
        return nofs.fileExists(path).then(function(exists) {
            if (exists) {
                return Promise.resolve();
            } else {
                return nofs.outputFile(path, new Buffer(0), opts);
            }
        });
    },
    ensureFileSync: function(path, opts) {
        if (opts == null) {
            opts = {};
        }
        if (!nofs.fileExistsSync(path)) {
            return nofs.outputFileSync(path, new Buffer(0), opts);
        }
    },

    /**
     * Check if a path exists, and if it is a file.
     * @param  {String}  path
     * @return {Promise} Resolves a boolean value.
     */
    fileExists: function(path) {
        return fs.stat(path).then(function(stats) {
            return stats.isFile();
        })["catch"](function() {
            return false;
        });
    },
    fileExistsSync: function(path) {
        if (fs.existsSync(path)) {
            return fs.statSync(path).isFile();
        } else {
            return false;
        }
    },

    /**
     * Get files by patterns.
     * @param  {String | Array} pattern The minimatch pattern.
     * Patterns that starts with '!' in the array will be used
     * to exclude paths.
     * @param {Object} opts Extends the options of [eachDir](#eachDir-opts).
     * **The `filter` property is fixed with the pattern, use `iter` instead**.
     * Defaults:
     * ```js
     * {
     *     all: false,
     *
     *     // The minimatch option object.
     *     pmatch: {},
     *
     *     // It will be called after each match. It can also return
     *     // a promise.
     *     iter: (fileInfo, list) => list.push(fileInfo.path)
     * }
     * ```
     * @return {Promise} Resolves the list array.
     * @example
     * ```js
     * // Get all js files.
     * nofs.glob(['**\/*.js', '**\/*.css']).then((paths) =>
     *     console.log(paths)
     * );
     *
     * // Exclude some files. "a.js" will be ignored.
     * nofs.glob(['**\/*.js', '!**\/a.js']).then((paths) =>
     *     console.log(paths)
     * );
     *
     * // Custom the iterator. Append '/' to each directory path.
     * nofs.glob('**\/*.js', {
     *     iter: (info, list) =>
     *         list.push(info.isDir ? (info.path + '/') : info.path
     * }).then((paths) =>
     *     console.log(paths)
     * );
     * ```
     */
    glob: function(patterns, opts) {
        var glob, iter, list, negateMath, pmatches, ref;
        if (opts == null) {
            opts = {};
        }
        _.defaults(opts, {
            pmatch: {},
            all: false,
            iter: function(fileInfo, list) {
                return list.push(fileInfo.path);
            }
        });
        opts.pmatch.dot = opts.all;
        if (_.isString(patterns)) {
            patterns = [patterns];
        }
        patterns = patterns.map(npath.normalize);
        list = [];
        ref = nofs.pmatch.matchMultiple(patterns, opts.pmatch), pmatches = ref.pmatches, negateMath = ref.negateMath;
        iter = opts.iter;
        opts.iter = function(fileInfo) {
            return iter(fileInfo, list);
        };
        glob = function(pm) {
            var newOpts;
            newOpts = _.defaults({
                filter: function(fileInfo) {
                    if (negateMath(fileInfo.path)) {
                        return;
                    }
                    if (fileInfo.path === '.') {
                        return pm.match('');
                    }
                    return pm.match(fileInfo.path);
                },
                searchFilter: function(fileInfo) {
                    if (fileInfo.path === '.') {
                        return true;
                    }
                    return pm.match(fileInfo.path, true);
                }
            }, opts);
            return nofs.eachDir(nofs.pmatch.getPlainPath(pm), newOpts);
        };
        return pmatches.reduce(function(p, pm) {
            return p.then(function() {
                return glob(pm);
            });
        }, Promise.resolve()).then(function() {
            return list;
        });
    },
    globSync: function(patterns, opts) {
        var glob, i, iter, len, list, negateMath, pm, pmatches, ref;
        if (opts == null) {
            opts = {};
        }
        _.defaults(opts, {
            pmatch: {},
            all: false,
            iter: function(fileInfo, list) {
                return list.push(fileInfo.path);
            }
        });
        opts.pmatch.dot = opts.all;
        if (_.isString(patterns)) {
            patterns = [patterns];
        }
        patterns = patterns.map(npath.normalize);
        list = [];
        ref = nofs.pmatch.matchMultiple(patterns, opts.pmatch), pmatches = ref.pmatches, negateMath = ref.negateMath;
        iter = opts.iter;
        opts.iter = function(fileInfo) {
            return iter(fileInfo, list);
        };
        glob = function(pm) {
            var newOpts;
            newOpts = _.defaults({
                filter: function(fileInfo) {
                    if (negateMath(fileInfo.path)) {
                        return;
                    }
                    if (fileInfo.path === '.') {
                        return pm.match('');
                    }
                    return pm.match(fileInfo.path);
                },
                searchFilter: function(fileInfo) {
                    if (fileInfo.path === '.') {
                        return true;
                    }
                    return pm.match(fileInfo.path, true);
                }
            }, opts);
            return nofs.eachDirSync(nofs.pmatch.getPlainPath(pm), newOpts);
        };
        for (i = 0, len = pmatches.length; i < len; i++) {
            pm = pmatches[i];
            glob(pm);
        }
        return list;
    },

    /**
     * Map file from a directory to another recursively with a
     * callback.
     * @param  {String}   from The root directory to start with.
     * @param  {String}   to This directory can be a non-exists path.
     * @param  {Object}   opts Extends the options of [eachDir](#eachDir-opts). But `cwd` is
     * fixed with the same as the `from` parameter. Defaults:
     * ```js
     * {
     *     // It will be called with each path. The callback can return
     *     // a `Promise` to keep the async sequence go on.
     *     iter: (src, dest, fileInfo) => Promise | Any,
     *
     *     // When isMapContent is true, and the current is a file.
     *     iter: (content, src, dest, fileInfo) => Promise | Any,
     *
     *     // When isMapContent is true, and the current is a folder.
     *     iter: (mode, src, dest, fileInfo) => Promise | Any,
     *
     *     isMapContent: false,
     *
     *     isIterFileOnly: true
     * }
     * ```
     * @return {Promise} Resolves a tree object.
     * @example
     * ```js
     * nofs.mapDir('from', 'to', {
     *     iter: (src, dest, info) =>
     *         console.log(src, dest, info)
     * });
     * ```
     * @example
     * ```js
     * // Copy and add license header for each files
     * // from a folder to another.
     * nofs.mapDir('from', 'to', {
     *     ismMapContent: true,
     *     iter: (content) =>
     *         'License MIT\n' + content
     * });
     * ```
     */
    mapDir: function(from, to, opts) {
        var iter, pm;
        if (opts == null) {
            opts = {};
        }

        _.defaults(opts, {
            iter: _.id,
            isIterFileOnly: true,
            isMapContent: false
        });

        if (pm = nofs.pmatch.isPmatch(from)) {
            from = nofs.pmatch.getPlainPath(pm);
            pm = npath.relative(from, pm.pattern);
            opts.filter = pm;
        }
        opts.cwd = from;
        iter = opts.iter;
        opts.iter = function(fileInfo) {
            var dest, src;
            src = npath.join(from, fileInfo.path);
            dest = npath.join(to, fileInfo.path);

            if (opts.isMapContent) {
                if (fileInfo.isDir) {
                    return iter(fileInfo.stats.mode, src, dest, fileInfo)
                    .then(function (mode) {
                        return nofs.mkdirs(dest, mode);
                    });
                } else {
                    return fs.readFile(src).then(function (content) {
                        return iter(content, src, dest, fileInfo);
                    }).then(function (content) {
                        return nofs.outputFile(dest, content, { mode: fileInfo.mode });
                    });
                }
            } else {
                return iter(src, dest, fileInfo);
            }
        };
        return nofs.eachDir('', opts);
    },
    mapDirSync: function(from, to, opts) {
        var iter, pm;
        if (opts == null) {
            opts = {};
        }

        _.defaults(opts, {
            iter: _.id,
            isIterFileOnly: true,
            isMapContent: false
        });

        if (pm = nofs.pmatch.isPmatch(from)) {
            from = nofs.pmatch.getPlainPath(pm);
            pm = npath.relative(from, pm.pattern);
            opts.filter = pm;
        }
        opts.cwd = from;
        iter = opts.iter;
        opts.iter = function(fileInfo) {
            var dest, src;
            src = npath.join(from, fileInfo.path);
            dest = npath.join(to, fileInfo.path);

            if (opts.isMapContent) {
                if (fileInfo.isDir) {
                    var mode = iter(fileInfo.stats.mode, src, dest, fileInfo);
                    nofs.mkdirsSync(dest, mode);
                } else {
                    var content = fs.readFileSync(src);
                    content = iter(content, src, dest, fileInfo);
                    nofs.outputFileSync(dest, content, { mode: fileInfo.mode });
                }
            } else {
                return iter(src, dest, fileInfo);
            }
        };
        return nofs.eachDirSync('', opts);
    },

    /**
     * Recursively create directory path, like `mkdir -p`.
     * @param  {String} path
     * @param  {String} mode Defaults: `0o777 & ~process.umask()`
     * @return {Promise}
     */
    mkdirs: function(path, mode) {
        var makedir;
        if (mode == null) {
            mode = 0x1ff & ~process.umask();
        }
        makedir = function(path) {
            // ys TODO:
            // Sometimes I think this async operation is
            // useless, since during the next process tick, the
            // dir may be created.
            // We may use dirExistsSync to avoid this bug, but
            // for the sake of pure async, I leave it still.
            return nofs.dirExists(path).then(function(exists) {
                var parentPath;
                if (exists) {
                    return Promise.resolve();
                } else {
                    parentPath = npath.dirname(path);
                    return makedir(parentPath).then(function() {
                        return fs.mkdir(path, mode)["catch"](function(err) {
                            if (err.code !== 'EEXIST') {
                                return Promise.reject(err);
                            }
                        });
                    });
                }
            });
        };
        return makedir(path);
    },
    mkdirsSync: function(path, mode) {
        var makedir;
        if (mode == null) {
            mode = 0x1ff & ~process.umask();
        }
        makedir = function(path) {
            var parentPath;
            if (!nofs.dirExistsSync(path)) {
                parentPath = npath.dirname(path);
                makedir(parentPath);
                return fs.mkdirSync(path, mode);
            }
        };
        return makedir(path);
    },

    /**
     * Moves a file or directory. Also works between partitions.
     * Behaves like the Unix `mv`.
     * @param  {String} from Source path.
     * @param  {String} to   Destination path.
     * @param  {Object} opts Defaults:
     * ```js
     * {
     *     isForce: false,
     *     isFollowLink: false
     * }
     * ```
     * @return {Promise} It will resolve a boolean value which indicates
     * whether this action is taken between two partitions.
     */
    move: function(from, to, opts) {
        var moveFile;
        if (opts == null) {
            opts = {};
        }
        _.defaults(opts, {
            isForce: false,
            isFollowLink: false
        });
        moveFile = function(src, dest) {
            if (opts.isForce) {
                return fs.rename(src, dest);
            } else {
                return fs.link(src, dest).then(function() {
                    return fs.unlink(src);
                });
            }
        };
        return fs.stat(from).then(function(stats) {
            return nofs.dirExists(to).then(function(exists) {
                if (exists) {
                    nofs.mkdirs(to);
                    return to = npath.join(to, npath.basename(from));
                } else {
                    return nofs.mkdirs(npath.dirname(to));
                }
            }).then(function() {
                if (stats.isDirectory()) {
                    return fs.rename(from, to);
                } else {
                    return moveFile(from, to);
                }
            });
        })["catch"](function(err) {
            if (err.code === 'EXDEV') {
                return nofs.copy(from, to, opts).then(function() {
                    return nofs.remove(from);
                });
            } else {
                return Promise.reject(err);
            }
        });
    },
    moveSync: function(from, to, opts) {
        var err, error, moveFile, stats;
        if (opts == null) {
            opts = {};
        }
        _.defaults(opts, {
            isForce: false
        });
        moveFile = function(src, dest) {
            if (opts.isForce) {
                fs.renameSync(src, dest);
            } else {
                fs.linkSync(src, dest)
                fs.unlinkSync(src);
            }
        };
        try {
            if (nofs.dirExistsSync(to)) {
                nofs.mkdirsSync(to);
                to = npath.join(to, npath.basename(from));
            } else {
                nofs.mkdirsSync(npath.dirname(to));
            }
            stats = fs.statSync(from);
            if (stats.isDirectory()) {
                return fs.renameSync(from, to);
            } else {
                return moveFile(from, to);
            }
        } catch (error) {
            err = error;
            if (err.code === 'EXDEV') {
                nofs.copySync(from, to, opts);
                return nofs.removeSync(from);
            } else {
                throw err;
            }
        }
    },

    /**
     * Almost the same as `writeFile`, except that if its parent
     * directories do not exist, they will be created.
     * @param  {String} path
     * @param  {String | Buffer} data
     * @param  {String | Object} opts <a id="outputFile-opts"></a>
     * Same with the [writeFile](#writeFile-opts).
     * @return {Promise}
     */
    outputFile: function(path, data, opts) {
        if (opts == null) {
            opts = {};
        }
        return nofs.fileExists(path).then(function(exists) {
            var dir;
            if (exists) {
                return nofs.writeFile(path, data, opts);
            } else {
                dir = npath.dirname(path);
                return nofs.mkdirs(dir, opts.mode).then(function() {
                    return nofs.writeFile(path, data, opts);
                });
            }
        });
    },
    outputFileSync: function(path, data, opts) {
        var dir;
        if (opts == null) {
            opts = {};
        }
        if (nofs.fileExistsSync(path)) {
            return nofs.writeFileSync(path, data, opts);
        } else {
            dir = npath.dirname(path);
            nofs.mkdirsSync(dir, opts.mode);
            return nofs.writeFileSync(path, data, opts);
        }
    },

    /**
     * Write a object to a file, if its parent directory doesn't
     * exists, it will be created.
     * @param  {String} path
     * @param  {Any} obj  The data object to save.
     * @param  {Object | String} opts Extends the options of [outputFile](#outputFile-opts).
     * Defaults:
     * ```js
     * {
     *     replacer: null,
     *     space: null
     * }
     * ```
     * @return {Promise}
     */
    outputJson: function(path, obj, opts) {
        var err, error, str;
        if (opts == null) {
            opts = {};
        }
        if (_.isString(opts)) {
            opts = {
                encoding: opts
            };
        }
        try {
            str = JSON.stringify(obj, opts.replacer, opts.space);
            str += '\n';
        } catch (error) {
            err = error;
            return Promise.reject(err);
        }
        return nofs.outputFile(path, str, opts);
    },
    outputJsonSync: function(path, obj, opts) {
        var str;
        if (opts == null) {
            opts = {};
        }
        if (_.isString(opts)) {
            opts = {
                encoding: opts
            };
        }
        str = JSON.stringify(obj, opts.replacer, opts.space);
        str += '\n';
        return nofs.outputFileSync(path, str, opts);
    },

    /**
     * The path module nofs is using.
     * It's the native [io.js](iojs.org) path lib.
     * nofs will force all the path separators to `/`,
     * such as `C:\a\b` will be transformed to `C:/a/b`.
     * @type {Object}
     */
    path: npath,

    /**
     * The `minimatch` lib. It has two extra methods:
     * - `isPmatch(String | Object) -> Pmatch | undefined`
     *     It helps to detect if a string or an object is a minimatch.
     *
     * - `getPlainPath(Pmatch) -> String`
     *     Helps to get the plain root path of a pattern. Such as `src/js/*.js`
     *     will get `src/js`
     *
     * [Documentation](https://github.com/isaacs/minimatch)
     *
     * [Offline Documentation](?gotoDoc=minimatch/readme.md)
     * @example
     * ```js
     * nofs.pmatch('a/b/c.js', '**\/*.js');
     * // output => true
     * nofs.pmatch.isPmatch('test*');
     * // output => true
     * nofs.pmatch.isPmatch('test/b');
     * // output => false
     * ```
     */
    pmatch: require('./pmatch'),

    /**
     * What promise this lib is using.
     * @type {Promise}
     */
    Promise: Promise,

    /**
     * Same as the [`yaku/lib/utils`](https://github.com/ysmood/yaku#utils).
     * @type {Object}
     */
    PromiseUtils: _.PromiseUtils,

    /**
     * Read A Json file and parse it to a object.
     * @param  {String} path
     * @param  {Object | String} opts Same with the native `nofs.readFile`.
     * @return {Promise} Resolves a parsed object.
     * @example
     * ```js
     * nofs.readJson('a.json').then((obj) =>
     *     console.log(obj.name, obj.age)
     * );
     * ```
     */
    readJson: function(path, opts) {
        if (opts == null) {
            opts = {};
        }
        return fs.readFile(path, opts).then(function(data) {
            var err, error;
            try {
                return JSON.parse(data + '');
            } catch (error) {
                err = error;
                return Promise.reject(err);
            }
        });
    },
    readJsonSync: function(path, opts) {
        var data;
        if (opts == null) {
            opts = {};
        }
        data = fs.readFileSync(path, opts);
        return JSON.parse(data + '');
    },

    /**
     * Walk through directory recursively with a iterator.
     * @param  {String}   path
     * @param  {Object}   opts Extends the options of [eachDir](#eachDir-opts),
     * with some extra options:
     * ```js
     * {
     *     iter: (prev, path, isDir, stats) -> Promise | Any,
     *
     *     // The init value of the walk.
     *     init: undefined,
     *
     *     isIterFileOnly: true
     * }
     * ```
     * @return {Promise} Final resolved value.
     * @example
     * ```js
     * // Concat all files.
     * nofs.reduceDir('dir/path', {
     *     init: '',
     *     iter: (val, { path }) =>
     *         nofs.readFile(path).then((str) =>
     *             val += str + '\n'
     *         )
     * }).then((ret) =>
     *     console.log(ret)
     * );
     * ```
     */
    reduceDir: function(path, opts) {
        var iter, prev;
        if (opts == null) {
            opts = {};
        }
        _.defaults(opts, {
            isIterFileOnly: true
        });
        prev = Promise.resolve(opts.init);
        iter = opts.iter;
        opts.iter = function(fileInfo) {
            return prev = prev.then(function(val) {
                val = iter(val, fileInfo);
                if (!val || !val.then) {
                    return Promise.resolve(val);
                }
            });
        };
        return nofs.eachDir(path, opts).then(function() {
            return prev;
        });
    },
    reduceDirSync: function(path, opts) {
        var iter, prev;
        if (opts == null) {
            opts = {};
        }
        _.defaults(opts, {
            isIterFileOnly: true
        });
        prev = opts.init;
        iter = opts.iter;
        opts.iter = function(fileInfo) {
            return prev = iter(prev, fileInfo);
        };
        nofs.eachDirSync(path, opts);
        return prev;
    },

    /**
     * Remove a file or directory peacefully, same with the `rm -rf`.
     * @param  {String} path
     * @param {Object} opts Extends the options of [eachDir](#eachDir-opts). But
     * the `isReverse` is fixed with `true`. Defaults:
     * ```js
     * { isFollowLink: false }
     * ```
     * @return {Promise}
     */
    remove: function(path, opts) {
        var removeOpts;
        if (opts == null) {
            opts = {};
        }
        _.defaults(opts, {
            isFollowLink: false
        });
        opts.isReverse = true;
        removeOpts = _.extend({
            iter: function(arg) {
                var isDir, path;
                path = arg.path, isDir = arg.isDir;
                if (isDir) {
                    return fs.rmdir(path);
                } else {
                    return fs.unlink(path);
                }
            }
        }, opts, {
            isAutoPmatch: false
        });
        opts.iter = function(arg) {
            var isDir, path;
            path = arg.path, isDir = arg.isDir;
            if (isDir) {
                return fs.rmdir(path)["catch"](function(err) {
                    if (err.code === 'ENOTEMPTY') {
                        return nofs.eachDir(path, removeOpts);
                    }
                    return Promise.reject(err);
                });
            } else {
                return fs.unlink(path);
            }
        };
        return nofs.eachDir(path, opts);
    },
    removeSync: function(path, opts) {
        var removeOpts;
        if (opts == null) {
            opts = {};
        }
        _.defaults(opts, {
            isFollowLink: false
        });
        opts.isReverse = true;
        removeOpts = _.extend({
            iter: function(arg) {
                var isDir, path;
                path = arg.path, isDir = arg.isDir;
                if (isDir) {
                    return fs.rmdirSync(path);
                } else {
                    return fs.unlinkSync(path);
                }
            }
        }, opts, {
            isAutoPmatch: false
        });
        opts.iter = function(arg) {
            var err, error, isDir, path;
            path = arg.path, isDir = arg.isDir;
            if (isDir) {
                try {
                    return fs.rmdirSync(path);
                } catch (error) {
                    err = error;
                    if (err.code === 'ENOTEMPTY') {
                        return nofs.eachDirSync(path, removeOpts);
                    }
                    return Promise.reject(err);
                }
            } else {
                return fs.unlinkSync(path);
            }
        };
        return nofs.eachDirSync(path, opts);
    },

    /**
     * Change file access and modification times.
     * If the file does not exist, it is created.
     * @param  {String} path
     * @param  {Object} opts Default:
     * ```js
     * {
     *     atime: Date.now(),
     *     mtime: Date.now(),
     *     mode: undefined
     * }
     * ```
     * @return {Promise} If new file created, resolves true.
     */
    touch: function(path, opts) {
        var now;
        if (opts == null) {
            opts = {};
        }
        now = new Date;
        _.defaults(opts, {
            atime: now,
            mtime: now
        });
        return nofs.fileExists(path).then(function(exists) {
            return (exists ? fs.utimes(path, opts.atime, opts.mtime) : nofs.outputFile(path, new Buffer(0), opts)).then(function() {
                return !exists;
            });
        });
    },
    touchSync: function(path, opts) {
        var exists, now;
        if (opts == null) {
            opts = {};
        }
        now = new Date;
        _.defaults(opts, {
            atime: now,
            mtime: now
        });
        exists = nofs.fileExistsSync(path);
        if (exists) {
            fs.utimesSync(path, opts.atime, opts.mtime);
        } else {
            nofs.outputFileSync(path, new Buffer(0), opts);
        }
        return !exists;
    },

    /**
     * <a id="writeFile-opts"></a>
     * Watch a file. If the file changes, the handler will be invoked.
     * You can change the polling interval by using `process.env.pollingWatch`.
     * Use `process.env.watchPersistent = 'off'` to disable the persistent.
     * Why not use `nofs.watch`? Because `nofs.watch` is unstable on some file
     * systems, such as Samba or OSX.
     * @param  {String}   path    The file path
     * @param  {Object} opts Defaults:
     * ```js
     * {
     *     handler: (path, curr, prev, isDeletion) => {},
     *
     *     // Auto unwatch the file while file deletion.
     *     autoUnwatch: true,
     *
     *     persistent: process.env.watchPersistent != 'off',
     *     interval: +process.env.pollingWatch || 300
     * }
     * ```
     * @return {Promise} It resolves the `StatWatcher` object:
     * ```js
     * {
     *     path,
     *     handler
     * }
     * ```
     * @example
     * ```js
     * process.env.watchPersistent = 'off'
     * nofs.watchPath('a.js', {
     *     handler: (path, curr, prev, isDeletion) => {
     *         if (curr.mtime !== prev.mtime)
     *             console.log(path);
     *     }
     * }).then((watcher) =>
     *     nofs.unwatchFile(watcher.path, watcher.handler)
     * );
     * ```
     */
    watchPath: function(path, opts) {
        var handler, watcher;
        if (opts == null) {
            opts = {};
        }
        _.defaults(opts, {
            autoUnwatch: true,
            persistent: process.env.watchPersistent !== 'off',
            interval: +process.env.pollingWatch || 300
        });
        handler = function(curr, prev) {
            var isDeletion;
            isDeletion = curr.mtime.getTime() === 0;
            opts.handler(path, curr, prev, isDeletion);
            if (opts.autoUnwatch && isDeletion) {
                return fs.unwatchFile(path, handler);
            }
        };
        watcher = fs.watchFile(path, opts, handler);
        return Promise.resolve(_.extend(watcher, {
            path: path,
            handler: handler
        }));
    },

    /**
     * Watch files, when file changes, the handler will be invoked.
     * It is build on the top of `nofs.watchPath`.
     * @param  {Array} patterns String array with minimatch syntax.
     * Such as `['*\/**.css', 'lib\/**\/*.js']`.
     * @param  {Object} opts Same as the `nofs.watchPath`.
     * @return {Promise} It contains the wrapped watch listeners.
     * @example
     * ```js
     * nofs.watchFiles('*.js', handler: (path, curr, prev, isDeletion) =>
     *     console.log (path)
     * );
     * ```
     */
    watchFiles: function(patterns, opts) {
        if (opts == null) {
            opts = {};
        }
        return nofs.glob(patterns).then(function(paths) {
            return Promise.all(paths.map(function(path) {
                return nofs.watchPath(path, opts);
            }));
        });
    },

    /**
     * Watch directory and all the files in it.
     * It supports three types of change: create, modify, move, delete.
     * By default, `move` event is disabled.
     * It is build on the top of `nofs.watchPath`.
     * @param {String} root
     * @param  {Object} opts Defaults:
     * ```js
     * {
     *     handler: (type, path, oldPath, stats, oldStats) => {},
     *
     *     patterns: '**', // minimatch, string or array
     *
     *     // Whether to watch POSIX hidden file.
     *     all: false,
     *
     *     // The minimatch options.
     *     pmatch: {},
     *
     *     isEnableMoveEvent: false
     * }
     * ```
     * @return {Promise} Resolves a object that keys are paths,
     * values are listeners.
     * @example
     * ```js
     * // Only current folder, and only watch js and css file.
     * nofs.watchDir('lib', {
     *  pattern: '*.+(js|css)',
     *  handler: (type, path, oldPath, stats, oldStats) =>
     *      console.log(type, path, stats.isDirectory(), oldStats.isDirectory())
     * });
     * ```
     */
    watchDir: function(root, opts) {
        var dirHandler, fileHandler, isSameFile, match, negateMath, ref, watchedList;
        if (opts == null) {
            opts = {};
        }

        _.defaults(opts, {
            patterns: '**',
            pmatch: {},
            all: false,
            error: function(err) {
                try {
                    return console.error(err);
                } catch (err) {}
            }
        });

        opts.pmatch.dot = opts.all;
        if (_.isString(opts.patterns)) {
            opts.patterns = [opts.patterns];
        }

        opts.patterns = opts.patterns.map(function(p) {
            if (p[0] === '!') {
                return '!' + npath.join(root, p.slice(1));
            } else {
                return npath.join(root, p);
            }
        });
        ref = nofs.pmatch.matchMultiple(opts.patterns, opts.pmatch), match = ref.match, negateMath = ref.negateMath;

        watchedList = {};

        // TODO: move event
        isSameFile = function(statsA, statsB) {
            // On Unix just "ino" will do the trick, but on Windows
            // "ino" is always zero.
            if (statsA.ctime.ino !== 0 && statsA.ctime.ino === statsB.ctime.ino) {
                return true;
            }

            // Since "size" for Windows is always zero, and the unit of "time"
            // is second, the code below is not reliable.
            return statsA.mtime.getTime() === statsB.mtime.getTime() && statsA.ctime.getTime() === statsB.ctime.getTime() && statsA.size === statsB.size;
        };

        fileHandler = function(path, curr, prev, isDelete) {
            if (isDelete) {
                opts.handler('delete', path, null, curr, prev);
                return delete watchedList[path];
            } else {
                return opts.handler('modify', path, null, curr, prev);
            }
        };

        dirHandler = function(dir, curr, prev, isDelete) {
            // Possible Event Order
            // 1. modify event: file modify.
            // 2. delete event: file delete -> parent modify.
            // 3. create event: parent modify -> file create.
            // 4.   move event: file delete -> parent modify -> file create.

            if (isDelete) {
                opts.handler('delete', dir, null, curr, prev);
                delete watchedList[dir];
                return;
            }

            // Prevent high frequency concurrent fs changes,
            // we should to use Sync function here. But for
            // now if we don't need `move` event, everything is OK.
            return nofs.eachDir(dir, {
                all: opts.all,
                iter: function(fileInfo) {
                    var path;
                    path = fileInfo.path;
                    if (watchedList[path]) {
                        return;
                    }
                    if (fileInfo.isDir) {
                        if (curr) {
                            opts.handler('create', path, null, fileInfo.stats);
                        }
                        return nofs.watchPath(path, {
                            handler: dirHandler
                        }).then(function(listener) {
                            if (listener) {
                                return watchedList[path] = listener;
                            }
                        });
                    } else if (!negateMath(path) && match(path)) {
                        if (curr) {
                            opts.handler('create', path, null, fileInfo.stats);
                        }
                        return nofs.watchPath(path, {
                            handler: fileHandler
                        }).then(function(listener) {
                            if (listener) {
                                return watchedList[path] = listener;
                            }
                        });
                    }
                }
            });
        };

        return dirHandler(root).then(function() {
            return watchedList;
        });
    },

    /**
     * A `writeFile` shim for `< Node v0.10`.
     * @param  {String} path
     * @param  {String | Buffer} data
     * @param  {String | Object} opts
     * @return {Promise}
     */
    writeFile: function(path, data, opts) {
        var encoding, flag, mode;
        if (opts == null) {
            opts = {};
        }
        switch (typeof opts) {
            case 'string':
                encoding = opts;
                break;
            case 'object':
                encoding = opts.encoding, flag = opts.flag, mode = opts.mode;
                break;
            default:
                return Promise.reject(new TypeError('Bad arguments'));
        }
        if (flag == null) {
            flag = 'w';
        }
        if (mode == null) {
            mode = 0x1b6;
        }
        return fs.open(path, flag, mode).then(function(fd) {
            var buf, pos;
            buf = data.constructor.name === 'Buffer' ? data : new Buffer('' + data, encoding);
            pos = flag.indexOf('a') > -1 ? null : 0;
            return fs.write(fd, buf, 0, buf.length, pos).then(function() {
                return fs.close(fd);
            });
        });
    },
    writeFileSync: function(path, data, opts) {
        var buf, encoding, fd, flag, mode, pos;
        if (opts == null) {
            opts = {};
        }
        switch (typeof opts) {
            case 'string':
                encoding = opts;
                break;
            case 'object':
                encoding = opts.encoding, flag = opts.flag, mode = opts.mode;
                break;
            default:
                throw new TypeError('Bad arguments');
        }
        if (flag == null) {
            flag = 'w';
        }
        if (mode == null) {
            mode = 0x1b6;
        }
        fd = fs.openSync(path, flag, mode);
        buf = data.constructor.name === 'Buffer' ? data : new Buffer('' + data, encoding);
        pos = flag.indexOf('a') > -1 ? null : 0;
        fs.writeSync(fd, buf, 0, buf.length, pos);
        return fs.closeSync(fd);
    }
});

(function() {
    var k, name, results;
    results = [];
    for (k in nofs) {
        if (k.slice(-4) === 'Sync') {
            name = k.slice(0, -4);
            fs[name] = _.PromiseUtils.callbackify(nofs[name]);
        }
        results.push(fs[k] = nofs[k]);
    }
    return results;
})();

require('./alias')(fs);

module.exports = fs;
