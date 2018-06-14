'use strict';

const config = require('./htmlinliner.config');
const fs = require('fs');

const noClosingTag = "[^>]*?";

const createRegexForScript = () => {
  const scriptStart = `<script${noClosingTag}src${noClosingTag}`;
  const scriptWithoutClosing = `${scriptStart}/>`;
  const scriptWithClosing = `${scriptStart}>\s*</script>`;
  const script = `(${scriptWithoutClosing}|${scriptWithClosing})`;
  return new RegExp(script, 'g');
};

const createRegexForStyleSheet = () => {
  const styleSheetIndicator = `(rel='stylesheet'|rel="stylesheet")`;
  const styleSheet = `<link${noClosingTag}${styleSheetIndicator}${noClosingTag}>`;
  return new RegExp(styleSheet, 'g');
};

const createRegexForWebcomponent = () => {
  const webcomponentIndicator = `(rel='import'|rel="import")`;
  const webcomponent = `<link${noClosingTag}${webcomponentIndicator}${noClosingTag}>`;
  return new RegExp(webcomponent, 'g');
};

const fileMarkerForLink = 'href';
const fileMarkerForScript = 'src';

/**
 *
 * @param str string to search in
 * @param regex has to have flag g
 */
const getAllMatches = (str, regex) => {
  const results = [];
  let match;
  while ((match = regex.exec(str)) !== null) {
    results.push(match);
  }
  return results;
};

const getAttributeValue = (str, attr) => {
  attr += '=';
  const indexOfAttr = str.indexOf(attr);
  const quote = str.charAt(indexOfAttr + attr.length);
  const start = indexOfAttr + attr.length + 1;
  const end = str.indexOf(quote, start);
  return str.substring(start, end);
};

const findFile = (path, fileToFind) => {
  let files = [{dir: '', file: path}];
  const innerFindFile = function self() {
    if (files.length === 0) {
      throw `${fileToFind} was not found in ${path}`;
    }
    const {dir, file} = files.shift();
    const fullFilePath = [dir, file].filter(el => el).join('/');
    let stat;
    try {
      stat = fs.statSync(fullFilePath);
    }
    catch (e) {
      if (e.code === 'ENOENT' && fullFilePath === path) {
        throw `${path} does not exist`;
      }
      else {
        return self();
      }
    }
    if (stat.isDirectory()) {
      files = files.concat(fs.readdirSync(fullFilePath).filter(f => f).map(f => ({dir: fullFilePath, file: f})));
    }
    else {
      if (file === fileToFind) {
        return fullFilePath;
      }
    }
    return self();
  };
  return innerFindFile();
};

const readTagsFromHtml = (htmlContent, regex, attr, filePath) => {
  return getAllMatches(htmlContent, regex).map(match => ({
    startIndex: match.index,
    tagString: match[0],
    referredFile: findFile(filePath, getAttributeValue(match[0], attr))
  }));
};


const HtmlFile = class {
  constructor(fileName, config, defaultConfig) {
    this.fileName = fileName;
    this._content = fs.readFileSync(this.fileName);
    this._styleSheets = readTagsFromHtml(this._content, createRegexForStyleSheet(), fileMarkerForLink, config.stylesheetPath || defaultConfig.stylesheetPath);
    this._scripts = readTagsFromHtml(this._content, createRegexForScript(), fileMarkerForScript, config.scriptPath || defaultConfig.scriptPath);
    this._htmlPages = readTagsFromHtml(this._content, createRegexForWebcomponent(), fileMarkerForLink, config.htmlPath || defaultConfig.htmlPath);
  }
};

const parseAllFiles = () => config.files.map(file => new HtmlFile(file.file, config.configs[file.config], config.configs.default));

const sortFilesTopologically = files => {
  const filesSet = new Set(files.map(file => file.fileName));
  const filesWithDependencies = files.map(file => {
    const dependencies = new Set(file._htmlPages.map(htmlPage => htmlPage.referredFile)
      .filter(htmlFile => filesSet.has(htmlFile)));
    return {file, dependencies};
  });

  const res = [];
  while (filesWithDependencies.length > 0) {
    const nextFile = filesWithDependencies.find(fileWithDeps => fileWithDeps.dependencies.size === 0);
    if (!nextFile) {
      throw 'the files contain circular dependencies';
    }
    else {
      res.push(nextFile.file);
      filesWithDependencies.splice(filesWithDependencies.indexOf(nextFile), 1);
      filesWithDependencies.forEach(fileWithDeps => fileWithDeps.dependencies.delete(nextFile.file.fileName));
    }
  }
  return res;
};

const inlineToHtml = () => {
  const sortedFiles = sortFilesTopologically(parseAllFiles());

};

inlineToHtml();