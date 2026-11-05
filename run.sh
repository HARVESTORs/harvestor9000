#!/bin/sh
set -e
as evilshop.s -o evilshop.o && ld evilshop.o -o evilshop && exec ./evilshop
