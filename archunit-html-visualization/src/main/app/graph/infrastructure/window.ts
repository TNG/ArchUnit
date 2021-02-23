'use strict';

interface ArchUnitWindow {
  getInnerWidth: () => number,
  getInnerHeight: () => number,
}

const init = {
  getInnerWidth: (): number => window.innerWidth,
  getInnerHeight: (): number => window.innerHeight,
};

export {ArchUnitWindow, init};
