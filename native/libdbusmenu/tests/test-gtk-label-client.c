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

#include <gtk/gtk.h>
#include <libdbusmenu-gtk/menu.h>

static GMainLoop * mainloop = NULL;
static gboolean passed = TRUE;
static guint death_timer = 0;

#if 0
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
		g_debug("\tFailed as ID %d is not equal to %d", layout->id, dbusmenu_menuitem_get_id(mi));
		return FALSE;
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
	for (i = 0; children != NULL && layout->submenu[i].id != 0; children = g_list_next(children), i++) {
		if (!verify_root_to_layout(DBUSMENU_MENUITEM(children->data), &layout->submenu[i])) {
			return FALSE;
		}
	}

	if (children == NULL && layout->submenu[i].id == 0) {
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
#endif

static gboolean
timer_func (gpointer data)
{
	passed = TRUE;
	g_main_loop_quit(mainloop);
	return FALSE;
}

#if 0
static gboolean layout_verify_timer (gpointer data);

static void
layout_updated (DbusmenuClient * client, gpointer data)
{
	g_debug("Layout Updated");
	g_timeout_add (250, layout_verify_timer, client);
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
		death_timer = g_timeout_add_seconds(10, timer_func, data);
	}

	layouton++;
	
	if (layouts[layouton].id == 0) {
		g_main_loop_quit(mainloop);
	}

	return FALSE;
}
#endif

int
main (int argc, char ** argv)
{
	gtk_init(&argc, &argv);

	g_debug("Client Initialized.  Waiting.");
	/* Make sure the server starts up and all that */
	g_usleep(500000);

	g_debug("Building Window");
	GtkWidget * window = gtk_window_new(GTK_WINDOW_TOPLEVEL);
	GtkWidget * menubar = gtk_menu_bar_new();
	GtkWidget * menuitem = gtk_menu_item_new_with_label("Test");
	gtk_menu_item_set_submenu(GTK_MENU_ITEM(menuitem), GTK_WIDGET(dbusmenu_gtkmenu_new ("glib.label.test", "/org/test")));
	gtk_widget_show(menuitem);
	gtk_menu_shell_append(GTK_MENU_SHELL(menubar), menuitem);
	gtk_widget_show(menubar);
	gtk_container_add(GTK_CONTAINER(window), menubar);
	gtk_window_set_title(GTK_WINDOW(window), "libdbusmenu-gtk test");
	gtk_widget_show(window);

	death_timer = g_timeout_add_seconds(60, timer_func, window);

	g_debug("Entering Mainloop");
	mainloop = g_main_loop_new(NULL, FALSE);
	g_main_loop_run(mainloop);

	if (passed) {
		g_debug("Quiting");
		return 0;
	} else {
		g_debug("Quiting as we're a failure");
		return 1;
	}
}
