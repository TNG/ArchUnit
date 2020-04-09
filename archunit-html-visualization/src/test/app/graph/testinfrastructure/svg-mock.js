const SvgSelectionMock = require('./svg-selection-mock').SvgSelectionMock;
const RootSvgMock = require('./svg-selection-mock').RootSvgMock;

module.exports = {
  createEmptyElement: () => SvgSelectionMock.fromDom(),
  createSvgRoot: () => new RootSvgMock(),
  // following functions are not unused although the IDE states otherwise
  createGroup: (elementId) => SvgSelectionMock.fromDom('g', {id: elementId}),
  select: (domElement) => SvgSelectionMock.fromDom(domElement)
};
