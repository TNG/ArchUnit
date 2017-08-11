'use strict';

const nodeKinds = require('./node-kinds.json');

const isLeaf = node => node._filteredChildren.length === 0;

const escapeRegExp = str => {
  return str.replace(/[-[\]/{}()+?.\\^$|]/g, '\\$&');
};

const stringContains = substring => {
  return string => {
    const withoutLeadingWhitespace = substring.replace(/^\s+/, '');
    const escaped = escapeRegExp(withoutLeadingWhitespace);
    const pattern = escaped.replace(/ +$/, '$').replace(/\*/g, '.*');
    return new RegExp(pattern).test(string);
  }
};

/**
 * Checks, if a String contains a certain substring. The '*' represents arbitrary many characters.
 * E.g.
 * - stringContains('foo')('foobar') => true
 * - stringContains('foo')('goobar') => false
 * - stringContains('f*ar')('foobar') => true
 * If the subString ends with a whitespace, it only matches Strings ending in the subString (minus the whitespace)
 * E.g.
 * - stringContains('foo ')('foobar') => false
 * - stringContains('bar ')('foobar') => true
 * Left whitespace is ignored.
 *
 * @param substring The string the text must contain.
 */
module.exports.stringContains = stringContains;

const nameContainsFilter = (filterString, exclude) => {
  const stringContainsSubstring = stringContains(filterString);

  const filter = node => {
    if (node.getType() === nodeKinds.package) {
      return node._filteredChildren.reduce((acc, c) => acc || filter(c), false);
    }
    else {
      let res = stringContainsSubstring(node.getFullName());
      res = exclude ? !res : res;
      return res || (!isLeaf(node) && node._filteredChildren.reduce((acc, c) => acc || filter(c), false));
    }
  };
  return filter;
};

module.exports.nameContainsFilter = nameContainsFilter;