/*
Testing for the various objects just by themselves.

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
#include <glib-object.h>

#include <libdbusmenu-glib/menuitem.h>

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

/* Check to make sure a new ID is bigger than 0 */
static void
test_object_menuitem_id (void)
{
	/* Build a menu item */
	DbusmenuMenuitem * item = dbusmenu_menuitem_new();

	/* Test to make sure it's a happy object */
	g_assert(item != NULL);
	g_assert(G_IS_OBJECT(item));
	g_assert(DBUSMENU_IS_MENUITEM(item));

	g_assert(dbusmenu_menuitem_get_id(item) > 0);

	/* Set up a check to make sure it gets destroyed on unref */
	g_object_add_weak_pointer(G_OBJECT(item), (gpointer *)&item);
	g_object_unref(item);

	/* Did it go away? */
	g_assert(item == NULL);

	return;
}

/* Set a string prop, make sure it's stored as one */
static void
test_object_menuitem_props_string (void)
{
	/* Build a menu item */
	DbusmenuMenuitem * item = dbusmenu_menuitem_new();
	GVariant * out = NULL;

	/* Test to make sure it's a happy object */
	g_assert(item != NULL);

	/* Setting a string */
	dbusmenu_menuitem_property_set(item, "string", "value");
	out = dbusmenu_menuitem_property_get_variant(item, "string");
	g_assert(out != NULL);
	g_assert(g_variant_type_equal(g_variant_get_type(out), G_VARIANT_TYPE_STRING));
	g_assert(!g_strcmp0(g_variant_get_string(out, NULL), "value"));
	g_assert(!g_strcmp0(dbusmenu_menuitem_property_get(item, "string"), "value"));

	g_object_unref(item);

	return;
}

/* Set an integer prop, make sure it's stored as one */
static void
test_object_menuitem_props_int (void)
{
	/* Build a menu item */
	DbusmenuMenuitem * item = dbusmenu_menuitem_new();
	GVariant * out = NULL;

	/* Test to make sure it's a happy object */
	g_assert(item != NULL);

	/* Setting a string */
	dbusmenu_menuitem_property_set_int(item, "int", 12345);
	out = dbusmenu_menuitem_property_get_variant(item, "int");
	g_assert(out != NULL);
	g_assert(g_variant_type_equal(g_variant_get_type(out), G_VARIANT_TYPE_INT32));
	g_assert(g_variant_get_int32(out) == 12345);
	g_assert(dbusmenu_menuitem_property_get_int(item, "int") == 12345);

	g_object_unref(item);

	return;
}

/* Set a boolean prop, make sure it's stored as one */
static void
test_object_menuitem_props_bool (void)
{
	/* Build a menu item */
	DbusmenuMenuitem * item = dbusmenu_menuitem_new();
	GVariant * out = NULL;

	/* Test to make sure it's a happy object */
	g_assert(item != NULL);

	/* Setting a string */
	dbusmenu_menuitem_property_set_bool(item, "boolean", TRUE);
	out = dbusmenu_menuitem_property_get_variant(item, "boolean");
	g_assert(out != NULL);
	g_assert(g_variant_type_equal(g_variant_get_type(out), G_VARIANT_TYPE_BOOLEAN));
	g_assert(g_variant_get_boolean(out));
	/* g_assert(dbusmenu_menuitem_property_get_int(item, "boolean") == 0); */

	g_object_unref(item);

	return;
}

/* Set the same property several times with
   different types. */
static void
test_object_menuitem_props_swap (void)
{
	/* Build a menu item */
	DbusmenuMenuitem * item = dbusmenu_menuitem_new();

	/* Test to make sure it's a happy object */
	g_assert(item != NULL);

	/* Setting a boolean */
	dbusmenu_menuitem_property_set_bool(item, "swapper", TRUE);
	g_assert(dbusmenu_menuitem_property_get_bool(item, "swapper"));

	/* Setting a int */
	dbusmenu_menuitem_property_set_int(item, "swapper", 5432);
	g_assert(dbusmenu_menuitem_property_get_int(item, "swapper") == 5432);

	/* Setting a string */
	dbusmenu_menuitem_property_set(item, "swapper", "mystring");
	g_assert(!g_strcmp0(dbusmenu_menuitem_property_get(item, "swapper"), "mystring"));

	/* Setting a boolean */
	dbusmenu_menuitem_property_set_bool(item, "swapper", FALSE);
	g_assert(!dbusmenu_menuitem_property_get_bool(item, "swapper"));

	g_object_unref(item);

	return;
}

/* A helper to put a value into a pointer for eval. */
static void
test_object_menuitem_props_signals_helper (DbusmenuMenuitem * mi, gchar * property, GVariant * value, GVariant ** out)
{
	if (!g_strcmp0(property, "swapper")) {
		*out = value;
	} else {
		g_warning("Signal handler got: %s", property);
	}
	return;
}

/* Set the same property several times with
   different types. */
static void
test_object_menuitem_props_signals (void)
{
	/* Build a menu item */
	DbusmenuMenuitem * item = dbusmenu_menuitem_new();
	GVariant * out = NULL;

	/* Test to make sure it's a happy object */
	g_assert(item != NULL);

	/* Setting up our callback */
	g_signal_connect(G_OBJECT(item), DBUSMENU_MENUITEM_SIGNAL_PROPERTY_CHANGED, G_CALLBACK(test_object_menuitem_props_signals_helper), &out);

	/* Setting a boolean */
	dbusmenu_menuitem_property_set_bool(item, "swapper", TRUE);
	g_assert(out != NULL);
	g_assert(g_variant_get_boolean(out));
	out = NULL;

	/* Setting a int */
	dbusmenu_menuitem_property_set_int(item, "swapper", 5432);
	g_assert(out != NULL);
	g_assert(g_variant_get_int32(out) == 5432);
	out = NULL;

	/* Setting a string */
	dbusmenu_menuitem_property_set(item, "swapper", "mystring");
	g_assert(out != NULL);
	g_assert(!g_strcmp0(g_variant_get_string(out, NULL), "mystring"));
	out = NULL;

	/* Setting a boolean */
	dbusmenu_menuitem_property_set_bool(item, "swapper", FALSE);
	g_assert(out != NULL);
	g_assert(!g_variant_get_boolean(out));
	out = NULL;

	g_object_unref(item);

	return;
}

/* Set a boolean prop, as a string too! */
static void
test_object_menuitem_props_boolstr (void)
{
	/* Build a menu item */
	DbusmenuMenuitem * item = dbusmenu_menuitem_new();

	/* Test to make sure it's a happy object */
	g_assert(item != NULL);

	/* Setting a bool */
	dbusmenu_menuitem_property_set_bool(item, "boolean", TRUE);
	g_assert(dbusmenu_menuitem_property_get_bool(item, "boolean"));

	/* Setting "true" */
	dbusmenu_menuitem_property_set(item, "boolean", "true");
	g_assert(dbusmenu_menuitem_property_get_bool(item, "boolean"));

	/* Setting "True" */
	dbusmenu_menuitem_property_set(item, "boolean", "True");
	g_assert(dbusmenu_menuitem_property_get_bool(item, "boolean"));

	/* Setting "TRUE" */
	dbusmenu_menuitem_property_set(item, "boolean", "TRUE");
	g_assert(dbusmenu_menuitem_property_get_bool(item, "boolean"));

	/* Setting "false" */
	dbusmenu_menuitem_property_set(item, "boolean", "false");
	g_assert(!dbusmenu_menuitem_property_get_bool(item, "boolean"));

	/* Setting "False" */
	dbusmenu_menuitem_property_set(item, "boolean", "False");
	g_assert(!dbusmenu_menuitem_property_get_bool(item, "boolean"));

	/* Setting "FALSE" */
	dbusmenu_menuitem_property_set(item, "boolean", "FALSE");
	g_assert(!dbusmenu_menuitem_property_get_bool(item, "boolean"));

	g_object_unref(item);

	return;
}

/* Set and then remove a prop */
static void
test_object_menuitem_props_removal (void)
{
	/* Build a menu item */
	DbusmenuMenuitem * item = dbusmenu_menuitem_new();

	/* Test to make sure it's a happy object */
	g_assert(item != NULL);

	/* Set the property and ensure that it's set */
	dbusmenu_menuitem_property_set_variant(item, "myprop", g_variant_new_int32(34));
	g_assert(dbusmenu_menuitem_property_get_variant(item, "myprop") != NULL);

	/* Remove the property and ensure it goes away */
	dbusmenu_menuitem_property_set_variant(item, "myprop", NULL);
	g_assert(dbusmenu_menuitem_property_get_variant(item, "myprop") == NULL);

	/* Set the property again */
	dbusmenu_menuitem_property_set_variant(item, "myprop", g_variant_new_int32(34));
	g_assert(dbusmenu_menuitem_property_get_variant(item, "myprop") != NULL);

	/* Remove the property with a NULL string */
	dbusmenu_menuitem_property_set(item, "myprop", NULL);
	g_assert(dbusmenu_menuitem_property_get_variant(item, "myprop") == NULL);

	g_object_unref(item);

	return;
}

/* Build the test suite */
static void
test_glib_objects_suite (void)
{
	g_test_add_func ("/dbusmenu/glib/objects/menuitem/base",          test_object_menuitem);
	g_test_add_func ("/dbusmenu/glib/objects/menuitem/id",            test_object_menuitem_id);
	g_test_add_func ("/dbusmenu/glib/objects/menuitem/props_string",  test_object_menuitem_props_string);
	g_test_add_func ("/dbusmenu/glib/objects/menuitem/props_int",     test_object_menuitem_props_int);
	g_test_add_func ("/dbusmenu/glib/objects/menuitem/props_bool",    test_object_menuitem_props_bool);
	g_test_add_func ("/dbusmenu/glib/objects/menuitem/props_swap",    test_object_menuitem_props_swap);
	g_test_add_func ("/dbusmenu/glib/objects/menuitem/props_signals", test_object_menuitem_props_signals);
	g_test_add_func ("/dbusmenu/glib/objects/menuitem/props_boolstr", test_object_menuitem_props_boolstr);
	g_test_add_func ("/dbusmenu/glib/objects/menuitem/props_removal", test_object_menuitem_props_removal);
	return;
}

gint
main (gint argc, gchar * argv[])
{
	g_test_init(&argc, &argv, NULL);

	/* Test suites */
	test_glib_objects_suite();


	return g_test_run ();
}
