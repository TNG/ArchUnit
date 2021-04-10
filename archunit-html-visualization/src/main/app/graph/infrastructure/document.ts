'use strict';

const {DivSelection} = require('./svg-selection');

interface ArchUnitDocument {
  selectDiv: (domElement: Element) => typeof DivSelection
  // onKeyUp: (keyupHandler) => document.onkeyup = keyupHandler,
  getClientWidth: () => number,
  getClientHeight: () => number
}

const init: ArchUnitDocument = {
  selectDiv: (domElement: Element) => DivSelection.fromDom(domElement),
  // onKeyUp: (keyupHandler) => document.onkeyup = keyupHandler,
  getClientWidth: () => document.documentElement.clientWidth,
  getClientHeight: () => document.documentElement.clientHeight
};

export {ArchUnitDocument, init}
