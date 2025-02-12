module.exports = function(fs) {
  var alias, aliasList, k, list, results;
  aliasList = {
    ensureFile: ['createFile'],
    mkdirs: ['ensureDir', 'mkdirp'],
    outputJson: ['outputJSON'],
    readJson: ['readJSON'],
    remove: ['delete']
  };
  results = [];
  for (k in aliasList) {
    list = aliasList[k];
    results.push((function() {
      var i, len, results1;
      results1 = [];
      for (i = 0, len = list.length; i < len; i++) {
        alias = list[i];
        fs[alias] = fs[k];
        results1.push(fs[alias + 'Sync'] = fs[k + 'Sync']);
      }
      return results1;
    })());
  }
  return results;
};
