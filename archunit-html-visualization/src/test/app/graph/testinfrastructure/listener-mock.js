'use strict';

const expect = require('chai').expect;

const createListenerMock = (...listenerFunctionNames) => {
  const listener = {};
  let listenerFunctionCalls;
  const reset = () => {
    listenerFunctionCalls = new Map();
    listenerFunctionNames.forEach(listenerFunctionName => listenerFunctionCalls.set(listenerFunctionName, []));
  };

  reset();

  listenerFunctionNames.forEach(listenerFunctionName => listener[listenerFunctionName] = (...args) => {
    listenerFunctionCalls.get(listenerFunctionName).push({args: args});
  });

  const test = {
    that: {
      listenerFunction: (listenerFunctionName) => ({
        was: {
          called: {
            once: () => {
              expect(listenerFunctionCalls.get(listenerFunctionName).length).to.equal(1);
            },
            times: (numberOfCalls) => {
              expect(listenerFunctionCalls.get(listenerFunctionName).length).to.equal(numberOfCalls);
            },
            with: {
              nodes: (...nodeFullNames) => {
                const fistArgumentOfEveryCall = listenerFunctionCalls.get(listenerFunctionName).map(call => call.args[0].getFullName());
                expect(fistArgumentOfEveryCall).to.have.members(nodeFullNames);
              }
            }
          },
          not: {
            called: () => {
              expect(listenerFunctionCalls.get(listenerFunctionName)).to.be.empty;
            }
          }
        }
      })
    }
  };

  return {listener, test, reset};
};

module.exports.createListenerMock = createListenerMock;