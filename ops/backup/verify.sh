#!/usr/bin/env bash
# Weekly self-test: rebuild the database "as of now" from the newest dump plus every binlog in a
# throwaway MySQL container, then compare it with the live database (restore.sh verify).
exec "$(dirname "$(readlink -f "$0")")/restore.sh" verify
