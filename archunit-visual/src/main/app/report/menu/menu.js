'use strict';

import {defineCustomElement} from '../web-component-infrastructure';

(function () {
  defineCustomElement('visualization-menu', class extends HTMLElement {
    initializeSettings(settingsInit) {
      this.shadowRoot.querySelector('#settings-menu').initialize(settingsInit);
      return this;
    }

    onSettingsChanged(onChanged) {
      this.shadowRoot.querySelector('#settings-menu').onSettingsChanged(onChanged);
      return this;
    }

    onNodeTypeFilterChanged(onChanged) {
      this.shadowRoot.querySelector('#filter-menu').onNodeTypeFilterChanged(onChanged);
      return this;
    }

    onDependencyFilterChanged(onChanged) {
      this.shadowRoot.querySelector('#filter-menu').onDependencyFilterChanged(onChanged);
      return this;
    }

    onNodeNameFilterChanged(onChanged) {
      this.shadowRoot.querySelector('#filter-bar').onChanged(onChanged);
      return this;
    }

    changeNodeNameFilter(filterString) {
      this.shadowRoot.querySelector('#filter-bar').changeNodeNameFilter(filterString);
    }

    initializeLegend(legendStyles) {
      this.shadowRoot.querySelector('#legend-menu').initialize(legendStyles);
      return this;
    }
  });
}());