'use strict';

import {defineCustomElement} from '../web-component-infrastructure';

(function () {
  let filterStringInput;
  let onChanged;

  defineCustomElement('visualization-filter-bar', class extends HTMLElement {
    postConnected() {
      filterStringInput = this.shadowRoot.querySelector('#filter-string');

      const onValueChanged = (() => {
        let timer = 0;
        return () => {
          clearTimeout(timer);
          timer = setTimeout(() => onChanged(filterStringInput.value), 500);
        }
      })();
      filterStringInput.oninput = onValueChanged;
    }

    onChanged(newOnChanged) {
      onChanged = newOnChanged;
    }

    changeNodeNameFilter(filterString) {
      this.shadowRoot.querySelector('#filter-string').value = filterString;
    }
  });
}());