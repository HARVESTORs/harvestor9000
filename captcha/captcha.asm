section .text
global _start
_start: mov eax,60 ; exit 0, everyone is human
  xor edi,edi
  syscall
