	.text
			  # _main () (a)
			  # t1	%rax
			  # a	%rbx
			  # t2	%rax
			  # t3	%rax
			  # t4	%rax
			  # t5	%rax
			  # t6	%rax
			  # t7	%rax
			  # t8	%rdi
			  # t9	%rax
			  # t10	%rax
			  # t11	%rdi
	.p2align 4,0x90
	.globl _main
_main:
	pushq %rbx
			  #  t1 = call _malloc(8)
	movq $8,%rdi
	call _malloc
			  #  a = t1
	movq %rax,%rbx
			  #  t2 = 0 * 4
	movq $4,%r10
	movq $0,%r11
	movq %r11,%rax
	imulq %r10,%rax
			  #  t3 = a + t2
	movq %rax,%r11
	movq %rbx,%rax
	addq %r11,%rax
			  #  [t3] = 1
	movq $1,%r10
	movl %r10d,(%rax)
			  #  t4 = 1 * 4
	movq $4,%r10
	movq $1,%r11
	movq %r11,%rax
	imulq %r10,%rax
			  #  t5 = a + t4
	movq %rax,%r11
	movq %rbx,%rax
	addq %r11,%rax
			  #  [t5] = 2
	movq $2,%r10
	movl %r10d,(%rax)
			  #  t6 = 0 * 4
	movq $4,%r10
	movq $0,%r11
	movq %r11,%rax
	imulq %r10,%rax
			  #  t7 = a + t6
	movq %rax,%r11
	movq %rbx,%rax
	addq %r11,%rax
			  #  t8 = [t7]
	movslq (%rax),%rdi
			  #  call _printInt(t8)
	call _printInt
			  #  t9 = 1 * 4
	movq $4,%r10
	movq $1,%r11
	movq %r11,%rax
	imulq %r10,%rax
			  #  t10 = a + t9
	movq %rax,%r11
	movq %rbx,%rax
	addq %r11,%rax
			  #  t11 = [t10]
	movslq (%rax),%rdi
			  #  call _printInt(t11)
	call _printInt
			  #  return 
	popq %rbx
	ret
