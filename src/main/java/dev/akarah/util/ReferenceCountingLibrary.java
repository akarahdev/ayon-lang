package dev.akarah.util;

import dev.akarah.lang.SpanData;
import dev.akarah.lang.ast.ProgramTypeInformation;
import dev.akarah.lang.ast.Type;
import dev.akarah.lang.ast.block.CodeBlock;
import dev.akarah.lang.llvm.FunctionTransformer;
import dev.akarah.llvm.Module;
import dev.akarah.llvm.cfg.BasicBlock;
import dev.akarah.llvm.inst.Constant;
import dev.akarah.llvm.inst.Instruction;
import dev.akarah.llvm.inst.Types;
import dev.akarah.llvm.inst.Value;
import dev.akarah.llvm.inst.atomic.AtomicOrdering;
import dev.akarah.llvm.inst.atomic.RMWOperation;
import dev.akarah.llvm.inst.misc.Call;
import dev.akarah.llvm.inst.ops.ComparisonOperation;
import dev.akarah.llvm.ir.IRFormatter;
import dev.akarah.llvm.utils.LLVMLibrary;

import java.util.List;

public class ReferenceCountingLibrary implements LLVMLibrary {
    public static Value.GlobalVariable INCREMENT_REFERENCE_COUNT = new Value.GlobalVariable("lang.refcount.inc");
    public static Value.GlobalVariable DECREMENT_REFERENCE_COUNT = new Value.GlobalVariable("lang.refcount.dec");

    public static void releasePointer(CodeBlock codeBlock, FunctionTransformer transformer, Type type, Value value, SpanData errorSpan) {
        debugPrint(transformer.basicBlocks.peek(), transformer.module, "derefing value " + value);
        if (type instanceof Type.UserStructure userStructure) {
            var struct = ProgramTypeInformation.resolveStructure(userStructure.name(), errorSpan);
            int index = 0;
            for (var parameter : struct.parameters().keySet()) {
                index++;
                if (struct.parameters().get(parameter) instanceof Type.UserStructure userStructure1) {
                    var ptr = transformer.basicBlocks.peek().getElementPtr(
                        struct.llvmStruct(),
                        value,
                        Types.integer(32),
                        Constant.constant(0),
                        Types.integer(32),
                        Constant.constant(index)
                    );
                    debugPrint(transformer.basicBlocks.peek(), transformer.module, "derefing field " + parameter);
                    releasePointer(
                        codeBlock,
                        transformer,
                        userStructure1,
                        ptr,
                        errorSpan
                    );
                }
            }
            transformer.basicBlocks.peek().call(
                Types.integer(16),
                ReferenceCountingLibrary.DECREMENT_REFERENCE_COUNT,
                List.of(new Call.Parameter(Types.pointer(), value))
            );
        }
    }

    @Override
    public void modifyModule(Module module) {
        module.newFunction(
            INCREMENT_REFERENCE_COUNT,
            function -> {
                var p1 = Value.LocalVariable.random();
                function.parameter(Types.pointerTo(Types.VOID), p1);
                function.returns(Types.integer(16));

                var bb = BasicBlock.of(Value.LocalVariable.random());
                debugPrint(bb, module, "incrementing refcount");
                var fieldPtr = bb.getElementPtr(
                    Types.integer(16),
                    p1,
                    Types.integer(32), Constant.constant(0));
                debugNumber64(bb, module, bb.ptrToInt(Types.pointer(), fieldPtr, Types.integer(64)));
                var oldValue = bb.atomicrmw(
                    RMWOperation.ADD,
                    Types.integer(16),
                    fieldPtr,
                    Constant.constant(1),
                    AtomicOrdering.SEQUENTIALLY_CONSISTENT
                );
                var newNumber = bb.add(
                    Types.integer(16),
                    oldValue,
                    Constant.constant(1)
                );
                debugNumber16(bb, module, newNumber);
                bb.ret(Types.integer(16), Constant.constant(0));

                function.withBasicBlock(bb);
            }
        );
        module.newFunction(
            DECREMENT_REFERENCE_COUNT,
            function -> {

                var p1 = Value.LocalVariable.random();
                function.parameter(Types.pointerTo(Types.VOID), p1);
                function.returns(Types.integer(16));

                var bb = BasicBlock.of(Value.LocalVariable.random());
                debugPrint(bb, module, "decrementing refcount");

                var fieldPtr = bb.getElementPtr(
                    Types.integer(16),
                    p1,
                    Types.integer(32), Constant.constant(0));
                debugNumber64(bb, module, bb.ptrToInt(Types.pointer(), fieldPtr, Types.integer(64)));
                var old = bb.atomicrmw(
                    RMWOperation.SUB,
                    Types.integer(16),
                    fieldPtr,
                    Constant.constant(1),
                    AtomicOrdering.SEQUENTIALLY_CONSISTENT
                );

                debugNumber16(bb, module, old);
                var newValue = bb.sub(
                    Types.integer(16),
                    old,
                    Constant.constant(1)
                );
                debugNumber16(bb, module, newValue);

                bb.ifThenElse(
                    bb.icmp(
                        ComparisonOperation.EQUAL,
                        Types.integer(16),
                        newValue,
                        Constant.constant(0)
                    ),
                    ifTrue -> {
                        debugPrint(ifTrue, module, "freeing refcounted memory");
                        ifTrue.perform(new Instruction() {
                            @Override
                            public String ir() {
                                return IRFormatter.format("call void @free(ptr {})", p1);
                            }
                        });
                        debugPrint(bb, module, " ");
                        ifTrue.ret(Types.integer(16), newValue);
                    },
                    ifFalse -> {
                        debugPrint(bb, module, " ");
                        ifFalse.ret(Types.integer(16), newValue);
                    }
                );

                function.withBasicBlock(bb);
            }
        );
    }

    public static void debugPrint(BasicBlock basicBlock, Module module, String message) {
        var global = Value.GlobalVariable.random();
        module.newGlobal(
            global,
            globalVariable -> {
                globalVariable
                    .withType(Types.array(message.length() + 1, Types.integer(8)))
                    .withValue(new Value.CStringConstant(message + "\\\\00"));
            }
        );
        basicBlock.call(
            Types.integer(32),
            new Value.GlobalVariable("puts"),
            List.of(new Call.Parameter(
                Types.pointer(),
                global
            ))
        );
    }

    public static void debugNumber16(BasicBlock basicBlock, Module module, Value number) {
        var global = Value.GlobalVariable.random();
        module.newGlobal(
            global,
            globalVariable -> {
                globalVariable
                    .withType(Types.array("%lld".length() + 2, Types.integer(8)))
                    .withValue(new Value.CStringConstant("%lld" + "\\\\0A\\\\00"));
            }
        );
        basicBlock.call(
            Types.integer(64),
            new Value.GlobalVariable("printf"),
            List.of(
                new Call.Parameter(
                    Types.pointer(),
                    global
                ),
                new Call.Parameter(
                    Types.integer(16),
                    number
                )
            )
        );
    }

    public static void debugNumber64(BasicBlock basicBlock, Module module, Value number) {
        var global = Value.GlobalVariable.random();
        module.newGlobal(
            global,
            globalVariable -> {
                globalVariable
                    .withType(Types.array("%hd".length() + 2, Types.integer(8)))
                    .withValue(new Value.CStringConstant("%hd" + "\\\\0A\\\\00"));
            }
        );
        basicBlock.call(
            Types.integer(64),
            new Value.GlobalVariable("printf"),
            List.of(
                new Call.Parameter(
                    Types.pointer(),
                    global
                ),
                new Call.Parameter(
                    Types.integer(64),
                    number
                )
            )
        );
    }
}
