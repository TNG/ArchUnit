'use strict';

import * as d3 from 'd3';
import {defineCustomElement} from '../web-component-infrastructure';

(function () {
  defineCustomElement('visualization-legend', class extends HTMLElement {
    postConnected() {
    }

    initialize(legendItems) {
      const WIDTH = 50;
      const GROUP_HEIGHT = 25;
      legendItems.forEach((item, i) => item.i = i);
      const items = d3.select(this.shadowRoot.querySelector('#legendContainer')).append("g").selectAll()
        .data(legendItems).enter().append("g").attr("transform", i => "translate(" + 5 + "," + (20 + GROUP_HEIGHT * i.i) + ")");
      const rects = items.append("line").attr("x1", 0).attr("y1", -6).attr("x2", WIDTH).attr("y2", -6);
      rects.each(function (r) {
        r.styles.filter(s => s.value).forEach(s => d3.select(this).style(s.name, s.value));
      });
      rects.style("stroke-width", "6px");
      items.append("text").text(i => i.title).attr("x", WIDTH + 5);
      d3.select(this.shadowRoot.querySelector('#legendContainer')).attr("height", () => legendItems.length * GROUP_HEIGHT + 20);
    }
  });
})();