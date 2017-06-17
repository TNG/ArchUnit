module.exports.workarounds = {
  getOwnerDocument: () => document.currentScript ?
      document.currentScript.ownerDocument :
      document._currentScript.ownerDocument
};

module.exports.WebComponentElement = class WebComponentElement extends HTMLElement {
  constructor(ownerDocument) {
    super();
    this._ownerDocument = ownerDocument;
  }

  connectedCallback() {
    this.attachShadow({mode: 'open'});
    let template = this._ownerDocument.querySelectorAll('.component-template');
    if (template.length !== 1) {
      throw new Error('The passed ownerDocument must specify exactly one element with class=\'component-template\'');
    }
    this.shadowRoot.appendChild(template[0].content.cloneNode(true));

    if (this.postConnected) {
      this.postConnected();
    }
  }

  getShadowRoot() {
    return this.shadowRoot;
  }
};