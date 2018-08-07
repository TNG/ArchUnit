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

const formatStarAsWildcard = str => str.replace(/\*/g, '.*');

const createRegexStringOfOptionsStrings = optionStrings =>
  //a matching string must either equal an option-string or be a prefix of an option-string, so that the following symbol is . or $
  optionStrings.map(s => `^${s}(?=\\.|\\$|$)`);

const joinToRegexOptions = optionsStrings => optionsStrings.join('|');

/**
 * creates a function, that checks, if a string matches the given filterExpression.
 *
 * A string matches the filterExpression, if it equals any of the optional strings
 * AND does not equal any of the optional strings starting with '~'. That means that the
 * set of the united "normal" optional strings (without '~') intersected with the set
 * of the united optional string starting with '~' is the result set of matching strings.
 * The '*' represents arbitrary many characters.
 * E.g.
 * - stringEquals('')('anystring') => true
 * - stringEquals('foobar')('foobar') => true
 * - stringEquals('foo|bar')('foo') => true
 * - stringEquals('foo|bar')('bar') => true
 * - stringEquals('foo')('foobar') => false
 * - stringEquals('f*ar')('foobar') => true
 * - stringEquals('f*ar|~foobar')('foobar') => false
 * Left and right whitespaces are ignored (for each of the optional strings).
 *
 * @param filterExpression string out of any number of optional strings, which must be separated by '|'
 * and can start with '~' to signal that this string should be excluded.
 */
const stringEquals = filterExpression => {
  const withoutLeadingOrClosingWhitespace = filterExpression
  //remove leading whitespaces
    .replace(/^\s+/, '')
    //remove leading whitespaces before the "options" separated by |
    .replace(/\|\s+/g, '|')
    //remove closing whitespaces
    .replace(/\s+$/, '')
    //remove closing whitespaces before '|'
    .replace(/\s+\|/g, '|')
    //remove whitespaces after '~'
    .replace(/~\s+/g, '~');

  const split = withoutLeadingOrClosingWhitespace.split('|');
  const partitionedAndEscaped = {include: [], exclude: []};
  split.forEach(s => {
    const exclude = s.startsWith('~');
    const rawString = exclude ? s.substring(1) : s;
    const escaped = escapeRegExp(rawString);
    const escapedWithStarAsWildcard = formatStarAsWildcard(escaped);
    (exclude ? partitionedAndEscaped.exclude : partitionedAndEscaped.include).push(escapedWithStarAsWildcard);
  });

  const includeRegexString = joinToRegexOptions(createRegexStringOfOptionsStrings(partitionedAndEscaped.include));
  const includeRegex = new RegExp(includeRegexString);

  //the exclude-array needs an additional empty string: if there are no strings excluded,
  //the resulting empty exclude-regex would match every string, but actually it should match no string
  const excludeRegexString = joinToRegexOptions([...createRegexStringOfOptionsStrings(partitionedAndEscaped.exclude), '^$']);
  const excludeRegex = new RegExp(excludeRegexString);
  return string => {
    return includeRegex.test(string) && !excludeRegex.test(string);
  }
};

export default {not, and, stringEquals};