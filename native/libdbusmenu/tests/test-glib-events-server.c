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

#include <libdbusmenu-glib/server.h>
#include <libdbusmenu-glib/menuitem.h>

static DbusmenuServer * server = NULL;
static GMainLoop * mainloop = NULL;
static gboolean passed = TRUE;

static void
handle_event (void) {
	g_debug("Handle event");
	g_main_loop_quit(mainloop);
	return;
}

static gboolean
timer_func (gpointer data)
{
	passed = FALSE;
	g_debug("Never got a signal");
	g_main_loop_quit(mainloop);
	return FALSE;
}

static void
on_bus (GDBusConnection * connection, const gchar * name, gpointer user_data)
{
	server = dbusmenu_server_new("/org/test");
	DbusmenuMenuitem * menuitem = dbusmenu_menuitem_new();
	dbusmenu_server_set_root(server, menuitem);

	g_signal_connect(G_OBJECT(menuitem), DBUSMENU_MENUITEM_SIGNAL_ITEM_ACTIVATED, G_CALLBACK(handle_event), NULL);

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
	               NULL,
	               NULL);

	g_timeout_add_seconds(3, timer_func, NULL);

	mainloop = g_main_loop_new(NULL, FALSE);
	g_main_loop_run(mainloop);

	if (passed) {
		int i;

		for (i = 0; i < 5; i++) {
			g_debug("Ignoring signals: %d", i);
			g_usleep(1000 * 1000);
		}
	}

	if (passed) {
		g_debug("Test Passed");
		return 0;
	} else {
		g_debug("Test Failed");
		return 1;
	}
}
