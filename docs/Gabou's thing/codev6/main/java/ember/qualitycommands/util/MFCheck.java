package ember.qualitycommands.util;

import org.objectweb.asm.*;
import java.io.IOException;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;

public class MFCheck {
    public static boolean isMethodEmpty(Class<?> clazz, String methodName) throws IOException {
        ClassReader reader = new ClassReader(clazz.getName());
        final boolean[] isEmpty = {false};

        reader.accept(new ClassVisitor(Opcodes.ASM9) {
            @Override
            public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
                if (name.equals(methodName)) {
                    return new MethodVisitor(Opcodes.ASM9) {
                        int instructionCount = 0;

                        @Override
                        public void visitInsn(int opcode) {
                            // Count instructions except return
                            if (opcode != Opcodes.RETURN && opcode != Opcodes.ARETURN &&
                                opcode != Opcodes.IRETURN && opcode != Opcodes.LRETURN &&
                                opcode != Opcodes.FRETURN && opcode != Opcodes.DRETURN) {
                                instructionCount++;
                            }
                        }

                        @Override
                        public void visitEnd() {
                            isEmpty[0] = (instructionCount == 0);
                        }
                    };
                }
                return null;
            }
        }, 0);

        return isEmpty[0];
    }

}
