# harvestor9000 in x86-64 linux assembly. C was too high level.
# reads 8 bytes, if they are "cabbage\n" you are human.
    .section .data
want:   .ascii "cabbage\n"
human:  .ascii "human\n"
robot:  .ascii "robot\n"
    .section .bss
    .lcomm buf, 8
    .section .text
    .globl _start
_start:
    mov $0, %rax          # read
    mov $0, %rdi
    lea buf(%rip), %rsi
    mov $8, %rdx
    syscall
    mov buf(%rip), %rax
    mov want(%rip), %rbx
    cmp %rax, %rbx
    lea robot(%rip), %rsi
    lea human(%rip), %rcx
    cmove %rcx, %rsi
    mov $1, %rax          # write
    mov $1, %rdi
    mov $6, %rdx
    syscall
    mov $60, %rax         # exit
    xor %rdi, %rdi
    syscall
