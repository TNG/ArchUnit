const SvgSelectionMock = require('./svg-selection-mock').SvgSelectionMock;

module.exports = {
  select: () => SvgSelectionMock.fromDom(),
  createGroup: (elementId) => SvgSelectionMock.fromDom('g', {id: elementId})
};