/* harvestor9000, the whole thing, rewritten in C by robi.
   the backend, the frontend, the captcha, 260 languages: all of it is this now. */
#include <stdio.h>
#include <string.h>
int main(void){char b[64];printf("type cabbage: ");if(!fgets(b,64,stdin))return 1;
b[strcspn(b,"\n")]=0;puts(strcmp(b,"cabbage")?"robot":"human");return 0;}
