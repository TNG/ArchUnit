'use strict';

import * as d3 from 'd3';
import {defineCustomElement} from '../web-component-infrastructure';

const checkBoxesFrom = function (nodes) {
  const checkBoxes = Array.from(nodes);

  const checkboxIdToCheckedBoolean = () => checkBoxes.reduce((result, box) => {
    result[box.id] = box.checked;
    return result;
  }, {});

  return {
    onCheckboxClicked: onClicked => {
      checkBoxes.forEach(box => {
        box.onclick = () => onClicked(checkboxIdToCheckedBoolean())
      });

      onClicked(checkboxIdToCheckedBoolean()); // Sync the initial state
    }
  };
};

(function () {
  let nodeCheckBoxes;
  let dependencyCheckBoxes;

  //webComponents.
  defineCustomElement('visualization-filter', class extends HTMLElement {
    postConnected() {
      nodeCheckBoxes = checkBoxesFrom(this.shadowRoot.querySelectorAll('.nodeCheckBox'));
    }

    onNodeTypeFilterChanged(onChanged) {
      nodeCheckBoxes.onCheckboxClicked(onChanged);
    }

    initializeDependencyFilter(dependencyTypes) {
      const filterDivs = d3.select(this.shadowRoot.querySelector('#dependency-types'))
        .selectAll('div')
        .data(dependencyTypes)
        .enter()
        .append('div').attr('class', 'filter');

      filterDivs.append('input')
        .attr('type', 'checkbox')
        .attr('class', 'dependencyCheckBox')
        .attr('id', d => d)
        .attr('checked', 'true');
      filterDivs.append('label')
        .attr('for', d => d)
        .text(d => d);

      dependencyCheckBoxes = checkBoxesFrom(this.shadowRoot.querySelectorAll('.dependencyCheckBox'));
    }

    onDependencyFilterChanged(onDependencyFilterChanged) {
      dependencyCheckBoxes.onCheckboxClicked(onDependencyFilterChanged);
    }
  });
}());