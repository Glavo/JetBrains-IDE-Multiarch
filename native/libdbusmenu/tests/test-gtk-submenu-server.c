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

#include <libdbusmenu-glib/menuitem.h>
#include <libdbusmenu-glib/server.h>

static GMainLoop *mainloop = NULL;

static gboolean
timer_func (gpointer data)
{
  g_main_loop_quit (mainloop);

  return FALSE;
}

static gboolean
show_item (gpointer pmi)
{
	DbusmenuMenuitem * mi = DBUSMENU_MENUITEM(pmi);
	g_debug("Showing item");

	dbusmenu_menuitem_show_to_user(mi, 0);

	return FALSE;
}

DbusmenuMenuitem *
add_item(DbusmenuMenuitem * parent, const char * label)
{
	DbusmenuMenuitem * item = dbusmenu_menuitem_new();
	dbusmenu_menuitem_property_set(item, "label", label);
	dbusmenu_menuitem_child_append(parent, item);
	return item;
}

static void
on_bus (GDBusConnection * connection, const gchar * name, gpointer user_data)
{
	DbusmenuServer * server = dbusmenu_server_new("/org/test");
	DbusmenuMenuitem * root = dbusmenu_menuitem_new();
	dbusmenu_server_set_root(server, root);

	DbusmenuMenuitem * item;
	item = add_item(root, "Folder 1");
	dbusmenu_menuitem_property_set(item, "disposition", "alert");
	add_item(item, "1.1");
	add_item(item, "1.2");
	add_item(item, "1.3");

	g_timeout_add_seconds(2, show_item, item);

	item = add_item(root, "Folder 2");
	add_item(item, "2.1");
	add_item(item, "2.2");
	add_item(item, "2.3");

	g_timeout_add_seconds(4, show_item, item);

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

    g_timeout_add_seconds(6, timer_func, NULL);

	mainloop = g_main_loop_new(NULL, FALSE);
	g_main_loop_run(mainloop);

	g_debug("Quiting");

	return 0;
}

