/*
Testing for the various objects just by themselves.

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

#include <libdbusmenu-glib/menuitem.h>
#include <libdbusmenu-gtk/menuitem.h>
#include <gdk/gdkkeysyms.h>

#define TEST_IMAGE  SRCDIR "/" "test-gtk-objects.jpg"

/* Building the basic menu item, make sure we didn't break
   any core GObject stuff */
static void
test_object_menuitem (void)
{
	/* Build a menu item */
	DbusmenuMenuitem * item = dbusmenu_menuitem_new();

	/* Test to make sure it's a happy object */
	g_assert(item != NULL);
	g_assert(G_IS_OBJECT(item));
	g_assert(DBUSMENU_IS_MENUITEM(item));

	/* Set up a check to make sure it gets destroyed on unref */
	g_object_add_weak_pointer(G_OBJECT(item), (gpointer *)&item);
	g_object_unref(item);

	/* Did it go away? */
	g_assert(item == NULL);

	return;
}

/* Setting and getting a pixbuf */
static void
test_object_prop_pixbuf (void)
{
	const gchar * prop_name = "image-test";

	/* Build a menu item */
	DbusmenuMenuitem * item = dbusmenu_menuitem_new();

	/* Test to make sure it's a happy object */
	g_assert(item != NULL);
	g_assert(G_IS_OBJECT(item));
	g_assert(DBUSMENU_IS_MENUITEM(item));

	/* Load our image */
	GdkPixbuf * pixbuf = gdk_pixbuf_new_from_file(TEST_IMAGE, NULL);
	g_assert(pixbuf != NULL);

	/* Set the property */
	gboolean success = dbusmenu_menuitem_property_set_image(item, prop_name, pixbuf);
	g_assert(success);
	g_object_unref(pixbuf);

	/* Check to see if it's set */
	GVariant * val = dbusmenu_menuitem_property_get_variant(item, prop_name);
	g_assert(val != NULL);

	/* Get the pixbuf back! */
	GdkPixbuf * newpixbuf = dbusmenu_menuitem_property_get_image(item, prop_name);
	g_assert(newpixbuf != NULL);
	g_object_unref(newpixbuf);

	g_object_unref(item);

	return;
}

/* Setting and getting a shortcut */
static void
test_object_prop_shortcut (void)
{
	/* Build a menu item */
	DbusmenuMenuitem * item = dbusmenu_menuitem_new();

	/* Test to make sure it's a happy object */
	g_assert(item != NULL);
	g_assert(G_IS_OBJECT(item));
	g_assert(DBUSMENU_IS_MENUITEM(item));

#if GTK_CHECK_VERSION(3,0,0)
	guint key = GDK_KEY_c;
#else
	guint key = GDK_c;
#endif
	GdkModifierType modifier = GDK_CONTROL_MASK;

	/* Set a shortcut */
	gboolean success = dbusmenu_menuitem_property_set_shortcut(item, key, modifier);
	g_assert(success);

	/* Check for value */
	GVariant * val = dbusmenu_menuitem_property_get_variant(item, DBUSMENU_MENUITEM_PROP_SHORTCUT);
	g_assert(val != NULL);

	/* Check to see if we love it */
	guint newkey = 0;
	GdkModifierType newmodifier = 0;
	dbusmenu_menuitem_property_get_shortcut(item, &newkey, &newmodifier);

	g_assert(key == newkey);
	g_assert(newmodifier == modifier);

	g_object_unref(item);

	return;
}

/* Build the test suite */
static void
test_gtk_objects_suite (void)
{
	g_test_add_func ("/dbusmenu/gtk/objects/menuitem/base",          test_object_menuitem);
	g_test_add_func ("/dbusmenu/gtk/objects/menuitem/prop_pixbuf",   test_object_prop_pixbuf);
	g_test_add_func ("/dbusmenu/gtk/objects/menuitem/prop_shortcut", test_object_prop_shortcut);
	return;
}

gint
main (gint argc, gchar * argv[])
{
	gtk_init(&argc, &argv);

	g_test_init(&argc, &argv, NULL);

	/* Test suites */
	test_gtk_objects_suite();

	return g_test_run ();
}
