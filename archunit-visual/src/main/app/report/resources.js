'use strict';

const jsonRoot = 'injectJsonClassesToVisualizeHere';
const jsonViolations = 'injectJsonViolationsToVisualizeHere';

const getResources = () => ({
  root: JSON.parse(jsonRoot),
  violations: JSON.parse(jsonViolations)
});

export default {getResources};