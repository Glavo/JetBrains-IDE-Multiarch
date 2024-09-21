/*
A small tool to grab the dbusmenu structure that a program is
exporting.

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

#include <gdk/gdk.h>
#include <gdk/gdkx.h>
#include <gtk/gtk.h>

#include <libdbusmenu-glib/client.h>
#include <libdbusmenu-glib/menuitem.h>
#include <libdbusmenu-glib/menuitem-private.h>

#include <X11/Xlib.h>

static GMainLoop * mainloop = NULL;

static gint
list_str_cmp (gconstpointer a, gconstpointer b)
{
	return g_strcmp0((gchar *)a, (gchar *)b);
}

static void
print_menuitem (DbusmenuMenuitem * item, int depth)
{
	gchar * space = g_strnfill(depth, ' ');
	g_print("%s\"id\": %d", space, dbusmenu_menuitem_get_id(item));

	GList * properties_raw = dbusmenu_menuitem_properties_list(item);
	GList * properties = g_list_sort(properties_raw, list_str_cmp);
	GList * property;
	for (property = properties; property != NULL; property = g_list_next(property)) {
		GVariant * variant = dbusmenu_menuitem_property_get_variant(item, (gchar *)property->data);
		gchar * str = g_variant_print(variant, FALSE);
		g_print(",\n%s\"%s\": %s", space, (gchar *)property->data, str);
		g_free(str);
		g_variant_unref(variant);
	}
	g_list_free(properties);

	GList * children = dbusmenu_menuitem_get_children(item);
	if (children != NULL) {
		gchar * childspace = g_strnfill(depth + 4, ' ');
		g_print(",\n%s\"submenu\": [\n%s{\n", space, childspace);
		GList * child;
		for (child = children; child != NULL; child = g_list_next(child)) {
			print_menuitem(DBUSMENU_MENUITEM(child->data), depth + 4 + 2);
			if (child->next != NULL) {
				g_print("\n%s},\n%s{\n", childspace, childspace);
			}
		}
		g_print("\n%s}\n%s]", childspace, space);
		g_free(childspace);
	}

	g_free(space);

	return;
}

/* Prints the final JSON file recursively */
static gboolean
root_timeout (gpointer data)
{
	DbusmenuMenuitem * newroot = DBUSMENU_MENUITEM(data);

	g_print("{\n");
	print_menuitem(newroot, 2);
	g_print("\n}\n");

	g_main_quit(mainloop);
	return FALSE;
}

/* Variables to deal with the number of items that we're watching to
   realized */
static int realized_count = 0;
static DbusmenuMenuitem * root = NULL;

/* Decrements the realization count, and if it gets to the end prints
   out everything. */
static void
decrement_count (void)
{
	realized_count--;

	if (realized_count == 0) {
		root_timeout(root);
	}

	return;
}

/* Checks whether we need to watch a menu item, and recurses down to
   its children as well */
static void
check_realizations (DbusmenuMenuitem * item)
{
	g_return_if_fail(DBUSMENU_IS_MENUITEM(item));

	if (!dbusmenu_menuitem_realized(item)) {
		realized_count++;

		g_signal_connect(G_OBJECT(item), DBUSMENU_MENUITEM_SIGNAL_REALIZED, G_CALLBACK(decrement_count), NULL);
	}

	GList * children = dbusmenu_menuitem_get_children(item);
	if (children != NULL) {
		GList * child;
		for (child = children; child != NULL; child = g_list_next(child)) {
			check_realizations(DBUSMENU_MENUITEM(child->data));
		}
	}

	return;
}

/* A setup for when we get our first root.  We set up the basic realization
   counter and set it to run.  We'll print when it counts down */
static void
new_root_cb (DbusmenuClient * client, DbusmenuMenuitem * newroot)
{
	if (newroot == NULL) {
		g_printerr("ERROR: Unable to create Dbusmenu Root\n");
		g_main_loop_quit(mainloop);
		return;
	}

	root = newroot;
	check_realizations(root);

	realized_count++;
	decrement_count();

	return;
}

/* Window clicking ***************************************************/
static GdkFilterReturn
click_filter (GdkXEvent *gdk_xevent,
              GdkEvent  *event,
              gpointer   data);

static Window
find_real_window (Window w, int depth)
{
	if (depth > 5) {
		return None;
	}
	/*static*/ Atom wm_state = XInternAtom(gdk_display, "WM_STATE", False);
	Atom type;
	int format;
	unsigned long nitems, after;
	unsigned char* prop;
	if (XGetWindowProperty(gdk_display, w, wm_state, 0, 0, False, AnyPropertyType,
				&type, &format, &nitems, &after, &prop) == Success) {
		if (prop != NULL) {
			XFree(prop);
		}
		if (type != None) {
			return w;
		}
	}
	Window root, parent;
	Window* children;
	unsigned int nchildren;
	Window ret = None;
	if (XQueryTree(gdk_display, w, &root, &parent, &children, &nchildren) != 0) {
		unsigned int i;
		for(i = 0; i < nchildren && ret == None; ++i) {
			ret = find_real_window(children[ i ], depth + 1);
		}
		if (children != NULL) {
			XFree(children);
		}
	}
	return ret;
}

static Window
get_window_under_cursor (void)
{
	Window root;
	Window child;
	uint mask;
	int rootX, rootY, winX, winY;
	XQueryPointer(gdk_display, gdk_x11_get_default_root_xwindow(), &root, &child, &rootX, &rootY, &winX, &winY, &mask);
	if (child == None) {
		return None;
	}
	return find_real_window(child, 0);
}

static void
uninstall_click_filter (void)
{
	GdkWindow *root;

	root = gdk_get_default_root_window ();
	gdk_window_remove_filter (root, (GdkFilterFunc) click_filter, NULL);

	gdk_pointer_ungrab (GDK_CURRENT_TIME);
	gdk_keyboard_ungrab (GDK_CURRENT_TIME);

	gtk_main_quit ();
}

static GdkFilterReturn
click_filter (GdkXEvent *gdk_xevent,
              GdkEvent  *event,
              gpointer   data)

{
	XEvent *xevent = (XEvent *) gdk_xevent;
	gboolean *success = (gboolean *)data;

	switch (xevent->type) {
	case ButtonPress:
		uninstall_click_filter();
		*success = TRUE;
		return GDK_FILTER_REMOVE;
	case KeyPress:
		if (xevent->xkey.keycode == XKeysymToKeycode(gdk_display, XK_Escape)) {
			uninstall_click_filter();
			*success = FALSE;
			return GDK_FILTER_REMOVE;
		}
		break;
	default:
		break;
	}

	return GDK_FILTER_CONTINUE;
}

static gboolean
install_click_filter (gpointer data)
{
	GdkGrabStatus  status;
	GdkCursor     *cross;
	GdkWindow     *root;

	root = gdk_get_default_root_window();

	gdk_window_add_filter(root, (GdkFilterFunc) click_filter, data);

	cross = gdk_cursor_new(GDK_CROSS);
	status = gdk_pointer_grab(root, FALSE, GDK_BUTTON_PRESS_MASK,
	                          NULL, cross, GDK_CURRENT_TIME);
	gdk_cursor_unref(cross);

	if (status != GDK_GRAB_SUCCESS) {
		g_warning("Pointer grab failed.\n");
		uninstall_click_filter();
		return FALSE;
	}

	status = gdk_keyboard_grab(root, FALSE, GDK_CURRENT_TIME);
	if (status != GDK_GRAB_SUCCESS) {
		g_warning("Keyboard grab failed.\n");
		uninstall_click_filter();
		return FALSE;
	}

	gdk_flush();
	return FALSE;
}

static gboolean
wait_for_click (void)
{
	gboolean success;
	g_idle_add (install_click_filter, (gpointer)(&success));
	gtk_main ();
	return success;
}

static gchar * dbusname = NULL;
static gchar * dbusobject = NULL;

static gboolean
init_dbus_vars_from_window(Window window)
{
	GError *error;
	GDBusProxy *proxy;

	error = NULL;

	proxy = g_dbus_proxy_new_for_bus_sync (G_BUS_TYPE_SESSION,
	                                       G_DBUS_PROXY_FLAGS_NONE,
	                                       NULL,
	                                       "com.canonical.AppMenu.Registrar",
	                                       "/com/canonical/AppMenu/Registrar",
	                                       "com.canonical.AppMenu.Registrar",
	                                       NULL,
	                                       &error);
	if (error != NULL) {
		g_warning("Unable to get registrar proxy: %s", error->message);
		g_error_free(error);
		return FALSE;
	}

	error = NULL;
	GVariant * retval;
	GVariant * args[1];
	args[0] = g_variant_new("u", window);

	retval = g_dbus_proxy_call_sync(proxy,
	                                "GetMenuForWindow",
	                                g_variant_new_tuple(args, 1),
	                                G_DBUS_CALL_FLAGS_NONE,
	                                -1,
	                                NULL,
	                                &error);

	if (error != NULL) {
		g_warning("Unable to call 'GetMenuForWindow' on registrar: %s", error->message);
		g_error_free(error);
		return FALSE;
	}

	g_variant_get(retval, "(so)", &dbusname, &dbusobject);

	g_variant_unref(retval);
	g_object_unref(proxy);

	return TRUE;
}

/* Option parser *****************************************************/
static gboolean
option_dbusname (const gchar * arg, const gchar * value, gpointer data, GError ** error)
{
	if (dbusname != NULL) {
		g_set_error(error, G_OPTION_ERROR, G_OPTION_ERROR_BAD_VALUE, "DBus name already set to '%s' can't reset it to '%s'.", dbusname, value);
		return FALSE;
	}

	dbusname = g_strdup(value);
	return TRUE;
}

static gboolean
option_dbusobject (const gchar * arg, const gchar * value, gpointer data, GError ** error)
{
	if (dbusobject != NULL) {
		g_set_error(error, G_OPTION_ERROR, G_OPTION_ERROR_BAD_VALUE, "DBus name already set to '%s' can't reset it to '%s'.", dbusobject, value);
		return FALSE;
	}

	dbusobject = g_strdup(value);
	return TRUE;
}

void
usage (void)
{
	g_printerr("dbusmenu-dumper --dbus-name=<name> --dbus-object=<object>\n");
	return;
}

static GOptionEntry general_options[] = {
	{"dbus-name",     'd',  0,                        G_OPTION_ARG_CALLBACK,  option_dbusname, "The name of the program to connect to (i.e. org.test.bob", "dbusname"},
	{"dbus-object",   'o',  0,                        G_OPTION_ARG_CALLBACK,  option_dbusobject, "The path to the Dbus object (i.e /org/test/bob/alvin)", "dbusobject"},
	{NULL}
};

int
main (int argc, char ** argv)
{
	GError * error = NULL;
	GOptionContext * context;

	context = g_option_context_new("- Grab the entires in a DBus Menu");

	g_option_context_add_main_entries(context, general_options, "dbusmenu-dumper");

	if (!g_option_context_parse(context, &argc, &argv, &error)) {
		g_printerr("option parsing failed: %s\n", error->message);
		g_error_free(error);
		return 1;
	}

	if (dbusname == NULL && dbusobject == NULL) {
		gtk_init(&argc, &argv);
		if (!wait_for_click()) {
			return 1;
		}
		Window window = get_window_under_cursor();
		if (window == None) {
			g_printerr("ERROR: could not get the id for the pointed window\n");
			return 1;
		}
		if (!init_dbus_vars_from_window(window)) {
			g_printerr("ERROR: could not find a menu for the pointed window\n");
			return 1;
		}
	} else {
		if (dbusname == NULL) {
			g_printerr("ERROR: dbus-name not specified\n");
			usage();
			return 1;
		}

		if (dbusobject == NULL) {
			g_printerr("ERROR: dbus-object not specified\n");
			usage();
			return 1;
		}
	}

	DbusmenuClient * client = dbusmenu_client_new (dbusname, dbusobject);
	if (client == NULL) {
		g_printerr("ERROR: Unable to create Dbusmenu Client\n");
		return 1;
	}

	g_signal_connect(G_OBJECT(client), DBUSMENU_CLIENT_SIGNAL_ROOT_CHANGED, G_CALLBACK(new_root_cb), NULL);

	mainloop = g_main_loop_new(NULL, FALSE);
	g_main_loop_run(mainloop);

	return 0;
}

