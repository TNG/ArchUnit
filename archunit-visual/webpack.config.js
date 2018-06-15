const path = require('path');
const srcPath = './src/main/app/report/';
const buildPath = './build/resources/main/com/tngtech/archunit/visual/report';

module.exports = env => {
  if (!env || !env.buildPath) {
    env = {buildPath};
  }
  return {
    optimization: {
      splitChunks: {
        cacheGroups: {
          d3: {
            test: /d3/,
            name: 'd3',
            chunks: 'all',
            minSize: 0,
            minChunks: 2
          },
          webComponentInfrastructure: {
            test: /web-component-infrastructure/,
            name: 'web-component-infrastructure',
            chunks: 'all',
            minSize: 0,
            minChunks: 2
          }
        }
      },
      minimize: false
    },
    entry: {
      'report-bundle': `${srcPath}report.js`,
      'webcomponents-loader': './node_modules/@webcomponents/webcomponentsjs/webcomponents-loader.js',
      'filter': `${srcPath}menu/filter.js`,
      'filter-bar': `${srcPath}menu/filter-bar`,
      'legend': `${srcPath}menu/legend.js`,
      'menu': `${srcPath}menu/menu.js`,
      'settings': `${srcPath}menu/settings.js`,
      'violation-menu': `${srcPath}violation-menu/violation-menu.js`
    },
    output: {
      filename: '[name].js',
      chunkFilename: '[name].js',
      path: path.resolve(__dirname, env.buildPath)
    },
    module: {
      rules: [
        {
          test: /.css$/,
          use: [
            {loader: 'style-loader', options: {attrs: {id: 'visualization-styles'}}},
            {loader: 'css-loader'}
          ],
          include: /visualizationstyles.css/
        },
        {
          test: /.css$/,
          use: [
            {loader: 'style-loader'},
            {loader: 'css-loader'}
          ],
          exclude: /visualizationstyles.css/
        }
      ]
    }
  }
};

//TODO: change watch-js package.json