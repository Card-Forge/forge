const { contextBridge, ipcRenderer } = require('electron');
contextBridge.exposeInMainWorld('forge', {
  status: () => ipcRenderer.invoke('status'),
  request: (method, params = {}) => ipcRenderer.invoke('engine', method, params),
  art: name => ipcRenderer.invoke('art', name),
  importFile: () => ipcRenderer.invoke('import-file'),
  exportFile: kind => ipcRenderer.invoke('export-file', kind),
  copyDeck: () => ipcRenderer.invoke('copy-deck'),
  onStatus: callback => {
    const listener = (_event, status) => callback(status);
    ipcRenderer.on('engine-status', listener);
    return () => ipcRenderer.removeListener('engine-status', listener);
  }
});
