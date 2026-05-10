package net.Gabou.identity2.util;

import org.objectweb.asm.*;
import java.io.IOException;

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

                        private void countInstruction() {
                            instructionCount++;
                        }

                        @Override
                        public void visitInsn(int opcode) {
                            if (opcode != Opcodes.RETURN && opcode != Opcodes.ARETURN &&
                                opcode != Opcodes.IRETURN && opcode != Opcodes.LRETURN &&
                                opcode != Opcodes.FRETURN && opcode != Opcodes.DRETURN) {
                                countInstruction();
                            }
                        }

                        @Override
                        public void visitIntInsn(int opcode, int operand) {
                            countInstruction();
                        }

                        @Override
                        public void visitVarInsn(int opcode, int varIndex) {
                            countInstruction();
                        }

                        @Override
                        public void visitTypeInsn(int opcode, String type) {
                            countInstruction();
                        }

                        @Override
                        public void visitFieldInsn(int opcode, String owner, String name, String descriptor) {
                            countInstruction();
                        }

                        @Override
                        public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
                            countInstruction();
                        }

                        @Override
                        public void visitInvokeDynamicInsn(String name, String descriptor, Handle bootstrapMethodHandle, Object... bootstrapMethodArguments) {
                            countInstruction();
                        }

                        @Override
                        public void visitJumpInsn(int opcode, Label label) {
                            countInstruction();
                        }

                        @Override
                        public void visitLdcInsn(Object value) {
                            countInstruction();
                        }

                        @Override
                        public void visitIincInsn(int varIndex, int increment) {
                            countInstruction();
                        }

                        @Override
                        public void visitTableSwitchInsn(int min, int max, Label dflt, Label... labels) {
                            countInstruction();
                        }

                        @Override
                        public void visitLookupSwitchInsn(Label dflt, int[] keys, Label[] labels) {
                            countInstruction();
                        }

                        @Override
                        public void visitMultiANewArrayInsn(String descriptor, int numDimensions) {
                            countInstruction();
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
