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

#define NUMBER_TESTS    5
#define NUMBER_ENTRIES  5

guint ordering [NUMBER_TESTS][NUMBER_ENTRIES] = {
	{0, 1, 2, 3, 4},
	{1, 2, 3, 4, 0},
	{3, 1, 4, 2, 0},
	{4, 3, 2, 1, 0},
	{0, 1, 2, 3, 4}
};

gchar * names [NUMBER_ENTRIES] = {
	"One", "Two", "Three", "Four", "Five"
};

DbusmenuMenuitem * entries[NUMBER_ENTRIES] = {0};
DbusmenuMenuitem * root = NULL;

gint test = 0;

static DbusmenuServer * server = NULL;
static GMainLoop * mainloop = NULL;

static gboolean
timer_func (gpointer data)
{
	if (test == NUMBER_TESTS) {
		g_main_loop_quit(mainloop);
		return FALSE;
	}

	g_debug("Testing pattern %d", test);

	int i;
	for (i = 0; i < NUMBER_ENTRIES; i++) {
		g_debug("Putting entry '%d' at position '%d'", i, ordering[test][i]);
		dbusmenu_menuitem_child_reorder(root, entries[i], ordering[test][i]);
		dbusmenu_menuitem_property_set(entries[i], "label", names[ordering[test][i]]);
	}

	test++;
	return TRUE;
}

static void
on_bus (GDBusConnection * connection, const gchar * name, gpointer user_data)
{
	server = dbusmenu_server_new("/org/test");
	root = dbusmenu_menuitem_new();
	dbusmenu_server_set_root(server, root);

	int i;
	for (i = 0; i < NUMBER_ENTRIES; i++) {
		entries[i] = dbusmenu_menuitem_new();
		dbusmenu_menuitem_child_append(root, entries[i]);
	}

	timer_func(NULL);
	g_timeout_add_seconds(5, timer_func, NULL);

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

