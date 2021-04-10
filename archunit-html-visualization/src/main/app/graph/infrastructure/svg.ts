const {SvgSelection} = require("./svg-selection");

export interface SVG {
  select: (domElement: Element) => typeof SvgSelection
  createGroup: (elementId: string) => typeof SvgSelection
}

module.exports = {
  select: (domElement: Element) => SvgSelection.fromDom(domElement),
  createGroup: (elementId: string) => {
    const result = document.createElementNS('http://www.w3.org/2000/svg', 'g');
    result.setAttribute('id', elementId);
    return SvgSelection.fromDom(result);
  }
};
