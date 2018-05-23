const path = require('path');
const HtmlWebpackInlineSourcePlugin = require('html-webpack-inline-source-plugin');
const HtmlWebpackPlugin = require('html-webpack-plugin');

module.exports = {
  entry: './src/main/app/report/visualization.js',
  output: {
    filename: 'visualization-bundle.js',
    path: path.resolve(__dirname, 'build/resources/main/com/tngtech/archunit/visual/report') //FIXME
  },
  plugins: [
    new HtmlWebpackPlugin({
      template: path.resolve(__dirname, 'src/main/app/report/report.html'),
      filename: 'report.html',
      inlineSource: '.(js|css)$',
    }),
    new HtmlWebpackInlineSourcePlugin(),
  ]
};

//TODO: css-files werden irgendwie nicht geinlined
//TODO: nicht nur ins report.html sollen entsprechende Sachen geinlined werden, sondern auch in die anderen html-files,
// welche wiederum ins report.html geinlined werden sollen