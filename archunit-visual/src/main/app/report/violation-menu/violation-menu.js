'use strict';

import {defineCustomElement} from '../web-component-infrastructure';
import * as d3 from 'd3';

(function () {
  defineCustomElement('violation-menu', class extends HTMLElement {
    postConnected() {
      const showBar = d3.select(this.shadowRoot.querySelector('#showbar'));
      const verticalDivider = d3.select(this.shadowRoot.querySelector('#verticalDivider'));
      let violationMenuIsVisible = verticalDivider.style('display') === 'flex';
      showBar.on('click', () => {
        violationMenuIsVisible = !violationMenuIsVisible;
        verticalDivider.style('display', violationMenuIsVisible ? 'flex' : 'none');
        showBar.text(violationMenuIsVisible ? '>' : '<');
      });
    }

    initialize(violations, showViolationsOfRule, hideViolationsOfRule, onHideAllDependenciesChanged) {
      const getViolationGroupOfRule = rule => violations.filter(violationGroup => violationGroup.rule === rule)[0];

      const violationRuleList = d3.select(this.shadowRoot.querySelector('.violation-rule-list'));
      const rules = violations.map(violationGroup => violationGroup.rule);
      const violationRules = violationRuleList.selectAll('span').data(rules).enter().append('span').text(rule => rule);

      const markedRules = new Set();
      violationRules.on('click', function (rule) {
        if (markedRules.has(rule)) {
          markedRules.delete(rule);
          d3.select(this).attr('class', '');
          hideViolationsOfRule(getViolationGroupOfRule(rule));
        }
        else {
          markedRules.add(rule);
          d3.select(this).attr('class', 'marked');
          showViolationsOfRule(getViolationGroupOfRule(rule));
        }
      });

      this._onHideAllDependenciesChanged(onHideAllDependenciesChanged);
    }

    _onHideAllDependenciesChanged(callback) {
      const checkbox = this.shadowRoot.querySelector('#hideAllDepsWhenRuleSelected');
      checkbox.onclick = () => callback(checkbox.checked);
      callback(checkbox.checked); //sync initial state
    }
  });
}());