# [nofs](https://github.com/ysmood/nofs)

## Overview

`nofs` extends Node's native `fs` module with some useful methods. It tries
to make your functional programming experience better. It's one of the core
lib of [nokit][].

[![NPM version](https://badge.fury.io/js/nofs.svg)](http://badge.fury.io/js/nofs) [![Build Status](https://travis-ci.org/ysmood/nofs.svg)](https://travis-ci.org/ysmood/nofs) [![Build status](https://ci.appveyor.com/api/projects/status/github/ysmood/nofs?svg=true)](https://ci.appveyor.com/project/ysmood/nofs)
 [![Deps Up to Date](https://david-dm.org/ysmood/nofs.svg?style=flat)](https://david-dm.org/ysmood/nofs)

## Features

- Introduce `map` and `reduce` to folders.
- Recursive `glob`, `move`, `copy`, `remove`, etc.
- **Promise** by default.
- Unified intuitive API. Supports both **Promise**, **Sync** and **Callback** paradigms.
- Very light weight. Only depends on `yaku` and `minimath`.

## Install

```shell
npm install nofs
```

## API Convention

### Path & Pattern

Only functions like `readFile` which may confuse the user don't support pattern.

### Promise & Callback

If you call an async function without callback, it will return a promise.
For example the `nofs.remove('dir', () => 'done!')` are the same with
`nofs.remove('dir').then(() => 'done!')`.

### [eachDir](#eachDir)

It is the core function for directory manipulation. Other abstract functions
like `mapDir`, `reduceDir`, `glob` are built on top of it. You can play
with it if you don't like other functions.

### nofs & Node Native fs

Only the callback of `nofs.exists`
is slightly different, it will also gets two arguments `(err, exists)`.

`nofs` only extends the native module, no pollution will be found. You can
still require the native `fs`, and call `fs.exists` as easy as pie.

### Inheritance of Options

A Function's options may inherit other function's, especially the functions it calls internally. Such as the `glob` extends the `eachDir`'s
option, therefore `glob` also has a `filter` option.

## Quick Start

```js
// You can replace "require('fs')" with "require('nofs')"
let fs = require('nofs');

/*
 * Callback
 */
fs.outputFile('x.txt', 'test', (err) => {
    console.log('done');
});


/*
 * Sync
 */
fs.readFileSync('x.txt');
fs.copySync('dir/a', 'dir/b');


/*
 * Promise & async/await
 */
(async () => {
    await fs.mkdirs('deep/dir/path');
    await fs.outputFile('a.txt', 'hello world');
    await fs.move('dir/path', 'other');
    await fs.copy('one/**/*.js', 'two');

    // Get all files, except js files.
    let list = await fs.glob(['deep/**', '!**/*.js']);
    console.log(list);

    // Remove only js files.
    await fs.remove('deep/**/*.js');
})();


/*
 * Concat all css files.
 */
fs.reduceDir('dir/**/*.css', {
    init: '/* Concated by nofs */\n',
    iter (sum, { path }) {
        return fs.readFile(path).then(str =>
            sum += str + '\n'
        );
    }
}).then(concated =>
    console.log(concated)
);



/*
 * Play with the low level api.
 * Filter all the ignored files with high performance.
 */
let patterns = fs.readFileSync('.gitignore', 'utf8').split('\n');

let filter = ({ path }) => {
    for (let p of patterns) {
        // This is only a demo, not full git syntax.
        if (path.indexOf(p) === 0)
            return false;
    }
    return true;
}

fs.eachDir('.', {
    searchFilter: filter, // Ensure subdirectory won't be searched.
    filter: filter,
    iter: (info) => info  // Directly return the file info object.
}).then((tree) =>
    // Instead a list as usual,
    // here we get a file tree for further usage.
    console.log(tree)
);
```


## Changelog

Goto [changelog](doc/changelog.md)

## Function Name Alias

For some naming convention reasons, `nofs` also uses some common alias for fucntion names. See [src/alias.js](src/alias.js).

## FAQ

- `Error: EMFILE`?

  > This is due to system's default file descriptor number settings for one process.
  > Latest node will increase the value automatically.
  > See the [issue list](https://github.com/joyent/node/search?q=EMFILE&type=Issues&utf8=%E2%9C%93) of `node`.

## API

__No native `fs` funtion will be listed.__

  - [Promise](#promise)
  - [copyDir(src, dest, opts)](#copydirsrc-dest-opts)
  - [copyFile(src, dest, opts)](#copyfilesrc-dest-opts)
  - [copy(from, to, opts)](#copyfrom-to-opts)
  - [dirExists(path)](#direxistspath)
  - [eachDir(spath, opts)](#eachdirspath-opts)
  - [ensureFile(path, opts)](#ensurefilepath-opts)
  - [fileExists(path)](#fileexistspath)
  - [glob(pattern, opts)](#globpattern-opts)
  - [mapDir(from, to, opts)](#mapdirfrom-to-opts)
  - [mkdirs(path, mode)](#mkdirspath-mode)
  - [move(from, to, opts)](#movefrom-to-opts)
  - [outputFile(path, data, opts)](#outputfilepath-data-opts)
  - [outputJson(path, obj, opts)](#outputjsonpath-obj-opts)
  - [path](#path)
  - [pmatch](#pmatch)
  - [Promise](#promise)
  - [PromiseUtils](#promiseutils)
  - [readJson(path, opts)](#readjsonpath-opts)
  - [reduceDir(path, opts)](#reducedirpath-opts)
  - [remove(path, opts)](#removepath-opts)
  - [touch(path, opts)](#touchpath-opts)
  - [watchPath(path, opts)](#watchpathpath-opts)
  - [watchFiles(patterns, opts)](#watchfilespatterns-opts)
  - [watchDir(root, opts)](#watchdirroot-opts)
  - [writeFile(path, data, opts)](#writefilepath-data-opts)

- ### **[Promise](src/main.js?source#L17)**

    Here I use [Yaku](https://github.com/ysmood/yaku) only as an ES6 shim for Promise.
    No APIs other than ES6 spec will be used. In the
    future it will be removed.

- ### **[copyDir(src, dest, opts)](src/main.js?source#L58)**

    Copy an empty directory.

    - **<u>param</u>**: `src` { _String_ }

    - **<u>param</u>**: `dest` { _String_ }

    - **<u>param</u>**: `opts` { _Object_ }

        ```js
        {
            isForce: false,
            mode: auto
        }
        ```

    - **<u>return</u>**: { _Promise_ }

- ### **[copyFile(src, dest, opts)](src/main.js?source#L138)**

    Copy a single file.

    - **<u>param</u>**: `src` { _String_ }

    - **<u>param</u>**: `dest` { _String_ }

    - **<u>param</u>**: `opts` { _Object_ }

        ```js
        {
            isForce: false,
            mode: auto
        }
        ```

    - **<u>return</u>**: { _Promise_ }

- ### **[copy(from, to, opts)](src/main.js?source#L269)**

    Like `cp -r`.

    - **<u>param</u>**: `from` { _String_ }

        Source path.

    - **<u>param</u>**: `to` { _String_ }

        Destination path.

    - **<u>param</u>**: `opts` { _Object_ }

        Extends the options of [eachDir](#eachDir-opts).
        Defaults:
        ```js
        {
            // Overwrite file if exists.
            isForce: false,
            isIterFileOnly: false

            filter: (fileInfo) => true
        }
        ```

    - **<u>return</u>**: { _Promise_ }

    - **<u>example</u>**:

        Copy the contents of the directory rather than copy the directory itself.
        ```js
        nofs.copy('dir/path/**', 'dest/path');

        nofs.copy('dir/path', 'dest/path', {
            filter: (fileInfo) => {
                return /\d+/.test(fileInfo.path);
            }
        });
        ```

- ### **[dirExists(path)](src/main.js?source#L392)**

    Check if a path exists, and if it is a directory.

    - **<u>param</u>**: `path` { _String_ }

    - **<u>return</u>**: { _Promise_ }

        Resolves a boolean value.

- ### **[eachDir(spath, opts)](src/main.js?source#L535)**

    <a name='eachDir'></a>
    Concurrently walks through a path recursively with a callback.
    The callback can return a Promise to continue the sequence.
    The resolving order is also recursive, a directory path resolves
    after all its children are resolved.

    - **<u>param</u>**: `spath` { _String_ }

        The path may point to a directory or a file.

    - **<u>param</u>**: `opts` { _Object_ }

        Optional. <a id='eachDir-opts'></a> Defaults:
        ```js
        {
            // Callback on each path iteration.
            iter: (fileInfo) => Promise | Any,

            // Auto check if the spath is a minimatch pattern.
            isAutoPmatch: true,

            // Include entries whose names begin with a dot (.), the posix hidden files.
            all: true,

            // To filter paths. It can also be a RegExp or a glob pattern string.
            // When it's a string, it extends the Minimatch's options.
            filter: (fileInfo) => true,

            // The current working directory to search.
            cwd: '',

            // Call iter only when it is a file.
            isIterFileOnly: false,

            // Whether to include the root directory or not.
            isIncludeRoot: true,

            // Whehter to follow symbol links or not.
            isFollowLink: true,

            // Iterate children first, then parent folder.
            isReverse: false,

            // When isReverse is false, it will be the previous iter resolve value.
            val: any,

            // If it return false, sub-entries won't be searched.
            // When the `filter` option returns false, its children will
            // still be itered. But when `searchFilter` returns false, children
            // won't be itered by the iter.
            searchFilter: (fileInfo) => true,

            // If you want sort the names of each level, you can hack here.
            // Such as `(names) => names.sort()`.
            handleNames: (names) => names
        }
        ```
        The argument of `opts.iter`, `fileInfo` object has these properties:
        ```js
        {
            path: String,
            name: String,
            baseDir: String,
            isDir: Boolean,
            children: [fileInfo],
            stats: fs.Stats,
            val: Any
        }
        ```
        Assume we call the function: `nofs.eachDir('dir', { iter: (f) => f })`,
        the resolved directory object array may look like:
        ```js
        {
            path: 'some/dir/path',
            name: 'path',
            baseDir: 'some/dir',
            isDir: true,
            val: 'test',
            children: [
                {
                    path: 'some/dir/path/a.txt', name: 'a.txt',
                    baseDir: 'dir', isDir: false, stats: { ... }
                },
                { path: 'some/dir/path/b.txt', name: 'b.txt', ... }
            ],
            stats: {
                size: 527,
                atime: 'Mon, 10 Oct 2011 23:24:11 GMT',
                mtime: 'Mon, 10 Oct 2011 23:24:11 GMT',
                ctime: 'Mon, 10 Oct 2011 23:24:11 GMT'
                ...
            }
        }
        ```
        The `stats` is a native `fs.Stats` object.

    - **<u>return</u>**: { _Promise_ }

        Resolves a directory tree object.

    - **<u>example</u>**:

        ```js
        // Print all file and directory names, and the modification time.
        nofs.eachDir('dir/path', {
            iter: (obj, stats) =>
                console.log(obj.path, stats.mtime)
        });

        // Print path name list.
        nofs.eachDir('dir/path', { iter: (curr) => curr })
        .then((tree) =>
            console.log(tree)
        );

        // Find all js files.
        nofs.eachDir('dir/path', {
            filter: '**/*.js',
            iter: ({ path }) =>
                console.log(paths)
        });

        // Find all js files.
        nofs.eachDir('dir/path', {
            filter: /\.js$/,
            iter: ({ path }) =>
                console.log(paths)
        });

        // Custom filter.
        nofs.eachDir('dir/path', {
            filter: ({ path, stats }) =>
                path.slice(-1) != '/' && stats.size > 1000
            iter: (path) =>
                console.log(path)
        });
        ```

- ### **[ensureFile(path, opts)](src/main.js?source#L852)**

    Ensures that the file exists.
    Change file access and modification times.
    If the file does not exist, it is created.
    If the file exists, it is NOT MODIFIED.

    - **<u>param</u>**: `path` { _String_ }

    - **<u>param</u>**: `opts` { _Object_ }

    - **<u>return</u>**: { _Promise_ }

- ### **[fileExists(path)](src/main.js?source#L878)**

    Check if a path exists, and if it is a file.

    - **<u>param</u>**: `path` { _String_ }

    - **<u>return</u>**: { _Promise_ }

        Resolves a boolean value.

- ### **[glob(pattern, opts)](src/main.js?source#L935)**

    Get files by patterns.

    - **<u>param</u>**: `pattern` { _String | Array_ }

        The minimatch pattern.
        Patterns that starts with '!' in the array will be used
        to exclude paths.

    - **<u>param</u>**: `opts` { _Object_ }

        Extends the options of [eachDir](#eachDir-opts).
        **The `filter` property is fixed with the pattern, use `iter` instead**.
        Defaults:
        ```js
        {
            all: false,

            // The minimatch option object.
            pmatch: {},

            // It will be called after each match. It can also return
            // a promise.
            iter: (fileInfo, list) => list.push(fileInfo.path)
        }
        ```

    - **<u>return</u>**: { _Promise_ }

        Resolves the list array.

    - **<u>example</u>**:

        ```js
        // Get all js files.
        nofs.glob(['**/*.js', '**/*.css']).then((paths) =>
            console.log(paths)
        );

        // Exclude some files. "a.js" will be ignored.
        nofs.glob(['**/*.js', '!**/a.js']).then((paths) =>
            console.log(paths)
        );

        // Custom the iterator. Append '/' to each directory path.
        nofs.glob('**/*.js', {
            iter: (info, list) =>
                list.push(info.isDir ? (info.path + '/') : info.path
        }).then((paths) =>
            console.log(paths)
        );
        ```

- ### **[mapDir(from, to, opts)](src/main.js?source#L1081)**

    Map file from a directory to another recursively with a
    callback.

    - **<u>param</u>**: `from` { _String_ }

        The root directory to start with.

    - **<u>param</u>**: `to` { _String_ }

        This directory can be a non-exists path.

    - **<u>param</u>**: `opts` { _Object_ }

        Extends the options of [eachDir](#eachDir-opts). But `cwd` is
        fixed with the same as the `from` parameter. Defaults:
        ```js
        {
            // It will be called with each path. The callback can return
            // a `Promise` to keep the async sequence go on.
            iter: (src, dest, fileInfo) => Promise | Any,

            // When isMapContent is true, and the current is a file.
            iter: (content, src, dest, fileInfo) => Promise | Any,

            // When isMapContent is true, and the current is a folder.
            iter: (mode, src, dest, fileInfo) => Promise | Any,

            isMapContent: false,

            isIterFileOnly: true
        }
        ```

    - **<u>return</u>**: { _Promise_ }

        Resolves a tree object.

    - **<u>example</u>**:

        ```js
        nofs.mapDir('from', 'to', {
            iter: (src, dest, info) =>
                console.log(src, dest, info)
        });
        ```

    - **<u>example</u>**:

        ```js
        // Copy and add license header for each files
        // from a folder to another.
        nofs.mapDir('from', 'to', {
            ismMapContent: true,
            iter: (content) =>
                'License MIT\n' + content
        });
        ```

- ### **[mkdirs(path, mode)](src/main.js?source#L1170)**

    Recursively create directory path, like `mkdir -p`.

    - **<u>param</u>**: `path` { _String_ }

    - **<u>param</u>**: `mode` { _String_ }

        Defaults: `0o777 & ~process.umask()`

    - **<u>return</u>**: { _Promise_ }

- ### **[move(from, to, opts)](src/main.js?source#L1231)**

    Moves a file or directory. Also works between partitions.
    Behaves like the Unix `mv`.

    - **<u>param</u>**: `from` { _String_ }

        Source path.

    - **<u>param</u>**: `to` { _String_ }

        Destination path.

    - **<u>param</u>**: `opts` { _Object_ }

        Defaults:
        ```js
        {
            isForce: false,
            isFollowLink: false
        }
        ```

    - **<u>return</u>**: { _Promise_ }

        It will resolve a boolean value which indicates
        whether this action is taken between two partitions.

- ### **[outputFile(path, data, opts)](src/main.js?source#L1323)**

    Almost the same as `writeFile`, except that if its parent
    directories do not exist, they will be created.

    - **<u>param</u>**: `path` { _String_ }

    - **<u>param</u>**: `data` { _String | Buffer_ }

    - **<u>param</u>**: `opts` { _String | Object_ }

        <a id="outputFile-opts"></a>
        Same with the [writeFile](#writeFile-opts).

    - **<u>return</u>**: { _Promise_ }

- ### **[outputJson(path, obj, opts)](src/main.js?source#L1368)**

    Write a object to a file, if its parent directory doesn't
    exists, it will be created.

    - **<u>param</u>**: `path` { _String_ }

    - **<u>param</u>**: `obj` { _Any_ }

        The data object to save.

    - **<u>param</u>**: `opts` { _Object | String_ }

        Extends the options of [outputFile](#outputFile-opts).
        Defaults:
        ```js
        {
            replacer: null,
            space: null
        }
        ```

    - **<u>return</u>**: { _Promise_ }

- ### **[path](src/main.js?source#L1409)**

    The path module nofs is using.
    It's the native [io.js](iojs.org) path lib.
    nofs will force all the path separators to `/`,
    such as `C:\a\b` will be transformed to `C:/a/b`.

    - **<u>type</u>**: { _Object_ }

- ### **[pmatch](src/main.js?source#L1433)**

    The `minimatch` lib. It has two extra methods:
    - `isPmatch(String | Object) -> Pmatch | undefined`
        It helps to detect if a string or an object is a minimatch.

    - `getPlainPath(Pmatch) -> String`
        Helps to get the plain root path of a pattern. Such as `src/js/*.js`
        will get `src/js`

    [Documentation](https://github.com/isaacs/minimatch)

    [Offline Documentation](?gotoDoc=minimatch/readme.md)

    - **<u>example</u>**:

        ```js
        nofs.pmatch('a/b/c.js', '**/*.js');
        // output => true
        nofs.pmatch.isPmatch('test*');
        // output => true
        nofs.pmatch.isPmatch('test/b');
        // output => false
        ```

- ### **[Promise](src/main.js?source#L1439)**

    What promise this lib is using.

    - **<u>type</u>**: { _Promise_ }

- ### **[PromiseUtils](src/main.js?source#L1445)**

    Same as the [`yaku/lib/utils`](https://github.com/ysmood/yaku#utils).

    - **<u>type</u>**: { _Object_ }

- ### **[readJson(path, opts)](src/main.js?source#L1459)**

    Read A Json file and parse it to a object.

    - **<u>param</u>**: `path` { _String_ }

    - **<u>param</u>**: `opts` { _Object | String_ }

        Same with the native `nofs.readFile`.

    - **<u>return</u>**: { _Promise_ }

        Resolves a parsed object.

    - **<u>example</u>**:

        ```js
        nofs.readJson('a.json').then((obj) =>
            console.log(obj.name, obj.age)
        );
        ```

- ### **[reduceDir(path, opts)](src/main.js?source#L1512)**

    Walk through directory recursively with a iterator.

    - **<u>param</u>**: `path` { _String_ }

    - **<u>param</u>**: `opts` { _Object_ }

        Extends the options of [eachDir](#eachDir-opts),
        with some extra options:
        ```js
        {
            iter: (prev, path, isDir, stats) -> Promise | Any,

            // The init value of the walk.
            init: undefined,

            isIterFileOnly: true
        }
        ```

    - **<u>return</u>**: { _Promise_ }

        Final resolved value.

    - **<u>example</u>**:

        ```js
        // Concat all files.
        nofs.reduceDir('dir/path', {
            init: '',
            iter: (val, { path }) =>
                nofs.readFile(path).then((str) =>
                    val += str + '\n'
                )
        }).then((ret) =>
            console.log(ret)
        );
        ```

- ### **[remove(path, opts)](src/main.js?source#L1561)**

    Remove a file or directory peacefully, same with the `rm -rf`.

    - **<u>param</u>**: `path` { _String_ }

    - **<u>param</u>**: `opts` { _Object_ }

        Extends the options of [eachDir](#eachDir-opts). But
        the `isReverse` is fixed with `true`. Defaults:
        ```js
        { isFollowLink: false }
        ```

    - **<u>return</u>**: { _Promise_ }

- ### **[touch(path, opts)](src/main.js?source#L1655)**

    Change file access and modification times.
    If the file does not exist, it is created.

    - **<u>param</u>**: `path` { _String_ }

    - **<u>param</u>**: `opts` { _Object_ }

        Default:
        ```js
        {
            atime: Date.now(),
            mtime: Date.now(),
            mode: undefined
        }
        ```

    - **<u>return</u>**: { _Promise_ }

        If new file created, resolves true.

- ### **[watchPath(path, opts)](src/main.js?source#L1730)**

    <a id="writeFile-opts"></a>
    Watch a file. If the file changes, the handler will be invoked.
    You can change the polling interval by using `process.env.pollingWatch`.
    Use `process.env.watchPersistent = 'off'` to disable the persistent.
    Why not use `nofs.watch`? Because `nofs.watch` is unstable on some file
    systems, such as Samba or OSX.

    - **<u>param</u>**: `path` { _String_ }

        The file path

    - **<u>param</u>**: `opts` { _Object_ }

        Defaults:
        ```js
        {
            handler: (path, curr, prev, isDeletion) => {},

            // Auto unwatch the file while file deletion.
            autoUnwatch: true,

            persistent: process.env.watchPersistent != 'off',
            interval: +process.env.pollingWatch || 300
        }
        ```

    - **<u>return</u>**: { _Promise_ }

        It resolves the `StatWatcher` object:
        ```js
        {
            path,
            handler
        }
        ```

    - **<u>example</u>**:

        ```js
        process.env.watchPersistent = 'off'
        nofs.watchPath('a.js', {
            handler: (path, curr, prev, isDeletion) => {
                if (curr.mtime !== prev.mtime)
                    console.log(path);
            }
        }).then((watcher) =>
            nofs.unwatchFile(watcher.path, watcher.handler)
        );
        ```

- ### **[watchFiles(patterns, opts)](src/main.js?source#L1769)**

    Watch files, when file changes, the handler will be invoked.
    It is build on the top of `nofs.watchPath`.

    - **<u>param</u>**: `patterns` { _Array_ }

        String array with minimatch syntax.
        Such as `['*/**.css', 'lib/**/*.js']`.

    - **<u>param</u>**: `opts` { _Object_ }

        Same as the `nofs.watchPath`.

    - **<u>return</u>**: { _Promise_ }

        It contains the wrapped watch listeners.

    - **<u>example</u>**:

        ```js
        nofs.watchFiles('*.js', handler: (path, curr, prev, isDeletion) =>
            console.log (path)
        );
        ```

- ### **[watchDir(root, opts)](src/main.js?source#L1814)**

    Watch directory and all the files in it.
    It supports three types of change: create, modify, move, delete.
    By default, `move` event is disabled.
    It is build on the top of `nofs.watchPath`.

    - **<u>param</u>**: `root` { _String_ }

    - **<u>param</u>**: `opts` { _Object_ }

        Defaults:
        ```js
        {
            handler: (type, path, oldPath, stats, oldStats) => {},

            patterns: '**', // minimatch, string or array

            // Whether to watch POSIX hidden file.
            all: false,

            // The minimatch options.
            pmatch: {},

            isEnableMoveEvent: false
        }
        ```

    - **<u>return</u>**: { _Promise_ }

        Resolves a object that keys are paths,
        values are listeners.

    - **<u>example</u>**:

        ```js
        // Only current folder, and only watch js and css file.
        nofs.watchDir('lib', {
         pattern: '*.+(js|css)',
         handler: (type, path, oldPath, stats, oldStats) =>
             console.log(type, path, stats.isDirectory(), oldStats.isDirectory())
        });
        ```

- ### **[writeFile(path, data, opts)](src/main.js?source#L1932)**

    A `writeFile` shim for `< Node v0.10`.

    - **<u>param</u>**: `path` { _String_ }

    - **<u>param</u>**: `data` { _String | Buffer_ }

    - **<u>param</u>**: `opts` { _String | Object_ }

    - **<u>return</u>**: { _Promise_ }



## Benckmark

See the `benchmark` folder.

```
Node v0.10, Intel Core i7 2.3GHz SSD, find 91,852 js files in 191,585 files:

node-glob: 9939ms
nofs-glob: 8787ms
```

Nofs is slightly faster.

## Lisence

MIT


[nokit]: https://github.com/ysmood/nokit