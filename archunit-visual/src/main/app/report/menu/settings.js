'use strict';

import {defineCustomElement} from '../web-component-infrastructure';

(function () {
  let circleFontSizeInput;
  let circlePaddingInput;
  let onChanged;

  defineCustomElement('visualization-settings', class extends HTMLElement {
    postConnected() {
      circleFontSizeInput = this.shadowRoot.querySelector('#circleFontSize');
      circlePaddingInput = this.shadowRoot.querySelector('#circlePadding');

      const onOneValueChanged = () => onChanged(circleFontSizeInput.value, circlePaddingInput.value);
      circleFontSizeInput.oninput = onOneValueChanged;
      circlePaddingInput.oninput = onOneValueChanged;
    }

    initialize(settingsInit) {
      circleFontSizeInput.value = settingsInit.initialCircleFontSize;
      circlePaddingInput.value = settingsInit.initialCirclePadding;
    }

    onSettingsChanged(newOnChanged) {
      onChanged = newOnChanged;
    }
  });
})();