'use strict';

const createJsonFromClassNames = require('../testinfrastructure/class-names-to-json-transformer').createJsonFromClassNames;

const guiElementsMock = require('../testinfrastructure/gui-elements-mock');
const AppContext = require('../../../../main/app/graph/app-context');
const appContext = AppContext.newInstance({guiElements: guiElementsMock});
const Root = appContext.getRoot();

const createRootFromClassNames = (...classNames) => {
  const jsonRoot = createJsonFromClassNames(...classNames);
  const root = new Root(jsonRoot, null, () => Promise.resolve(), () => void 0, () => void 0);
  root.getLinks = () => [];
  //TODO: add other necessary functions
  return root;
};

const createRootFromClassNamesAndLayout = async (...classNames) => {
  const root = createRootFromClassNames(...classNames);
  root.relayoutCompletely();
  await root._updatePromise;
  return root;
};

module.exports = {
  createRootFromClassNames,
  createRootFromClassNamesAndLayout,
  appContext
};