'use strict';

const guiElementsMock = require('../testinfrastructure/gui-elements-mock');
const AppContext = require('../../../../main/app/graph/app-context');
const appContext = AppContext.newInstance({guiElements: guiElementsMock});
const Root = appContext.getRoot();

const createRootFromJson = (jsonRoot) => {
  const root = new Root(jsonRoot, null, () => Promise.resolve(), () => void 0, () => void 0);
  root.getLinks = () => [];
  //TODO: add other necessary functions
  return root;
};

const createRootWithLayoutFromJson = async (jsonRoot) => {
  const root = createRootFromJson(jsonRoot);
  root.relayoutCompletely();
  await root._updatePromise;
  return root;
};

module.exports = {
  createRootFromJson,
  createRootWithLayoutFromJson,
  appContext
};