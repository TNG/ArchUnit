const path = require('path');
const buildPath = './build/resources/main/com/tngtech/archunit/visual/report';
const srcPath = './src/main/app/report';

module.exports = {
  configs: {
    default: {
      scriptPath: buildPath,
      stylesheetPath: undefined,
      htmlPath: undefined,
      outputPath: buildPath
    },
    reportConfig: {
      stylesheetPath: srcPath,
      htmlPath: srcPath
    },
    menuConfig: {
      stylesheetPath: `${srcPath}/menu`,
      htmlPath: `${srcPath}/menu`
    },
    violationMenuConfig: {
      stylesheetPath: `${srcPath}/violation-menu`,
      htmlPath: `${srcPath}/violation-menu`
    }
  },
  files: [
    {
      file: `${srcPath}/report.html`,
      config: 'reportConfig'

    },
    {
      file: `${srcPath}/menu/filter.html`,
      config: 'menuConfig'

    },
    {
      file: `${srcPath}/menu/filter-bar.html`,
      config: 'menuConfig'

    },
    {
      file: `${srcPath}/menu/legend.html`,
      config: 'menuConfig'

    },
    {
      file: `${srcPath}/menu/menu.html`,
      config: 'menuConfig'

    },
    {
      file: `${srcPath}/menu/settings.html`,
      config: 'menuConfig'

    },
    {
      file: `${srcPath}/violation-menu/violation-menu.html`,
      config: 'violationMenuConfig'
    }
  ]
};