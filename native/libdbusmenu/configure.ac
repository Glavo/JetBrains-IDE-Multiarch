
AC_INIT(libdbusmenu, 16.04.0, ted@canonical.com)
AC_COPYRIGHT([Copyright 2009-2016 Canonical])

AC_PREREQ(2.62)

AM_CONFIG_HEADER(config.h)
AM_INIT_AUTOMAKE(libdbusmenu, 16.04.0, [-Wno-portability])

AM_MAINTAINER_MODE

IT_PROG_INTLTOOL([0.35.0])
AC_ISC_POSIX
AC_PROG_CC
AM_PROG_CC_C_O
AC_STDC_HEADERS
AC_PROG_LIBTOOL

AC_SUBST(VERSION)
AC_CONFIG_MACRO_DIR([m4])

m4_ifdef([AM_SILENT_RULES],[AM_SILENT_RULES([yes])])

AC_PATH_PROG([GLIB_MKENUMS], [glib-mkenums])

###########################
# GTK Doc 
###########################

GTK_DOC_CHECK([1.14],[--flavour no-tmpl])

###########################
# Dependencies - GLib
###########################

GLIB_REQUIRED_VERSION=2.35.4

PKG_CHECK_MODULES(DBUSMENUGLIB, glib-2.0 >= $GLIB_REQUIRED_VERSION
                                gio-2.0 >= $GLIB_REQUIRED_VERSION)

AC_SUBST(DBUSMENUGLIB_CFLAGS)
AC_SUBST(DBUSMENUGLIB_LIBS)

###########################
# Dependencies - GTK
###########################

GTK_REQUIRED_VERSION=2.16
GTK3_REQUIRED_VERSION=2.91

AC_ARG_ENABLE([gtk],
	AC_HELP_STRING([--disable-gtk], [Disable libdbusmenu-gtk library]),
	[enable_gtk=$enableval], [enable_gtk=auto])
AM_CONDITIONAL([WANT_LIBDBUSMENUGTK], [test "x$enable_gtk" != "xno"])

AC_ARG_WITH([gtk],
  [AS_HELP_STRING([--with-gtk],
    [Which version of gtk to use @<:@default=3@:>@])],
  [],
  [with_gtk=3])
AM_CONDITIONAL([USE_GTK3], [test "x$with_gtk" = "x3"])

AS_IF([test "x$enable_gtk" != "xno"],[
AS_IF([test "x$with_gtk" = x3],
        [PKG_CHECK_MODULES(DBUSMENUGTK,  gtk+-3.0 >= $GTK3_REQUIRED_VERSION
                                         atk
                                         glib-2.0 >= $GLIB_REQUIRED_VERSION,
                                         [have_gtk=yes]
)
        ],
      [test "x$with_gtk" = x2],
        [PKG_CHECK_MODULES(DBUSMENUGTK,  gtk+-2.0 >= $GTK_REQUIRED_VERSION
                                         atk
                                         glib-2.0 >= $GLIB_REQUIRED_VERSION,
                                         [have_gtk=yes]
)
        ],
      [AC_MSG_FAILURE([Value for --with-gtk was neither 2 nor 3])]
)
])
AC_SUBST(DBUSMENUGTK_CFLAGS)
AC_SUBST(DBUSMENUGTK_LIBS)

###########################
# Dependencies - dumper
###########################

X11_REQUIRED_VERSION=1.3

AC_ARG_ENABLE([dumper],
	AC_HELP_STRING([--disable-dumper], [Disable dbusmenudumper tool]),
	[enable_dumper=$enableval], [enable_dumper=auto])
AM_CONDITIONAL([WANT_DBUSMENUDUMPER], [test "x$enable_dumper" != "xno"])

AS_IF([test "x$enable_dumper" != "xno"],[
PKG_CHECK_MODULES(DBUSMENUDUMPER, gtk+-2.0 >= $GTK_REQUIRED_VERSION
                                  x11 >= $X11_REQUIRED_VERSION,
                                  [have_dumper=yes]
)
])

AC_SUBST(DBUSMENUDUMPER_CFLAGS)
AC_SUBST(DBUSMENUDUMPER_LIBS)

###########################
# Dependencies - Testing
###########################

JSON_GLIB_REQUIRED_VERSION=0.13.4
GIO_UNIX_REQUIRED_VERSION=2.24

AC_ARG_ENABLE([tests],
	AC_HELP_STRING([--disable-tests], [Disable tests]),
	[enable_tests=$enableval], [enable_tests=auto])
AM_CONDITIONAL([WANT_TESTS], [test "x$enable_tests" != "xno"])

AS_IF([test "x$enable_tests" != "xno"],[
PKG_CHECK_MODULES(DBUSMENUTESTS,  json-glib-1.0 >= $JSON_GLIB_REQUIRED_VERSION
                                  gio-unix-2.0  >= $GIO_UNIX_REQUIRED_VERSION,
                                  [have_tests=yes]
)
PKG_CHECK_MODULES(DBUSMENUTESTSVALGRIND, valgrind, have_valgrind=yes, have_valgrind=no)
AM_CONDITIONAL([HAVE_VALGRIND], [test "x$have_valgrind" = "xyes"])
])

AC_SUBST(DBUSMENUTESTS_CFLAGS)
AC_SUBST(DBUSMENUTESTS_LIBS)

###########################
# GObject Introspection
###########################

GOBJECT_INTROSPECTION_CHECK([0.6.7])

PKG_CHECK_EXISTS([gobject-introspection-1.0 >= 0.10],
				 introspection_ten=yes,
				 introspection_ten=no)

AM_CONDITIONAL(INTROSPECTION_TEN, [test "x$introspection_ten" = "xyes"])

if test "$enable_introspection" = "yes"; then
	# Redefine introspection dirs using proper prefixes

	INTROSPECTION_GIRDIR=`$PKG_CONFIG --define-variable=datadir=${datadir} --variable=girdir gobject-introspection-1.0`
	AC_SUBST(INTROSPECTION_GIRDIR)

	INTROSPECTION_TYPELIBDIR=`$PKG_CONFIG --define-variable=libdir=${libdir} --variable=typelibdir gobject-introspection-1.0`
	AC_SUBST(INTROSPECTION_TYPELIBDIR)
fi

###########################
# Vala API Generation
###########################

AC_ARG_ENABLE([vala],
	AC_HELP_STRING([--disable-vala], [Disable vala]),
	[enable_vala=$enableval], [enable_vala=auto])

AS_IF([test "x$enable_vala" != "xno"],[
	AM_COND_IF([HAVE_INTROSPECTION],,[
		AC_MSG_ERROR([Vala bindings require introspection support, please --enable-introspection])
	])
AC_PATH_PROG([VALA_API_GEN], [vapigen])
])
AM_CONDITIONAL([HAVE_VALA], [test -n "$VALA_API_GEN"])

###########################
# XSLT Processor
###########################

AC_PATH_PROG([XSLT_PROC], [xsltproc])

###########################
# Lib versioning 
###########################

LIBDBUSMENU_CURRENT=4
LIBDBUSMENU_REVISION=12
LIBDBUSMENU_AGE=0

AC_SUBST(LIBDBUSMENU_CURRENT)
AC_SUBST(LIBDBUSMENU_REVISION)
AC_SUBST(LIBDBUSMENU_AGE)

###########################
# Internationalization
###########################

GETTEXT_PACKAGE=libdbusmenu
AC_SUBST(GETTEXT_PACKAGE)
AC_DEFINE_UNQUOTED(GETTEXT_PACKAGE, "$GETTEXT_PACKAGE", [Name of the default gettext domain])

AM_GLIB_GNU_GETTEXT

###########################
# Massive Debugging
###########################

with_massivedebugging="no"
AC_ARG_ENABLE(massivedebugging, AS_HELP_STRING([--enable-massivedebugging], [Print out so much information your brain will hurt]), with_massivedebugging=$enableval, with_massivedebugging=no)
if test "x$with_massivedebugging" = "xyes"; then
	AC_DEFINE([MASSIVEDEBUGGING], [1], [Print everyting])
fi

###########################
# gcov coverage reporting
###########################

m4_include([m4/gcov.m4])
AC_TDD_GCOV
AM_CONDITIONAL([HAVE_GCOV], [test "x$ac_cv_check_gcov" = xyes])
AM_CONDITIONAL([HAVE_LCOV], [test "x$ac_cv_check_lcov" = xyes])
AM_CONDITIONAL([HAVE_GCOVR], [test "x$ac_cv_check_gcovr" = xyes])
AC_SUBST(COVERAGE_CFLAGS)
AC_SUBST(COVERAGE_LDFLAGS)

###########################
# Files
###########################

AC_OUTPUT([
Makefile
po/Makefile.in
libdbusmenu-glib/Makefile
libdbusmenu-glib/dbusmenu-glib-0.4.pc
libdbusmenu-gtk/Makefile
libdbusmenu-gtk/dbusmenu-gtk-0.4.pc
libdbusmenu-gtk/dbusmenu-gtk3-0.4.pc
tools/Makefile
tools/testapp/Makefile
tests/Makefile
tests/dbusmenu-jsonloader-0.4.pc
docs/Makefile
docs/libdbusmenu-glib/Makefile
docs/libdbusmenu-glib/reference/Makefile
docs/libdbusmenu-glib/reference/version.xml
docs/libdbusmenu-gtk/Makefile
docs/libdbusmenu-gtk/reference/Makefile
docs/libdbusmenu-gtk/reference/version.xml
])

###########################
# Results
###########################

AC_MSG_NOTICE([

libdbusmenu Configuration:

	Prefix:                 $prefix
	Massive Debugging:      $with_massivedebugging
	GTK+ Version:           $with_gtk
])

AS_IF([test "x$have_tests" = "xyes"],
	AC_MSG_NOTICE([	Tests:                  yes]),
	AC_MSG_NOTICE([	Tests:                  no])
)

AS_IF([test "x$enable_gtk" = "xno"],
	AC_MSG_NOTICE([	Gtk:                    no]),
	AC_MSG_NOTICE([	Gtk:                    yes (gtk$with_gtk)])
)

AM_COND_IF([HAVE_VALA],
	AC_MSG_NOTICE([	Vala bindings:          yes]),
	AC_MSG_NOTICE([	Vala bindings           no])
)

AS_IF([test "x$have_dumper" = "xyes"],
   AC_MSG_NOTICE([	Dumper:                 yes]),
   AC_MSG_NOTICE([	Dumper:                 no])
)

AS_IF([test "x$use_gcov" = "xyes"],
	AC_MSG_NOTICE([	Coverage reporting:     yes]),
	AC_MSG_NOTICE([	Coverage reporting:     no])
)
