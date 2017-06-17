module.exports.defineCustomElement = function (tagName, elementClass) {
  const templateId = `#${tagName}-template`;
  let template = document.currentScript.ownerDocument.querySelector(templateId);
  if (!template) {
    throw new Error(`Class for tag '${tagName}' must be defined together with a template with id ${templateId}`);
  }
  Object.assign(elementClass.prototype, {
    connectedCallback: function () {
      this.attachShadow({mode: 'open'});
      this.shadowRoot.appendChild(template.content.cloneNode(true));

      if (this.postConnected) {
        this.postConnected();
      }
    }
  });
  window.customElements.define(tagName, elementClass);
};