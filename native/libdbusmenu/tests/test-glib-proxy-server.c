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

#include "test-glib-proxy.h"

static void
set_props (DbusmenuMenuitem * mi, gchar ** props)
{
	if (props == NULL) return;

	guint i;
	for (i = 0; props[i] != NULL; i += 2) {
		dbusmenu_menuitem_property_set(mi, props[i], props[i+1]);
	}

	return;
}

static DbusmenuMenuitem *
layout2menuitem (proplayout_t * layout)
{
	if (layout == NULL || layout->id == -1) return NULL;

	DbusmenuMenuitem * local = dbusmenu_menuitem_new();
	set_props(local, layout->properties);
	
	if (layout->submenu != NULL) {
		guint count;
		for (count = 0; layout->submenu[count].id != -1; count++) {
			DbusmenuMenuitem * child = layout2menuitem(&layout->submenu[count]);
			if (child != NULL) {
				dbusmenu_menuitem_child_append(local, child);
			}
		}
	}

	/* g_debug("Layout to menu return: 0x%X", (unsigned int)local); */
	return local;
}

static guint layouton = 0;
static DbusmenuServer * server = NULL;
static GMainLoop * mainloop = NULL;
static guint death_timer = 0;

static gboolean
timer_func (gpointer data)
{
	g_debug("Death timer.  Oops.  Got to: %d", layouton);
	g_main_loop_quit(mainloop);
	return FALSE;
}

static void
layout_change (DbusmenuMenuitem * oldroot, guint timestamp, gpointer data)
{
	if (layouts[layouton].id == -1) {
		g_main_loop_quit(mainloop);
		return;
	}
	g_debug("Updating to Layout %d", layouton);

	DbusmenuMenuitem * mi = layout2menuitem(&layouts[layouton]);
	g_signal_connect(G_OBJECT(mi), DBUSMENU_MENUITEM_SIGNAL_ITEM_ACTIVATED, G_CALLBACK(layout_change), NULL);
	dbusmenu_menuitem_property_set_int(mi, LAYOUT_ON, layouton);
	dbusmenu_server_set_root(server, mi);
	g_object_unref(G_OBJECT(mi));
	layouton++;

	/* Extend our death */
	if (death_timer != 0) {
		g_source_remove(death_timer);
	}
	death_timer = g_timeout_add_seconds(60, timer_func, data);

	return;
}

static void
on_bus (GDBusConnection * connection, const gchar * name, gpointer user_data)
{
	server = dbusmenu_server_new("/org/test");
	layout_change(NULL, 0, NULL);

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
	               "test.proxy.server",
	               G_BUS_NAME_OWNER_FLAGS_NONE,
	               on_bus,
	               NULL,
	               name_lost,
	               NULL,
	               NULL);

	mainloop = g_main_loop_new(NULL, FALSE);
	g_main_loop_run(mainloop);

	g_object_unref(G_OBJECT(server));
	g_debug("Quiting");

	return 0;
}

