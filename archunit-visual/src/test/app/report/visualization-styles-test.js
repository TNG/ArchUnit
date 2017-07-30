'use strict';

const visualizationStyles = require('./main-files').get('visualization-styles');
const expect = require("chai").expect;

const NODE_TEXT_STYLE_SELECTOR = '.node text';
const CIRCLE_STYLE_SELECTOR = '.circle';
const LINE_STYLE_SELECTOR = 'line';

describe('visualization-styles', () => {
  it('should retrieve the node font-size in pixels', () => {
    const expectedPixels = 15;
    const styles = multipleStylesContaining(
      nodeTextStyleWithFontSizeInPixels(expectedPixels),
      circleStyleWithPaddingInPixels(99));

    expect(styles.getNodeFontSize()).to.equal(expectedPixels);
  });

  it('should update the node font-size', () => {
    const expectedPixels = 15;
    const styles = multipleStylesContaining(
      nodeTextStyleWithFontSizeInPixels(99),
      circleStyleWithPaddingInPixels(99));

    expect(styles.getNodeFontSize()).to.equal(99);

    styles.setNodeFontSize(expectedPixels);

    expect(styles.getNodeFontSize()).to.equal(expectedPixels);
  });

  it('should retrieve the circle padding in pixels', () => {
    const expectedPixels = 15;
    const styles = multipleStylesContaining(
      nodeTextStyleWithFontSizeInPixels(99),
      circleStyleWithPaddingInPixels(expectedPixels));

    expect(styles.getCirclePadding()).to.equal(expectedPixels);
  });

  it('should update the circle padding', () => {
    const expectedPixels = 15;
    const styles = multipleStylesContaining(
      nodeTextStyleWithFontSizeInPixels(99),
      circleStyleWithPaddingInPixels(99));

    expect(styles.getCirclePadding()).to.equal(99);

    styles.setCirclePadding(expectedPixels);

    expect(styles.getCirclePadding()).to.equal(expectedPixels);
  });

  it('should retrieve the line style when stroke and dasharray is defined', () => {
    const cssClass = "methodCall";
    const title = "method call";
    const expectedStroke = "#ff0000";
    const expectedStrokeDasharray = "2px";
    const styles = multipleStylesContaining(lineStyleWithStrokeAsHexAndStrokeDasharrayInPixels(cssClass, "rgb(255, 0, 0)", expectedStrokeDasharray));

    expect(styles.getLineStyle(cssClass, title)).to.deep.equal({
      title: title,
      styles: [
        {
          name: "stroke",
          value: expectedStroke
        },
        {
          name: "stroke-dasharray",
          value: expectedStrokeDasharray
        }
      ]
    })
  });

  it('should retrieve the line style when only stroke is defined', () => {
    const cssClass = "methodCall";
    const title = "method call";
    const expectedStroke = "#ff0000";
    const styles = multipleStylesContaining(lineStyleWithStrokeAsHexAndStrokeDasharrayInPixels(cssClass, "rgb(255, 0, 0)"));

    expect(styles.getLineStyle(cssClass, title)).to.deep.equal({
      title: title,
      styles: [
        {
          name: "stroke",
          value: expectedStroke
        },
        {
          name: "stroke-dasharray",
          value: undefined
        }
      ]
    })
  });

  it('should retrieve the line style when only stroke-dasharray is defined', () => {
    const cssClass = "methodCall";
    const title = "method call";
    const expectedStrokeDasharray = "2px";
    const styles = multipleStylesContaining(lineStyleWithStrokeAsHexAndStrokeDasharrayInPixels(cssClass, undefined, expectedStrokeDasharray));

    expect(styles.getLineStyle(cssClass, title)).to.deep.equal({
      title: title,
      styles: [
        {
          name: "stroke",
          value: "#000000"
        },
        {
          name: "stroke-dasharray",
          value: expectedStrokeDasharray
        }
      ]
    })
  });
});

describe('StyleStub', () => {
  it('should return style values just like the DOM API', () => {
    const styleStub = new StyleStub({
      'foo': 'bar',
      'foo-bar': 'baz'
    });

    expect(styleStub.getPropertyValue('foo-bar')).to.equal('baz');
  });

  it('should update style values just like the DOM API', () => {
    const styleStub = new StyleStub({
      'foo': 'bar',
      'foo-bar': 'baz'
    });

    styleStub.setProperty('foo-bar', 'changed');

    expect(styleStub.getPropertyValue('foo-bar')).to.equal('changed');
  });
});

function multipleStylesContaining() {
  let allCssRules = [toBeIgnored('one')];
  Array.from(arguments).forEach(r => allCssRules.push(r));
  allCssRules.push(toBeIgnored('two'));
  allCssRules = simulateNoRealArray(allCssRules);

  return visualizationStyles.from({
    cssRules: allCssRules
  });
}

// NOTE: The DOM object is no real Array either
function simulateNoRealArray(arr) {
  return new Set(arr);
}

function toBeIgnored(suffix) {
  return {
    selectorText: `.wrong${suffix}`,
    style: new StyleStub({
      "font-size": '8px'
    })
  };
}

function lineStyleWithStrokeAsHexAndStrokeDasharrayInPixels(cssClass, stroke, strokeDashArray) {
  return {
    selectorText: LINE_STYLE_SELECTOR + "." + cssClass,
    style: new StyleStub({
      "stroke": stroke,
      "stroke-dasharray": strokeDashArray
    })
  }
}

// FIXME: Suppose we allow custom CSS, how to handle non-pixel font-sizes??
function nodeTextStyleWithFontSizeInPixels(pixelSize) {
  return {
    selectorText: NODE_TEXT_STYLE_SELECTOR,
    style: new StyleStub({
      "font-style": 'bold',
      "font-size": `${pixelSize}px`
    })
  };
}

// FIXME: Suppose we allow custom CSS, how to handle non-pixel padding??
function circleStyleWithPaddingInPixels(pixelSize) {
  return {
    selectorText: CIRCLE_STYLE_SELECTOR,
    style: new StyleStub({
      "margin": '99em',
      "padding": `${pixelSize}px`
    })
  };
}

class StyleStub {
  constructor(stylesJson) {
    this._styles = stylesJson;
  }

  getPropertyValue(key) {
    return this._styles[key];
  }

  setProperty(key, value) {
    this._styles[key] = value;
  }
}