let cssAdapter = {
  getCssSheet: (document, sheetName) => {
    let styles = document.getElementById(sheetName);
    return styles.sheet || styles.stylesheet;
  },

  getRuleIndex: (sheet, selectorText) => {
    let classes = sheet.rule || sheet.cssRules;
    for (let i = 0; i < classes.length; i++) {
      if (classes[i].selectorText === selectorText) {
        return i;
      }
    }
    return -1;
  },

  getRuleByIndex: (sheet, index) => {
    let classes = sheet.rules || sheet.cssRules;
    let rule = classes[index];
    return rule.cssText ? rule.cssText : rule.style.cssText;
  },

  getRuleBySelectorText: (sheet, selectorText) => {
    let index = cssAdapter.getRuleIndex(sheet, selectorText);
    return cssAdapter.getRuleByIndex(sheet, index);
  },

  getStyleFromRule: (rule, style) => {
    let startOfStyle = rule.indexOf(style);
    let startOfValue = rule.indexOf(":", startOfStyle) + 1;
    let endOfValue = rule.indexOf(";", startOfStyle);
    return rule.slice(startOfValue, endOfValue);
  },

  getStyle: (sheet, selectorText, style) => {
    let rule = cssAdapter.getRuleBySelectorText(sheet, selectorText);
    return cssAdapter.getStyleFromRule(rule, style);
  },

  getStyleAsNumber: (sheet, selectorText, style) => {
    let styleValue = cssAdapter.getStyle(sheet, selectorText, style);
    return parseInt(styleValue, 10);
  },

  setStyle: (sheet, selectorText, style, value, unit) => {
    let i = cssAdapter.getRuleIndex(sheet, selectorText);
    let rule = cssAdapter.getRuleByIndex(sheet, i);
    let styleIndex = rule.indexOf(style);
    let partBeforeNewPart = rule.slice(0, styleIndex);
    let newPart = style + ": " + value + unit;
    let partAfterNewPart = rule.slice(rule.indexOf(";", styleIndex));
    let newRule = partBeforeNewPart + newPart + partAfterNewPart;
    sheet.deleteRule(i);
    sheet.insertRule(newRule, i);
  }
};

module.exports.cssAdapter = cssAdapter;