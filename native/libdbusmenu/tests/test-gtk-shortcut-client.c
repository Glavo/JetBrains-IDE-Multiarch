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
#include <libdbusmenu-gtk/client.h>

static GMainLoop * mainloop = NULL;
static gboolean passed = TRUE;
static guint death_timer = 0;

static gboolean
timer_func (gpointer data)
{
	passed = TRUE;
	g_main_loop_quit(mainloop);
	return FALSE;
}

int
main (int argc, char ** argv)
{
	gtk_init(&argc, &argv);

	g_debug("Building Window");
	GtkWidget * window = gtk_window_new(GTK_WINDOW_TOPLEVEL);
	GtkWidget * menubar = gtk_menu_bar_new();
	GtkWidget * menuitem = gtk_menu_item_new_with_label("Test");

	DbusmenuGtkMenu * dmenu = dbusmenu_gtkmenu_new ("glib.label.test", "/org/test");
	DbusmenuGtkClient * dclient = dbusmenu_gtkmenu_get_client(dmenu);

	GtkAccelGroup * agroup = gtk_accel_group_new();
	dbusmenu_gtkclient_set_accel_group(dclient, agroup);

	gtk_menu_item_set_submenu(GTK_MENU_ITEM(menuitem), GTK_WIDGET(dmenu));
	gtk_widget_show(menuitem);
	gtk_menu_shell_append(GTK_MENU_SHELL(menubar), menuitem);
	gtk_widget_show(menubar);
	gtk_container_add(GTK_CONTAINER(window), menubar);
	gtk_window_set_title(GTK_WINDOW(window), "libdbusmenu-gtk test");
	gtk_window_add_accel_group(GTK_WINDOW(window), agroup);
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
