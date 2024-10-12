package dev.akarah.util;

import dev.akarah.llvm.Module;
import dev.akarah.llvm.cfg.BasicBlock;
import dev.akarah.llvm.inst.Constant;
import dev.akarah.llvm.inst.Instruction;
import dev.akarah.llvm.inst.Types;
import dev.akarah.llvm.inst.Value;
import dev.akarah.llvm.inst.misc.Call;
import dev.akarah.llvm.inst.ops.ComparisonOperation;
import dev.akarah.llvm.ir.IRFormatter;
import dev.akarah.llvm.utils.LLVMLibrary;

import java.util.List;

public class ReferenceCountingLibrary implements LLVMLibrary {
    public static Value.GlobalVariable INCREMENT_REFERENCE_COUNT = new Value.GlobalVariable("lang.refcount.inc");
    public static Value.GlobalVariable DECREMENT_REFERENCE_COUNT = new Value.GlobalVariable("lang.refcount.dec");

    @Override
    public void modifyModule(Module module) {
        module.newFunction(
            INCREMENT_REFERENCE_COUNT,
            function -> {
                var p1 = Value.LocalVariable.random();
                function.parameter(Types.pointerTo(Types.VOID), p1);
                function.returns(Types.integer(16));

                var bb = BasicBlock.of(Value.LocalVariable.random());
                var fieldPtr = bb.getElementPtr(
                    Types.integer(16),
                    p1,
                    Types.integer(32), Constant.constant(0));
                var addedValue = bb.add(
                    Types.integer(16),
                    bb.load(
                        Types.integer(16),
                        fieldPtr
                    ),
                    Constant.constant(1)
                );
                bb.store(
                    Types.integer(16),
                    addedValue,
                    fieldPtr
                );
                bb.ret(Types.integer(16), addedValue);

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
                var fieldPtr = bb.getElementPtr(
                    Types.integer(16),
                    p1,
                    Types.integer(32), Constant.constant(0));
                var addedValue = bb.sub(
                    Types.integer(16),
                    bb.load(
                        Types.integer(16),
                        fieldPtr
                    ),
                    Constant.constant(1)
                );
                bb.store(
                    Types.integer(16),
                    addedValue,
                    fieldPtr
                );
                bb.ifThenElse(
                    bb.icmp(
                        ComparisonOperation.SIGNED_LESS_THAN_OR_EQUAL,
                        Types.integer(16),
                        addedValue,
                        Constant.constant(0)
                    ),
                    ifTrue -> {
                        ifTrue.perform(new Instruction() {
                            @Override
                            public String ir() {
                                return IRFormatter.format("call void @free(ptr {})", p1);
                            }
                        });
                        ifTrue.ret(Types.integer(16), addedValue);
                    },
                    ifFalse -> {
                        ifFalse.ret(Types.integer(16), addedValue);
                    }
                );

                function.withBasicBlock(bb);
            }
        );
    }
}
