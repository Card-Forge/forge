'use strict';
var _, minimatch;

minimatch = require('minimatch');

_ = require('./utils');

_.extend(minimatch, {

  /**
  	 * Check if a string is a minimatch pattern.
  	 * @param  {String | Object}
  	 * @return {Pmatch | undefined}
   */
  isPmatch: function(target) {
    var pm;
    if (_.isString(target)) {
      if (!target) {
        return;
      }
      pm = new minimatch.Minimatch(target);
      if (minimatch.isNotPlain(pm)) {
        return pm;
      }
    } else if (target instanceof minimatch.Minimatch) {
      return target;
    }
  },
  isNotPlain: function(pm) {
    return pm.set.length > 1 || !_.all(pm.set[0], _.isString);
  },
  matchMultiple: function(patterns, opts) {
    var j, len, match, negateMath, negates, p, pmatches;
    negates = [];
    pmatches = [];
    if (!opts.nonegate) {
      for (j = 0, len = patterns.length; j < len; j++) {
        p = patterns[j];
        (p[0] === '!' ? negates : pmatches).push(p);
      }
      if (pmatches.length === 0) {
        pmatches = negates;
        negates = [];
      }
    }
    pmatches = pmatches.map(function(p) {
      return new minimatch.Minimatch(p, opts);
    });
    negates = negates.length === 0 ? null : negates.map(function(p) {
      return new minimatch.Minimatch(p.slice(1), opts);
    });
    match = function(path, partial) {
      return _.any(pmatches, function(pm) {
        return pm.match(path, partial);
      });
    };
    negateMath = function(path, partial) {
      if (!negates) {
        return;
      }
      return _.any(negates, function(pm) {
        return pm.match(path, partial);
      });
    };
    return {
      pmatches: pmatches,
      negateMath: negateMath,
      match: match
    };
  },

  /**
  	 * Get the plain path of the pattern.
  	 * @param  {Pmatch} pm
  	 * @return {String}
   */
  getPlainPath: function(pm) {
    var base, i, j, k, l, len, p, paths, ref, res, rest, same;
    paths = pm.set.map(function(p) {
      var j, len, plain, s;
      plain = [];
      for (j = 0, len = p.length; j < len; j++) {
        s = p[j];
        if (_.isString(s)) {
          plain.push(s);
        } else {
          return plain;
        }
      }
      return plain;
    });
    if (paths.length === 1) {
      res = paths[0];
    } else {
      l = Math.min.apply(0, paths.map(function(p) {
        return p.length;
      }));
      rest = paths.slice(1);
      res = [];
      for (i = j = 0, ref = l; 0 <= ref ? j < ref : j > ref; i = 0 <= ref ? ++j : --j) {
        base = paths[0][i];
        same = true;
        for (k = 0, len = rest.length; k < len; k++) {
          p = rest[k];
          if (p[i] !== base) {
            same = false;
            continue;
          }
        }
        if (same) {
          res.push(p[i]);
        }
      }
    }
    return res.join('/');
  }
});

module.exports = minimatch;
