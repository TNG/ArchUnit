/*
 * Copyright 2014-2022 TNG Technology Consulting GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.tngtech.archunit.library.dependencies;

import static com.google.common.base.Preconditions.checkState;
import static java.util.Arrays.copyOf;
import static java.util.Arrays.fill;

class PrimitiveDataTypes {
    private PrimitiveDataTypes() {
    }

    static final class IntArray {
        private static final int UNINITIALIZED_MARKER = -1;

        private final int[] array;

        IntArray(int size) {
            array = new int[size];
            reset();
        }

        boolean isSet(int index) {
            return array[index] != UNINITIALIZED_MARKER;
        }

        int get(int index) {
            int result = array[index];
            checkState(result != UNINITIALIZED_MARKER, "Value at index %d is uninitialized", index);
            return result;
        }

        void set(int index, int value) {
            array[index] = value;
        }

        void reset() {
            fill(array, UNINITIALIZED_MARKER);
        }
    }

    static final class IntStack {
        private final int[] stack;
        private int pointer = 0;

        IntStack(int maxSize) {
            stack = new int[maxSize];
        }

        void push(int number) {
            stack[pointer++] = number;
        }

        int pop() {
            return stack[--pointer];
        }

        void reset() {
            pointer = 0;
        }

        int[] asArray() {
            return copyOf(stack, pointer);
        }
    }
}
