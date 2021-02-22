'use strict';

const NODE_TEXT_STYLE_SELECTOR = '.node text';
const CIRCLE_STYLE_SELECTOR = '.circle';

interface VisualizationStyles {
  getNodeFontSize: () => number,

  setNodeFontSize: (sizeInPixels: number) => void,

  getCirclePadding: () => number,

  setCirclePadding: (paddingInPixels: number) => void,

  getDependencyTitleFontSize: () => number
}

const DEPENDENCY_TEXT_STYLE_SELECTOR = 'text.access';

// const rgbToHex = (rgbString: string, defaultHex:string) => {
//   if (!rgbString) {
//     return defaultHex;
//   }
//   const numbersAsString: string[] = rgbString.split(",");
//   const numbers = numbersAsString.map(n => parseInt(n.replace(/[rgb()\s]/g, "")));
//   const numbersAsHex = numbers.map(n => {
//     const hex = n.toString(16);
//     return hex.length === 1 ? "0" + hex : hex;
//   });
//   return numbersAsHex.reduce((acc, n) => acc + n, "#");
// };

const stylesFrom = (styleSheet: CSSStyleSheet): VisualizationStyles => {
  const unique = (elements: CSSPageRule[]): CSSPageRule => {
    if (elements.length === 0) {
      return null;
    }
    if (elements.length !== 1) {
      throw new Error('Expecting exactly one element in ' + elements);
    }
    return elements[0];
  };
  const cssRules = Array.from(styleSheet.cssRules) as CSSPageRule[]; // CSSPageRule extends CSSRule
  const nodeTextRule = unique(cssRules.filter(rule => rule.selectorText === NODE_TEXT_STYLE_SELECTOR));
  const circleRule = unique(cssRules.filter(rule => rule.selectorText === CIRCLE_STYLE_SELECTOR));
  const dependencyTextRule = unique(cssRules.filter(rule => rule.selectorText === DEPENDENCY_TEXT_STYLE_SELECTOR));

  return {
    getNodeFontSize: () => parseInt(nodeTextRule.style.getPropertyValue('font-size'), 10),

    setNodeFontSize: (sizeInPixels: number) => {
      nodeTextRule.style.setProperty('font-size', `${sizeInPixels}px`);
    },

    getCirclePadding: () => parseInt(circleRule.style.getPropertyValue('padding'), 10),

    setCirclePadding: (paddingInPixels: number) => {
      circleRule.style.setProperty('padding', `${paddingInPixels}px`)
    },

    getDependencyTitleFontSize: () => parseInt(dependencyTextRule.style.getPropertyValue('font-size'), 10),

    // getLineStyle: (kind, title) => {
    //   const rule = unique(cssRules.filter(rule => rule.selectorText === LINE_STYLE_PREFIX + kind));
    //   return {
    //     title: title,
    //     styles: [{
    //       name: "stroke",
    //       value: rgbToHex(rule.style.getPropertyValue('stroke'), "#000000")
    //     }, {
    //       name: "stroke-dasharray",
    //       value: rule.style.getPropertyValue('stroke-dasharray')
    //     }]
    //   };
    // }
  };
};

export {VisualizationStyles, stylesFrom};
