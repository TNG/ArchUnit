module.exports.workarounds = {
  getOwnerDocument: () => document.currentScript ?
      document.currentScript.ownerDocument :
      document._currentScript.ownerDocument
};