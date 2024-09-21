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

#include <json-glib/json-glib.h>
#include "json-loader.h"

static JsonArray * root_array = NULL;
static guint layouton = 0;
static DbusmenuServer * server = NULL;
static GMainLoop * mainloop = NULL;

static gboolean
timer_func (gpointer data)
{
	if (layouton == json_array_get_length(root_array)) {
		g_debug("Completed %d layouts", layouton);
		g_main_loop_quit(mainloop);
		return FALSE;
	}
	g_debug("Updating to Layout %d", layouton);

	dbusmenu_server_set_root(server, dbusmenu_json_build_from_node(json_array_get_element(root_array, layouton)));
	layouton++;

	return TRUE;
}

static void
on_bus (GDBusConnection * connection, const gchar * name, gpointer user_data)
{
	server = dbusmenu_server_new("/org/test");

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
	JsonParser * parser = json_parser_new();
	GError * error = NULL;
	if (!json_parser_load_from_file(parser, argv[1], &error)) {
		g_debug("Failed parsing file %s because: %s", argv[1], error->message);
		return 1;
	}
	JsonNode * root_node = json_parser_get_root(parser);
	if (JSON_NODE_TYPE(root_node) != JSON_NODE_ARRAY) {
		g_debug("Root node is not an array, fail.  It's an: %s", json_node_type_name(root_node));
		return 1;
	}

	root_array = json_node_get_array(root_node);
	g_debug("%d layouts in test description '%s'", json_array_get_length(root_array), argv[1]);


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

