'use strict';

/**
 * Takes a predicate p (i.e. a function T -> boolean) as input. Returns an inverted predicate,
 * i.e. if for any predicate p and any input x p(x) == true, then not(p)(x) == false.
 */
module.exports.not = predicate => input => !predicate(input);

/**
 * Takes two predicates p1 and p2 (i.e. functions T -> boolean) as input. Returns a new predicate p,
 * where p(x) == true iff p1(x) == true AND p2(x) == true.
 */
module.exports.and = (...predicates) => input => predicates.every(p => p(input));

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