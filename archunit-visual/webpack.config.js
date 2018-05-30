const path = require('path');
//const HtmlWebpackInlineSourcePlugin = require('html-webpack-inline-source-plugin');
//const HtmlWebpackPlugin = require('html-webpack-plugin');

const buildPath = 'build/resources/main/com/tngtech/archunit/visual/report';

module.exports = {
  optimization: {
    minimize: false
  },
  entry: {
    'visualization-bundle': ['./src/main/app/report/visualization.js',
      './src/main/app/report/web-component-infrastructure.js'],
    'webcomponents-loader': './node_modules/@webcomponents/webcomponentsjs/webcomponents-loader.js'
  },
  output: {
    filename: '[name].js',
    path: path.resolve(__dirname, buildPath)
  }
  /*,
  plugins: [
    new HtmlWebpackPlugin({
      template: path.resolve(__dirname, buildPath + '/report.html'),
      filename: 'report.html',
      inlineSource: '.js$',
    }),
    new HtmlWebpackInlineSourcePlugin(),
  ]*/
};

//TODO: css-files werden irgendwie nicht geinlined
//TODO: nicht nur ins report.html sollen entsprechende Sachen geinlined werden, sondern auch in die anderen html-files,
// welche wiederum ins report.html geinlined werden sollen