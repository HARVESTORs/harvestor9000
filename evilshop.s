# evilshop.s - the entire harvestor9000 (spring boot, 18 java files, maven, docker)
# rewritten in x86-64 linux assembly. no libc. no jvm. no regrets.
# minor optimizations done.
#
# build:  as evilshop.s -o evilshop.o && ld evilshop.o -o evilshop
# run:    ./evilshop        (listens on 0.0.0.0:8080, like spring did)
#
# architecture: one process, one thread, one buffer, zero sessions.
# every request gets an answer from a hardcoded table. the state is a lie.
# the business logic (pricing, warranty roulette, cancellation maze) is
# pre-computed. it was always going to say no anyway.

    .set SYS_READ, 0
    .set SYS_WRITE, 1
    .set SYS_CLOSE, 3
    .set SYS_SOCKET, 41
    .set SYS_ACCEPT, 43
    .set SYS_BIND, 49
    .set SYS_LISTEN, 50
    .set SYS_SETSOCKOPT, 54
    .set SYS_EXIT, 60

# ---------------------------------------------------------------- data

    .section .rodata

sockaddr:                       # AF_INET, port 8080 (big endian), 0.0.0.0
    .word 2
    .byte 0x1f, 0x90
    .long 0
    .quad 0

one: .long 1

# --- responses: header + body. lengths computed by the assembler.

.macro RESP name, status, ctype, body
\name\()_body: .ascii "\body"
\name\()_body_end:
\name\()_hdr:  .ascii "HTTP/1.1 \status\r\nContent-Type: \ctype\r\nAccess-Control-Allow-Origin: *\r\nAccess-Control-Allow-Headers: *\r\nAccess-Control-Allow-Methods: *\r\nConnection: close\r\nServer: cabbage/1920\r\n\r\n"
\name\()_hdr_end:
.endm

.macro JSON name, body
    RESP \name, "200 OK", "application/json", "\body"
.endm

JSON health, "{\"status\":\"UP\",\"name\":\"EvilTech — Department of Margin Extraction\",\"mode\":\"UNIVERSITY_SIMULATION\",\"currency\":\"HUF\",\"ai\":\"LOCAL_VILLAIN\",\"payments\":\"MOCK_ONLY\",\"mechanisms\":\"/api/mechanisms\",\"simulated\":true,\"runtime\":\"x86-64 assembly\"}"
RESP session, "201 Created", "application/json", "{\"token\":\"cabbage\",\"alias\":\"robi\",\"simulated\":true}"
JSON me, "{\"alias\":\"robi\",\"token\":\"cabbage\",\"segment\":\"WHALE\",\"simulated\":true}"
JSON context, "{\"ok\":true,\"note\":\"device context ignored. we already know.\"}"
JSON products, "[{\"id\":\"laptop-sensible\",\"name\":\"SensibleBook 14 — shareholder disappointment\",\"price\":249990.00},{\"id\":\"laptop-overlord\",\"name\":\"OverlordBook Ultra — now with executive aura\",\"price\":899990.00},{\"id\":\"laptop-decoy\",\"name\":\"DecoyBook Pro — almost the same price, fewer things\",\"price\":849990.00},{\"id\":\"phone-sensible\",\"name\":\"PocketSense — alarmingly reasonable\",\"price\":129990.00},{\"id\":\"phone-emperor\",\"name\":\"EmperorPhone Obsidian — a status subscription\",\"price\":599990.00},{\"id\":\"phone-decoy\",\"name\":\"Phone of Questionable Value\",\"price\":579990.00}]"
JSON product, "{\"id\":\"phone-emperor\",\"name\":\"EmperorPhone Obsidian — a status subscription\",\"price\":599990.00,\"note\":\"every product is this product now\"}"
JSON visit, "{\"ok\":true,\"priceDelta\":\"+5%\",\"note\":\"we saw you looking\"}"
JSON cart, "{\"items\":[],\"total\":0,\"note\":\"cart is stateless. like the developer.\"}"
JSON cartadd, "{\"ok\":true,\"items\":[{\"lineId\":\"1\",\"productId\":\"phone-emperor\",\"qty\":1}],\"addedWarranty\":true}"
JSON quote, "{\"subtotal\":599990.00,\"warranty\":49990.00,\"serviceFee\":19990.00,\"total\":669970.00,\"financing\":\"48 months, apr: yes\"}"
JSON checkout, "{\"orderId\":\"ORD-1920\",\"status\":\"PLACED\",\"subscriptionEnrolled\":true}"
JSON orders, "[{\"orderId\":\"ORD-1920\",\"total\":669970.00}]"
JSON spin, "{\"result\":\"LOSE\",\"note\":\"the wheel is decorative\"}"
JSON sub, "{\"active\":true,\"plan\":\"EvilPrime\",\"monthly\":4990}"
JSON cancel, "{\"step\":1,\"of\":9,\"question\":\"are you sure? what about the gorilla?\"}"
JSON canceld, "{\"cancelled\":false,\"reason\":\"direct cancellation is a myth\"}"
JSON ledger, "[{\"event\":\"margin extracted\",\"amount\":669970.00}]"
JSON events, "[]"
JSON advance, "{\"ok\":true,\"day\":2}"
JSON propaganda, "{\"headline\":\"you deserve the obsidian edition\"}"
JSON mechanisms, "[\"decoy pricing\",\"drip fees\",\"warranty roulette\",\"cancellation maze\",\"written in assembly\"]"
JSON ok, "{\"ok\":true}"
RESP notfound, "404 Not Found", "application/json", "{\"error\":\"PRODUCT_NOT_FOUND\",\"message\":\"That product escaped the boardroom.\"}"

index_hdr: .ascii "HTTP/1.1 200 OK\r\nContent-Type: text/html; charset=utf-8\r\nConnection: close\r\n\r\n"
index_hdr_end:
index_body: .incbin "res/index.html"
index_body_end:
jpeg_hdr: .ascii "HTTP/1.1 200 OK\r\nContent-Type: image/jpeg\r\nConnection: close\r\n\r\n"
jpeg_hdr_end:
jpeg_body: .incbin "res/gorilla.jpeg"
jpeg_body_end:

# --- routing table: method+path prefix, response. first match wins.
# longest prefixes first, because we do not parse, we vibe.

.macro ROUTE pfx, name
    .quad 1f, 2f-1f, \name\()_hdr, \name\()_hdr_end-\name\()_hdr, \name\()_body, \name\()_body_end-\name\()_body
    .pushsection .rodata.str
1:  .ascii "\pfx"
2:
    .popsection
.endm

routes:
    ROUTE "OPTIONS ", ok
    ROUTE "GET / ", index
    ROUTE "GET /index.html", index
    ROUTE "GET /captcha/images/gorilla.jpeg", jpeg
    ROUTE "GET /api/health", health
    ROUTE "POST /api/sessions", session
    ROUTE "GET /api/me", me
    ROUTE "PUT /api/context", context
    ROUTE "GET /api/products ", products
    ROUTE "GET /api/products?", products
    ROUTE "GET /api/products/", product
    ROUTE "POST /api/products/", visit
    ROUTE "GET /api/cart", cart
    ROUTE "POST /api/cart/items", cartadd
    ROUTE "DELETE /api/cart/items/", cart
    ROUTE "POST /api/checkout/quote", quote
    ROUTE "POST /api/checkout", checkout
    ROUTE "GET /api/orders", orders
    ROUTE "POST /api/warranty/spin", spin
    ROUTE "GET /api/subscription", sub
    ROUTE "POST /api/subscription/cancel-direct", canceld
    ROUTE "POST /api/subscription/cancel", cancel
    ROUTE "POST /api/subscription", sub
    ROUTE "GET /api/ledger", ledger
    ROUTE "GET /api/events", events
    ROUTE "POST /api/demo/advance", advance
    ROUTE "GET /api/propaganda", propaganda
    ROUTE "GET /api/mechanisms", mechanisms
routes_end:

    .set ROUTE_SIZE, 48

# ---------------------------------------------------------------- bss

    .section .bss
    .lcomm reqbuf, 8192
    .lcomm listenfd, 8

# ---------------------------------------------------------------- code

    .section .text
    .globl _start
_start:
    mov $SYS_SOCKET, %eax           # socket(AF_INET, SOCK_STREAM, 0)
    mov $2, %edi
    mov $1, %esi
    xor %edx, %edx
    syscall
    test %rax, %rax
    js die
    mov %rax, listenfd(%rip)

    mov %rax, %rdi                  # setsockopt(SO_REUSEADDR), restart in peace
    mov $SYS_SETSOCKOPT, %eax
    mov $1, %esi
    mov $2, %edx
    lea one(%rip), %r10
    mov $4, %r8d
    syscall

    mov $SYS_BIND, %eax
    mov listenfd(%rip), %rdi
    lea sockaddr(%rip), %rsi
    mov $16, %edx
    syscall
    test %rax, %rax
    js die

    mov $SYS_LISTEN, %eax
    mov listenfd(%rip), %rdi
    mov $128, %esi
    syscall

accept_loop:
    mov $SYS_ACCEPT, %eax
    mov listenfd(%rip), %rdi
    xor %esi, %esi
    xor %edx, %edx
    syscall
    test %rax, %rax
    js accept_loop
    mov %rax, %r12                  # r12 = client fd

    mov $SYS_READ, %eax             # one read. if the request is split, tough.
    mov %r12, %rdi
    lea reqbuf(%rip), %rsi
    mov $8192, %edx
    syscall
    test %rax, %rax
    jle close_client
    mov %rax, %r13                  # r13 = request length

    lea routes(%rip), %rbx
route_loop:
    lea routes_end(%rip), %rax
    cmp %rax, %rbx
    jae not_found
    mov 8(%rbx), %rcx               # prefix length
    cmp %r13, %rcx
    ja next_route
    mov (%rbx), %rsi                # prefix
    lea reqbuf(%rip), %rdi
    repe cmpsb
    je found
next_route:
    add $ROUTE_SIZE, %rbx
    jmp route_loop

not_found:
    lea notfound_hdr(%rip), %rsi
    mov $(notfound_hdr_end-notfound_hdr), %rdx
    lea notfound_body(%rip), %r14
    mov $(notfound_body_end-notfound_body), %r15
    jmp respond

found:
    mov 16(%rbx), %rsi
    mov 24(%rbx), %rdx
    mov 32(%rbx), %r14
    mov 40(%rbx), %r15

respond:
    mov %r12, %rdi
    call write_all
    mov %r14, %rsi
    mov %r15, %rdx
    mov %r12, %rdi
    call write_all

close_client:
    mov $SYS_CLOSE, %eax
    mov %r12, %rdi
    syscall
    jmp accept_loop

# write_all(rdi=fd, rsi=buf, rdx=len): loops until written or the socket dies
write_all:
    test %rdx, %rdx
    jz 2f
1:  mov $SYS_WRITE, %eax
    push %rdi
    push %rsi
    push %rdx
    syscall
    pop %rdx
    pop %rsi
    pop %rdi
    test %rax, %rax
    jle 2f
    add %rax, %rsi
    sub %rax, %rdx
    jnz 1b
2:  ret

die:
    mov $SYS_EXIT, %eax
    mov $1, %edi                    # exit(1): port taken, trianon
    syscall
