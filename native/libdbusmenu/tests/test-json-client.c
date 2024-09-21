/*
Test to check the json-loader and dbusmenu-dumper

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

#include <glib.h>
#include <gio/gio.h>

GMainLoop * mainloop = NULL;

gboolean
timeout_func (gpointer user_data)
{
	char ** argv = (char **)user_data;

	g_usleep(500000);

	g_debug("Initing");

	gchar * command = g_strdup_printf("%s --dbus-name=org.dbusmenu.test --dbus-object=/org/test", argv[1]);
	g_debug("Executing: %s", command);

	gchar * output;
	g_spawn_command_line_sync(command, &output, NULL, NULL, NULL);

	GFile * ofile = g_file_new_for_commandline_arg(argv[2]);
	if (ofile != NULL) {
		g_file_replace_contents(ofile, output, g_utf8_strlen(output, -1), NULL, FALSE, 0, NULL, NULL, NULL);
	}

	g_spawn_command_line_sync("gdbus call --session --dest org.dbusmenu.test --object-path /org/test --method com.canonical.dbusmenu.Event 0 clicked \"<0>\" 0", NULL, NULL, NULL, NULL);

	g_main_loop_quit(mainloop);
	return TRUE;
}

int
main (int argc, char ** argv)
{
	g_timeout_add_seconds(1, timeout_func, argv);

	mainloop = g_main_loop_new(NULL, FALSE);
	g_main_loop_run(mainloop);

	return 0;
}
