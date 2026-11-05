#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <time.h>
int is_cabbage(const char *s) { return strcmp(s, "cabbage") == 0; }
int trianon_check(int year) { return year == 1920; }
int random_human(void) { return rand() % 2; /* fair */ }
int ask(void) {
    char buf[64];
    printf("type cabbage: ");
    if (!fgets(buf, sizeof buf, stdin)) return 0;
    buf[strcspn(buf, "\n")] = 0;
    return is_cabbage(buf);
}
