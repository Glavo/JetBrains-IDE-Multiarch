
SUBDIRS = testapp

libexec_PROGRAMS =

if WANT_DBUSMENUDUMPER
libexec_PROGRAMS += dbusmenu-dumper
endif

libexec_SCRIPTS = dbusmenu-bench

dbusmenu_dumper_SOURCES = \
	dbusmenu-dumper.c

dbusmenu_dumper_CFLAGS = \
	-I $(srcdir)/.. \
	$(DBUSMENUGLIB_CFLAGS) \
	$(DBUSMENUDUMPER_CFLAGS) \
	-Wall -Werror

dbusmenu_dumper_LDADD = \
	../libdbusmenu-glib/libdbusmenu-glib.la \
	$(DBUSMENUGLIB_LIBS) \
	$(DBUSMENUDUMPER_LIBS)

doc_DATA = README.dbusmenu-bench

EXTRA_DIST = \
	$(doc_DATA) \
	dbusmenu-bench
