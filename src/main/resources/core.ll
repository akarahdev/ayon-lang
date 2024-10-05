@core__object = type { i16, i16, [0 x i8] }

define i32 core__increase_ref_count(%Object %obj) {
  entry:
    %refcount_ptr = getelementptr %Object, %Object* %obj, i32 0, i32 1
    %refcount_amt = load i16, %Object* %refcount_ptr
    %refcount_incd = add i16 %refcount_amt, i16 1
    store i16 1, i16* %refcount_ptr
}