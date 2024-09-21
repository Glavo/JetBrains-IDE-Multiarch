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

#include <libdbusmenu-glib/client.h>
#include <libdbusmenu-glib/menuitem.h>

#include "test-glib-properties.h"

#define DEATH_TIME 60

static guint layouton = 0;
static GMainLoop * mainloop = NULL;
static gboolean passed = TRUE;
static guint death_timer = 0;

static gboolean
verify_props (DbusmenuMenuitem * mi, gchar ** properties)
{
	if (properties == NULL) {
		return TRUE;
	}

	/* Verify they're all there and correct */
	guint i;
	for (i = 0; properties[i] != NULL; i += 2) {
		const gchar * value = dbusmenu_menuitem_property_get(mi, properties[i]);
		if (g_strcmp0(value, properties[i + 1])) {
			g_debug("\tFailed as property '%s' should be '%s' and is '%s'", properties[i], properties[i+1], value);
			return FALSE;
		}
	}

	/* Verify that we don't have any extras */
	// GList * props = dbusmenu_menuitem_properties_list(mi);

	return TRUE;
}

static gboolean
verify_root_to_layout(DbusmenuMenuitem * mi, proplayout_t * layout)
{
	g_debug("Verifying ID: %d", layout->id);

	if (layout->id != dbusmenu_menuitem_get_id(mi)) {
		if (!dbusmenu_menuitem_get_root(mi)) {
			g_debug("\tFailed as ID %d is not equal to %d", layout->id, dbusmenu_menuitem_get_id(mi));
			return FALSE;
		}
	}

	if (!verify_props(mi, layout->properties)) {
		g_debug("\tFailed as unable to verify properties.");
		return FALSE;
	}

	GList * children = dbusmenu_menuitem_get_children(mi);

	if (children == NULL && layout->submenu == NULL) {
		g_debug("\tPassed: %d", layout->id);
		return TRUE;
	}
	if (children == NULL || layout->submenu == NULL) {
		if (children == NULL) {
			g_debug("\tFailed as there are no children but we have submenus");
		} else {
			g_debug("\tFailed as we have children but no submenu");
		}
		return FALSE;
	}

	guint i = 0;
	for (i = 0; children != NULL && layout->submenu[i].id != -1; children = g_list_next(children), i++) {
		if (!verify_root_to_layout(DBUSMENU_MENUITEM(children->data), &layout->submenu[i])) {
			return FALSE;
		}
	}

	if (children == NULL && layout->submenu[i].id == -1) {
		g_debug("\tPassed: %d", layout->id);
		return TRUE;
	}

	if (children != NULL) {
		g_debug("\tFailed as there are still children but no submenus.  (ID: %d)", layout->id);
	} else {
		g_debug("\tFailed as there are still submenus but no children.  (ID: %d)", layout->id);
	}
	return FALSE;
}

static gboolean
timer_func (gpointer data)
{
	g_debug("Death timer.  Oops.  Got to: %d", layouton);
	passed = FALSE;
	g_main_loop_quit(mainloop);
	return FALSE;
}

static gboolean layout_verify_timer (gpointer data);

static void
layout_updated (DbusmenuClient * client, gpointer data)
{
	g_debug("Layout Updated");
	g_timeout_add (500, layout_verify_timer, client);
	return;
}

static gboolean
layout_verify_timer (gpointer data)
{
	DbusmenuMenuitem * menuroot = dbusmenu_client_get_root(DBUSMENU_CLIENT(data));
	proplayout_t * layout = &layouts[layouton];
	
	if (!verify_root_to_layout(menuroot, layout)) {
		g_debug("FAILED LAYOUT: %d", layouton);
		passed = FALSE;
	} else {
		/* Extend our death */
		g_source_remove(death_timer);
		death_timer = g_timeout_add_seconds(DEATH_TIME, timer_func, data);
	}

	layouton++;
	
	if (layouts[layouton].id == -1) {
		g_main_loop_quit(mainloop);
	}

	return FALSE;
}

int
main (int argc, char ** argv)
{
	/* Make sure the server starts up and all that */
	g_usleep(500000);

	DbusmenuClient * client = dbusmenu_client_new(":1.0", "/org/test");
	g_signal_connect(G_OBJECT(client), DBUSMENU_CLIENT_SIGNAL_LAYOUT_UPDATED, G_CALLBACK(layout_updated), NULL);

	death_timer = g_timeout_add_seconds(DEATH_TIME, timer_func, client);

	mainloop = g_main_loop_new(NULL, FALSE);
	g_main_loop_run(mainloop);

	g_object_unref(G_OBJECT(client));

	if (passed) {
		g_debug("Quiting");
		return 0;
	} else {
		g_debug("Quiting as we're a failure");
		return 1;
	}
}
