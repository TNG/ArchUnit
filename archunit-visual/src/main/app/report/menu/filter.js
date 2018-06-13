'use strict';

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
import {defineCustomElement} from '../web-component-infrastructure';

(function () {
  let nodeCheckBoxes;
  let dependencyCheckBoxes;

  //webComponents.
  defineCustomElement('visualization-filter', class extends HTMLElement {
    postConnected() {
      nodeCheckBoxes = checkBoxesFrom(this.shadowRoot.querySelectorAll('.nodeCheckBox'));
      dependencyCheckBoxes = checkBoxesFrom(this.shadowRoot.querySelectorAll('.dependencyCheckBox'));
    }

    onNodeTypeFilterChanged(onChanged) {
      nodeCheckBoxes.onCheckboxClicked(onChanged);
    }

    onDependencyFilterChanged(onDependencyFilterChanged) {
      dependencyCheckBoxes.onCheckboxClicked(onDependencyFilterChanged);
    }
  });
}());