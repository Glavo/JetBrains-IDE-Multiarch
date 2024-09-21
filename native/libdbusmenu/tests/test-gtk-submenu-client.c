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

static gboolean check_menu_content(GtkMenu * menu, char ** content)
{
	GList * child = gtk_container_get_children(GTK_CONTAINER(menu));
	char ** expected = content;
	for (; child != NULL; child = g_list_next(child), ++expected) {
		if (*expected == NULL) {
			g_warning("Too many gtk items");
			return FALSE;
		}
		const char * label = gtk_menu_item_get_label(GTK_MENU_ITEM(child->data));
		if (g_strcmp0(label, *expected) != 0) {
			g_warning("Expected '%s', got '%s'", *expected, label);
			return FALSE;
		}
	}
	if (*expected != NULL) {
		g_warning("Not enough gtk items");
		return FALSE;
	}
	return TRUE;
}

static void
abort_test(const char * message)
{
	if (message) {
		g_warning("%s", message);
	}
	passed = FALSE;
	g_main_loop_quit(mainloop);
}

static gboolean
timer_func (gpointer data)
{
	static char * root_content[] = { "Folder 1", "Folder 2", NULL };
	static char * folder1_content[] = { "1.1", "1.2", "1.3", NULL };
	static char * folder2_content[] = { "2.1", "2.2", "2.3", NULL };

	GtkMenuItem * root_item = GTK_MENU_ITEM(data);
	GtkMenu * menu = GTK_MENU(gtk_menu_item_get_submenu(root_item));

	/* Root */
	if (!check_menu_content(menu, root_content)) {
		abort_test("Checking root content failed");
		return FALSE;
	}

	/* Folder 1 */
	GList * child = gtk_container_get_children(GTK_CONTAINER(menu));
	GtkMenuItem * item = GTK_MENU_ITEM(child->data);
	GtkMenu * folder_menu = GTK_MENU(gtk_menu_item_get_submenu(item));
	if (!folder_menu) {
		abort_test("Folder 1 has no menu");
		return FALSE;
	}

	if (!check_menu_content(folder_menu, folder1_content)) {
		abort_test("Checking folder1 content failed");
		return FALSE;
	}

	/* Folder 2 */
	child = g_list_next(child);
	item = GTK_MENU_ITEM(child->data);
	folder_menu = GTK_MENU(gtk_menu_item_get_submenu(item));
	if (!folder_menu) {
		abort_test("Folder 2 has no menu");
		return FALSE;
	}

	if (!check_menu_content(folder_menu, folder2_content)) {
		abort_test("Checking folder2 content failed");
		return FALSE;
	}

	passed = TRUE;
	return FALSE;
}

gboolean
finished_func (gpointer user_data)
{
	g_main_loop_quit(mainloop);
	return FALSE;
}

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

	g_timeout_add_seconds(2, timer_func, menuitem);
	g_timeout_add_seconds(6, finished_func, menuitem);

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
