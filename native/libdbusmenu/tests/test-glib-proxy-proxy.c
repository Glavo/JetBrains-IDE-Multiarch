#include <glib.h>
#include <gio/gio.h>

#include <libdbusmenu-glib/menuitem.h>
#include <libdbusmenu-glib/menuitem-proxy.h>
#include <libdbusmenu-glib/server.h>
#include <libdbusmenu-glib/client.h>

#include "test-glib-proxy.h"

static DbusmenuServer * server = NULL;
static DbusmenuClient * client = NULL;
static GMainLoop * mainloop = NULL;

void
root_changed (DbusmenuClient * client, DbusmenuMenuitem * newroot, gpointer user_data)
{
	g_debug("New root: %p", newroot);

	if (newroot == NULL) {
		g_debug("Root removed, exiting");
		g_main_loop_quit(mainloop);
		return;
	}

	DbusmenuMenuitemProxy * pmi = dbusmenu_menuitem_proxy_new(newroot);
	dbusmenu_server_set_root(server, DBUSMENU_MENUITEM(pmi));
	return;
}

static void
on_bus (GDBusConnection * connection, const gchar * name, gpointer user_data)
{
	client = dbusmenu_client_new((gchar *)user_data, "/org/test");

	g_signal_connect(client, DBUSMENU_CLIENT_SIGNAL_ROOT_CHANGED, G_CALLBACK(root_changed), server);

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
	if (argc != 3) {
		g_error ("Need two params");
		return 1;
	}
	
	gchar * whoami = argv[1];
	gchar * myproxy = argv[2];

	g_debug("I am '%s' and I'm proxying '%s'", whoami, myproxy);

	server = dbusmenu_server_new("/org/test");

	g_bus_own_name(G_BUS_TYPE_SESSION,
	               whoami,
	               G_BUS_NAME_OWNER_FLAGS_NONE,
	               on_bus,
	               NULL,
	               name_lost,
	               myproxy,
	               NULL);

	mainloop = g_main_loop_new(NULL, FALSE);
	g_main_loop_run(mainloop);

	g_object_unref(G_OBJECT(server));
	g_debug("Quiting");

	return 0;
}
