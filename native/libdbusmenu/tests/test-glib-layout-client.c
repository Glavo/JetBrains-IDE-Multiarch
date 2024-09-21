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

#include "test-glib-layout.h"

static guint layouton = 0;
static GMainLoop * mainloop = NULL;
static gboolean passed = TRUE;

static gboolean
verify_root_to_layout(DbusmenuMenuitem * mi, layout_t * layout)
{
	g_debug("Verifying ID: %d", layout->id);

	if (layout->id != dbusmenu_menuitem_get_id(mi)) {
		if (!(dbusmenu_menuitem_get_root(mi) && dbusmenu_menuitem_get_id(mi) == 0)) {
			g_debug("Failed as ID %d is not equal to %d", layout->id, dbusmenu_menuitem_get_id(mi));
			return FALSE;
		}
	}

	GList * children = dbusmenu_menuitem_get_children(mi);

	if (children == NULL && layout->submenu == NULL) {
		return TRUE;
	}
	if (children == NULL || layout->submenu == NULL) {
		if (children == NULL) {
			g_debug("Failed as there are no children but we have submenus");
		} else {
			g_debug("Failed as we have children but no submenu");
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
		return TRUE;
	}

	if (children != NULL) {
		g_debug("Failed as there are still children but no submenus.  (ID: %d)", layout->id);
	} else {
		g_debug("Failed as there are still submenus but no children.  (ID: %d)", layout->id);
	}
	return FALSE;
}

static void
layout_updated (DbusmenuClient * client, gpointer data)
{
	g_debug("Layout Updated");

	DbusmenuMenuitem * menuroot = dbusmenu_client_get_root(client);
	if (menuroot == NULL) {
		g_debug("Root NULL, waiting");
		return;
	}

	layout_t * layout = &layouts[layouton];
	
	if (!verify_root_to_layout(menuroot, layout)) {
		g_debug("Failed layout: %d", layouton);
		passed = FALSE;
	}

	layouton++;

	if (layouts[layouton].id == -1) {
		g_main_loop_quit(mainloop);
	}

	return;
}

static gboolean
timer_func (gpointer data)
{
	g_debug("Death timer.  Oops.  Got to: %d", layouton);
	passed = FALSE;
	g_main_loop_quit(mainloop);
	return FALSE;
}

int
main (int argc, char ** argv)
{
	DbusmenuClient * client = dbusmenu_client_new("org.dbusmenu.test", "/org/test");
	g_signal_connect(G_OBJECT(client), DBUSMENU_CLIENT_SIGNAL_LAYOUT_UPDATED, G_CALLBACK(layout_updated), NULL);

	g_timeout_add_seconds(60, timer_func, client);

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
