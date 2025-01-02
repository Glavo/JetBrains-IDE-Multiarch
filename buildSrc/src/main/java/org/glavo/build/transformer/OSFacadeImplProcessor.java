/*
 * Copyright 2025 Glavo
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

package org.glavo.build.transformer;

import org.glavo.classfile.ClassFile;
import org.glavo.classfile.ClassModel;
import org.glavo.classfile.ClassTransform;
import org.glavo.classfile.constantpool.PoolEntry;
import org.glavo.classfile.constantpool.StringEntry;
import org.glavo.classfile.instruction.ConstantInstruction;

import java.util.Objects;

final class OSFacadeImplProcessor {

    public static byte[] process(byte[] bytes) {
        ClassFile cf = ClassFile.of();
        ClassModel cm = cf.parse(bytes);

        StringEntry cEntry = null;
        StringEntry utilEntry = null;

        for (PoolEntry entry : cm.constantPool()) {
            if (entry instanceof StringEntry stringEntry) {
                switch (stringEntry.stringValue()) {
                    case "c" -> cEntry = stringEntry;
                    case "util" -> utilEntry = stringEntry;
                }
            }
        }

        StringEntry finalCEntry = cEntry;
        StringEntry finalUtilEntry = utilEntry;

        Objects.requireNonNull(utilEntry, "utilEntry");
        Objects.requireNonNull(cEntry, "cEntry");

        ClassTransform ct = ClassTransform.transformingMethodBodies(model -> model.methodName().equalsString("<clinit>"),
                (builder, code) -> {
                    if (code instanceof ConstantInstruction.LoadConstantInstruction ldc
                        && ldc.constantEntry().index() == finalUtilEntry.index()) {
                        builder.with(ConstantInstruction.ofLoad(ldc.opcode(), finalCEntry));
                    } else {
                        builder.with(code);
                    }
                });

        return cf.transform(cm, ct);
    }
}
