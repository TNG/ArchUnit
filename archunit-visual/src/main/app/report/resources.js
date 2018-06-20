'use strict';

const getResources = () => ({
  root: JSON.parse(window.jsonRoot),
  violations: JSON.parse(window.jsonViolations)
});

export default {getResources};