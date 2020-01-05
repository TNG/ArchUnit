/*
 * Copyright 2014-2020 TNG Technology Consulting GmbH
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
package com.tngtech.archunit;

import java.lang.annotation.Documented;
import java.lang.annotation.Inherited;

/**
 * Any element annotated with this annotation, is meant for internal use ONLY. Users of ArchUnit should never
 * directly access / extend / instantiate any object / member annotated with {@link Internal}.<br>
 * If you do so, you do at your own risk and such code might break with any (even minor) new release.
 */
@Inherited
@Documented
public @interface Internal {
}
