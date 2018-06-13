'use strict';

import resources from './resources';
import appContext from './app-context';
import createGraph from './graph';

export default (svgElement, foldAllNodes) => createGraph(appContext.newInstance(), resources, svgElement, foldAllNodes);