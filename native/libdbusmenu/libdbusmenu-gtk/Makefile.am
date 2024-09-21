
if USE_GTK3
VER=3
GTKGIR=Gtk-3.0
GTKVALA=gtk+-3.0
lib_LTLIBRARIES = libdbusmenu-gtk3.la
else
VER=
GTKGIR=Gtk-2.0
GTKVALA=gtk+-2.0
lib_LTLIBRARIES = libdbusmenu-gtk.la
endif

BUILT_SOURCES = 
CLEANFILES = 
DISTCLEANFILES = 
EXTRA_DIST = \
	dbusmenu-gtk-0.4.pc.in \
	dbusmenu-gtk3-0.4.pc.in

##############
# Enum Stuff
##############

include $(top_srcdir)/Makefile.am.enum

glib_enum_h = genericmenuitem-enum-types.h
glib_enum_c = genericmenuitem-enum-types.c
glib_enum_headers = $(srcdir)/genericmenuitem.h


#####################
# Include Directory
#####################

libdbusmenu_gtkincludedir=$(includedir)/libdbusmenu-gtk$(VER)-0.4/libdbusmenu-gtk/

libdbusmenu_gtkinclude_HEADERS = \
	dbusmenu-gtk.h \
	client.h \
	menu.h \
	menuitem.h \
	parser.h

libdbusmenu_gtk_la_SOURCES = \
	client.h \
	client.c \
	genericmenuitem.h \
	genericmenuitem.c \
	genericmenuitem-enum-types.h \
	genericmenuitem-enum-types.c \
	menu.h \
	menu.c \
	menuitem.h \
	menuitem.c \
	parser.h \
	parser.c

libdbusmenu_gtk_la_LDFLAGS = \
	$(COVERAGE_LDFLAGS) \
	-version-info $(LIBDBUSMENU_CURRENT):$(LIBDBUSMENU_REVISION):$(LIBDBUSMENU_AGE) \
	-no-undefined \
	-export-symbols-regex "^[^_].*"

libdbusmenu_gtk_la_CFLAGS = \
	$(DBUSMENUGTK_CFLAGS) \
	$(COVERAGE_CFLAGS) \
	-I$(top_srcdir) \
	-Wall -Werror -Wno-error=deprecated-declarations \
	-DG_LOG_DOMAIN="\"LIBDBUSMENU-GTK\""

libdbusmenu_gtk_la_LIBADD = \
	$(top_builddir)/libdbusmenu-glib/libdbusmenu-glib.la \
	$(DBUSMENUGTK_LIBS)

# We duplicate these here because Automake won't let us use $(VER) on the left hand side.
# Since we carefully use $(VER) in the right hand side above, we can assign the same values.
# Only one version of the library is every compiled at the same time, so it is safe to reuse
# the right hand sides like this.
libdbusmenu_gtk3includedir = $(libdbusmenu_gtkincludedir)
libdbusmenu_gtk3include_HEADERS = $(libdbusmenu_gtkinclude_HEADERS)
libdbusmenu_gtk3_la_SOURCES = $(libdbusmenu_gtk_la_SOURCES)
libdbusmenu_gtk3_la_LDFLAGS = $(libdbusmenu_gtk_la_LDFLAGS)
libdbusmenu_gtk3_la_CFLAGS = $(libdbusmenu_gtk_la_CFLAGS)
libdbusmenu_gtk3_la_LIBADD = $(libdbusmenu_gtk_la_LIBADD)

pkgconfig_DATA = dbusmenu-gtk$(VER)-0.4.pc
pkgconfigdir = $(libdir)/pkgconfig

#########################
# GObject Introsepction 
#########################

-include $(INTROSPECTION_MAKEFILE)
INTROSPECTION_GIRS = 

if INTROSPECTION_TEN
INTROSPECTION_SCANNER_ARGS = --add-include-path=$(srcdir) \
	--warn-all \
	--add-include-path=$(top_builddir)/libdbusmenu-glib \
	$(addprefix --c-include=libdbusmenu-gtk/, $(libdbusmenu_gtkinclude_HEADERS)) \
	--symbol-prefix=dbusmenu \
	--identifier-prefix=DbusmenuGtk
else
INTROSPECTION_SCANNER_ARGS = --add-include-path=$(srcdir) \
	--warn-all \
	--add-include-path=$(top_builddir)/libdbusmenu-glib \
	$(addprefix --c-include=libdbusmenu-gtk/, $(libdbusmenu_gtkinclude_HEADERS))
endif

INTROSPECTION_COMPILER_ARGS = --includedir=$(builddir) --includedir=$(top_builddir)/libdbusmenu-glib

if HAVE_INTROSPECTION

introspection_sources = $(filter-out genericmenuitem%, $(libdbusmenu_gtkinclude_HEADERS) $(libdbusmenu_gtk_la_SOURCES))

DbusmenuGtk$(VER)-0.4.gir: libdbusmenu-gtk$(VER).la
DbusmenuGtk_0_4_gir_INCLUDES = \
	GObject-2.0 \
	$(GTKGIR) \
	Dbusmenu-0.4 
DbusmenuGtk_0_4_gir_CFLAGS = \
	$(DBUSMENUGTK_CFLAGS) \
	-I$(top_srcdir)
DbusmenuGtk_0_4_gir_LIBS = libdbusmenu-gtk$(VER).la \
	$(top_builddir)/libdbusmenu-glib/libdbusmenu-glib.la
DbusmenuGtk_0_4_gir_FILES = $(addprefix $(srcdir)/, $(introspection_sources))
DbusmenuGtk_0_4_gir_NAMESPACE = DbusmenuGtk$(VER)
DbusmenuGtk_0_4_gir_EXPORT_PACKAGES = dbusmenu-gtk$(VER)-0.4

# We duplicate these for the same reason as libdbusmenu_gtk3includedir above
DbusmenuGtk3_0_4_gir_INCLUDES = $(DbusmenuGtk_0_4_gir_INCLUDES)
DbusmenuGtk3_0_4_gir_CFLAGS = $(DbusmenuGtk_0_4_gir_CFLAGS)
DbusmenuGtk3_0_4_gir_LDFLAGS = $(DbusmenuGtk_0_4_gir_LDFLAGS)
DbusmenuGtk3_0_4_gir_LIBS = $(DbusmenuGtk_0_4_gir_LIBS)
DbusmenuGtk3_0_4_gir_FILES = $(DbusmenuGtk_0_4_gir_FILES)
DbusmenuGtk3_0_4_gir_NAMESPACE = $(DbusmenuGtk_0_4_gir_NAMESPACE)
DbusmenuGtk3_0_4_gir_EXPORT_PACKAGES = $(DbusmenuGtk_0_4_gir_EXPORT_PACKAGES)

INTROSPECTION_GIRS += DbusmenuGtk$(VER)-0.4.gir

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
vapi_DATA = DbusmenuGtk$(VER)-0.4.vapi

DbusmenuGtk$(VER)-0.4.vapi: DbusmenuGtk$(VER)-0.4.tmp.gir Makefile.am
	$(VALA_API_GEN) --library=DbusmenuGtk$(VER)-0.4 \
		--pkg gdk-pixbuf-2.0 \
		--pkg $(GTKVALA) \
		--pkg atk \
		--pkg Dbusmenu-0.4 \
		--vapidir=$(top_builddir)/libdbusmenu-glib \
		$<

DbusmenuGtk$(VER)-0.4.tmp.gir: DbusmenuGtk$(VER)-0.4.gir
	$(SED) \
		-e "s|GdkPixbuf.Pixbuf|Gdk.Pixbuf|g" \
		-e "s|Atk.ImplementorIface|Atk.Implementor|g" \
		$< > $@

CLEANFILES += $(vapi_DATA) DbusmenuGtk$(VER)-0.4.tmp.gir

endif
endif

