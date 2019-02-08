const SvgSelection = require('./svg-selection');

module.exports = {
  select: (domElement) => SvgSelection.fromDom(domElement),
  createGroup: (elementId) => {
    const result = document.createElementNS('http://www.w3.org/2000/svg', 'g');
    result.setAttribute('id', elementId);
    return SvgSelection.fromDom(result);
  }
};