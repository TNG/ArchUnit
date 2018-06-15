'use strict';

const parseEnv = () => {
  const env = {};
  const envMarker = '--env.';
  const separator = '=';
  process.argv.slice(2).forEach(val => {
    if (val.startsWith(envMarker)) {
      const sepPos = val.indexOf(separator);
      const optionName = val.substring(envMarker.length, sepPos);
      const optionValue = val.substring(sepPos + 1);
      env[optionName] = optionValue;
    }
  });
  return env;
};

let config = require('./htmlinliner.config');
if (typeof config === 'function') {
  config = config(parseEnv());
}

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
    tagString: match[0],
    referredFile: findFile(filePath, getAttributeValue(match[0], attr))
  }));
};

//TODO: check configs!!
const HtmlFile = class {
  constructor(fileName, config, defaultConfig) {
    this.fileName = fileName;
    this._createOutputFileName(config, defaultConfig);
    this.writeOutput = config.writeOutput || defaultConfig.writeOutput;
    this._content = fs.readFileSync(this.fileName);
    this._styleSheets = readTagsFromHtml(this._content, createRegexForStyleSheet(), fileMarkerForLink, config.stylesheetPath || defaultConfig.stylesheetPath);
    this._scripts = readTagsFromHtml(this._content, createRegexForScript(), fileMarkerForScript, config.scriptPath || defaultConfig.scriptPath);
    this._htmlPages = readTagsFromHtml(this._content, createRegexForWebcomponent(), fileMarkerForLink, config.htmlPath || defaultConfig.htmlPath);
  }

  _createOutputFileName(config, defaultConfig) {
    const start = this.fileName.lastIndexOf('/') + 1;
    const path = config.outputPath || defaultConfig.outputPath;
    const simpleFileName = this.fileName.substring(start);
    this.outputFileName = [path, simpleFileName].filter(e => e).join('/');
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

const replaceTagInFile = (htmlFile, tag, referredFileContent, open, close) => {
  const tagIndex = htmlFile._content.indexOf(tag.tagString);
  htmlFile._content = htmlFile._content.slice(0, tagIndex)
    + open
    + referredFileContent
    + close
    + htmlFile._content.slice(tagIndex + tag.tagString.length);
};

const replaceCSSOrJsTagInFile = (htmlFile, tag, open, close) => {
  const referredFileContent = fs.readFileSync(tag.referredFile);
  replaceTagInFile(htmlFile, tag, referredFileContent, open, close);
};

const replaceHtmlTagInFile = (htmlFile, tag, fileMap) => {
  let referredFileContent;
  if (fileMap.has(tag.referredFile)) {
    referredFileContent = fileMap.get(tag.referredFile)._content;
  }
  else {
    let referredFileContent = fs.readFileSync(tag.referredFile);
  }
  replaceTagInFile(htmlFile, tag, referredFileContent, '', '');
};

/**
 * TODO: optimize: files with writeOutput=false that are not referenced by another file should be ignored completely
 */
const inlineToHtml = () => {
  const sortedFiles = sortFilesTopologically(parseAllFiles());
  const fileMap = new Map(sortedFiles.map(file => [file.fileName, file]));
  sortedFiles.forEach(file => {
    file._styleSheets.forEach(styleSheetTag => replaceCSSOrJsTagInFile(file, styleSheetTag, '<style>', '</style>'));
    file._scripts.forEach(scriptTag => replaceCSSOrJsTagInFile(file, scriptTag, '<script>', '</script>'));
    file._htmlPages.forEach(htmlTag => replaceHtmlTagInFile(file, htmlTag, fileMap));
  });

  sortedFiles.filter(file => file.writeOutput).forEach(file => {
    fs.writeFile(file.outputFileName, file._content, err => {
      if (err) {
        throw err;
      }
      console.log(`Writing ${file.outputFileName}`);
    });
  })
};

inlineToHtml();