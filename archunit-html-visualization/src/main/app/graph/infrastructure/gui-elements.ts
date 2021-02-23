'use strict';

const d3 = require('d3');
const svg = require('./svg');
import {init as documentInit} from './document';
import {init as windowInit} from './window';
import {stylesFrom, VisualizationStyles} from '../visualization-styles';

const getEmbeddedVisualizationStyles = (): VisualizationStyles => stylesFrom(d3.select('#visualization-styles').property('sheet'));

export {getEmbeddedVisualizationStyles, svg, documentInit, windowInit};
