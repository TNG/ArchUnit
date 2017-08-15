'use strict';

const d3 = require('d3');

module.exports.newInstance = getCalculateTextWidth => {
  return {
    calculateTextWidth: getCalculateTextWidth(),
    /**
     * Creates a circle packing for the supplied circles (circles are represented as {x: $x, y: $y, r: $radius}).
     * Only the radius of the supplied circles is relevant,
     * x- and y-coordinates will be calculated and and overridden.
     * Returns a circle (represented as {x: $x, y: $y, r: $radius}) enclosing the created circle packing.
     * @param circles An array representing circles
     * @param padding The padding between the circles
     */
    packCirclesAndReturnEnclosingCircle: (circles, padding = 0) => {
      circles.forEach(c => c.r += padding);
      d3.packSiblings(circles);
      const enclosingCircle = d3.packEnclose(circles);
      circles.forEach(c => c.r -= padding);
      return enclosingCircle;
    }
  };
};