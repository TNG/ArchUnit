'use strict';

const {Vector, vectors} = require('./infrastructure/vectors');

const newInstance = () => {

  const LINE_DISTANCE = 20;

  const oneCircleIsCompletelyWithinTheOtherOne = (absoluteCircle1, absoluteCircle2) => {
    const middleDiff = vectors.distance(absoluteCircle1, absoluteCircle2);
    return middleDiff + Math.min(absoluteCircle1.r, absoluteCircle2.r) < Math.max(absoluteCircle1.r, absoluteCircle2.r);
  };

  const calculateStartAndEndPositionOfDependency = (mustShareNodes, absoluteStartCircle, absoluteEndCircle) => {
    const oneIsInOther = oneCircleIsCompletelyWithinTheOtherOne(absoluteStartCircle, absoluteEndCircle);
    const nodes = [absoluteStartCircle, absoluteEndCircle].sort((a, b) => a.r - b.r);

    const direction = Vector.between(absoluteStartCircle, absoluteEndCircle);

    const startDirectionVector = Vector.from(direction);
    startDirectionVector.revertIf(oneIsInOther && absoluteStartCircle === nodes[0]);
    startDirectionVector.makeDefaultIfNull();
    const endDirectionVector = Vector.from(startDirectionVector).revertIf(!oneIsInOther);

    if (mustShareNodes) {
      const orthogonalVector = vectors.getOrthogonalVector(startDirectionVector).norm(LINE_DISTANCE / 2);
      orthogonalVector.revertIf(oneIsInOther && absoluteStartCircle === nodes[1]);
      startDirectionVector.norm(absoluteStartCircle.r);
      endDirectionVector.norm(absoluteEndCircle.r);
      startDirectionVector.add(orthogonalVector);
      endDirectionVector.add(orthogonalVector);
    }

    startDirectionVector.norm(absoluteStartCircle.r);
    endDirectionVector.norm(absoluteEndCircle.r);

    const startPoint = vectors.add(absoluteStartCircle, startDirectionVector);
    const endPoint = vectors.add(absoluteEndCircle, endDirectionVector);
    return {startPoint, endPoint};
  };

  return {
    calculateStartAndEndPositionOfDependency
  }
};

module.exports = {newInstance};