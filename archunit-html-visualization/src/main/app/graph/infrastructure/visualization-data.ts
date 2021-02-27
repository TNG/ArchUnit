'use strict';

// eslint-disable-next-line @typescript-eslint/no-unused-vars
import {JsonGraph} from "../node/json-types";

function getJsonGraph(): JsonGraph {
  // eslint-disable-next-line @typescript-eslint/ban-ts-comment
  // @ts-ignore
  return JSON.parse(window.jsonGraph);
}

interface VisualizationData {
  getJsonGraph(): JsonGraph
}

const init = (): VisualizationData => {
  return {
    getJsonGraph(): JsonGraph {
      return getJsonGraph();
    }
  }
}

// module.exports = WindowProperties;
export {init, VisualizationData}
