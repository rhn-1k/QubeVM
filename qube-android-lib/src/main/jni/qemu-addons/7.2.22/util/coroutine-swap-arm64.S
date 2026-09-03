 /*
Copyright (C) Rhn 2026
 */

// QUBE: raw ARM64 context switch, without libc, avoids PAC longjmp crash on Snapdragon
// coro_swap(save_sp, new_sp) save regs, switch sp, restore regs, return
// 160B frame: x19-x28, fp, lr, d8-d15
    .text
    .align 4
    .global coro_swap
    .type coro_swap, %function
coro_swap:
    sub     sp, sp, #160
    stp     x19, x20, [sp, #0]
    stp     x21, x22, [sp, #16]
    stp     x23, x24, [sp, #32]
    stp     x25, x26, [sp, #48]
    stp     x27, x28, [sp, #64]
    stp     x29, x30, [sp, #80]
    stp     d8,  d9,  [sp, #96]
    stp     d10, d11, [sp, #112]
    stp     d12, d13, [sp, #128]
    stp     d14, d15, [sp, #144]

    mov     x2, sp
    str     x2, [x0] // QUBE: *save_sp = current sp

    mov     sp, x1 // QUBE: switch to new stack

    ldp     x19, x20, [sp, #0]
    ldp     x21, x22, [sp, #16]
    ldp     x23, x24, [sp, #32]
    ldp     x25, x26, [sp, #48]
    ldp     x27, x28, [sp, #64]
    ldp     x29, x30, [sp, #80]
    ldp     d8,  d9,  [sp, #96]
    ldp     d10, d11, [sp, #112]
    ldp     d12, d13, [sp, #128]
    ldp     d14, d15, [sp, #144]
    add     sp, sp, #160
    ret
    .size coro_swap, . - coro_swap

// QUBE: one-time setup of a coroutine's first stack frame
    .align 4
    .global coro_bootstrap_init
    .type coro_bootstrap_init, %function
coro_bootstrap_init:
    // QUBE: build frame so first coro_swap() jumps into the trampoline
    sub     x3, x0, #160
    str     x1, [x3, #0] // QUBE: x19 slot = entry fn
    str     x2, [x3, #8] // QUBE: x20 slot = arg
    adrp    x4, coro_entry_trampoline
    add     x4, x4, :lo12:coro_entry_trampoline
    str     x4, [x3, #88] // QUBE: x30/lr slot = trampoline
    str     xzr, [x3, #80] // QUBE: x29/fp slot = 0
    mov     x0, x3 // QUBE: return the prepared sp
    ret
    .size coro_bootstrap_init, . - coro_bootstrap_init

coro_entry_trampoline:
    // QUBE: x19=entry, x20=arg, from coro_bootstrap_init
    mov     x0, x20
    blr     x19
    // QUBE: entry() must never return
    brk     #0
    .size coro_entry_trampoline, . - coro_entry_trampoline
