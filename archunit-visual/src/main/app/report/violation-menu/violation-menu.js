'use strict';

import {defineCustomElement} from '../web-component-infrastructure';
import * as d3 from 'd3';

(function () {
  defineCustomElement('violation-menu', class extends HTMLElement {
    postConnected() {
      const showBar = d3.select(this.shadowRoot.querySelector('#showbar'));
      const menuContainer = d3.select(this.shadowRoot.querySelector('#menuContainer'));
      let violationMenuIsVisible = menuContainer.style('display') === 'flex';
      showBar.on('click', () => {
        violationMenuIsVisible = !violationMenuIsVisible;
        menuContainer.style('display', violationMenuIsVisible ? 'flex' : 'none');
        showBar.text(violationMenuIsVisible ? '>' : '<');
      });
    }

    initialize(violations, showViolationsOfRule, hideViolationsOfRule) {
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
    }

    onHideAllDependenciesChanged(callback) {
      const checkbox = this.shadowRoot.querySelector('#hideAllDepsWhenRuleSelected');
      checkbox.onclick = () => callback(checkbox.checked);
      callback(checkbox.checked); //sync initial state
    }

    onHideNodesWithoutViolationsChanged(callback) {
      const checkbox = this.shadowRoot.querySelector('#hideNodesWithoutViolationsWhenRuleSelected');
      checkbox.onclick = () => callback(checkbox.checked);
      callback(checkbox.checked); //sync initial state
    }

    onClickUnfoldNodesToShowAllViolations(callback) {
      const button = this.shadowRoot.querySelector('#unfoldNodesToShowAllViolations');
      button.onclick = () => callback();
    }

    onClickFoldNodesToHideNodesWithoutViolations(callback) {
      const button = this.shadowRoot.querySelector('#foldNodesToHideNodesWithoutViolations');
      button.onclick = () => callback();
    }
  });
}());