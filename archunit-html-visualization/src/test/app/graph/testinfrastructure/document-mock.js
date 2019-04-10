'use strict';

const DivSelectionMock = require('./svg-selection-mock').DivSelectionMock;
const clientWidth = 500;
const clientHeight = 500;

let keyup;

module.exports = {
  selectDiv: (domElement) => DivSelectionMock.fromDom(domElement),
  onKeyUp: (keyupHandler) => keyup = keyupHandler,
  getClientWidth: () => clientWidth,
  getClientHeight: () => clientHeight,
  ctrlKeyup: () => keyup({key: 'Control'})
};