'use strict';

const DivSelection = require('./svg-selection').DivSelection;

module.exports = {
  selectDiv: (domElement) => DivSelection.fromDom(domElement),
  onKeyUp: (keyupHandler) => document.onkeyup = keyupHandler,
  getClientWidth: () => document.documentElement.clientWidth,
  getClientHeight: () => document.documentElement.clientHeight
};