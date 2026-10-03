 /*
Copyright (C) Rhn 2026
 */

// Raw x86_64 context switch, without libc
// coro_swap(save_sp, new_sp), save regs, switch sp, restore regs, return
// 48B frame: rbx, rbp, r12-r15

    .text
    .p2align 4
    .globl coro_swap
    .type coro_swap, @function
coro_swap:
    subq    $48, %rsp
    movq    %rbx, 0(%rsp)
    movq    %rbp, 8(%rsp)
    movq    %r12, 16(%rsp)
    movq    %r13, 24(%rsp)
    movq    %r14, 32(%rsp)
    movq    %r15, 40(%rsp)

    movq    %rsp, (%rdi) // *save_sp = current sp
    movq    %rsi, %rsp // switch to new stack

    movq    0(%rsp), %rbx
    movq    8(%rsp), %rbp
    movq    16(%rsp), %r12
    movq    24(%rsp), %r13
    movq    32(%rsp), %r14
    movq    40(%rsp), %r15
    addq    $48, %rsp
    ret
    .size coro_swap, . - coro_swap

// one-time setup of a coroutine's first stack frame
    .p2align 4
    .globl coro_bootstrap_init
    .type coro_bootstrap_init, @function
coro_bootstrap_init:
    // QUBE: build frame so first coro_swap() jumps into the trampoline
    subq    $64, %rdi
    movq    %rsi, 16(%rdi) // r12 slot = entry fn
    movq    %rdx, 24(%rdi) // r13 slot = arg
    leaq    coro_entry_trampoline(%rip), %rax
    movq    %rax, 48(%rdi) // return address = trampoline
    movq    %rdi, %rax // return the prepared sp
    ret
    .size coro_bootstrap_init, . - coro_bootstrap_init

coro_entry_trampoline:
    // r12=entry, r13=arg, from coro_bootstrap_init
    movq    %r13, %rdi
    subq    $8, %rsp // align stack for the entry call
    call    *%r12
    // entry() must never return
    ud2
    .size coro_entry_trampoline, . - coro_entry_trampoline