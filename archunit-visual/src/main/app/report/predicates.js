'use strict';

/**
 * Takes a predicate p (i.e. a function T -> boolean) as input. Returns an inverted predicate,
 * i.e. if for any predicate p and any input x p(x) == true, then not(p)(x) == false.
 */
const not = predicate => input => !predicate(input);

/**
 * Takes two predicates p1 and p2 (i.e. functions T -> boolean) as input. Returns a new predicate p,
 * where p(x) == true iff p1(x) == true AND p2(x) == true.
 */
const and = (...predicates) => input => predicates.every(p => p(input));

const escapeRegExp = str => {
  //FIXME: is this correct??
  //* and | are not escaped, as they have special meaning for the filter
  return str.replace(/[-[\]/{}()+?.\\^$]/g, '\\$&');
};

/**
 * Checks, if a String equals a certain string of some optional strings, which are separated by '|'.
 * The '*' represents arbitrary many characters.
 * E.g.
 * - stringEquals('foobar')('foobar') => true
 * - stringEquals('foo')('foobar') => false
 * - stringEquals('f*ar')('foobar') => true
 * Left and right whitespaces are ignored (for each of the optional strings).
 *
 * @param substring The string the text must contain.
 */
const stringEquals = substring => {
  const withoutLeadingOrClosingWhitespace = substring
  //remove leading whitespaces
    .replace(/^\s+/, '')
    //remove leading whitespaces before the "options" separated by |
    .replace(/\|\s+/g, '|')
    //remove closing whitespaces
    .replace(/\s+$/, '')
    //remove closing whitespaces before '|'
    .replace(/\s+\|/g, '|');
  const escaped = escapeRegExp(withoutLeadingOrClosingWhitespace);
  const pattern = escaped
  //match only the names that are exactly the same as one of the "options"
    .replace(/\|/g, '$|^')
    //a star in the substring stands for any characters
    .replace(/\*/g, '.*');
  const patternWithLeadingAndEndingMarker = `^${pattern}$`;
  const regex = new RegExp(patternWithLeadingAndEndingMarker);
  return string => {
    return regex.test(string);
  }
};

export default {not, and, stringEquals};