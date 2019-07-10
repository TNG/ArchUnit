const SvgSelectionMock = require('./svg-selection-mock').SvgSelectionMock;
const RootSvgMock = require('./svg-selection-mock').RootSvgMock;

module.exports = {
  createEmptyElement: () => SvgSelectionMock.fromDom(),
  createSvgRoot: () => new RootSvgMock(),
  createGroup: (elementId) => SvgSelectionMock.fromDom('g', {id: elementId})
};