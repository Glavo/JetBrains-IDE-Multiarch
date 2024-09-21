#include <libdbusmenu-glib/server.h>
#include <libdbusmenu-glib/menuitem.h>

static DbusmenuMenuitem * root_menuitem = NULL;
static GMainLoop * mainloop = NULL;

gchar * dummies[] = {
	"Bob", "Jim", "Alvin", "Mary", NULL
};

static void
dummy_users (DbusmenuMenuitem * root) {
	int count;
	for (count = 0; dummies[count] != NULL; count++) {
		DbusmenuMenuitem * mi = dbusmenu_menuitem_new();
		g_debug("Creating item: %d %s", dbusmenu_menuitem_get_id(mi), dummies[count]);
		g_debug("\tRoot ID: %d", dbusmenu_menuitem_get_id(root));
		dbusmenu_menuitem_property_set(mi, "label", dummies[count]);
		dbusmenu_menuitem_child_add_position(root, mi, count);
	}

	return;
}

static gboolean
quititall (gpointer data)
{
	g_main_loop_quit(mainloop);
	return FALSE;
}

int
main (int argc, char ** argv)
{
    DbusmenuServer * server = dbusmenu_server_new("/test/object");
    root_menuitem = dbusmenu_menuitem_new();
    dbusmenu_server_set_root(server, root_menuitem);
	g_debug("Root ID: %d", dbusmenu_menuitem_get_id(root_menuitem));

	dummy_users(root_menuitem);

	g_timeout_add_seconds(1, quititall, NULL);

    mainloop = g_main_loop_new(NULL, FALSE);
    g_main_loop_run(mainloop);

    return 0;
}

