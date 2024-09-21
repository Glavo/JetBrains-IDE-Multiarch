
libexec_PROGRAMS =

if WANT_TESTS
libexec_PROGRAMS += dbusmenu-testapp
endif

dbusmenu_testapp_SOURCES = \
	main.c

dbusmenu_testapp_CFLAGS = \
	-I $(srcdir)/../.. \
	$(DBUSMENUTESTS_CFLAGS) \
	$(DBUSMENUGLIB_CFLAGS) \
	-Wall -Werror

dbusmenu_testapp_LDADD = \
	$(builddir)/../../libdbusmenu-glib/libdbusmenu-glib.la \
	$(DBUSMENUTESTS_LIBS)
