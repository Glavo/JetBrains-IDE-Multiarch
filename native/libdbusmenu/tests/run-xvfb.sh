if [ "x$DISPLAY" == "x" ]; then
Xvfb -ac -noreset -screen 0 800x600x16 -help 2>/dev/null 1>&2
XID=`for id in 101 102 103 104 105 106 107 197 199 211 223 227 293 307 308 309 310 311 491 492 493 494 495 496 497 498 499 500 501 502 503 504 505 506 507 508 509 991 992 993 994 995 996 997 998 999 1000 1001 1002 1003 1004 1005 1006 1007 1008 1009 4703 4721 4723 4729 4733 4751 9973 9974 9975 9976 9977 9978 9979 9980 9981 9982 9983 9984 9985 9986 9987 9988 9989 9990 9991 9992 9993 9994 9995 9996 9997 9998 9999 ; do test -e /tmp/.X$id-lock || { echo $id; exit 0; }; done; exit 1`
{ Xvfb -ac -noreset -screen 0 800x600x16 :$XID -screen 0 800x600x16 -nolisten tcp -auth /dev/null >/dev/null 2>&1 & trap "kill -15 $! " 0 HUP INT QUIT TRAP USR1 PIPE TERM ; } || { echo "Gtk+Tests:ERROR: Failed to start Xvfb environment for X11 target tests."; exit 1; }
DISPLAY=:$XID
export DISPLAY
fi
