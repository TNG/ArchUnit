'use strict';

const not = predicate => input => !predicate(input);

/**
 * Takes a predicate p (i.e. a function T -> boolean) as input. Returns an inverted predicate,
 * i.e. if for any predicate p and any input x p(x) == true, then not(p)(x) == false.
 */
module.exports.not = not;

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

const nodeFilter = nodePredicate => {
  const filter = node => {
    if (node.isPackage()) {
      return node._filteredChildren.reduce((acc, c) => acc || filter(c), false);
    }
    else {
      const res = nodePredicate(node);
      return res || (!node.isLeaf() && node._filteredChildren.reduce((acc, c) => acc || filter(c), false));
    }
  };
  return filter;
};

const nodeNameSatisfies = stringPredicate => node => stringPredicate(node.getFullName());

const nameContainsFilter = (filterString, exclude) => {
  const stringContainsSubstring = stringContains(filterString);
  const stringPredicate = exclude ? not(stringContainsSubstring) : stringContainsSubstring;
  return nodeFilter(nodeNameSatisfies(stringPredicate));
};

module.exports.nameContainsFilter = nameContainsFilter;