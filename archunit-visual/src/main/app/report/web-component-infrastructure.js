'use strict';

let shadyCssStyler = {
  prepareTemplate: (template, name) => ShadyCSS.prepareTemplate(template, name),
  styleElement: component => ShadyCSS.styleElement(component)
};

let noOpStyler = {
  prepareTemplate: () => null,
  styleElement: () => null
};

// The ShadyCSS Polyfill doesn't work out of the box, we have to activate it, if the Polyfill is present
let styler = window.ShadyCSS ? shadyCssStyler : noOpStyler;

module.exports.defineCustomElement = function (tagName, elementClass) {
  const templateId = `#${tagName}-template`;
  let template = document.currentScript.ownerDocument.querySelector(templateId);
  if (!template) {
    throw new Error(`Class for tag '${tagName}' must be defined together with a template with id ${templateId}`);
  }

  styler.prepareTemplate(template, tagName);

  Object.assign(elementClass.prototype, {
    connectedCallback: function () {
      styler.styleElement(this);

      this.attachShadow({mode: 'open'});
      this.shadowRoot.appendChild(template.content.cloneNode(true));

      if (this.postConnected) {
        this.postConnected();
      }
    }
  });
  window.customElements.define(tagName, elementClass);
};