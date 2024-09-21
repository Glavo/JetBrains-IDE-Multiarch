/*
A test for libdbusmenu to ensure its quality.

Copyright 2009 Canonical Ltd.

Authors:
    Ted Gould <ted@canonical.com>

This program is free software: you can redistribute it and/or modify it 
under the terms of the GNU General Public License version 3, as published 
by the Free Software Foundation.

This program is distributed in the hope that it will be useful, but 
WITHOUT ANY WARRANTY; without even the implied warranties of 
MERCHANTABILITY, SATISFACTORY QUALITY, or FITNESS FOR A PARTICULAR 
PURPOSE.  See the GNU General Public License for more details.

You should have received a copy of the GNU General Public License along 
with this program.  If not, see <http://www.gnu.org/licenses/>.
*/

#include <glib.h>
#include <gio/gio.h>
#include <gdk/gdkkeysyms.h>

#include <libdbusmenu-glib/menuitem.h>
#include <libdbusmenu-glib/server.h>
#include <libdbusmenu-gtk/menuitem.h>

GMainLoop * mainloop = NULL;
DbusmenuServer * server = NULL;

gboolean
timer_func (gpointer userdata)
{
	g_main_loop_quit(mainloop);
	return FALSE;
}

void
build_menu (void)
{
	DbusmenuMenuitem * item;

	DbusmenuMenuitem * root = dbusmenu_menuitem_new();

	item = dbusmenu_menuitem_new();
	dbusmenu_menuitem_property_set(item, DBUSMENU_MENUITEM_PROP_LABEL, "Control-L");
#if GTK_CHECK_VERSION(3,0,0)
	dbusmenu_menuitem_property_set_shortcut(item, GDK_KEY_l, GDK_CONTROL_MASK);
#else
	dbusmenu_menuitem_property_set_shortcut(item, GDK_l, GDK_CONTROL_MASK);
#endif
	dbusmenu_menuitem_child_append(root, item);
	g_object_unref(item);


	dbusmenu_server_set_root(server, root);
	g_object_unref(root);

	return;
}

static void
on_bus (GDBusConnection * connection, const gchar * name, gpointer user_data)
{
	server = dbusmenu_server_new("/org/test");
	build_menu();

	g_timeout_add_seconds(10, timer_func, NULL);

	return;
}

static void
name_lost (GDBusConnection * connection, const gchar * name, gpointer user_data)
{
	g_error("Unable to get name '%s' on DBus", name);
	g_main_loop_quit(mainloop);
	return;
}

int
main (int argc, char ** argv)
{
	g_bus_own_name(G_BUS_TYPE_SESSION,
	               "glib.label.test",
	               G_BUS_NAME_OWNER_FLAGS_NONE,
	               on_bus,
	               NULL,
	               name_lost,
	               NULL,
	               NULL);

	mainloop = g_main_loop_new(NULL, FALSE);
	g_main_loop_run(mainloop);

	g_debug("Quiting");

	return 0;
}

