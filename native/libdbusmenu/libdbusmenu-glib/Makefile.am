
BUILT_SOURCES =
CLEANFILES =
DISTCLEANFILES =
EXTRA_DIST = \
	clean-namespaces.xslt \
	dbusmenu-glib-0.4.pc.in \
	dbus-menu.xml \
	dbus-menu-clean.xml \
	client-marshal.list \
	menuitem-marshal.list \
	server-marshal.list

include $(top_srcdir)/Makefile.am.enum

lib_LTLIBRARIES = \
	libdbusmenu-glib.la

libdbusmenu_glibincludedir=$(includedir)/libdbusmenu-glib-0.4/libdbusmenu-glib/


EXPORTED_OBJECTS = \
	menuitem.h \
	menuitem-proxy.h \
	server.h \
	client.h

libdbusmenu_glibinclude_HEADERS = \
	$(EXPORTED_OBJECTS) \
	enum-types.h \
	dbusmenu-glib.h \
	types.h

libdbusmenu_glib_la_SOURCES = \
	dbus-menu-clean.xml.h \
	dbus-menu-clean.xml.c \
	defaults.h \
	defaults.c \
	enum-types.h \
	enum-types.c \
	menuitem.h \
	menuitem.c \
	menuitem-marshal.h \
	menuitem-marshal.c \
	menuitem-private.h \
	menuitem-proxy.h \
	menuitem-proxy.c \
	server.h \
	server.c \
	server-marshal.h \
	server-marshal.c \
	client-marshal.h \
	client-marshal.c \
	client-menuitem.h \
	client-menuitem.c \
	client-private.h \
	client.h \
	client.c

libdbusmenu_glib_la_LDFLAGS = \
	$(COVERAGE_LDFLAGS) \
	-version-info $(LIBDBUSMENU_CURRENT):$(LIBDBUSMENU_REVISION):$(LIBDBUSMENU_AGE) \
	-no-undefined \
	-export-symbols-regex "^[^_].*"

libdbusmenu_glib_la_CFLAGS = \
	$(DBUSMENUGLIB_CFLAGS) \
	$(COVERAGE_CFLAGS) \
	-Wall -Werror -Wno-error=deprecated-declarations \
	-DG_LOG_DOMAIN="\"LIBDBUSMENU-GLIB\""

libdbusmenu_glib_la_LIBADD = \
	$(DBUSMENUGLIB_LIBS)

pkgconfig_DATA = dbusmenu-glib-0.4.pc
pkgconfigdir = $(libdir)/pkgconfig

ENUMHEADERS = \
	menuitem.h \
	menuitem-proxy.h \
	server.h \
	client.h \
	types.h

glib_enum_h = enum-types.h
glib_enum_c = enum-types.c
glib_enum_headers = $(addprefix $(srcdir)/, $(ENUMHEADERS))

DISTCLEANFILES += \
	enum-types.c \
	enum-types.h

%.xml.h: %.xml
	echo "extern const char * $(subst -,_,$(subst .,_,$(basename $(notdir $@))));" > $@

%.xml.c: %.xml
	echo "const char * $(subst -,_,$(subst .,_,$(basename $(notdir $@)))) = " > $@
	sed -e "s:\":\\\\\":g" -e s:^:\": -e s:\$$:\\\\n\": $< >> $@
	echo ";" >> $@

dbus-menu-clean.xml: dbus-menu.xml
	$(XSLT_PROC) $(srcdir)/clean-namespaces.xslt $< > $@ || (rm -f $@ && /bin/false)

CLEANFILES += dbus-menu-clean.xml

BUILT_SOURCES += \
	dbus-menu-clean.xml.c \
	dbus-menu-clean.xml.h \
	client-marshal.h \
	client-marshal.c \
	menuitem-marshal.h \
	menuitem-marshal.c \
	server-marshal.h \
	server-marshal.c

CLEANFILES += $(BUILT_SOURCES)

client-marshal.h: $(srcdir)/client-marshal.list
	glib-genmarshal --header \
		--prefix=_dbusmenu_client_marshal $(srcdir)/client-marshal.list \
		> client-marshal.h

client-marshal.c: $(srcdir)/client-marshal.list
	glib-genmarshal --body \
		--prefix=_dbusmenu_client_marshal $(srcdir)/client-marshal.list \
		> client-marshal.c

server-marshal.h: $(srcdir)/server-marshal.list
	glib-genmarshal --header \
		--prefix=_dbusmenu_server_marshal $(srcdir)/server-marshal.list \
		> server-marshal.h

server-marshal.c: $(srcdir)/server-marshal.list
	glib-genmarshal --body \
		--prefix=_dbusmenu_server_marshal $(srcdir)/server-marshal.list \
		> server-marshal.c

menuitem-marshal.h: $(srcdir)/menuitem-marshal.list
	glib-genmarshal --header \
		--prefix=_dbusmenu_menuitem_marshal $(srcdir)/menuitem-marshal.list \
		> menuitem-marshal.h

menuitem-marshal.c: $(srcdir)/menuitem-marshal.list
	glib-genmarshal --body \
		--prefix=_dbusmenu_menuitem_marshal $(srcdir)/menuitem-marshal.list \
		> menuitem-marshal.c

#########################
# GObject Introsepction 
#########################

-include $(INTROSPECTION_MAKEFILE)
INTROSPECTION_GIRS = 

if INTROSPECTION_TEN
INTROSPECTION_SCANNER_ARGS = --add-include-path=$(srcdir) \
	--warn-all \
        --add-include-path=$(srcdir) \
        $(addprefix --c-include=libdbusmenu-glib/, $(libdbusmenu_glibinclude_HEADERS)) \
        --symbol-prefix=dbusmenu \
        --identifier-prefix=Dbusmenu
else
INTROSPECTION_SCANNER_ARGS = --add-include-path=$(srcdir) \
	--warn-all \
        --add-include-path=$(srcdir) \
        $(addprefix --c-include=libdbusmenu-glib/, $(libdbusmenu_glibinclude_HEADERS))
endif

INTROSPECTION_COMPILER_ARGS = --includedir=$(builddir)

if HAVE_INTROSPECTION

introspection_objects = \
	$(addprefix $(srcdir)/, $(EXPORTED_OBJECTS)) \
	$(builddir)/enum-types.h

introspection_sources = \
	$(addprefix $(srcdir)/, $(EXPORTED_OBJECTS)) \
	$(srcdir)/dbusmenu-glib.h \
	$(srcdir)/types.h \
	$(introspection_objects:.h=.c)

Dbusmenu-0.4.gir: libdbusmenu-glib.la
Dbusmenu_0_4_gir_INCLUDES = \
	GObject-2.0
Dbusmenu_0_4_gir_CFLAGS = \
	$(DBUSMENUGLIB_CFLAGS) \
	-I$(top_srcdir)
Dbusmenu_0_4_gir_LIBS = libdbusmenu-glib.la
Dbusmenu_0_4_gir_FILES = $(introspection_sources)
Dbusmenu_0_4_gir_NAMESPACE = Dbusmenu
Dbusmenu_0_4_gir_VERSION = 0.4
Dbusmenu_0_4_gir_EXPORT_PACKAGES = dbusmenu-glib-0.4
Dbusmenu_0_4_gir_SCANNER_FLAGS = $(INTROSPECTION_SCANNER_ARGS)

INTROSPECTION_GIRS += Dbusmenu-0.4.gir

girdir = $(datadir)/gir-1.0
gir_DATA = $(INTROSPECTION_GIRS)

typelibdir = $(INTROSPECTION_TYPELIBDIR)
typelib_DATA = $(INTROSPECTION_GIRS:.gir=.typelib)

CLEANFILES += $(gir_DATA) $(typelib_DATA)

endif

#########################
# VAPI Files
#########################

if HAVE_VALA
if HAVE_INTROSPECTION

vapidir = $(datadir)/vala/vapi
vapi_DATA = Dbusmenu-0.4.vapi

Dbusmenu-0.4.vapi: Dbusmenu-0.4.gir
	$(VALA_API_GEN) --library=Dbusmenu-0.4 $<

CLEANFILES += $(vapi_DATA)

endif
endif

