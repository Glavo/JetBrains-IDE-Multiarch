/*
Testing for the various objects just by themselves.

Copyright 2011 Canonical Ltd.

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

#include <libdbusmenu-glib/menuitem-private.h>
#include <libdbusmenu-gtk/parser.h>

/* Just makes sure we can connect here people */
static void
test_parser_runs (void)
{
	GtkWidget * gmi = gtk_menu_item_new_with_label("Test Item");
	g_assert(gmi != NULL);
	g_object_ref_sink(gmi);

	DbusmenuMenuitem * mi = dbusmenu_gtk_parse_menu_structure(gmi);
	g_assert(mi != NULL);

	g_object_unref(gmi);
	g_object_unref(mi);

	return;
}

const gchar * test_parser_children_builder =
"<?xml version=\"1.0\"?>"
"<interface>"
"<requires lib=\"gtk+\" version=\"2.16\"/>"
/* Start menu bar */
"<object class=\"GtkMenuBar\" id=\"menubar\"><property name=\"visible\">True</property>"
/* Child 1 */
"<child><object class=\"GtkMenuItem\" id=\"child_one\"><property name=\"visible\">True</property><property name=\"label\">Child One</property></object></child>"
/* Child 2 */
"<child><object class=\"GtkMenuItem\" id=\"child_two\"><property name=\"visible\">True</property><property name=\"label\">Child Two</property></object></child>"
/* Child 3 */
"<child><object class=\"GtkMenuItem\" id=\"child_three\"><property name=\"visible\">True</property><property name=\"label\">Child Three</property></object></child>"
/* Child 4 */
"<child><object class=\"GtkMenuItem\" id=\"child_four\"><property name=\"visible\">True</property><property name=\"label\">Child Four</property></object></child>"
/* Stop menubar */
"</object>"
"</interface>";

/* Checks the log level to let warnings not stop the program */
static gboolean
test_parser_children_log_handler (const gchar * domain, GLogLevelFlags level, const gchar * message, gpointer user_data)
{
	if (level & (G_LOG_LEVEL_WARNING | G_LOG_LEVEL_MESSAGE | G_LOG_LEVEL_INFO | G_LOG_LEVEL_DEBUG)) {
		return FALSE;
	}

	return TRUE;
}

/* Ensure the parser can find children */
static void
test_parser_children (void) {
	/* Hide GTK errors */
	g_test_log_set_fatal_handler(test_parser_children_log_handler, NULL);

	GtkBuilder * builder = gtk_builder_new();
	g_assert(builder != NULL);

	GError * error = NULL;
	gtk_builder_add_from_string(builder, test_parser_children_builder, -1, &error);
	if (error != NULL) {
		g_error("Unable to parse UI definition: %s", error->message);
		g_error_free(error);
		error = NULL;
	}

	GtkWidget * menu = GTK_WIDGET(gtk_builder_get_object(builder, "menubar"));
	g_assert(menu != NULL);

	DbusmenuMenuitem * mi = dbusmenu_gtk_parse_menu_structure(menu);
	g_assert(mi != NULL);

/*
	GPtrArray * xmlarray = g_ptr_array_new();
	dbusmenu_menuitem_buildxml(mi, xmlarray);
	g_debug("XML: %s", g_strjoinv("", (gchar **)xmlarray->pdata));
*/

	GList * children = dbusmenu_menuitem_get_children(mi);
	g_assert(children != NULL);

	g_assert(g_list_length(children) == 4);

	g_object_unref(mi);
	g_object_unref(menu);

	return;
}

/* Build the test suite */
static void
test_gtk_parser_suite (void)
{
	g_test_add_func ("/dbusmenu/gtk/parser/base",          test_parser_runs);
	g_test_add_func ("/dbusmenu/gtk/parser/children",      test_parser_children);
	return;
}

gint
main (gint argc, gchar * argv[])
{
	gtk_init(&argc, &argv);
	g_test_init(&argc, &argv, NULL);

	/* Test suites */
	test_gtk_parser_suite();


	return g_test_run ();
}
