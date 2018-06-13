'use strict';

import {defineCustomElement} from '../web-component-infrastructure';

(function () {
  let filterStringInput;
  let excludeInput;
  let onChanged;

  defineCustomElement('visualization-filter-bar', class extends HTMLElement {
    postConnected() {
      filterStringInput = this.shadowRoot.querySelector('#filter-string');
      excludeInput = this.shadowRoot.querySelector('#exclude');

      const onValueChanged = (() => {
        let timer = 0;
        return () => {
          clearTimeout(timer);
          timer = setTimeout(() => onChanged(filterStringInput.value, excludeInput.checked), 500);
        }
      })();
      filterStringInput.oninput = onValueChanged;
      excludeInput.onclick = onValueChanged;
    }

    onChanged(newOnChanged) {
      onChanged = newOnChanged;
    }
  });
}());