'use strict';

const createJsonFromClassNames = require('../testinfrastructure/class-names-to-json-transformer').createJsonFromClassNames;

const guiElementsMock = require('../testinfrastructure/gui-elements-mock');
const AppContext = require('../../../../main/app/graph/app-context');
const appContext = AppContext.newInstance({guiElements: guiElementsMock});
const Root = appContext.getRoot();

const replaceFunctionsOn = (objectToReplaceOn) => ({
  byFunctionsOf: objectWithReplacingFunctions => {
    Object.keys(objectToReplaceOn).forEach(key => {
      if (typeof objectToReplaceOn[key] === 'function') {
        if (typeof objectWithReplacingFunctions[key] === 'function') {
          objectToReplaceOn[key] = objectWithReplacingFunctions[key];
        }
      }
    });
  }
});

const getClassNamesAndRootListenersFromArgs = args => {
  const listeners = {
    onSizeChanged: () => Promise.resolve(),
    onSizeExpanded: () => void 0,
    onNodeFilterStringChanged: () => void 0
  };
  let classNames = args;
  if (args.length > 0) {
    const potentialListenerFunctions = args[args.length - 1];
    if (typeof potentialListenerFunctions === 'object') {
      replaceFunctionsOn(listeners).byFunctionsOf(potentialListenerFunctions);
      classNames = args.slice(0, args.length - 1);
    }
  }
  return {classNames, listeners};
};

/**
 * @param args the class names of all leaf nodes; the last argument may be an object with the listener functions for the root, i.e.
 * {onSizeChanged, onSizeExpanded, onNodeFilterStringChanged}
 */
const createRootFromClassNames = (...args) => {
  const {listeners, classNames} = getClassNamesAndRootListenersFromArgs(args);
  const jsonRoot = createJsonFromClassNames(...classNames);
  const root = new Root(jsonRoot, null, listeners.onSizeChanged, listeners.onSizeExpanded, listeners.onNodeFilterStringChanged);
  root.getLinks = () => [];
  //TODO: add other necessary functions
  return root;
};

/**
 * @param args the class names of all leaf nodes; the last argument may be an object with the listener functions for the root, i.e.
 * {onSizeChanged, onSizeExpanded, onNodeFilterStringChanged}
 */
const createRootFromClassNamesAndLayout = async (...args) => {
  const root = createRootFromClassNames(...args);
  root.relayoutCompletely();
  await root._updatePromise;
  return root;
};

module.exports = {
  createRootFromClassNames,
  createRootFromClassNamesAndLayout,
  getVisualizationStyles: () => appContext.getVisualizationStyles()
};