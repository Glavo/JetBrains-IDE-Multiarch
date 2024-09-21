/*
Test to check the json-loader and dbusmenu-dumper

Copyright 2010 Canonical Ltd.

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

#include "callgrind.h"
#include <libdbusmenu-glib/server.h>
#include <libdbusmenu-glib/menuitem.h>

#include "json-loader.h"

static GMainLoop * mainloop = NULL;

static void
root_activate (void)
{
	g_debug("Dumping callgrind data");
	CALLGRIND_DUMP_STATS_AT("exported");
	CALLGRIND_STOP_INSTRUMENTATION;
	g_timeout_add(500, (GSourceFunc)g_main_loop_quit, mainloop);
}

static void
on_bus (GDBusConnection * connection, const gchar * name, gpointer user_data)
{
	gchar ** argv = (gchar **)user_data;

	DbusmenuServer * server = dbusmenu_server_new("/org/test");

	DbusmenuMenuitem * root = dbusmenu_json_build_from_file(argv[1]);
	if (root == NULL) {
		g_warning("Unable to build root");
		g_main_loop_quit(mainloop);
		return;
	}

	g_signal_connect(G_OBJECT(root), DBUSMENU_MENUITEM_SIGNAL_ITEM_ACTIVATED, G_CALLBACK(root_activate), NULL);

	g_debug("Starting Callgrind");
	CALLGRIND_START_INSTRUMENTATION;
	CALLGRIND_ZERO_STATS;
	CALLGRIND_TOGGLE_COLLECT;
	dbusmenu_server_set_root(server, root);

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
	               "org.dbusmenu.test",
	               G_BUS_NAME_OWNER_FLAGS_NONE,
	               on_bus,
	               NULL,
	               name_lost,
	               argv,
	               NULL);

	mainloop = g_main_loop_new(NULL, FALSE);
	g_main_loop_run(mainloop);

	g_debug("Quiting");

	return 0;
}
